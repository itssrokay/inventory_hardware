import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
import {categoryChoices,matchingCategories,inCategory} from '../public/categories.js';
import {configureTranslations} from '../public/i18n.js';
import {search} from '../public/domain.js';
configureTranslations(JSON.parse(await readFile(new URL('../../app/src/main/assets/ui/translations.json',import.meta.url))));
test('many categories remain individually selectable with exact names, counts and blank category',()=>{
 const products=Array.from({length:80},(_,i)=>({category:`Category ${i}`}));
 products.push({category:'Category 79'},{category:''},{category:'All items'});
 const choices=categoryChoices(products);
 assert.equal(choices.length,82);assert.equal(choices.find(c=>c.value==='Category 79').count,2);
 assert.deepEqual(matchingCategories(choices,'category 79'),[{value:'Category 79',count:2}]);
 assert.equal(inCategory(products,null).length,83);
 assert.equal(inCategory(products,'').length,1);assert.equal(inCategory(products,'All items').length,1);
 assert.equal(inCategory(products,'Category 79').length,2);
});
test('Hindi and Hinglish family search retains canonical category and counts variants',()=>{
 const products=[{category:'Storefit · Water tanks',variants:[1,2,3]},{category:'Wirefit · Electrical',variants:[1]}];
 const choices=categoryChoices(products,p=>p.variants.length);
 assert.equal(matchingCategories(choices,'पानी')[0].value,'Storefit · Water tanks');
 assert.equal(matchingCategories(choices,'paani')[0].count,3);
 assert.equal(matchingCategories(categoryChoices([{category:''}]),'बिना')[0].value,'');
 assert.equal(matchingCategories(choices,'zzzz').length,0);
});
test('category search and escape to all items retain query and never lose later matches',()=>{
 const products=Array.from({length:90},(_,i)=>({id:String(i),name:'Socket',category:i<45?'Plumbing':'Electrical',brand:'Prince',aliases:[],updatedAt:i,variants:[{spec:'20 mm',pricePaise:1700,unit:'piece'}]}));
 const query='socket 20mm';assert.equal(search(inCategory(products,'Electrical'),query).length,45);
 assert.equal(search(inCategory(products,null),query).length,90);
 assert.ok(search(inCategory(products,null),query).some(h=>h.product.id==='89'));
});
