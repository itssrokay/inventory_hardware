import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
import {referenceDrafts,referenceMatches,activeReferences,referenceSpec} from '../public/reference.js';
import {validateDraft,validateBackup} from '../public/domain.js';
const catalog=JSON.parse(await readFile(new URL('../../app/src/main/assets/reference/prince-2026.json',import.meta.url),'utf8'));
const all=catalog.products.flatMap(p=>p.variants.map(v=>({...v,page:p.page,product:p})));
const byCode=(code,page)=>all.filter(v=>v.code?.toUpperCase()===code&&(!page||v.page===page));
test('all references are unconfirmed with no shop prices or quantities; IDs are unique',()=>{
 assert.equal(catalog.source.effectiveDate,'2026-01-01');assert.equal(catalog.source.filenameDate,'2026-01-30');
 assert.equal(new Set(all.map(v=>v.id)).size,all.length);
 assert.equal(catalog.audit.unresolvedCodeOccurrences,0);
 for(const v of all){assert.equal(v.stockStatus,'unconfirmed');assert.equal(v.shopPricePaise,null);assert.ok(!('quantity' in v));}
});
test('source sizes, lengths, classes, colours, price bases and original anomalies survive',()=>{
 const cpvc=byCode('SIM207559',7)[0];assert.match(cpvc.context,/5 mtr/);assert.equal(cpvc.sourcePrices[0].paise,45500);
 const coupler=byCode('SIM107608',8)[0];assert.equal(coupler.sourcePrices[0].paise,1200);assert.equal(coupler.fields.find(f=>f.label==='Small Std. Pkg.').text,'200');
 const ppr=byCode('SIM204622',24);assert.equal(ppr.length,2);assert.deepEqual(ppr.map(v=>v.sourcePrices[0].paise).sort((a,b)=>a-b),[10600,12200]);
 const hdpe=byCode('SIM205970',64)[0];assert.match(hdpe.context,/SDR 33 PN 4/);assert.equal(hdpe.sourcePrices[0].paise,18680);
 const agri=byCode('SIM200115',65)[0];assert.match(agri.context,/5mtr/);assert.equal(agri.sourcePrices[0].paise,6700);
 const wire=byCode('SIM206482',82)[0];assert.match(wire.context,/Black/);assert.equal(wire.fields[0].text,'20');assert.equal(wire.fields[1].text,'100');assert.ok(wire.issues.some(s=>s.includes('inch')));
 const outlier=all.find(v=>v.page===86&&v.sourcePrices.some(p=>p.paise===8586000));assert.ok(outlier);assert.ok(outlier.issues.some(s=>s.includes('85860.00')));
 const air=all.find(v=>v.page===88&&v.product.name==='AIR VENT');assert.equal(air.code,null);assert.equal(air.sourcePrices.length,0);assert.match(air.rawRow,/104.55/);
 assert.equal(byCode('IM07284',57).length,1);
 const borewell=byCode('SIM200860',78);assert.equal(borewell.length,1);assert.equal(borewell[0].fields.find(f=>f.label==='Mtr.').text,'3');assert.equal(borewell[0].fields.find(f=>f.label==='Size in mm').text,'40');
 const size40=catalog.products.find(p=>p.page===65).variants.filter(v=>v.fields.find(f=>f.label==='Size in mm')?.text==='40');assert.equal(size40.length,3);assert.ok(size40.every(v=>v.sourcePrices[0].paise===6700));
 assert.equal(catalog.products.find(p=>p.page===91).variants.length,3);
});
test('partial activation opens blank reviewed prices and units without touching shop data',()=>{
 const chosen=byCode('SIM107608',8)[0],products=[];
 const drafts=referenceDrafts(catalog,new Set([chosen.id]),products);
 assert.equal(products.length,0);assert.equal(drafts.length,1);assert.equal(drafts[0].variants.length,1);
 assert.equal(drafts[0].variants[0].price,'');assert.equal(drafts[0].variants[0].unit,'');assert.ok(validateDraft(drafts[0]));
 assert.equal(chosen.sourcePrices[0].paise,1200);
});
test('carried links prevent repeat activation and preserve existing locations during later size additions',()=>{
 const chosen=byCode('SIM107608',8)[0],other=byCode('SIM107609',8)[0];
 const d=referenceDrafts(catalog,new Set([chosen.id]),[])[0];
 const p={...d,id:'shop',aliases:['socket'],zone:'A2',locationNote:'Top shelf',photo:null,variants:[{id:'v',spec:referenceSpec(chosen),unit:'piece',pricePaise:950,updatedAt:1,sourceReferenceId:chosen.id}],updatedAt:1};
 assert.equal(referenceDrafts(catalog,new Set([chosen.id]),[p]).length,0);
 const next=referenceDrafts(catalog,new Set([chosen.id,other.id]),[p])[0];assert.equal(next.targetId,'shop');assert.equal(next.zone,'A2');assert.equal(next.variants[0].sourceReferenceId,other.id);assert.equal(activeReferences([p]).size,1);
 const backup={format:'dukaan-web-preview',version:2,products:[p],history:[],misses:[],settings:{}};
 assert.equal(validateBackup(backup).products[0].variants[0].sourceReferenceId,chosen.id);
 assert.throws(()=>validateBackup({...backup,products:[p,{...p,id:'duplicate'}]}),/duplicate reference/);
});
test('reference search reaches exact codes without exposing them to the shop search collection',()=>{
 const matches=referenceMatches(catalog,'SIM107608');assert.equal(matches.reduce((n,p)=>n+p.variants.length,0),1);
 assert.ok(matches[0].variants[0].label.includes('15'));assert.equal(referenceMatches(catalog,'SIM107608','Wirefit · Electrical').length,0);
 const threaded=referenceMatches(catalog,'SIM201030').flatMap(p=>p.variants);assert.equal(threaded.length,1);assert.equal(threaded[0].code,'SIM201030');
});
