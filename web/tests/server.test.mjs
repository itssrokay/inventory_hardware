import test from 'node:test';
import assert from 'node:assert/strict';
import { once } from 'node:events';
import { createApp } from '../server.mjs';

async function withServer(options, run) {
  const server = createApp(options); server.listen(0, '127.0.0.1'); await once(server, 'listening');
  const base = `http://127.0.0.1:${server.address().port}`;
  try { await run(base); } finally { server.closeAllConnections(); await new Promise(resolve => server.close(resolve)); }
}
const post = text => ({ method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ text }) });
test('only preview files are served; env files and source are never exposed', async () => {
  await withServer({ keyProvider: async () => 'sk-synthetic-key-only' }, async base => {
    assert.equal((await fetch(base)).status, 200);
    for (const path of ['/.env.local', '/../.env.local', '/server.mjs', '/README.md', '/app/src/main/assets/listing-instructions.txt']) assert.equal((await fetch(base + path)).status, 404);
    const config = await (await fetch(base + '/api/config')).json();
    assert.equal(config.aiConfigured, true); assert.ok(!JSON.stringify(config).includes('sk-synthetic'));
  });
});
test('AI proxy forwards strict schema and keeps credentials out of the response', async () => {
  let payload;
  await withServer({ keyProvider: async () => 'sk-synthetic-key-only', openAiFetch: async (url, request) => {
    assert.equal(url, 'https://api.openai.com/v1/responses');
    assert.equal(request.headers.Authorization, 'Bearer sk-synthetic-key-only');
    payload = JSON.parse(request.body);
    return Response.json({ status: 'completed', output: [{ content: [{ type: 'output_text', text: '{"items":[],"warnings":["Missing price"]}' }] }] });
  } }, async base => {
    const response = await fetch(base + '/api/parse', post('a padlock'));
    assert.deepEqual(await response.json(), { items: [], warnings: ['Missing price'] });
    assert.equal(payload.store, false); assert.equal(payload.text.format.strict, true); assert.equal(payload.text.format.schema.additionalProperties, false);
  });
});
test('cross-origin requests and malformed input never reach OpenAI', async () => {
  await withServer({ keyProvider: async () => 'sk-synthetic-key-only', openAiFetch: async () => { assert.fail('Should not call OpenAI'); } }, async base => {
    assert.equal((await fetch(base + '/api/parse', { ...post('bolt'), headers: { 'Content-Type': 'application/json', Origin: 'https://unrelated.example' } })).status, 403);
    assert.equal((await fetch(base + '/api/parse', post(''))).status, 400);
    assert.equal((await fetch(base + '/api/parse', { method: 'POST', body: 'bolt' })).status, 415);
    assert.equal((await fetch(base + '/api/parse', { ...post('bolt'), body: '{bad json' })).status, 400);
  });
});
test('missing key and upstream failure show safe errors', async () => {
  await withServer({ keyProvider: async () => '' }, async base => { assert.equal((await fetch(base + '/api/parse', post('bolt'))).status, 503); });
  await withServer({ keyProvider: async () => 'sk-synthetic-key-only', openAiFetch: async () => Response.json({ error: 'private upstream detail' }, { status: 401 }) }, async base => {
    const response = await fetch(base + '/api/parse', post('bolt')); assert.equal(response.status, 502);
    assert.ok(!(await response.text()).includes('private upstream detail'));
  });
});

test('project cockpit reads documentation through an explicit allowlist', async () => {
  await withServer({ keyProvider: async () => { assert.fail('Project notebook must not read the API key'); } }, async base => {
    assert.equal((await fetch(base + '/project')).status, 200);
    assert.equal((await fetch(base + '/project/app.js')).status, 200);
    const data = await (await fetch(base + '/project/data.json')).json();
    assert.ok(Array.isArray(data.events));
    assert.ok(data.events.some(e => e.kind === 'decision'));
    assert.equal(data.project.architecture.lanes.length, 2);
    assert.equal((await fetch(base + '/project/doc/ARCHITECTURE.md')).status, 200);
    assert.equal((await fetch(base + '/project/doc/ADR-001.md')).status, 200);
    for (const path of ['/project/doc/.env.local', '/project/doc/server.mjs', '/project/doc/../../.env.local']) assert.equal((await fetch(base + path)).status, 404);
    assert.equal((await fetch(base + '/project/data.json', post('write attempt'))).status, 404);
  });
});

test('image-only listing forwards a vision input and existing categories with no auto-save', async () => {
  const image='data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aX1cAAAAASUVORK5CYII=';
  let payload;
  await withServer({keyProvider:async()=>'sk-synthetic-key-only',openAiFetch:async(url,request)=>{payload=JSON.parse(request.body);return Response.json({status:'completed',output:[{content:[{type:'output_text',text:JSON.stringify({items:[],warnings:['Enter missing price']})}]}]});}},async base=>{
    const response=await fetch(base+'/api/parse',{...post(''),body:JSON.stringify({text:'',image,categories:['Plumbing']})});
    assert.equal(response.status,200);assert.deepEqual((await response.json()).warnings,['Enter missing price']);
    assert.equal(payload.input[0].content[1].type,'input_image');assert.equal(payload.input[0].content[1].image_url,image);assert.equal(payload.store,false);
    assert.ok(payload.instructions.includes('Plumbing'));
  });
});
test('category suggestions use their own schema and cannot expose upstream details', async()=>{
  await withServer({keyProvider:async()=>'sk-synthetic-key-only',openAiFetch:async(url,request)=>{
    const payload=JSON.parse(request.body);assert.equal(payload.text.format.name,'catalog_category');assert.deepEqual(payload.text.format.schema.required,['category','aliases','reason']);
    return Response.json({status:'completed',output:[{content:[{type:'output_text',text:JSON.stringify({category:'Plumbing',aliases:['socket'],reason:'A PVC pipe fitting.'})}]}]});
  }},async base=>{const response=await fetch(base+'/api/categorize',post('Prince PVC socket'));assert.equal((await response.json()).category,'Plumbing');});
});
test('image validation rejects remote URLs and oversized image inputs before calling OpenAI',async()=>{
  await withServer({keyProvider:async()=>'sk-synthetic-key-only',openAiFetch:async()=>assert.fail('Must not call OpenAI')},async base=>{
    for(const image of ['https://untrusted.example/photo.png','data:image/svg+xml;base64,PHN2Zz4=', 'data:image/jpeg;base64,'+'A'.repeat(2700001)]){
      assert.equal((await fetch(base+'/api/parse',{...post('socket'),body:JSON.stringify({text:'socket',image})})).status,400);
    }
  });
});

test('reference routes expose only the fixed library assets and never call AI or write a catalog',async()=>{
 await withServer({keyProvider:async()=>{assert.fail('Reference reads must not read credentials');},openAiFetch:async()=>{assert.fail('Reference reads must not call AI');}},async base=>{
  const response=await fetch(base+'/reference/catalog.json');assert.equal(response.status,200);
  const library=await response.json();assert.equal(library.source.stockStatus,'unconfirmed');assert.equal(library.source.shopPricePaise,null);
  assert.ok(library.products.length>600);
  for(const path of ['/reference/.env.local','/reference/../../.env.local','/reference/source.txt','/reference/pages/0.webp','/reference/pages/93.webp'])assert.equal((await fetch(base+path)).status,404);
  const viewer=await fetch(base+'/reference/viewer?page=8');assert.equal(viewer.status,200);assert.match(await viewer.text(),/Original source/);
  assert.equal((await fetch(base+'/reference/catalog.json',{method:'POST'})).status,404);
 });
});
