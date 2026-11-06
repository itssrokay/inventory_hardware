import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
import {performance} from 'node:perf_hooks';
import {configureTranslations} from '../public/i18n.js';
import {prepareReferenceSearch,referenceMatches,referenceResultsView} from '../public/reference.js';
import {relatedProducts,discountedPaise,decimal} from '../public/domain.js';
const catalog=JSON.parse(await readFile(new URL('../../app/src/main/assets/reference/prince-2026.json',import.meta.url)));
configureTranslations(JSON.parse(await readFile(new URL('../../app/src/main/assets/ui/translations.json',import.meta.url))));
test('cached reference queries preserve canonical matches, Hindi aliases, exact codes and family boundaries',()=>{
 const before=JSON.stringify(catalog),index=prepareReferenceSearch(catalog);
 assert.equal(prepareReferenceSearch(catalog),index);
 assert.deepEqual(referenceMatches(catalog,'कपलर').map(p=>p.id),referenceMatches(catalog,'coupler').map(p=>p.id));
 assert.equal(referenceMatches(catalog,'sim107608')[0].variants[0].code,'SIM107608');
 assert.equal(referenceMatches(catalog,'sim107608','Wirefit · Electrical').length,0);
 const times=[];
 for(let i=0;i<10;i++) {const t=performance.now();for(const query of ['c','co','cou','coupler','कपलर'])referenceMatches(catalog,query);times.push(performance.now()-t);}
 assert.ok(times.sort((a,b)=>a-b)[5]<100,'Five cached queries should typically complete within 100 ms');
 assert.equal(JSON.stringify(catalog),before);
 const broad=referenceResultsView(catalog,[],{query:'coupler'});assert.ok(!broad.includes('reference-group" open'));
 assert.match(referenceResultsView(catalog,[],{query:'SIM107608'}),/reference-group" open/);
});
test('discount preview is exact, repeatable and never changes a saved price',()=>{
 const variant={pricePaise:12345,unit:'piece',updatedAt:1};const before=JSON.stringify(variant);
 assert.equal(discountedPaise(decimal(variant.pricePaise),'25'),9259);
 assert.equal(discountedPaise(decimal(variant.pricePaise),'25'),9259);
 assert.equal(discountedPaise(decimal(variant.pricePaise),'100'),0);
 for(const input of ['-1','101','25.001','abc'])assert.equal(discountedPaise(decimal(variant.pricePaise),input),null);
 assert.equal(JSON.stringify(variant),before);
});
test('related browsing stays within the category, prioritizes brand and never merges records',()=>{
 const base={id:'1',name:'Socket',category:'Plumbing',brand:'Prince',variants:[{pricePaise:1200}]};
 const same={...base,id:'2',name:'Elbow'},different={...base,id:'3',name:'Adapter',brand:'Other'},wrong={...base,id:'4',category:'Electrical'};
 const all=[base,different,wrong,same],before=JSON.stringify(all);
 assert.deepEqual(relatedProducts(base,all).map(p=>p.id),['2','3']);assert.equal(JSON.stringify(all),before);
 assert.deepEqual(relatedProducts({...base,category:''},all),[]);
});
