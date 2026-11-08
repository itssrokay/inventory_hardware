import {validatePhotoAlbums} from './photos.js';
export const UNITS = ['piece', 'kg', 'ft', 'metre', 'box', 'dozen', 'packet'];
export const currency = value => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2, minimumFractionDigits: 0 }).format(value / 100);
export const decimal = value => String(value / 100);
export function paise(value) {
  const text = String(value ?? '').trim();
  if (!/^\d+(?:\.\d{1,2})?$/.test(text)) return null;
  const [whole, fraction = ''] = text.split('.');
  const amount = Number(whole) * 100 + Number(fraction.padEnd(2, '0'));
  return Number.isSafeInteger(amount) && amount <= 10_000_000_000 ? amount : null;
}
export function discountedPaise(price, discount = '') {
  const base = paise(price);
  if (base === null) return null;
  if (String(discount).trim() === '') return base;
  const basisPoints = paise(discount);
  if (basisPoints === null || basisPoints > 10000) return null;
  return Number((BigInt(base) * BigInt(10000 - basisPoints) + 5000n) / 10000n);
}
export function normalize(value) {
  return String(value).toLowerCase().replaceAll('½', '.5').replaceAll('¼', '.25').replaceAll('¾', '.75').normalize('NFKC')
    .replace(/[०-९]/g, c => String(c.charCodeAt(0) - 0x0966))
    .replace(/(\d)\s*["″]/g, '$1 inch').replace(/(\d)\s*['′]/g, '$1 ft')
    .replace(/(\d)\s*(?:inches|inch|in)\b/g, '$1 inch')
    .replace(/(\d)\s*(?:millimetres|millimeters|mm)\b/g, '$1 mm')
    .replace(/(\d)\s*(?:feet|foot|ft)\b/g, '$1 ft')
    .replace(/[^\p{L}\p{M}\p{N}.]+/gu, ' ').trim().replace(/\s+/g, ' ');
}
const fillers = new Set('price of the rate ka ki ke kya hai kitna kitne batao chahiye please do you have what is का की के दाम रेट है क्या कितना बताओ only from by show me all items products brand just sirf sirph केवल सिर्फ or'.split(' '));
export function tokens(text) { return normalize(text).split(' ').filter(t => t && !fillers.has(t)); }
export function similarity(a, b) {
  if (a === b) return 1;
  if (!a.length || !b.length) return 0;
  const d = Array.from({ length: a.length + 1 }, () => Array(b.length + 1).fill(0));
  for (let i = 0; i <= a.length; i++) d[i][0] = i;
  for (let j = 0; j <= b.length; j++) d[0][j] = j;
  for (let i = 1; i <= a.length; i++) for (let j = 1; j <= b.length; j++) {
    d[i][j] = Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + (a[i - 1] === b[j - 1] ? 0 : 1));
    if (i > 1 && j > 1 && a[i - 1] === b[j - 2] && a[i - 2] === b[j - 1]) d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + 1);
  }
  return 1 - d[a.length][b.length] / Math.max(a.length, b.length);
}
export function searchIntent(products, query) {
  let remaining = ` ${normalize(query)} `;
  const brands = [];
  for (const brand of [...new Set(products.map(p => p.brand).filter(Boolean))].sort((a,b) => b.length-a.length)) {
    const phrase = ` ${normalize(brand)} `;
    if (remaining.includes(phrase)) { brands.push(normalize(brand)); remaining = remaining.split(phrase).join(' '); }
  }
  return { brands, keywords: remaining.trim() };
}
export function singularWord(value) {
  if (value.length > 4 && /(?:ches|shes|xes|zes|sses)$/.test(value)) return value.slice(0,-2);
  return value.length > 3 && value.endsWith('s') && !value.endsWith('ss') ? value.slice(0,-1) : value;
}
export function search(products, query, category = '') {
  const intent = searchIntent(products, query);
  const terms = tokens(intent.keywords).map(singularWord);
  const phrase = terms.join(' ');
  const size = phrase.match(/\d+(?:\.\d+)?(?:\s+(?:inch|mm|ft))?/)?.[0];
  return products.filter(p => (!category || p.category === category) && (!intent.brands.length || intent.brands.includes(normalize(p.brand)))).flatMap(product => {
    if (!terms.length) return [{ product, variants: product.variants, score: 1 }];
    const names = [product.name, product.brand, product.category, ...product.aliases, ...product.variants.map(v => v.spec)].map(normalize);
    const words = names.flatMap(n => n.split(' ')).map(singularWord);
    const scores = terms.map(term => Math.max(0, ...words.map(word => {
      if (term === word) return 1;
      if (word.startsWith(term)) return .92;
      if (term.length >= 3 && word.length >= 3 && !/\d/.test(term)) {
        const value = similarity(term, word); return value >= .6 ? value * .8 : 0;
      }
      return 0;
    })));
    if (scores.some(s => s === 0)) return [];
    const variants = !size ? product.variants : product.variants.filter(v => {
      const spec = normalize(v.spec);
      const escaped = size.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      return new RegExp(`(?<![\\d.])${escaped}(?![${size.includes(' ') ? '\\p{L}' : ''}\\d.])`, 'u').test(spec);
    });
    return variants.length ? [{ product, variants, score: scores.reduce((a, b) => a + b, 0) / scores.length + (names.includes(phrase) ? 1 : 0) }] : [];
  }).sort((a, b) => b.score - a.score || b.product.updatedAt - a.product.updatedAt);
}
export function duplicate(draft, products) {
  return products.find(p => normalize(p.brand) === normalize(draft.brand) &&
    (normalize(p.name) === normalize(draft.name) || p.aliases.some(a => normalize(a) === normalize(draft.name))));
}
export function validateDraft(draft) {
  if (!draft.name?.trim()) return 'Enter an item name.';
  if (!draft.variants?.length) return 'Add at least one size and selling price.';
  if (draft.variants.some(v => !UNITS.includes(v.unit) || discountedPaise(v.price, v.discount) === null)) return 'Check each price, unit and discount (0–100%). Use up to two decimal places.';
  if (new Set(draft.variants.map(v => `${normalize(v.spec)}|${v.unit}`)).size !== draft.variants.length) return 'Each size and unit must be unique.';
  return null;
}
export function validateBackup(data) {
  if (data?.format !== 'dukaan-web-preview' || ![1,2,3].includes(data.version) || !Array.isArray(data.products) || !Array.isArray(data.history) || !Array.isArray(data.misses)) throw new Error('Choose a valid Dukaan web preview backup. Android ZIP backups use a separate format.');
  if (data.products.length > 5000) throw new Error('This backup is too large for the preview.');
  const ids = new Set();
  const sourceIds = new Set();
  for (const p of data.products) {
    if (typeof p.id !== 'string' || ids.has(p.id) || typeof p.name !== 'string' || !p.name.trim() ||
      !['brand', 'category', 'zone', 'locationNote'].every(k => typeof p[k] === 'string') ||
      !Array.isArray(p.aliases) || !p.aliases.every(a => typeof a === 'string') || !Number.isFinite(p.updatedAt) ||
      (p.photo && !/^data:image\/(?:jpeg|png|webp);base64,[A-Za-z0-9+/=]+$/.test(p.photo))) throw new Error('The backup contains an invalid item.');
    if(!validatePhotoAlbums(p))throw new Error('The backup contains invalid photo albums.');
    ids.add(p.id);
    if (!Array.isArray(p.variants) || !p.variants.length || p.variants.some(v => typeof v.id !== 'string' || typeof v.spec !== 'string' || !UNITS.includes(v.unit) || !Number.isSafeInteger(v.pricePaise) || v.pricePaise < 0 || v.pricePaise > 10_000_000_000 || !Number.isFinite(v.updatedAt))) throw new Error('The backup contains an invalid price or size.');
  }
  for (const p of data.products) for (const v of p.variants) {
    if(v.sourceReferenceId != null) {
      if(typeof v.sourceReferenceId !== 'string'||v.sourceReferenceId.length>250||!v.sourceReferenceId.length||sourceIds.has(v.sourceReferenceId)) throw new Error('Invalid or duplicate reference link in backup.');
      sourceIds.add(v.sourceReferenceId);
    }
  }
  for (const h of data.history) if (!h || typeof h.variantId !== 'string' || !Number.isSafeInteger(h.oldPrice) || !Number.isSafeInteger(h.newPrice) || h.oldPrice < 0 || h.newPrice < 0 || !Number.isFinite(h.changedAt)) throw new Error('Invalid price history in backup.');
  for (const m of data.misses) if (!m || typeof m.id !== 'string' || typeof m.query !== 'string' || !Number.isFinite(m.searchedAt)) throw new Error('Invalid search history in backup.');
  return data;
}

export function cropRegion(width, height, frameWidth, frameHeight, zoom, panX, panY) {
  if (![width,height,frameWidth,frameHeight,zoom,panX,panY].every(Number.isFinite) || width <= 0 || height <= 0 || frameWidth <= 0 || frameHeight <= 0 || zoom < 1) throw new Error('Invalid crop dimensions.');
  const scale = Math.max(frameWidth/width,frameHeight/height)*zoom;
  const cropWidth = Math.min(width, Math.max(1, Math.round(frameWidth/scale)));
  const cropHeight = Math.min(height, Math.max(1, Math.round(frameHeight/scale)));
  return {left:Math.max(0, Math.min(width-cropWidth, Math.round((width-frameWidth/scale)/2-panX/scale))),
    top:Math.max(0,Math.min(height-cropHeight, Math.round((height-frameHeight/scale)/2-panY/scale))),width:cropWidth,height:cropHeight};
}

// Related browsing never merges item records or their selling prices.
export function relatedProducts(product, catalog) {
  if (!product.category?.trim()) return [];
  return catalog.filter(other => other.id !== product.id && normalize(other.category) === normalize(product.category))
    .sort((a,b) => Number(Boolean(product.brand) && normalize(b.brand)===normalize(product.brand)) - Number(Boolean(product.brand) && normalize(a.brand)===normalize(product.brand)) || a.name.localeCompare(b.name) || String(a.id).localeCompare(String(b.id)))
    .slice(0,6);
}
