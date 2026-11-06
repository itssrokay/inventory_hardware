import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
import {configureTranslations,translate,productText,searchQuery,setLanguageMode,productSearchText} from '../public/i18n.js';
import {referenceMatches,withReferencePictures,referenceImageUrl,referenceDrafts} from '../public/reference.js';
const dictionary=JSON.parse(await readFile(new URL('../../app/src/main/assets/ui/translations.json',import.meta.url),'utf8'));
const catalog=JSON.parse(await readFile(new URL('../../app/src/main/assets/reference/prince-2026.json',import.meta.url),'utf8'));
configureTranslations(dictionary);
test('Hindi and Hinglish use separate text while captured prices, units and codes stay intact',()=>{
 assert.match(translate('Reference library','hi'),/[\u0900-\u097f]/);
 assert.equal(translate('Reference library','hinglish'),'Reference list');
 assert.equal(translate('702 product groups · 4105 variants · 0 linked shop variants','hi'),'702 प्रकार · 4105 साइज़ · दुकान में 0 जुड़े साइज़');
 assert.equal(translate('Save 2 items on this phone','en'),'Save 2 items on this phone');
 assert.match(translate('Save 2 items on this phone','hi'),/2/);
 assert.equal(translate('Selling price: ₹123.45 per piece','hi'),'बिक्री का दाम: ₹123.45 प्रति piece');
 const original='Size (mm): 15 · SIM107608 · SDR 11 · SCH 80';assert.equal(translate(original,'hi'),original);
 assert.equal(productText('COUPLER (SDR 11 / SCH 80)','hi'),'कपलर (SDR 11 / SCH 80)');
 assert.equal(productText('COUPLER (SDR 11 / SCH 80)','hinglish'),'Coupler (SDR 11 / SCH 80)');
});
test('all translation templates preserve their placeholders in both languages',()=>{
 const placeholders=s=>[...s.matchAll(/\{\d+}/g)].map(m=>m[0]).sort();
 for(const [key,value] of Object.entries(dictionary.labels)) {
  assert.equal(typeof value.hi,'string');assert.equal(typeof value.hinglish,'string');assert.ok(value.hi&&value.hinglish);
  for(const language of ['hi','hinglish'])assert.deepEqual(placeholders(key),placeholders(value[language]),key);
 }
});
test('Hindi/Hinglish product queries stay offline and do not mutate canonical references',()=>{
 const before=JSON.stringify(catalog);setLanguageMode('hi');
 assert.equal(searchQuery('कपलर SIM107608 15 mm'),'coupler SIM107608 15 mm');
 assert.equal(searchQuery('प्रिंस कपलर'),'Prince coupler');
 assert.ok(referenceMatches(catalog,'प्रिंस कपलर').some(p=>p.page===8));
 assert.match(productSearchText('CPVC COUPLER'),/कपलर/);
 assert.ok(referenceMatches(catalog,'कपलर').some(p=>p.page===8));
 assert.ok(referenceMatches(catalog,'kaplar').some(p=>p.page===8));
 const selected=catalog.products.find(p=>p.page===8).variants[0].id;
 const draft=referenceDrafts(catalog,new Set([selected]),[])[0];assert.match(draft.name,/COUPLER/);assert.equal(draft.variants[0].price,'');
 assert.equal(JSON.stringify(catalog),before);setLanguageMode('en');
});
test('reference pictures are source-bound, locally allowlisted and preserved in activation drafts',async()=>{
 const manifest=JSON.parse(await readFile(new URL('../../app/src/main/assets/reference/images.json',import.meta.url),'utf8'));
 const pictured=withReferencePictures(catalog,manifest);
 assert.equal(manifest.picturedGroups,520);assert.equal(manifest.picturedVariants,2404);assert.equal(Object.keys(manifest.images).length,manifest.picturedGroups);
 const group=pictured.products.find(p=>p.page===8&&p.name.startsWith('COUPLER'));assert.ok(referenceImageUrl(group.imageAsset));
 const bytes=await readFile(new URL('../../app/src/main/assets/'+group.imageAsset,import.meta.url));assert.equal(bytes.toString('ascii',8,12),'WEBP');
 const draft=referenceDrafts(pictured,new Set([group.variants[0].id]),[])[0];assert.equal(draft.referenceImageAsset,group.imageAsset);
 const custom={...draft,id:'shop',aliases:[],photo:'data:image/png;base64,dGVzdA==',variants:[{sourceReferenceId:group.variants[0].id}]};
 assert.equal(referenceDrafts(pictured,new Set([group.variants[1].id]),[custom])[0].photo,custom.photo);
 assert.equal(withReferencePictures(catalog,{...manifest,sourceSha256:'wrong'}),catalog);
 for(const bad of ['https://example.com/a.webp','reference/products/../../.env.local','/reference/products/x.webp'])assert.equal(referenceImageUrl(bad),null);
 assert.equal(pictured.products.find(p=>p.page===86).imageAsset,null);
});
