import {productPhotos,setProductPhotos,MAX_PHOTOS} from './photos.js';
import {categoryChoices,matchingCategories,inCategory} from './categories.js';
import { cropImage } from './crop.js';
import {configureTranslations,setLanguageMode,languageMode,translate,localizeUi,searchQuery,productText} from './i18n.js';
import {activeReferences, referenceDrafts, referenceView, referenceResultsView, prepareReferenceSearch, sourceNote, withReferencePictures, linkedReferenceProduct, linkedPicture, referenceImageUrl, referencePhotoData} from './reference.js';
import { UNITS, currency, decimal, normalize, search, duplicate, validateDraft, validateBackup, paise, discountedPaise, searchIntent, relatedProducts } from './domain.js';

const $ = (selector, root = document) => root.querySelector(selector);
const esc = value => String(value ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const uid = () => crypto.randomUUID();
const date = value => new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }).format(new Date(value));
const paths = {
  shop: '<path d="M3 10l2-6h14l2 6M4 10v10h16V10M3 10h18M9 20v-6h6v6M3 10a3 3 0 006 0 3 3 0 006 0 3 3 0 006 0"/>',
  catalog: '<rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/>',
  plus: '<path d="M12 5v14M5 12h14"/>', search: '<circle cx="10.5" cy="10.5" r="6.5"/><path d="M16 16l5 5"/>',
  mic: '<rect x="9" y="2" width="6" height="12" rx="3"/><path d="M5 10v2a7 7 0 0014 0v-2M12 19v3M9 22h6"/>',
  settings: '<path d="M4 7h16M4 17h16M8 4v6M16 14v6"/><circle cx="8" cy="7" r="2"/><circle cx="16" cy="17" r="2"/>',
  history: '<path d="M3 11a9 9 0 119 10M3 4v7h7M12 7v5l3 2"/>',
  missed: '<path d="M21 11a8 8 0 01-8 8H6l-4 3V11a9 9 0 0118-3M20 2v4M20 9v.01"/>',
  arrow: '<path d="M5 12h14M13 6l6 6-6 6"/>', check: '<path d="M5 12l4 4L19 6"/>',
  download: '<path d="M12 3v12M7 10l5 5 5-5M4 16v5h16v-5"/>', edit: '<path d="M16 3l5 5M4 16L17 3l4 4L8 20l-5 1z"/>',
  close: '<path d="M6 6l12 12M6 18L18 6"/>', photo: '<rect x="3" y="3" width="18" height="18" rx="3"/><circle cx="8" cy="8" r="1.5"/><path d="M3 17l5-5 4 4 4-6 5 7"/>',
  spark: '<path d="M12 3l3 6 6 3-6 3-3 6-3-6-6-3 6-3zM20 2v4M18 4h4"/>',
  shield: '<path d="M12 3l8 4v6c0 4-8 8-8 8s-8-4-8-8V7zM8 12l3 3 5-6"/>',
  chevron: '<path d="M6 9l6 6 6-6"/>', list: '<path d="M8 5h13M8 12h13M8 19h13M3 5h.01M3 12h.01M3 19h.01"/>',
  help: '<circle cx="12" cy="12" r="9"/><path d="M9 9a3 3 0 016 0c0 2-3 2-3 4M12 17h.01"/>',
  key: '<circle cx="8" cy="9" r="5"/><path d="M12 12l8 8M17 17l3-3M19 19l3-3"/>',
};
function icon(name, extra = '') { return `<svg class="icon ${extra}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.65" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${paths[name] || paths.shop}</svg>`; }
function kind(product) {
  const name = normalize(product.name + ' ' + product.category);
  return /lock|tala/.test(name) ? 'lock' : /screw|fastener/.test(name) ? 'screw' : /valve|plumb/.test(name) ? 'valve' : /plier/.test(name) ? 'pliers' : /adhes|fevicol/.test(name) ? 'glue' : 'tape';
}
function illustration(product) {
  const picture=product.photo||referenceImageUrl(linkedPicture(referenceState.catalog,product));
  if (picture) return `<img class="product-photo" src="${esc(picture)}" alt="${esc(product.name)}">`;
  const type = kind(product);
  const art = {
    lock: '<path d="M28 38V28a14 14 0 0128 0v10" fill="none" stroke="#a8a693" stroke-width="8"/><rect x="20" y="35" width="44" height="34" rx="7" fill="#b69c60"/><rect x="24" y="39" width="36" height="26" rx="5" fill="#c6ad73"/><circle cx="42" cy="49" r="4" fill="#605539"/><path d="M40 50h4v9h-4z" fill="#605539"/>',
    screw: '<g transform="rotate(38 42 42)"><path d="M35 24h14v39l-7 10-7-10z" fill="#9ba9ab"/><path d="M33 36l18-6M33 45l18-6M33 54l18-6M35 63l14-5" stroke="#68787c" stroke-width="3"/><rect x="28" y="15" width="28" height="12" rx="3" fill="#718287"/><path d="M33 21h18" stroke="#d3dee0" stroke-width="2"/></g>',
    valve: '<path d="M10 42h64v20H10z" fill="#bba06a"/><rect x="23" y="36" width="38" height="32" rx="5" fill="#d1b679"/><path d="M10 44h6v16h-6M68 44h6v16h-6" stroke="#897345" stroke-width="4"/><path d="M42 39V25" stroke="#8d8d81" stroke-width="8"/><path d="M20 20h43v10H20z" fill="#c85f4b"/>',
    pliers: '<g transform="rotate(-20 42 42)"><path d="M25 9l9 3 8 20 8-20 9-3-5 28-9 11-3-9-3 9-9-11z" fill="#85969b"/><circle cx="42" cy="36" r="7" fill="#a9b6b9"/><path d="M35 42l-9 29M49 42l9 29" stroke="#bb6653" stroke-width="12" stroke-linecap="round"/></g>',
    glue: '<rect x="22" y="21" width="40" height="52" rx="8" fill="#e1dfd3"/><path d="M24 36h36v24H24z" fill="#306981"/><path d="M24 48h36v12H24z" fill="#d5a251"/><path d="M31 12h22v12H31z" fill="#e19e42"/><path d="M37 3h10v12H37z" fill="#e19e42"/><path d="M30 42h24" stroke="#fff" stroke-width="3"/>',
    tape: '<rect x="17" y="19" width="51" height="48" rx="18" fill="#d9b44e"/><path d="M25 51v11h34V51" fill="#4d5857"/><circle cx="42" cy="39" r="13" fill="#5d655d"/><circle cx="42" cy="39" r="8" fill="#8c9589"/><path d="M66 48h11v12H65" fill="#a8b3b1"/><path d="M72 51v6" stroke="#586b6b"/>',
  };
  return `<svg class="product-art" viewBox="0 0 84 84" aria-hidden="true">${art[type]}</svg>`;
}
function sampleData() {
  const now = Date.now();
  const examples = [
    ['Padlock', 'Godrej', 'Locks', ['tala', 'ताला', 'lock'], [['50 mm', 'piece', 45000], ['65 mm', 'piece', 60000]], 'Aisle A'],
    ['Wood screw', '', 'Fasteners', ['pench', 'पेंच', 'screw'], [['1 inch', 'packet', 9000], ['1.5 inch', 'packet', 11000], ['2 inch', 'packet', 14000]], 'Drawer 03'],
    ['Brass ball valve', 'Zoloto', 'Plumbing', ['valve', 'नल', 'nal'], [['0.5 inch', 'piece', 28000], ['0.75 inch', 'piece', 42000]], 'Aisle B'],
    ['Combination pliers', 'Taparia', 'Tools', ['plas', 'प्लास', 'plier'], [['8 inch', 'piece', 32000]], 'Tool wall'],
    ['Fevicol SH', 'Pidilite', 'Adhesives', ['glue', 'गोंद', 'gond', 'fevicol'], [['500 g', 'piece', 16500], ['1 kg', 'piece', 29000]], 'Shelf C2'],
    ['Measuring tape', 'Stanley', 'Tools', ['tape', 'फीता', 'feeta'], [['3 metre', 'piece', 18000], ['5 metre', 'piece', 26000]], 'Tool wall'],
  ];
  return { products: examples.map(([name, brand, category, aliases, variants, zone]) => ({ id: uid(), name, brand, category, aliases, zone, locationNote: '', photo: null, updatedAt: now, demo: true,
    variants: variants.map(([spec, unit, pricePaise]) => ({ id: uid(), spec, unit, pricePaise, updatedAt: now })) })),
    history: [], misses: [], settings: { tax: 'GST not specified', language: 'hi-IN' } };
}
let database;
function openDatabase() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open('dukaan-local-preview', 1);
    request.onupgradeneeded = () => request.result.createObjectStore('state');
    request.onsuccess = () => { database = request.result; resolve(); };
    request.onerror = () => reject(new Error('Browser storage is unavailable. Enable site storage and reload.'));
  });
}
function readData() { return new Promise((resolve, reject) => {
  const request = database.transaction('state').objectStore('state').get('catalog');
  request.onsuccess = () => resolve(request.result); request.onerror = () => reject(request.error);
}); }
function writeData(data) { return new Promise((resolve, reject) => {
  const transaction = database.transaction('state', 'readwrite');
  transaction.objectStore('state').put(data, 'catalog'); transaction.oncomplete = resolve;
  transaction.onerror = () => reject(new Error('Could not save in this browser. Export a backup or free some browser storage.'));
}); }
const state = { page: location.hash === '#reference' ? 'reference' : 'catalog', query: '', category: null, resultLimit: 36, layout: 'grid', drafts: [], fullEdit: false, listing: '', listingImages: [], photoBusy:false, warnings: [], rewrites: null, busy: false,
  data: null, config: { aiConfigured: false, model: '' } };
const referenceState = {catalog:null,query:'',category:'',scope:'all',selected:new Set(),limit:20};
let referenceSearchTimer;
function renderReference() { clearTimeout(referenceSearchTimer);$('#main').innerHTML=referenceView(referenceState.catalog,state.data.products,referenceState); }
function scheduleReferenceSearch() {
  clearTimeout(referenceSearchTimer);
  const status=$('#reference-search-status');if(status&&status.textContent!==translate('Searching…'))status.textContent=translate('Searching…');
  referenceSearchTimer=setTimeout(()=>{
    if(state.page!=='reference'||!$('#reference-results'))return;
    $('#reference-results').innerHTML=referenceResultsView(referenceState.catalog,state.data.products,referenceState);
    if($('#reference-search-status'))$('#reference-search-status').textContent='';
  },150);
}
function languageSelector() {
  return `<label class="language-control"><span>App language</span><select class="language-switch" data-ui-language aria-label="App language">${[['en','English'],['hi','हिन्दी'],['hinglish','Hinglish']].map(([value,label])=>`<option value="${value}" ${value===(state.data.settings.uiLanguage||'en')?'selected':''}>${label}</option>`).join('')}</select></label>`;
}
let toastTimer;
function toast(text) { $('#toast').textContent = translate(text); $('#toast').classList.add('visible'); clearTimeout(toastTimer); toastTimer = setTimeout(() => $('#toast').classList.remove('visible'), 4500); }
async function changeData(change) { const next = structuredClone(state.data); change(next); await writeData(next); state.data = next; }
async function api(path, body) {
  const response = await fetch(path, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
  const data = await response.json(); if (!response.ok) throw new Error(data.error || 'Request failed.'); return data;
}
function shell() {
  setLanguageMode(state.data.settings.uiLanguage||'en');
  try {localStorage.setItem('dukaan-ui-language',languageMode());}catch {}
  const count = state.data.products.length;
  $('#app').innerHTML = `<aside class="sidebar">
    <a class="brand" href="#catalog" aria-label="Dukaan catalog"><span class="brand-mark">${icon('shop')}</span><span>dukaan<span class="brand-dot">.</span></span></a>
    <div class="workspace-label">YOUR WORKSPACE</div>
    <nav aria-label="Main navigation">${[['catalog', 'catalog', 'Catalog'], ['reference', 'catalog', 'Reference library'], ['add', 'plus', 'Add items'], ['history', 'history', 'Price history'], ['misses', 'missed', 'Missed searches']].map(([page, image, text]) =>
      `<button class="nav-item ${state.page === page ? 'active' : ''}" data-page="${page}">${icon(image)}<span>${text}</span>${page === 'catalog' ? `<span class="nav-count">${count}</span>` : ''}</button>`).join('')}</nav>
    <div class="sidebar-note"><span class="note-icon">${icon('shield')}</span><strong>Your shop. Your data.</strong><p>This preview saves your catalog in this browser.</p><span class="local-label"><i></i> Running on localhost</span></div>
    <div class="sidebar-bottom"><button class="nav-item ${state.page === 'settings' ? 'active' : ''}" data-page="settings">${icon('settings')}<span>Settings & backup</span></button>
      <a class="android-link" href="https://github.com/itssrokay/inventory_hardware/releases/latest/download/Dukaan-debug.apk" target="_blank" rel="noopener">${icon('download')}<span>Get the Android app</span>${icon('arrow')}</a>
      <div class="profile"><span class="avatar">D</span><div><strong>My hardware shop</strong><small>Local web preview</small></div><span class="version">v0.5</span></div></div>
    </aside><div class="mobile-bar"><a class="brand" href="#catalog"><span class="brand-mark">${icon('shop')}</span>dukaan.</a>${languageSelector()}</div>
    <div class="workspace"><div class="topbar"><span>${icon('shop')} My hardware shop <span class="breadcrumb">/</span> ${esc({ catalog: 'Catalog', reference: 'Reference library', add: 'Add items', history: 'Price history', misses: 'Missed searches', settings: 'Settings' }[state.page])}</span>${languageSelector()}</div>
    <main id="main" tabindex="-1"></main><footer class="page-footer"><span>Made for the way you run your shop.</span><span>हिन्दी, English & Hinglish</span></footer></div>
    <nav class="mobile-nav" aria-label="Mobile navigation">${[['catalog', 'catalog', 'Catalog'], ['add', 'plus', 'Add'], ['reference', 'catalog', 'Reference'], ['history', 'history', 'History'], ['settings', 'settings', 'Settings']].map(([p, i, label]) => `<button data-page="${p}" class="${state.page === p ? 'active' : ''}">${icon(i)}${label}</button>`).join('')}</nav>`;
  ({ catalog: renderCatalog, reference: renderReference, add: renderAdd, history: renderHistory, misses: renderMisses, settings: renderSettings })[state.page]();
  localizeUi(document);
}
function pageHeader(kicker, title, subtitle, action = '') {
  return `<header class="page-header"><div><p class="eyebrow">${kicker}</p><h1>${title}</h1><p class="subtitle">${subtitle}</p></div>${action}</header>`;
}
function renderCatalog() {
  $('#main').innerHTML = pageHeader('YOUR SHELVES, SEARCHABLE', 'The right price. Right here.', 'Find any item in seconds, however you call it.', `<button class="button primary" data-action="new">${icon('plus')}Add items</button>`) +
    `<section class="search-panel" aria-label="Search catalog"><div class="search-input-wrap">${icon('search')}<input id="search" aria-label="Search catalog" placeholder="Try “Prince only” or “sockets from Prince only”" value="${esc(state.query)}" autocomplete="off"><kbd>/</kbd><button class="icon-button mic" data-action="voice-search" aria-label="Speak a search">${icon('mic')}</button></div>
      <div class="search-foot"><span class="online-dot"></span><span>Local search works offline</span><span class="separator">·</span><span>Natural brand & item filters</span><button class="text-button" data-action="smart">${icon('spark')}Smart search</button></div></section>
      <div id="catalog-results"></div>`;
  renderResults();
}
function results() {
  const local = search(state.data.products, searchQuery(state.query)).filter(hit => state.category === null || (hit.product.category || '') === state.category);
  const intent = searchIntent(state.data.products, searchQuery(state.query));
  const constrained = inCategory(state.data.products, state.category).filter(p => !intent.brands.length || intent.brands.includes(normalize(p.brand)));
  if (state.rewrites?.query === state.query) return [...local, ...state.rewrites.phrases.flatMap(q => search(constrained, q))].filter((h, i, list) => list.findIndex(p => p.product.id === h.product.id) === i);
  return local;
}
function renderResults() {
  const products = state.data.products;
  const samples = products.filter(p => p.demo).length;
  const categories = categoryChoices(products);
  if (state.category !== null && !categories.some(c => c.value === state.category)) state.category = null;
  const hits = results();
  $('#catalog-results').innerHTML = (samples ? `<div class="demo-banner">${icon('help')}<div><strong>A little shelf inspiration.</strong> These ${samples} sample items help you explore. Prices are examples.</div><button class="text-button" data-action="clear-demo">Clear samples ${icon('close')}</button></div>` : '') +
    `<div class="catalog-toolbar"><div class="category-tabs" role="group" aria-label="Filter categories"><button class="button secondary" data-browse-categories>${icon('catalog')}Browse categories <span>${categories.length}</span></button>${state.category!==null?`<span class="active-category" data-preserve-text>${esc(state.category||translate('Uncategorized'))}</span><button class="text-button" data-all-categories>All items</button>`:'<span class="tiny">Search across all categories</span>'}</div><div class="view-toggle"><button class="icon-button ${state.layout === 'grid' ? 'selected' : ''}" data-layout="grid" aria-label="Grid view" aria-pressed="${state.layout === 'grid'}">${icon('catalog')}</button><button class="icon-button ${state.layout === 'list' ? 'selected' : ''}" data-layout="list" aria-label="List view" aria-pressed="${state.layout === 'list'}">${icon('list')}</button></div></div>

    ${searchIntent(products,state.query).brands.length?`<div class="natural-filter"><span>Brand: ${esc(searchIntent(products,state.query).brands.join(', '))}</span><small>Understood from your search · strict brand filter</small></div>`:''}
    <div class="results-caption"><span>${state.query ? `${hits.length} matching ${hits.length === 1 ? 'item' : 'items'}` : `${hits.length} ${hits.length === 1 ? 'item' : 'items'} on your shelves`}</span><span>Tap a product to view prices and discounts ${icon('edit')}</span></div>
    <section class="product-grid ${state.layout === 'list' ? 'list-view' : ''}" aria-label="Catalog results">${hits.slice(0,state.resultLimit).map(productCard).join('') || `<div class="empty-state">${icon('search')}<h2>${products.length ? 'No matching items yet' : 'Ready for your first item?'}</h2><p>${products.length ? 'Try a different name, spelling or size. Or let smart search help.' : 'Add an item with its sizes and selling prices to make your shelves searchable.'}</p>${state.category!==null?'<button class="button secondary" data-all-categories>Search all items</button>':''}<button class="button primary" data-action="${products.length ? 'smart' : 'new'}">${icon(products.length ? 'spark' : 'plus')}${products.length ? 'Try smart search' : 'Add your first item'}</button></div>`}</section>${hits.length>state.resultLimit?`<div class="load-more"><p>${translate('Showing {0} of {1} items').replace('{0}',state.resultLimit).replace('{1}',hits.length)}</p><button class="button secondary" data-more-items>Show more items</button></div>`:''}`;
}
function productCard({ product: p, variants }) {
  const theme = kind(p);
  const source=linkedReferenceProduct(referenceState.catalog,p);
  const originalName=source?`${source.category.split(' · ')[0]} ${source.name}`:null;
  const displayName=p.name===originalName?productText(p.name):p.name;
  return `<article class="product-card" data-product="${esc(p.id)}" data-view-product="${esc(p.id)}"><div class="product-top"><div class="product-thumb ${theme}">${illustration(p)}</div><div class="product-title"><span class="category-label">${esc(p.category || 'General')}</span><h2 data-preserve-text><button class="product-open" data-view-product="${esc(p.id)}">${esc(displayName)}</button></h2>${displayName!==p.name?`<small data-preserve-text>${esc(p.name)}</small>`:''}<p>${esc(p.brand || 'Unbranded')}${p.demo ? ' <span class="sample-label">SAMPLE</span>' : ''}</p></div><button class="icon-button edit-item" data-edit="${esc(p.id)}" aria-label="Edit ${esc(p.name)}">${icon('edit')}</button></div>
    <div class="variant-list">${variants.map(v => `<button class="price-row" data-view-product="${esc(p.id)}" aria-label="View ${esc(p.name)} ${esc(v.spec)} price"><span class="variant-spec">${esc(v.spec || 'Standard')}<span>${esc(translate("per"))} ${esc(translate(v.unit))}</span><small class="price-date">${esc(translate(`Updated ${date(v.updatedAt)}${v.sourceReferenceId?' · Confirmed shop price · reference linked':''}`))}</small></span><strong>${currency(v.pricePaise)}</strong>${icon('arrow')}</button>`).join('')}</div>
    <div class="product-meta"><span>${p.zone ? `${icon('shop')}${esc(p.zone)}` : esc(state.data.settings.tax)}</span><span>${p.demo ? 'Sample · ' : ''}Price updated ${date(Math.max(...p.variants.map(v=>v.updatedAt)))}</span></div></article>`;
}
function photoEditor(index,shelf) {
  const images=index===null?state.listingImages:shelf?(state.drafts[index].shelfPhotos||[]):productPhotos(state.drafts[index]);
  const title=index===null?'AI input images':shelf?'Shelf photos':'Product photos';
  return `<section class="photo-album"><h3>${translate(title)} (${images.length}/6)</h3><div class="photo-album-strip">${images.map((image,n)=>`<div class="photo-tile"><img src="${esc(image)}" alt="${esc(translate(title))} ${n+1}">${index!==null&&!shelf&&n===0?'<small>Cover photo</small>':''}<button type="button" class="text-button" data-album-action="crop" data-album-index="${index??'ai'}" data-album-shelf="${shelf}" data-photo-index="${n}" ${state.busy?'disabled':''}>Crop photo</button><button type="button" class="text-button" data-album-action="remove" data-album-index="${index??'ai'}" data-album-shelf="${shelf}" data-photo-index="${n}" ${state.busy?'disabled':''}>Remove photo</button>${index!==null&&!shelf&&n>0?`<button type="button" class="text-button" data-album-action="cover" data-album-index="${index}" data-album-shelf="false" data-photo-index="${n}" ${state.busy?'disabled':''}>Make cover</button>`:''}</div>`).join('')}</div><label class="button secondary photo-label">${icon('photo')}Add photos<input class="sr-only" type="file" accept="image/*" multiple data-album-upload="${index??'ai'}" data-album-shelf="${shelf}" ${state.busy||images.length>=6?'disabled':''}></label></section>`;
}
function albumFor(index,shelf) {return index===null?state.listingImages:shelf?(state.drafts[index].shelfPhotos||[]):productPhotos(state.drafts[index]);}
function assignAlbum(index,shelf,images) {if(index===null)state.listingImages=images;else if(shelf)state.drafts[index].shelfPhotos=images;else setProductPhotos(state.drafts[index],images);}
function emptyDraft() { return { name: '', brand: '', category: '', aliases: '', zone: '', locationNote: '', photo: null, extraPhotos:[], shelfPhotos:[], targetId: null, variants: [{ spec: '', unit: 'piece', price: '', discount: '' }] }; }
function renderAdd() {
  $('#main').innerHTML = pageHeader('LESS TYPING. MORE SELLING.', state.drafts.length ? 'A quick check, then save.' : 'Tell us what’s on the shelf.', state.drafts.length ? 'You’re in control. Check each size, selling price and unit.' : 'Speak, type or add an image. AI suggests items and categories; you review every price.') +
    (state.drafts.length ? `<div class="review-top"><span class="pill">${icon('check')}${state.drafts.length} ${state.drafts.length === 1 ? 'item' : 'items'} to review</span><button class="text-button" data-action="discard">Discard review</button></div>${state.warnings.length ? `<div class="warning">${state.warnings.map(esc).join('<br>')}</div>` : ''}<form id="review-form"><div class="draft-grid">${state.drafts.map(draftCard).join('')}</div><div class="save-bar"><div>${icon('shield')}Nothing is saved until you confirm.</div><button class="button primary" type="submit">${icon('check')}Save ${state.drafts.length} ${state.drafts.length === 1 ? 'item' : 'items'}</button></div><p id="review-error" class="form-error" role="alert"></p></form>` :
    `<div class="entry-layout"><section class="panel entry-panel"><div class="section-heading"><span class="accent-icon">${icon('spark')}</span><div><h2>Say it your way</h2><p>Hindi, English or a little of both.</p></div><span class="pill quiet">OpenAI</span></div>
      <label class="sr-only" for="listing">Items, sizes and prices</label><textarea id="listing" ${state.busy ? 'disabled' : ''} rows="6" maxlength="12000" placeholder="Godrej tala 50mm 450, 65mm 600.\nWood screw 1 inch 90 rupay per packet.">${esc(state.listing)}</textarea><div class="listing-image-controls">${photoEditor(null,false)}</div><p class="tiny">Only these selected images are sent when you request AI review.</p><div class="entry-actions"><button class="button secondary" data-action="voice-listing" ${state.busy ? 'disabled' : ''}>${icon('mic')}Speak items</button><button class="button primary" data-action="parse" ${state.busy || !state.config.aiConfigured ? 'disabled' : ''}>${icon('spark')}${state.busy ? (state.photoBusy?'Preparing photos…':'Understanding…') : 'Review items'}${icon('arrow')}</button></div><p class="quiet-copy">${icon('shield')}Submitted text and images go to OpenAI. AI categories stay editable; missing prices stay blank.</p></section>
      <aside class="entry-tips"><p class="eyebrow">JUST LIKE AT THE COUNTER</p><h2>One sentence.<br>Many sizes.</h2><p>Include a name, sizes, selling prices and the unit you sell by.</p><div class="example-note">“Godrej tala 50mm 450,<br>65mm 600”<span>1 item · 2 sizes · ready to check</span></div><p class="tiny">A missing price stays blank. We’ll ask you to fill it in.</p></aside></div>
      <div class="manual-entry"><span>${icon('edit')}Prefer to enter it yourself?</span><button class="text-button" data-action="manual" ${state.busy ? 'disabled' : ''}>Add an item manually ${icon('arrow')}</button><small>Works offline, too.</small></div>${!state.config.aiConfigured ? '<div class="warning">No OpenAI key was found on the local server. Add it to the project’s .env.local file; manual entry works now.</div>' : ''}`);
  if(state.busy) document.querySelectorAll('#review-form input,#review-form select,#review-form button').forEach(control=>control.disabled=true);
}
function draftCard(d, i) {
  const found = duplicate(d, state.data.products);
  return `<section class="panel draft-card" data-draft="${i}"><div class="draft-heading"><span class="pill ${d.targetId ? 'update' : ''}">${d.targetId ? 'Update existing' : 'New item'} · ${i + 1}</span><button type="button" class="icon-button" data-remove-draft="${i}" aria-label="Remove item ${i + 1} from review">${icon('close')}</button></div>
    <label>Item name <span>*</span><input name="name" data-field="name" value="${esc(d.name)}" required maxlength="200" placeholder="e.g. Padlock"></label><div class="form-row"><label>Brand<input data-field="brand" value="${esc(d.brand)}" maxlength="100" placeholder="e.g. Godrej"></label><label>Category<input data-field="category" value="${esc(d.category)}" maxlength="100" placeholder="e.g. Locks" list="categories-${i}"></label></div>
    ${state.data.products.length?`<button type="button" class="text-button" data-entry-category="${i}">Use an existing category</button>`:''}
    <datalist id="categories-${i}">${[...new Set(state.data.products.map(p => p.category))].map(c => `<option value="${esc(c)}">`).join('')}</datalist>
    ${d.targetId ? `<div class="duplicate-note">${icon('history')}Matching sizes will be updated.<button class="text-button" type="button" data-separate="${i}">Save as new instead</button></div>` : found ? `<div class="duplicate-note">${esc(found.name)} already exists.<button type="button" class="text-button" data-merge="${i}" data-target="${esc(found.id)}">Update existing item</button></div>` : ''}
    <button type="button" class="text-button" data-categorize="${i}" ${state.busy||!state.config.aiConfigured?'disabled':''}>${icon('spark')}Suggest category & aliases</button>
    <p class="field-heading">SIZES & SELLING PRICES</p>${d.variants.map((v, vi) => `${v.sourceReferenceId?sourceNote(referenceState.catalog,v.sourceReferenceId):''}<div class="variant-editor" data-variant="${vi}"><label>Size / spec<input data-variant-field="spec" value="${esc(v.spec)}" placeholder="50 mm" maxlength="1000"></label><label>Price (₹) <span>*</span><input data-variant-field="price" value="${esc(v.price)}" inputmode="decimal" type="number" min="0" max="100000000" step="0.01" required placeholder="450"></label><label>Per unit<select data-variant-field="unit" aria-label="Selling unit" required>${!v.unit?'<option value="" selected>Choose selling unit</option>':''}${UNITS.map(u => `<option ${u === v.unit ? 'selected' : ''}>${u}</option>`).join('')}</select></label>${d.variants.length > 1 ? `<button type="button" class="icon-button remove-size" data-remove-variant="${vi}" data-index="${i}" aria-label="Remove size ${vi + 1}">${icon('close')}</button>` : ''}</div><div class="discount-editor"><label>Discount (%) <small>Optional · applied once</small><input data-discount="${vi}" data-index="${i}" type="number" min="0" max="100" step="0.01" inputmode="decimal" placeholder="e.g. 25" value="${esc(v.discount||'')}"></label><output data-net="${i}-${vi}">${discountedPaise(v.price,v.discount)===null?'Enter a valid price and discount':`Selling price: ${currency(discountedPaise(v.price,v.discount))} per ${esc(v.unit)}`}</output></div>`).join('')}
    <button type="button" class="text-button add-size" data-add-variant="${i}">${icon('plus')}Add another size</button><label>Other names <small>Separate with commas</small><input data-field="aliases" value="${esc(d.aliases)}" placeholder="tala, ताला, lock" maxlength="2000"></label>
    ${state.listingImages.length?`<button type="button" class="text-button" data-copy-ai="${i}" ${state.busy||new Set([...productPhotos(d),...state.listingImages]).size>6?'disabled':''}>Use AI images as product photos</button>`:''}
    ${photoEditor(i,false)}${photoEditor(i,true)}<p class="tiny">Shelf photos stay local and are not sent with category suggestions.</p>
    <details ${d.zone || d.locationNote ? 'open' : ''}><summary>Optional shelf location ${icon('chevron')}</summary><div class="form-row"><label>Zone<input data-field="zone" value="${esc(d.zone)}" placeholder="Aisle A"></label><label>Location note<input data-field="locationNote" value="${esc(d.locationNote)}" placeholder="Second shelf"></label></div></details></section>`;
}
function renderHistory() {
  const rows = state.data.history.slice().reverse();
  $('#main').innerHTML = pageHeader('A LITTLE CONTEXT FOR EVERY PRICE', 'Keep up with changing rates.', 'Every saved price change leaves a clear trail.') +
    (rows.length ? `<section class="panel table-panel"><table><thead><tr><th>Item & size</th><th>Previous price</th><th>New price</th><th>Changed</th></tr></thead><tbody>${rows.map(h => {
      const p = state.data.products.find(p => p.variants.some(v => v.id === h.variantId)); const v = p?.variants.find(v => v.id === h.variantId);
      return `<tr><td><strong data-preserve-text>${esc(p?.name || 'Removed item')}</strong><small>${esc(v?.spec || 'Standard')}</small></td><td>${currency(h.oldPrice)}</td><td class="new-price">${currency(h.newPrice)}</td><td>${date(h.changedAt)}</td></tr>`;
    }).join('')}</tbody></table></section>` : `<div class="empty-state panel">${icon('history')}<h2>No price changes yet</h2><p>Tap a price in your catalog to update it. The old and new prices will appear here.</p><button class="button secondary" data-page="catalog">Back to catalog ${icon('arrow')}</button></div>`);
}
function renderMisses() {
  const misses = state.data.misses.filter(m => !m.resolvedItemId).slice().reverse();
  $('#main').innerHTML = pageHeader('A CATALOG THAT LEARNS YOUR WORDS', 'Every name has a place.', 'Connect an unsuccessful search to the right item. Next time, it will work offline.') +
    (misses.length ? `<div class="miss-grid">${misses.map(m => `<section class="panel"><span class="category-label">${date(m.searchedAt)}</span><h2 data-preserve-text>“${esc(m.query)}”</h2><p class="quiet-copy">Which item was your customer looking for?</p><label class="sr-only" for="miss-${esc(m.id)}">Choose item for ${esc(m.query)}</label><select id="miss-${esc(m.id)}" data-miss-select="${esc(m.id)}"><option value="">Choose an item…</option>${state.data.products.map(p => `<option data-preserve-text value="${esc(p.id)}">${esc(p.name)}${p.brand ? ' · ' + esc(p.brand) : ''}</option>`).join('')}</select><button class="button secondary" data-learn="${esc(m.id)}">${icon('plus')}Save as another name</button></section>`).join('')}</div>` : `<div class="empty-state panel">${icon('check')}<h2>You’re all caught up.</h2><p>Searches submitted with no local match appear here. A new alias is all it takes to help your catalog learn.</p><button class="button secondary" data-page="catalog">Back to catalog ${icon('arrow')}</button></div>`);
}
function renderSettings() {
  $('#main').innerHTML = pageHeader('MADE FOR YOUR COUNTER', 'A few things, just your way.', 'Set your price labels, keep a backup and make this preview your own.') +
    `<div class="settings-grid"><section class="panel"><h2>App language</h2>${languageSelector()}<p>Display language only; your saved names and original source stay unchanged.</p></section><section class="panel"><div class="section-heading"><span class="accent-icon">${icon('key')}</span><h2>OpenAI connection</h2></div><span class="connection ${state.config.aiConfigured ? 'ready' : ''}"><i></i>${state.config.aiConfigured ? 'Your local key is ready' : 'No local key found'}</span><p>Uses the key in your project’s <code>.env.local</code> file. The key stays on the localhost server and is never sent to your browser.</p><div class="settings-fact"><span>Model</span><strong>${esc(state.config.model || 'gpt-4.1-mini')}</strong></div><div class="settings-fact"><span>What is sent</span><strong>Submitted text, images & category names</strong></div><p class="tiny">Natural entry needs an internet connection. Catalog search and manual editing stay local.</p></section>
    <section class="panel"><div class="section-heading"><span class="accent-icon">${icon('settings')}</span><h2>At your counter</h2></div><label>Price convention<select id="tax">${['GST not specified', 'incl. GST', 'excl. GST'].map(t => `<option ${t === state.data.settings.tax ? 'selected' : ''}>${t}</option>`).join('')}</select></label><p class="tiny">Labels your prices. It does not calculate or add tax.</p><label>Voice language<select id="voice-language"><option value="hi-IN" ${state.data.settings.language === 'hi-IN' ? 'selected' : ''}>हिन्दी / Hinglish</option><option value="en-IN" ${state.data.settings.language === 'en-IN' ? 'selected' : ''}>English (India)</option></select></label><p class="tiny">Voice support depends on your browser’s speech service.</p></section>
    <section class="panel"><div class="section-heading"><span class="accent-icon">${icon('shield')}</span><h2>Keep a copy safe</h2></div><p>This web preview is stored in this browser, separately from the Android app. Export a backup before clearing browser data.</p><button class="button primary" data-action="export">${icon('download')}Export preview backup</button><label class="button secondary photo-label">${icon('history')}Restore preview backup<input class="sr-only" id="restore-backup" type="file" accept="application/json,.json"></label><p class="tiny">JSON format for this web preview. Android uses its own ZIP backups. API keys are never exported.</p></section>
    <section class="panel"><div class="section-heading"><span class="accent-icon">${icon('shop')}</span><h2>A fresh shelf</h2></div><p>Sample items make it easy to look around. Clear them to start your own catalog. Any items you add yourself will stay.</p><button class="button secondary" data-action="clear-demo">Clear sample items</button><p class="tiny">${state.data.products.filter(p => p.demo).length} sample items · ${state.data.products.filter(p => !p.demo).length} items added by you</p></section></div>`;
}

function openDialog(content, onSubmit) {
  const dialog = $('#dialog'); dialog.innerHTML = content; dialog.showModal();
  $('[data-dialog-close]', dialog)?.addEventListener('click', () => dialog.close());
  $('form', dialog)?.addEventListener('submit', async event => { event.preventDefault(); try { await onSubmit(new FormData(event.target)); } catch (error) { const target = $('.dialog-error', dialog); if (target) target.textContent = error.message; else toast(error.message); } });
}
let categoryPicker = null;
function chooseCategory({choices, selected, onSelect, reference=false, allowAll=true}) {
  categoryPicker={choices,selected,onSelect,reference,allowAll};
  openDialog(`<div class="dialog-heading"><h2>${translate(reference?'Choose a product family':'Choose a category')}</h2><button type="button" data-dialog-close class="icon-button" aria-label="Close">${icon('close')}</button></div><label>Find a category<input id="category-query" type="search" autocomplete="off"></label><div id="category-choices" class="category-choices"></div>`);
  renderCategoryChoices('');$('#category-query').focus();
}
function renderCategoryChoices(query) {
  const picker=categoryPicker, choices=matchingCategories(picker.choices,query);
  $('#category-choices').innerHTML=(picker.allowAll?`<button type="button" class="category-choice" data-pick-all><span>${translate(picker.reference?'All product families':'All items')}</span><small>${picker.choices.reduce((n,c)=>n+c.count,0)}</small></button>`:'')+choices.map(c=>`<button type="button" class="category-choice ${picker.selected===c.value?'selected':''}" data-pick-category="${esc(c.value)}" aria-pressed="${picker.selected===c.value}"><span data-preserve-text>${esc(c.value?(picker.reference?productText(c.value):c.value):translate('Uncategorized'))}</span><small>${c.count} ${translate(picker.reference?'variants':'items')}</small></button>`).join('')+(!choices.length?'<p>No categories match. Try another name.</p>':'');
}
function resetCategory() {state.category=null;state.resultLimit=36;renderResults();}
function confirmDialog(title, text, action, label = 'Confirm') {
  openDialog(`<form><div class="dialog-heading"><h2>${esc(title)}</h2><button type="button" data-dialog-close class="icon-button" aria-label="Close">${icon('close')}</button></div><p>${esc(text)}</p><div class="dialog-actions"><button type="button" data-action="close-dialog" class="button secondary">Cancel</button><button class="button primary">${esc(label)}</button></div><p class="dialog-error" role="alert"></p></form>`, async () => { await action(); $('#dialog').close(); shell(); });
}
function readOnlyAlbum(product,shelf) {
  const images=shelf?(product.shelfPhotos||[]):productPhotos(product);
  if(!images.length)return '';
  return `<section class="photo-album"><h3>${translate(shelf?'Shelf photos':'Product photos')}</h3><div class="photo-album-strip">${images.map((image,i)=>`<button type="button" class="photo-open" data-open-photo="${i}" data-photo-product="${esc(product.id)}" data-photo-shelf="${shelf}"><img src="${esc(image)}" alt="${translate(shelf?'Shelf photos':'Product photos')} ${i+1}"></button>`).join('')}</div></section>`;
}
let viewedProductId=null;
function viewProduct(id) {
  const product=state.data.products.find(p=>p.id===id);if(!product)return;
  viewedProductId=id;
  const source=linkedReferenceProduct(referenceState.catalog,product);
  const displayName=product.name===`${source?.category.split(' · ')[0]} ${source?.name}`?productText(product.name):product.name;
  const related=relatedProducts(product,state.data.products);
  openDialog(`<div class="dialog-heading product-details-heading"><div class="product-view-thumb">${illustration(product)}</div><div><p class="eyebrow">Product details</p><h2 data-preserve-text>${esc(displayName)}</h2>${displayName!==product.name?`<small data-preserve-text>${esc(product.name)}</small>`:''}<p data-preserve-text>${esc([product.brand,product.category].filter(Boolean).join(' · '))}</p></div><button type="button" data-dialog-close class="icon-button" aria-label="Close">${icon('close')}</button></div><div class="product-view-body"><p class="view-facts"><span data-preserve-text>${esc([product.zone,product.locationNote].filter(Boolean).join(' · '))}</span><span>${esc(state.data.settings.tax)}</span></p><label class="view-discount-control"><span>Discount (%)</span><input id="view-discount" type="number" inputmode="decimal" min="0" max="100" step="0.01" placeholder="e.g. 10"></label><p class="tiny">Preview only. Saved prices stay unchanged.</p><div id="view-prices"></div>${readOnlyAlbum(product,false)}${readOnlyAlbum(product,true)}${product.category?`<section class="related-products"><h3>Related products</h3><p class="tiny">Suggested from the same category; matching brands appear first.</p><button class="text-button" data-browse-related="${esc(product.id)}">Browse this category</button>${related.map(other=>`<button class="category-choice" data-view-product="${esc(other.id)}"><span data-preserve-text>${esc(other.name)}</span><small data-preserve-text>${esc(other.brand)}</small></button>`).join('')}</section>`:''}</div><div class="dialog-actions"><button type="button" class="button secondary" data-view-edit-item="${esc(product.id)}">Edit item</button><button type="button" class="button primary" data-action="close-dialog">Close</button></div>`);
  renderViewPrices();
}
function renderViewPrices() {
  const product=state.data.products.find(p=>p.id===viewedProductId);if(!product||!$('#view-prices'))return;
  const discount=$('#view-discount').value;
  const valid=discountedPaise('1',discount)!==null;
  $('#view-prices').innerHTML=(!valid?'<p class="form-error">Enter a discount from 0 to 100, with up to two decimal places.</p>':'')+product.variants.map(variant=>{
    const net=valid?discountedPaise(decimal(variant.pricePaise),discount):null;
    return `<section class="view-variant"><strong data-preserve-text>${esc(variant.spec||translate('Standard'))}</strong><p class="view-base" data-preserve-text>${currency(variant.pricePaise)} / ${esc(translate(variant.unit))}</p>${discount!==''&&net!==null?`<p class="view-net"><span>After discount</span>: <strong data-preserve-text>${currency(net)} / ${esc(translate(variant.unit))}</strong></p>`:''}<div class="view-price-actions"><small>${translate(`Updated ${date(variant.updatedAt)}`)}</small><button class="text-button" data-price="${esc(variant.id)}" data-item="${esc(product.id)}">Edit price</button></div></section>`;
  }).join('');
}
function priceDialog(itemId, variantId) {
  const product = state.data.products.find(p => p.id === itemId); const variant = product?.variants.find(v => v.id === variantId); if (!variant) return;
  const history = state.data.history.filter(h => h.variantId === variantId).slice().reverse();
  openDialog(`<form><div class="dialog-heading"><div><p class="eyebrow">QUICK PRICE UPDATE</p><h2 data-preserve-text>${esc(product.name)}</h2></div><button type="button" data-dialog-close class="icon-button" aria-label="Close">${icon('close')}</button></div><p>${esc(variant.spec || 'Standard')} · per ${esc(variant.unit)}</p><label>Selling price (₹)<input id="quick-price" name="price" type="number" min="0" max="100000000" step="0.01" inputmode="decimal" required value="${decimal(variant.pricePaise)}"></label><label>Discount (%) — optional<input id="quick-discount" name="discount" type="number" min="0" max="100" step="0.01" inputmode="decimal" placeholder="e.g. 25"></label><output id="quick-net" class="net-price"></output><p class="tiny">Price last updated ${date(variant.updatedAt)}. Discount applies once to the entered price. Your previous selling price stays in history.</p>${history.length ? `<details><summary>Price history ${icon('chevron')}</summary><div class="mini-history">${history.map(h => `<p>${currency(h.oldPrice)} → <strong>${currency(h.newPrice)}</strong><small>${date(h.changedAt)}</small></p>`).join('')}</div></details>` : ''}<p class="dialog-error" role="alert"></p><div class="dialog-actions"><button class="button secondary" type="button" data-action="close-dialog">Cancel</button><button class="button primary">${icon('check')}Save price</button></div></form>`, async form => {
    const amount = discountedPaise(form.get('price'),form.get('discount')); if (amount === null) throw new Error('Enter a valid price and a discount from 0 to 100%.');
    await changeData(data => { const p = data.products.find(p => p.id === itemId); const v = p.variants.find(v => v.id === variantId);
      if (v.pricePaise !== amount) { data.history.push({ variantId, oldPrice: v.pricePaise, newPrice: amount, changedAt: Date.now() }); v.pricePaise = amount; v.updatedAt = p.updatedAt = Date.now(); } });
    $('#dialog').close(); shell(); toast('Price updated. Previous price saved in history.');
  });
  updateQuickNet();
  $('#quick-price').select();
}
function updateQuickNet() {
  const output=$('#quick-net');if(!output)return;
  const value=discountedPaise($('#quick-price').value,$('#quick-discount').value);
  output.textContent=value===null?'Enter a valid price and discount':`Selling price to save: ${currency(value)}`;
}
async function beginEdit(id) {
  const p = state.data.products.find(p => p.id === id); if (!p) return;
  state.drafts = [{ ...structuredClone(p), targetId: p.id, aliases: p.aliases.join(', '), variants: p.variants.map(v => ({ spec: v.spec, unit: v.unit, price: decimal(v.pricePaise), sourceReferenceId:v.sourceReferenceId })) }];
  if(!state.drafts[0].photo)state.drafts[0].photo=await referencePhotoData(linkedPicture(referenceState.catalog,p));
  state.page = 'add'; state.fullEdit = true; state.warnings = []; shell(); window.scrollTo(0, 0);
}
async function saveDrafts() {
  if(state.busy) throw new Error('Wait for the current suggestion to finish before saving.');
  for (const draft of state.drafts) { const error = validateDraft(draft); if (error) throw new Error(error); }
  if (new Set(state.drafts.map(d => normalize(d.name) + '|' + normalize(d.brand))).size !== state.drafts.length) throw new Error('Combine duplicate product cards into one item before saving.');
  const count = state.drafts.length;
  await changeData(data => {
    for (const d of state.drafts) {
      const existing = d.targetId ? data.products.find(p => p.id === d.targetId) : null;
      if (d.targetId && !existing) throw new Error('The original item no longer exists. Reopen the item editor.');
      const now = Date.now();
      const variants = d.variants.map(v => {
        const linked=v.sourceReferenceId?data.products.flatMap(p=>p.variants.map(v=>({...v,itemId:p.id}))).find(old=>old.sourceReferenceId===v.sourceReferenceId):null;
        if(linked&&linked.itemId!==existing?.id)throw new Error('This reference variant is already carried. Edit its existing shop item.');
        const old = linked || existing?.variants.find(old => normalize(old.spec) === normalize(v.spec) && old.unit === v.unit);
        if(old?.sourceReferenceId&&v.sourceReferenceId&&old.sourceReferenceId!==v.sourceReferenceId)throw new Error('These source variants need distinct specifications.');
        const amount = discountedPaise(v.price, v.discount); const id = old?.id || uid();
        if (old && old.pricePaise !== amount) data.history.push({ variantId: id, oldPrice: old.pricePaise, newPrice: amount, changedAt: now });
        return { id, spec: v.spec.trim(), unit: v.unit, pricePaise: amount, updatedAt: old?.pricePaise === amount ? old.updatedAt : now, sourceReferenceId:v.sourceReferenceId||old?.sourceReferenceId||null };
      });
      if (existing && !state.fullEdit) variants.push(...existing.variants.filter(v => !variants.some(n => normalize(n.spec) === normalize(v.spec) && n.unit === v.unit)));
      const aliases = [...new Set([...d.aliases.split(',').map(a => a.trim()).filter(Boolean), ...(!state.fullEdit ? existing?.aliases || [] : [])])];
      const saved = { id: existing?.id || uid(), name: d.name.trim(), brand: d.brand.trim(), category: d.category.trim(), aliases, photo: state.fullEdit?d.photo:(d.photo||existing?.photo||null), extraPhotos:(state.fullEdit||productPhotos(d).length)?(d.extraPhotos||[]):(existing?.extraPhotos||[]), shelfPhotos:(state.fullEdit||d.shelfPhotos?.length)?(d.shelfPhotos||[]):(existing?.shelfPhotos||[]),
        zone: d.zone.trim(), locationNote: d.locationNote.trim(), variants, updatedAt: now, demo: existing?.demo || false };
      if (existing) data.products[data.products.findIndex(p => p.id === existing.id)] = saved; else data.products.unshift(saved);
    }
  });
  state.drafts = []; state.listing = ''; state.listingImages = []; state.query = ''; state.category = null; state.rewrites = null; state.page = 'catalog'; state.fullEdit = false;
  referenceState.selected.clear();
  shell(); window.scrollTo(0, 0); toast(`${count} ${count === 1 ? 'item' : 'items'} saved in this browser.`);
}
async function parseListing() {
  if (state.busy) return;
  if (!state.listing.trim() && !state.listingImages.length) { toast('Type, speak or add an image first.'); $('#listing')?.focus(); return; }
  state.busy = true; renderAdd();
  try {
    const parsed = await api('/api/parse', { text: state.listing, images: state.listingImages, categories: [...new Set(state.data.products.map(p=>p.category))] });
    state.drafts = parsed.items.map(item => {
      const d = { ...emptyDraft(), ...item, photo: null, aliases: item.aliases.join(', '), variants: item.variants.map(v => ({ spec: v.spec, unit: v.unit, price: v.sell_price == null ? '' : String(v.sell_price) })) };
      const found = duplicate(d, state.data.products);
      return found ? { ...d, targetId: found.id, zone: found.zone, locationNote: found.locationNote, photo: found.photo, extraPhotos:found.extraPhotos||[], shelfPhotos:found.shelfPhotos||[], aliases: [...new Set([...found.aliases, ...item.aliases])].join(', ') } : d;
    });
    state.warnings = parsed.warnings; state.fullEdit = false;
    if (!state.drafts.length) toast(parsed.warnings[0] || 'No items found. Try a name, size and price.');
  } finally { state.busy = false; if (state.page === 'add') renderAdd(); }
}
async function smartSearch() {
  if (state.busy) return;
  const query = state.query.trim(); if (!query) { $('#search')?.focus(); return; }
  const local = search(state.data.products, searchQuery(query));
  if (!local.length) await changeData(data => { if (!data.misses.some(m => m.query === query && !m.resolvedItemId)) data.misses.push({ id: uid(), query, searchedAt: Date.now(), resolvedItemId: null }); });
  if (local.length && local[0].score >= .72) { toast('Found locally — no AI needed.'); return; }
  if (!state.config.aiConfigured) { toast('No local match. Add an alias from Missed searches, or try another name.'); return; }
  state.busy = true;
  const button = $('[data-action="smart"]'); if (button) { button.disabled = true; button.innerHTML = `${icon('spark')}Understanding…`; }
  try {
    const parsed = await api('/api/search', { text: query, categories: [...new Set(state.data.products.map(p => p.category))] });
    if (state.query.trim() === query) { state.rewrites = { query: state.query, phrases: parsed.queries }; if (state.page === 'catalog') renderResults(); toast('Smart search checked your catalog.'); }
  } finally { state.busy = false; const button = $('[data-action="smart"]'); if (button) { button.disabled = false; button.innerHTML = `${icon('spark')}Smart search`; } }
}
function voice(target) {
  const Recognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (!Recognition) { toast('Voice input is not supported in this browser. You can type, or try Chrome.'); return; }
  const recognition = new Recognition(); recognition.lang = state.data.settings.language; recognition.interimResults = false;
  recognition.onresult = event => {
    const text = event.results[0][0].transcript;
    if (target === 'search') { state.query = text; state.page = 'catalog'; shell(); smartSearch().catch(e => toast(e.message)); }
    else { state.listing = [state.listing, text].filter(Boolean).join('. '); if (state.page === 'add') renderAdd(); }
  };
  recognition.onerror = () => toast('Voice input could not start. Check microphone access or type your items.');
  recognition.onend = () => document.body.classList.remove('listening');
  recognition.start(); document.body.classList.add('listening'); toast('Listening… speak naturally.');
}
async function uploadPhotos(files,index,shelf) {
  if(state.busy||!files.length)return;
  if(albumFor(index,shelf).length+files.length>MAX_PHOTOS)throw new Error('Choose up to 6 photos per album.');
  state.busy=true;state.photoBusy=true;renderAdd();
  try {for(const file of files) {const image=await cropImage(file);if(!image)break;assignAlbum(index,shelf,[...albumFor(index,shelf),image]);}}
  finally {state.busy=false;state.photoBusy=false;renderAdd();}
}
async function categorize(index) {
  if (state.busy) return;
  const draft=state.drafts[index];
  if(!draft.name.trim()&&!draft.photo) throw new Error('Enter an item name or add a photo first.');
  state.busy=true;renderAdd();
  const button=$(`[data-categorize="${index}"]`);if(button){button.disabled=true;button.textContent='Suggesting category…';}
  try {
    const suggestion=await api('/api/categorize',{text:JSON.stringify({name:draft.name,brand:draft.brand}),images:productPhotos(draft),categories:[...new Set(state.data.products.map(p=>p.category))]});
    draft.category=suggestion.category;draft.aliases=[...new Set([...draft.aliases.split(',').map(a=>a.trim()),...suggestion.aliases])].filter(Boolean).join(', ');
    toast(suggestion.reason||'Category suggested. Review it before saving.');
  } finally {state.busy=false;renderAdd();}
}
function exportBackup() {
  const content = { format: 'dukaan-web-preview', version: 3, exportedAt: Date.now(), ...state.data };
  const url = URL.createObjectURL(new Blob([JSON.stringify(content, null, 2)], { type: 'application/json' }));
  const a = document.createElement('a'); a.href = url; a.download = `dukaan-web-${new Date().toISOString().slice(0, 10)}.json`; a.click(); setTimeout(() => URL.revokeObjectURL(url), 1000);
  toast('Preview backup exported. Keep it somewhere safe.');
}
async function restore(file) {
  if (!file) return; if (file.size > 200_000_000) throw new Error('Choose a backup smaller than 200 MB.');
  const backup = validateBackup(JSON.parse(await file.text()));
  confirmDialog('Replace this browser’s catalog?', `Restore ${backup.products.length} items from this web preview backup. Export your current catalog first if you need to keep it. Your Android catalog and local API key will stay unchanged.`, async () => {
    const next = { products: backup.products, history: backup.history, misses: backup.misses, settings: { tax: ['GST not specified', 'incl. GST', 'excl. GST'].includes(backup.settings?.tax) ? backup.settings.tax : 'GST not specified', language: backup.settings?.language === 'en-IN' ? 'en-IN' : 'hi-IN', uiLanguage:['en','hi','hinglish'].includes(backup.settings?.uiLanguage)?backup.settings.uiLanguage:'en' } };
    await writeData(next); state.data = next; state.query = ''; state.category = null; state.rewrites = null; state.drafts = []; toast('Web preview backup restored.');
  }, 'Restore backup');
}

// Event delegation keeps dynamically rendered cards and review forms interactive.
document.addEventListener('click', event => {
  const button = event.target.closest('button, [data-page], [data-view-product]'); if (!button || button.disabled) return;
  const run = async () => {
    if (button.hasAttribute('data-reference-clear')) {referenceState.selected.clear();renderReference();return;}
    if (button.hasAttribute('data-reference-more')) {referenceState.limit+=20;renderReference();return;}
    if (button.hasAttribute('data-reference-review')) {
      if(state.drafts.length)throw new Error('Save or discard your current review before activating reference variants.');
      const drafts=referenceDrafts(referenceState.catalog,referenceState.selected,state.data.products);
      if(!drafts.length)throw new Error('Select variants that you carry first.');
      state.busy=true;button.disabled=true;
      try {for(const draft of drafts)if(!draft.photo)draft.photo=await referencePhotoData(draft.referenceImageAsset);}finally {state.busy=false;button.disabled=false;}
      state.drafts=drafts;state.fullEdit=false;state.warnings=['You are confirming that you carry these selected variants. Check each specification, selling unit, selling price and shop tax label. Source list rates stay separate. No stock quantity is recorded.'];state.page='add';shell();window.scrollTo(0,0);return;
    }
    if (button.dataset.page) { if (state.busy) return toast('Wait for the current request to finish.'); state.page = button.dataset.page; shell(); window.scrollTo(0, 0); return; }
    if (button.hasAttribute('data-browse-categories')) return chooseCategory({choices:categoryChoices(state.data.products),selected:state.category,onSelect:value=>{state.category=value;state.resultLimit=36;renderResults();}});
    if (button.hasAttribute('data-reference-family')) return chooseCategory({choices:categoryChoices(referenceState.catalog.products,p=>p.variants.length),selected:referenceState.category||null,reference:true,onSelect:value=>{referenceState.category=value||'';referenceState.limit=20;renderReference();}});
    if (button.hasAttribute('data-entry-category')) {const i=Number(button.dataset.entryCategory);return chooseCategory({choices:categoryChoices(state.data.products),selected:state.drafts[i].category,allowAll:false,onSelect:value=>{state.drafts[i].category=value;renderAdd();}});}
    if (button.hasAttribute('data-pick-category')||button.hasAttribute('data-pick-all')) {const value=button.hasAttribute('data-pick-all')?null:button.dataset.pickCategory;categoryPicker.onSelect(value);$('#dialog').close();return;}
    if (button.hasAttribute('data-all-categories')) return resetCategory();
    if (button.hasAttribute('data-more-items')) {state.resultLimit+=36;renderResults();return;}
    if (button.dataset.layout) { state.layout = button.dataset.layout; renderResults(); return; }
    if (button.hasAttribute('data-categorize')) return categorize(Number(button.dataset.categorize));
    if(button.hasAttribute('data-copy-ai')) {if(state.busy)return;const draft=state.drafts[Number(button.dataset.copyAi)];setProductPhotos(draft,[...productPhotos(draft),...state.listingImages]);renderAdd();return;}
    if(button.hasAttribute('data-album-action')) {
      if(state.busy)return;
      const index=button.dataset.albumIndex==='ai'?null:Number(button.dataset.albumIndex),shelf=button.dataset.albumShelf==='true',n=Number(button.dataset.photoIndex),album=[...albumFor(index,shelf)];
      if(button.dataset.albumAction==='remove')album.splice(n,1);
      if(button.dataset.albumAction==='cover')album.unshift(...album.splice(n,1));
      if(button.dataset.albumAction==='crop') {const image=await cropImage(album[n]);if(image)album[n]=image;}
      assignAlbum(index,shelf,album);renderAdd();return;
    }
    if(button.hasAttribute('data-open-photo')) {
      const product=state.data.products.find(p=>p.id===button.dataset.photoProduct),shelf=button.dataset.photoShelf==='true',images=shelf?product.shelfPhotos:productPhotos(product),n=Number(button.dataset.openPhoto);
      openDialog(`<div class="dialog-heading"><h2>${translate(shelf?'Shelf photos':'Product photos')} ${n+1}/${images.length}</h2><button type="button" data-dialog-close class="icon-button" aria-label="Close">${icon('close')}</button></div><img class="photo-full" src="${esc(images[n])}" alt="${esc(product.name)}"><button class="button secondary" data-view-product="${esc(product.id)}">Back to product</button>`);return;
    }
    if (button.hasAttribute('data-view-product')) return viewProduct(button.dataset.viewProduct);
    if (button.hasAttribute('data-view-edit-item')) {$('#dialog').close();return beginEdit(button.dataset.viewEditItem);}
    if (button.hasAttribute('data-browse-related')) {const product=state.data.products.find(p=>p.id===button.dataset.browseRelated);state.category=product.category;state.query='';state.rewrites=null;state.resultLimit=36;$('#dialog').close();renderCatalog();return;}
    if (button.dataset.edit) return beginEdit(button.dataset.edit);
    if (button.dataset.price) return priceDialog(button.dataset.item, button.dataset.price);
    if(state.busy && (button.hasAttribute('data-remove-draft')||button.hasAttribute('data-separate')||button.hasAttribute('data-merge'))) return toast('Wait for the suggestion to finish.');
    if (button.hasAttribute('data-remove-draft')) { state.drafts.splice(Number(button.dataset.removeDraft), 1); return renderAdd(); }
    if (button.hasAttribute('data-add-variant')) { state.drafts[Number(button.dataset.addVariant)].variants.push({ spec: '', unit: 'piece', price: '' }); return renderAdd(); }
    if (button.hasAttribute('data-remove-variant')) { state.drafts[Number(button.dataset.index)].variants.splice(Number(button.dataset.removeVariant), 1); return renderAdd(); }
    if (button.hasAttribute('data-separate')) { state.drafts[Number(button.dataset.separate)].targetId = null; return renderAdd(); }
    if (button.hasAttribute('data-merge')) { const d = state.drafts[Number(button.dataset.merge)], p = state.data.products.find(p => p.id === button.dataset.target); Object.assign(d, { targetId: p.id, photo: p.photo, extraPhotos:p.extraPhotos||[], shelfPhotos:p.shelfPhotos||[], zone: p.zone, locationNote: p.locationNote }); return renderAdd(); }
    if (button.dataset.learn) {
      const miss = state.data.misses.find(m => m.id === button.dataset.learn); const target = $(`[data-miss-select="${miss.id}"]`).value;
      if (!target) return toast('Choose the right item first.');
      await changeData(data => { const p = data.products.find(p => p.id === target); if (!p.aliases.includes(miss.query)) p.aliases.push(miss.query); data.misses.find(m => m.id === miss.id).resolvedItemId = target; });
      renderMisses(); return toast('Alias saved. The next search will find it offline.');
    }
    switch (button.dataset.action) {
      case 'new': state.page = 'add'; shell(); window.scrollTo(0, 0); break;
      case 'manual': state.drafts = [emptyDraft()]; state.fullEdit = false; state.warnings = []; renderAdd(); break;
      case 'discard': if(state.busy)return toast('Wait for the current request to finish.'); state.drafts = []; state.warnings = []; renderAdd(); break;
      case 'parse': await parseListing(); break;
      case 'smart': await smartSearch(); break;
      case 'voice-search': voice('search'); break;
      case 'voice-listing': voice('listing'); break;
      case 'close-dialog': $('#dialog').close(); break;
      case 'export': exportBackup(); break;
      case 'clear-demo': confirmDialog('Make room for your own items?', 'Remove the sample items and their sample price history. Items you added yourself will stay.', async () => { await changeData(data => { const variantIds = new Set(data.products.filter(p => p.demo).flatMap(p => p.variants.map(v => v.id))); data.products = data.products.filter(p => !p.demo); data.history = data.history.filter(h => !variantIds.has(h.variantId)); }); state.category = null; toast('Sample items cleared. Your catalog is ready.'); }, 'Clear sample items'); break;
    }
  };
  run().catch(error => toast(error.message));
});
document.addEventListener('input', event => {
  const input = event.target;
  if(input.id==='view-discount') {renderViewPrices();return;}
  if(input.id==='category-query') {renderCategoryChoices(input.value);return;}
  if(input.id==='reference-query') {
    referenceState.query=input.value;referenceState.limit=20;
    if(!event.isComposing)scheduleReferenceSearch();return;
  }
  if (['quick-price','quick-discount'].includes(input.id)) {updateQuickNet();return;}
  if (input.id === 'search') { state.query = input.value; state.rewrites = null; state.resultLimit=36; renderResults(); return; }
  if (input.id === 'listing') { state.listing = input.value; return; }
  const card = input.closest('[data-draft]'); if (!card) return;
  const draft = state.drafts[Number(card.dataset.draft)];
  if (input.dataset.field) draft[input.dataset.field] = input.value;
  if (input.dataset.variantField) draft.variants[Number(input.closest('[data-variant]').dataset.variant)][input.dataset.variantField] = input.value;
  if(input.hasAttribute('data-discount')) draft.variants[Number(input.dataset.discount)].discount=input.value;
  draft.variants.forEach((v,vi)=>{const output=$(`[data-net="${Number(card.dataset.draft)}-${vi}"]`);if(output){const amount=discountedPaise(v.price,v.discount);output.textContent=amount===null?'Enter a valid price and discount':`Selling price: ${currency(amount)} per ${v.unit}`;}});
});
document.addEventListener('compositionend',event=>{if(event.target.id==='reference-query')scheduleReferenceSearch();});
document.addEventListener('focusout', event => {
  const input = event.target;
  if (!['name', 'brand'].includes(input.dataset?.field)) return;
  const card = input.closest('[data-draft]'); if (!card) return;
  const i = Number(card.dataset.draft), draft = state.drafts[i];
  if (draft.targetId) return;
  card.querySelector('.duplicate-note')?.remove();
  const found = duplicate(draft, state.data.products);
  if (found) {
    const note = document.createElement('div'); note.className = 'duplicate-note';
    note.innerHTML = `${esc(found.name)} already exists.<button type="button" class="text-button" data-merge="${i}" data-target="${esc(found.id)}">Update existing item</button>`;
    card.querySelector('.field-heading').before(note);
  }
});
document.addEventListener('change', event => {
  const input = event.target;
  if(input.hasAttribute('data-ui-language')) {
    const value=input.value;if(!['en','hi','hinglish'].includes(value))return;
    changeData(data=>{data.settings.uiLanguage=value;}).then(()=>shell()).catch(error=>toast(error.message));return;
  }
  if(input.hasAttribute('data-reference-select')) {
    const id=input.dataset.referenceSelect;if(input.checked)referenceState.selected.add(id);else referenceState.selected.delete(id);
    // Keep expanded table cards open while selecting multiple sizes.
    const open=[...document.querySelectorAll('.reference-group')].map(e=>e.open);renderReference();document.querySelectorAll('.reference-group').forEach((e,i)=>e.open=open[i]);return;
  }
  if(input.id==='reference-family'||input.id==='reference-scope') {referenceState[input.id==='reference-family'?'category':'scope']=input.value;referenceState.limit=20;renderReference();return;}
  if(input.hasAttribute('data-album-upload')) {
    const index=input.dataset.albumUpload==='ai'?null:Number(input.dataset.albumUpload);
    uploadPhotos([...input.files],index,input.dataset.albumShelf==='true').catch(error=>toast(error.message));input.value='';return;
  }
  if (input.id === 'restore-backup') restore(input.files[0]).catch(error => toast(error.message));
  if (input.id === 'tax' || input.id === 'voice-language') changeData(data => { data.settings[input.id === 'tax' ? 'tax' : 'language'] = input.value; }).then(() => toast('Preference saved.')).catch(error => toast(error.message));
});
document.addEventListener('submit', event => {
  if (event.target.id === 'review-form') { event.preventDefault(); saveDrafts().catch(error => { $('#review-error').textContent = error.message; toast(error.message); }); }
});
document.addEventListener('keydown', event => {
  if (event.target.id === 'search' && event.key === 'Enter') { event.preventDefault(); smartSearch().catch(error => toast(error.message)); }
  if (event.key === '/' && !['INPUT', 'TEXTAREA', 'SELECT'].includes(document.activeElement.tagName)) { event.preventDefault(); if (state.page !== 'catalog') { state.page = 'catalog'; shell(); } $('#search').focus(); }
});
window.addEventListener('hashchange', () => { if (['#catalog','#reference'].includes(location.hash) && state.data) { state.page = location.hash.slice(1); shell(); } });
try {
  await openDatabase(); state.data = await readData();
  if (!state.data) { state.data = sampleData(); await writeData(state.data); }
  try {configureTranslations(await fetch('/ui/translations.json').then(r=>r.json()));}catch { /* Existing catalog stays usable if display assets are missing. */ }
  try { state.config = await fetch('/api/config').then(r => r.json()); } catch { /* Offline catalog remains available. */ }
  try { const response=await fetch('/reference/catalog.json');if(response.ok)referenceState.catalog=await response.json(); } catch { /* Shop data still works if the reference asset is unavailable. */ }
  if(referenceState.catalog) {try {const pictures=await fetch('/reference/images.json').then(r=>r.json());referenceState.catalog=withReferencePictures(referenceState.catalog,pictures);}catch {}}
  if(referenceState.catalog)prepareReferenceSearch(referenceState.catalog);
  shell();
  let pendingLocalization=false;const localizationRoots=new Set();
  new MutationObserver(records=>{
    for(const record of records) {const root=record.target.nodeType===Node.ELEMENT_NODE?record.target:record.target.parentElement;if(root)localizationRoots.add(root);}
    if(pendingLocalization)return;pendingLocalization=true;
    queueMicrotask(()=>{
      pendingLocalization=false;
      const roots=[...localizationRoots].filter(root=>root.isConnected);localizationRoots.clear();
      for(const root of roots)if(!roots.some(other=>other!==root&&other.contains(root)))localizeUi(root);
    });
  }).observe(document.body,{childList:true,subtree:true,characterData:true});
} catch (error) { $('#app').innerHTML = `<div class="startup-error"><h1>Let’s get your shop open.</h1><p>${esc(error.message)}</p><p>Enable browser storage, then reload this page.</p></div>`; }
