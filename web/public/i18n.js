let dictionary={labels:{},terms:{}};
let mode='en';
let templates=[],termPattern=null,termsByName=new Map(),labelsIgnoreCase=new Map(),termMatchers=[],searchWordsCache=new Map();
const escape=value=>value.replace(/[.*+?^${}()|[\]\\]/g,'\\$&');
export function configureTranslations(data) {
  dictionary=data;labelsIgnoreCase=new Map(Object.entries(data.labels).map(([k,v])=>[k.toLowerCase(),v]));termsByName=new Map(Object.entries(data.terms).map(([k,v])=>[k.toLowerCase(),v]));
  templates=Object.entries(data.labels).filter(([key])=>/\{\d+}/.test(key)).sort(([a],[b])=>b.replace(/\{\d+}/g,'').length-a.replace(/\{\d+}/g,'').length).map(([key,value])=>{
    const parts=key.split(/(\{\d+})/),keys=parts.filter(s=>/^\{\d+}$/.test(s));
    return {pattern:new RegExp('^'+parts.map(s=>/^\{\d+}$/.test(s)?'([\\s\\S]+?)':escape(s)).join('')+'$'),keys,value};
  });
  const names=Object.keys(data.terms).sort((a,b)=>b.length-a.length);
  termMatchers=Object.entries(data.terms).map(([term,value])=>({pattern:new RegExp('(?<![A-Za-z])'+escape(term)+'(?![A-Za-z])','i'),value}));searchWordsCache.clear();
  termPattern=names.length?new RegExp('(?<![A-Za-z])('+names.map(escape).join('|')+')(?![A-Za-z])','gi'):null;
}
export const languageMode=()=>mode;
export function setLanguageMode(value) {mode=['en','hi','hinglish'].includes(value)?value:'en';}
export function translate(text,language=mode) {
  if(language==='en')return text;
  const direct=dictionary.labels[text]||labelsIgnoreCase.get(text.toLowerCase());
  if(direct)return direct[language]||text;
  for(const template of templates) {
    const match=text.match(template.pattern);if(!match)continue;
    const values=Object.fromEntries(template.keys.map((key,i)=>[key,match[i+1]]));
    return template.value[language].replace(/\{\d+}/g,key=>values[key]??key);
  }
  return text.includes('\n')?text.split('\n').map(line=>translate(line,language)).join('\n'):text;
}
export function productText(text,language=mode) {
  if(language==='en'||!termPattern)return text;
  return text.replace(termPattern,word=>termsByName.get(word.toLowerCase())?.[language]||word);
}
export function productSearchText(text) {
  if(!searchWordsCache.has(text))searchWordsCache.set(text,text+' '+productText(text,'hi')+' '+productText(text,'hinglish')+' '+termMatchers.filter(({pattern})=>pattern.test(text)).flatMap(({value})=>value.aliases||[]).join(' '));
  return searchWordsCache.get(text);
}
export function searchQuery(text) {
  let result=text;
  for(const [term,value] of Object.entries(dictionary.terms)) {
    const aliases=[...new Set([...(value.aliases||[]),value.hi])].filter(s=>s&&!termsByName.has(s.toLowerCase())).sort((a,b)=>b.length-a.length);
    for(const alias of aliases)result=result.replace(new RegExp('(?<![\\p{L}\\p{N}])'+escape(alias)+'(?![\\p{L}\\p{N}])','giu'),()=>term);
  }
  return result;
}

/** Translate UI nodes only. Never rewrite editable values or canonical catalog/source fields. */
export function localizeUi(root=document) {
  if(root===document)document.documentElement.lang=mode==='hi'?'hi':mode==='hinglish'?'hi-Latn':'en';
  // An option's implicit value follows its text. Freeze it before translating labels.
  root.querySelectorAll('option').forEach(option=>{if(!option.hasAttribute('value'))option.setAttribute('value',option.value);});
  const skip='[data-preserve-text],[data-category]:not([data-category=""]),.product-title h2,.variant-spec,.category-label,.reference-group h2,.reference-check strong,.source-price,.reference-variant dl,.reference-variant pre,.reference-issues code,pre,script,style,textarea,input,.language-switch';
  const walker=document.createTreeWalker(root,NodeFilter.SHOW_TEXT);let node;
  while((node=walker.nextNode())) {
    if(node.parentElement?.closest(skip))continue;
    const text=node.nodeValue.trim();if(!text)continue;
    const replacement=translate(text);if(replacement!==text)node.nodeValue=node.nodeValue.replace(text,replacement);
  }
  for(const element of root.querySelectorAll('[placeholder],[aria-label],[title]')) {
    if(element.closest('[data-preserve-text]'))continue;
    for(const name of ['placeholder','aria-label','title']) {const value=element.getAttribute(name);if(value){const replacement=translate(value);if(value!==replacement)element.setAttribute(name,replacement);}}
  }
}
