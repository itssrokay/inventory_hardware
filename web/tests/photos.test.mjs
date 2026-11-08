import test from 'node:test';import assert from 'node:assert/strict';
import {productPhotos,setProductPhotos,validatePhotoAlbums} from '../public/photos.js';
import {validateBackup} from '../public/domain.js';
const a='data:image/jpeg;base64,YQ==',b='data:image/png;base64,Yg==',c='data:image/webp;base64,Yw==';
test('single-photo compatibility, cover changes and removal preserve a separate shelf album',()=>{
 const product={photo:a,shelfPhotos:[c]};assert.deepEqual(productPhotos(product),[a]);
 setProductPhotos(product,[b,a]);assert.equal(product.photo,b);assert.deepEqual(product.extraPhotos,[a]);assert.deepEqual(product.shelfPhotos,[c]);
 setProductPhotos(product,[]);assert.equal(product.photo,null);assert.deepEqual(product.extraPhotos,[]);assert.deepEqual(product.shelfPhotos,[c]);
 assert.throws(()=>setProductPhotos(product,Array.from({length:7},(_,i)=>a+i)),/6 photos/);
});
test('web v3 backups retain both albums and reject bad types, remote sources and excess photos',()=>{
 const product={id:'1',name:'Socket',brand:'Prince',category:'Plumbing',zone:'A2',locationNote:'Top',aliases:[],photo:a,extraPhotos:[b],shelfPhotos:[c],updatedAt:1,variants:[{id:'v1',spec:'20 mm',unit:'piece',pricePaise:1700,updatedAt:1}]};
 const backup={format:'dukaan-web-preview',version:3,products:[product],history:[],misses:[]};
 assert.deepEqual(validateBackup(JSON.parse(JSON.stringify(backup))).products[0].shelfPhotos,[c]);
 for(const bad of [{extraPhotos:'bad'},{shelfPhotos:['https://example.com/x.png']},{shelfPhotos:Array(7).fill(c)},{photo:null,extraPhotos:[b]}])assert.throws(()=>validateBackup({...backup,products:[{...product,...bad}]}));
 assert.equal(validatePhotoAlbums({photo:a}),true);
 assert.equal(validateBackup({...backup,version:2,products:[{...product,extraPhotos:undefined,shelfPhotos:undefined}]}).products[0].photo,a);
});
