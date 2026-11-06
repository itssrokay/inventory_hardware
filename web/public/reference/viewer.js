import {configureTranslations,setLanguageMode,localizeUi} from '/i18n.js';
try {configureTranslations(await fetch('/ui/translations.json').then(r=>r.json()));setLanguageMode(localStorage.getItem('dukaan-ui-language')||'en');}catch {}
const $=s=>document.querySelector(s);
let page=Math.max(1,Math.min(92,Number(new URL(location.href).searchParams.get('page'))||1));
function render(){
  $('#page').value=page;$('#previous').disabled=page===1;$('#next').disabled=page===92;
  $('#source-page').src=`/reference/pages/${page}.webp`;$('#source-page').alt=`Original Prince pricelist · PDF page ${page}`;
  $('#error').hidden=true;$('#sheet').scrollTo(0,0);
  history.replaceState(null,'',`/reference/viewer?page=${page}`);
  localizeUi(document);
}
$('#previous').addEventListener('click',()=>{page--;render();});
$('#next').addEventListener('click',()=>{page++;render();});
$('#page').addEventListener('change',event=>{page=Math.max(1,Math.min(92,Math.trunc(Number(event.target.value))||1));render();});
$('#zoom').addEventListener('change',event=>{$('#sheet').className=`zoom-${event.target.value}`;});
$('#source-page').addEventListener('error',()=>{$('#error').hidden=false;});
render();
