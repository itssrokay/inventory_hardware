import test from 'node:test';
import assert from 'node:assert/strict';
import { paise, normalize, search, validateDraft, duplicate, validateBackup } from '../public/domain.js';
const lock = { id: 'lock', name: 'Padlock', brand: 'Godrej', category: 'Locks', aliases: ['tala', 'ताला'], zone: '', locationNote: '', updatedAt: 1,
  variants: [{ id: '50', spec: '50 mm', unit: 'piece', pricePaise: 45000, updatedAt: 1 }, { id: '65', spec: '65 mm', unit: 'piece', pricePaise: 60000, updatedAt: 1 }] };
const screw = { ...lock, id: 'screw', name: 'Wood screw', brand: '', category: 'Fasteners', aliases: ['pench', 'पेंच'],
  variants: [{ id: 's1', spec: '1.5 inch', unit: 'packet', pricePaise: 9000, updatedAt: 1 }, { id: 's2', spec: '2 inch', unit: 'packet', pricePaise: 12000, updatedAt: 1 }] };
test('Hindi and Hinglish searches select the requested size', () => {
  assert.equal(search([lock, screw], 'ताला ५०mm का दाम')[0].variants[0].id, '50');
  assert.equal(search([lock, screw], 'Godrej tala 65mm ka rate')[0].variants[0].pricePaise, 60000);
});
test('fractional sizes and transposition typos match', () => {
  assert.equal(normalize('1½"'), '1.5 inch');
  assert.equal(search([screw], 'wodo screw 1.5 in')[0].variants[0].id, 's1');
  assert.equal(search([lock], '5mm tala').length, 0);
  assert.equal(search([screw], '5 inch screw').length, 0);
});
test('all meaningful words and the selected category must match', () => {
  assert.equal(search([lock, screw], 'Godrej screw').length, 0);
  assert.equal(search([lock, screw], 'tala', 'Fasteners').length, 0);
  assert.equal(search([lock, screw], '', 'Locks').length, 1);
});
test('prices use exact paise and invalid values block saving', () => {
  assert.equal(paise('475.50'), 47550);
  assert.equal(paise('0.1'), 10);
  for (const value of ['', '-1', '1.999', 'NaN', 'Infinity', '100000001']) assert.equal(paise(value), null);
  assert.ok(validateDraft({ name: 'Bolt', variants: [{ spec: '', unit: 'piece', price: '' }] }));
  assert.ok(validateDraft({ name: 'Bolt', variants: [{ spec: '', unit: 'piece', price: '4' }, { spec: '', unit: 'piece', price: '5' }] }));
});
test('duplicate detection respects aliases and brands', () => {
  assert.equal(duplicate({ name: 'tala', brand: 'godrej' }, [lock]).id, 'lock');
  assert.equal(duplicate({ name: 'Padlock', brand: 'Yale' }, [lock]), undefined);
});
test('backup validation rejects malformed prices and executable photo URLs', () => {
  const backup = { format: 'dukaan-web-preview', version: 1, products: [lock], history: [], misses: [] };
  assert.equal(validateBackup(backup), backup);
  assert.throws(() => validateBackup({ ...backup, products: [{ ...lock, photo: 'javascript:alert(1)' }] }));
  assert.throws(() => validateBackup({ ...backup, products: [{ ...lock, variants: [{ ...lock.variants[0], pricePaise: 1.23 }] }] }));
  assert.throws(() => validateBackup({ ...backup, products: [lock, lock] }));
});

test('natural brand filters are strict and match plural product names', () => {
  const socket={...lock,id:'prince-socket',name:'PVC socket',brand:'Prince',category:'Plumbing'};
  const valve={...socket,id:'prince-valve',name:'Ball valve'};
  const astral={...socket,id:'astral',name:'Prince compatible socket',brand:'Astral'};
  assert.deepEqual(search([socket,valve,astral],'Prince only').map(h=>h.product.id),['prince-socket','prince-valve']);
  assert.deepEqual(search([socket,valve,astral],'sockets from Prince only').map(h=>h.product.id),['prince-socket']);
  assert.equal(search([socket,valve,astral],'sockets from Prince only','Tools').length,0);
});
test('discount calculator rounds half-up to paise and never compounds a saved value', async () => {
  const {discountedPaise}=await import('../public/domain.js');
  assert.equal(discountedPaise('100','25'),7500);assert.equal(discountedPaise('75',''),7500);
  assert.equal(discountedPaise('0.01','50'),1);assert.equal(discountedPaise('450','100'),0);
  assert.equal(discountedPaise('450','0.10'),44955);
  for(const discount of ['-1','100.01','25.555','oops'])assert.equal(discountedPaise('100',discount),null);
});
test('cropped pixels match cover/zoom/pan and cannot escape the source', async () => {
  const {cropRegion}=await import('../public/domain.js');
  assert.deepEqual(cropRegion(200,100,100,100,1,0,0),{left:50,top:0,width:100,height:100});
  assert.deepEqual(cropRegion(200,100,100,100,2,0,0),{left:75,top:25,width:50,height:50});
  assert.equal(cropRegion(200,100,100,100,2,10000,0).left,0);
  assert.equal(cropRegion(200,100,100,100,2,-10000,0).left,150);
  assert.throws(()=>cropRegion(0,100,100,100,1,0,0));
});
