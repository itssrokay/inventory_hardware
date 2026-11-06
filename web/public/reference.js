import {normalize} from './domain.js';

export const activeReferences = products => new Set(products.flatMap(p=>p.variants).map(v=>v.sourceReferenceId).filter(Boolean));
export function referenceMatches(catalog, query='', category='') {
  const terms=normalize(query).split(' ').filter(Boolean);
  const code=/^(?:SIM|IM|IT|IP)\d{5,8}$/i.test(query.trim())?query.trim().toUpperCase():null;
  return catalog.products.filter(p=>!category||p.category===category).map(p=>({...p,variants:p.variants.filter(v=>code?v.code?.toUpperCase()===code:terms.every(t=>normalize(`Prince ${p.name} ${p.category} ${v.label} ${v.code||''} ${v.rawRow}`).includes(t)))})).filter(p=>p.variants.length);
}
export function referenceSpec(v) {
  const dimensions=v.fields.filter(f=>(/size|capacity|slot|shape|^type|^mtr/i.test(f.label)||/\d\s*mm/i.test(f.text))&&f.text&&f.text!=='-').map(f=>`${f.label}: ${f.text}`).join(' · ');
  return [dimensions,v.context.replace(/item\s*code/ig,'').replace(/^[ /]+|[ /]+$/g,''),v.code||'Code not listed'].filter(Boolean).join(' · ');
}
export function referenceDrafts(catalog, selected, products) {
  const active=activeReferences(products), drafts=new Map();
  for(const p of catalog.products) {
    const variants=p.variants.filter(v=>selected.has(v.id)&&!active.has(v.id));if(!variants.length)continue;
    const name=`${p.category.split(' · ')[0]} ${p.name}`,key=name+'|'+p.category;
    let draft=drafts.get(key);
    if(!draft) {
      const existing=products.find(p=>normalize(p.name)===normalize(name)&&normalize(p.brand)==='prince');
      draft={name,brand:'Prince',category:p.category,aliases:existing?.aliases.join(', ')||'',zone:existing?.zone||'',locationNote:existing?.locationNote||'',photo:existing?.photo||null,targetId:existing?.id||null,variants:[]};drafts.set(key,draft);
    }
    draft.variants.push(...variants.map(v=>({spec:referenceSpec(v),unit:'',price:'',discount:'',sourceReferenceId:v.id})));
  }
  return [...drafts.values()];
}

const esc=value=>String(value??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
export function sourceNote(catalog,id) {
  const p=catalog?.products.find(p=>p.variants.some(v=>v.id===id)),v=p?.variants.find(v=>v.id===id);
  if(!v)return `<p class="reference-source">Reference link: ${esc(id)}. Source library unavailable.</p>`;
  return `<aside class="reference-source"><strong>Source list · cover effective ${esc(catalog.source.effectiveDate)} · PDF p.${p.page}</strong><p>${esc(p.priceListLabel||'')}</p><p>${v.sourcePrices.map(p=>`${esc(p.label)}: ₹${esc(p.text)}`).join('<br>')||'No resolved source price'}</p><p>Source prices are separate from your selling price. Confirm the specification, selling unit and shop tax label. Quantity remains unknown.</p><a href="/reference/viewer?page=${p.page}" target="_blank" rel="noopener">Read original page ↗</a></aside>`;
}

export function referenceView(catalog,products,{query='',category='',selected=new Set(),limit=20,scope='all'}={}) {
  if(!catalog)return '<div class="empty-state panel"><h2>Reference library unavailable</h2><p>Your shop catalog is unchanged. Reload to retry loading the local library.</p></div>';
  const active=activeReferences(products), groups=referenceMatches(catalog,query,category).map(p=>({...p,variants:p.variants.filter(v=>scope==='all'||(scope==='unconfirmed'?!active.has(v.id):active.has(v.id)))})).filter(p=>p.variants.length);
  return `<header class="page-header"><div><p class="eyebrow">A PRICE LIST IS NOT YOUR STOCK LIST</p><h1>Reference library.</h1><p class="subtitle">Choose only the variants you carry. The rest stay hidden from your shop catalog.</p></div><a class="button secondary" href="/reference/viewer?page=1" target="_blank" rel="noopener">Read original PDF ↗</a></header>
    <section class="panel reference-intro"><span class="pill">Stock unconfirmed by default</span><h2>Prince Pipes · 2026 price list</h2><p>Effective <strong>${esc(catalog.source.effectiveDate)}</strong> on the cover. Filename date: ${esc(catalog.source.filenameDate)}. ${esc(catalog.source.taxBasis)}.</p><p>List rates, pack sizes and product codes are reference data. They do not confirm availability, stock quantities or your selling prices. Repeated codes and unclear source fields stay flagged for review.</p><span class="tiny">${catalog.audit.productGroups} product groups · ${catalog.audit.referenceVariants} variants · ${active.size} linked shop variants${catalog.audit.unresolvedCodeOccurrences?` · ${catalog.audit.unresolvedCodeOccurrences} code occurrences require source review`:''}</span></section>
    <div class="reference-controls"><label>Search reference<input id="reference-query" type="search" placeholder="Try coupler, 25 mm or SIM107608" value="${esc(query)}"></label><label>Product family<select id="reference-family"><option value="">All product families</option>${[...new Set(catalog.products.map(p=>p.category))].map(c=>`<option ${category===c?'selected':''}>${esc(c)}</option>`).join('')}</select></label><label>Status<select id="reference-scope">${[['all','All reference variants'],['unconfirmed','Stock unconfirmed'],['carried','Carried · shop price confirmed']].map(([k,v])=>`<option value="${k}" ${scope===k?'selected':''}>${v}</option>`).join('')}</select></label></div>
    <div class="reference-selection"><span><strong>${[...selected].filter(id=>!active.has(id)).length} selected</strong> · ${groups.reduce((n,p)=>n+p.variants.length,0)} matching variants · nothing saved yet</span><div><button class="button secondary" data-reference-clear>Clear selection</button><button class="button primary" data-reference-review ${![...selected].some(id=>!active.has(id))?'disabled':''}>Review carried products →</button></div></div>
    <div class="reference-groups">${groups.slice(0,limit).map(p=>`<details class="panel reference-group" ${query?'open':''}><summary><div><span class="category-label">${esc(p.category)} · PDF p.${p.page}</span><h2>${esc(p.name)}</h2><span class="tiny">${p.variants.length} variants · ${esc(p.priceListLabel||'')}</span></div><span>Choose sizes ↓</span></summary><a class="text-button" href="/reference/viewer?page=${p.page}" target="_blank" rel="noopener">Inspect source page ↗</a><div>${p.variants.map(v=>`<section class="reference-variant"><label class="reference-check"><input type="checkbox" data-reference-select="${esc(v.id)}" ${selected.has(v.id)||active.has(v.id)?'checked':''} ${active.has(v.id)?'disabled':''}><span><strong>${esc(referenceSpec(v))}</strong><small>${active.has(v.id)?'Carried · shop price confirmed · quantity unknown':'Stock unconfirmed · reference only'}</small></span></label><div class="reference-variant-detail"><p>${esc(p.priceListLabel||'')}</p><p>${v.sourcePrices.map(p=>`<span class="source-price">Source ${esc(p.label)}: <strong>₹${esc(p.text)}</strong></span>`).join('')||'No resolved source price. Inspect the original page.'}</p>${v.issues.length?`<p class="reference-issues">${v.issues.map(esc).join('<br>')}</p>`:''}<details><summary>Exact source fields & pack details</summary><dl>${v.fields.map(f=>`<div><dt>${esc(f.label)}</dt><dd>${esc(f.text||'—')}</dd></div>`).join('')}</dl>${!v.fields.length?`<pre>${esc(v.rawRow)}</pre>`:''}<p class="tiny">Pack details are manufacturer packaging, not shop quantities.</p></details></div></section>`).join('')}</div></details>`).join('')||'<div class="empty-state panel"><h2>No reference variants match</h2><p>Try a code, a size or another family.</p></div>'}</div>${groups.length>limit?'<button class="button secondary" data-reference-more>Show more groups</button>':''}`;
}
