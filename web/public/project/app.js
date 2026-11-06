const $ = selector => document.querySelector(selector);
const esc = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
let data;
let active = 'overview';
let filter = 'all';
let lane = 'android';
let query = '';
const commands = {
  done: 'python3 scripts/project_log.py done "What is now possible" --summary "Why it matters" --changed "Concrete change" --check "Actual verification and result"',
  decision: 'python3 scripts/project_log.py decision "Decision title" --summary "Resulting boundary" --context "Why a choice was needed" --choice "Chosen approach" --alternative "Another option and tradeoff" --consequence "Benefit or accepted cost"',
  note: 'python3 scripts/project_log.py note "Investigation or handoff" --summary "What is known so far" --check "Evidence collected" --next "What is still unresolved"',
};
function date(value) { return new Intl.DateTimeFormat('en-IN', {day:'numeric',month:'short',year:'numeric'}).format(new Date(value+'T12:00:00+05:30')); }
function superseding(id) { return data.events.find(e => e.supersedes === id); }
function status(entry) { return superseding(entry.id) ? 'superseded' : entry.status || entry.kind; }
function badge(kind, label) { return `<span class="badge ${esc(kind)}">${esc(label)}</span>`; }
function sections(entry) {
  return [['What changed','changes'],['Verification','checks'],['Remaining / next','next']].filter(([,field]) => entry[field]?.length).map(([label,field]) => `<section class="entry-section"><h4>${label}</h4><ul>${entry[field].map(value => `<li>${esc(value)}</li>`).join('')}</ul></section>`).join('');
}
function eventCard(entry, expanded = false) {
  const decision = entry.kind === 'decision';
  const label = decision ? `DECISION · ${status(entry).toUpperCase()}` : entry.kind === 'done' ? 'COMPLETED' : 'FIELD NOTE';
  return `<article class="event"><div class="event-rail"><span class="event-dot ${esc(entry.kind)}"></span></div><div class="event-content"><div class="event-meta">${badge(entry.kind,label)}<span>${esc(entry.id)}</span><time datetime="${esc(entry.date)}">${date(entry.date)}</time></div><h3>${esc(entry.title)}</h3><p>${esc(entry.summary)}</p><details ${expanded ? 'open' : ''}><summary>${decision ? 'Read the decision' : 'Changes, evidence & next steps'}<span>↗</span></summary>${decision ? decisionBody(entry) : sections(entry)}</details></div></article>`;
}
function decisionBody(entry) {
  const superseded = superseding(entry.id);
  return `${superseded ? `<p class="decision-notice">Replaced by ${esc(superseded.id)}: ${esc(superseded.title)}</p>` : ''}${entry.supersedes ? `<p class="decision-notice">Supersedes ${esc(entry.supersedes)}</p>` : ''}<section class="entry-section"><h4>Context</h4><p>${esc(entry.context)}</p></section><section class="entry-section"><h4>The choice</h4><p>${esc(entry.choice)}</p></section>${[['Alternatives considered','alternatives'],['Consequences','consequences']].filter(([,field]) => entry[field]?.length).map(([label,field]) => `<section class="entry-section"><h4>${label}</h4><ul>${entry[field].map(value => `<li>${esc(value)}</li>`).join('')}</ul></section>`).join('')}<a class="inline-link" href="/project/doc/${esc(entry.id)}.md" target="_blank" rel="noopener">Open Markdown record ↗</a>`;
}
function render() {
  const {project,events} = data;
  const done = events.filter(e => e.kind === 'done').length;
  const decisions = events.filter(e => e.kind === 'decision' && status(e) === 'accepted').length;
  $('#main').innerHTML = `<section class="hero"><div><p class="eyebrow">THE STORY BEHIND THE BUILD</p><h1>A clear head.<br>A useful paper trail.</h1><p class="hero-copy">What changed, why we chose it, and what comes next.<br>One shared notebook for Codex, Claude Code and you.</p></div><div class="hero-status"><span class="live-dot"></span> ${esc(project.stage)}<span class="update-date">Snapshot · ${date(project.updated)}</span><p>${esc(project.summary)}</p></div></section><div class="stats"><div><strong>${done.toString().padStart(2,'0')}</strong><span>completed outcomes</span></div><div><strong>${decisions.toString().padStart(2,'0')}</strong><span>accepted decisions</span></div><div><strong>${project.architecture.lanes.length.toString().padStart(2,'0')}</strong><span>app surfaces, separate data</span></div><div><strong>01</strong><span>shared workflow skill</span></div></div>
  <nav class="tabs" aria-label="Project notebook sections">${[['overview','Overview'],['timeline','Work log'],['architecture','Architecture'],['decisions','Decisions'],['workflow','How to log']].map(([id,label])=>`<button data-tab="${id}" aria-current="${active===id?'page':'false'}" class="${active===id?'active':''}">${label}${id==='decisions'?` <span>${decisions}</span>`:''}</button>`).join('')}<span class="repo-note">REPO FILES · LOCAL VIEW</span></nav><div id="content"></div><footer><span>Human-readable in Git. Comfortable to browse here.</span><span>Dates · Asia/Kolkata</span></footer>`;
  ({overview:overview,timeline:timeline,architecture:architecture,decisions:decisionList,workflow:workflow})[active]();
}
function overview() {
  const {project,events} = data;
  $('#content').innerHTML = `<div class="overview-grid"><section class="card build-state"><div class="section-title"><p class="eyebrow">WHERE WE ARE</p><h2>A working build.<br>A real-world trial next.</h2></div><div class="tracks">${project.tracks.map((track,i)=>`<div class="track"><span class="track-index">0${i+1}</span><div><h3>${esc(track.name)} ${badge(i===3?'pending':'state',track.status)}</h3><p>${esc(track.summary)}</p></div></div>`).join('')}</div><div class="link-row">${project.links.map(link=>`<a href="${esc(link.url)}" ${link.url.startsWith('http')?'target="_blank" rel="noopener"':''}>${esc(link.label)} ↗</a>`).join('')}</div></section><aside class="card next-card"><p class="eyebrow">THE NEXT GOOD STEPS</p><h2>What needs<br>our attention.</h2><ol>${project.next.map(task=>`<li><h3>${esc(task.title)}</h3><p>${esc(task.reason)}</p></li>`).join('')}</ol><p class="note">This is the current direction, not a claim that the remaining work is already complete.</p></aside></div><section class="recent"><div class="section-title row"><div><p class="eyebrow">LAST IN THE NOTEBOOK</p><h2>Recent outcomes.</h2></div><button class="text-button" data-tab="timeline">See the full work log →</button></div><div class="recent-grid">${events.filter(e=>e.kind==='done').slice(-3).reverse().map(e=>`<article class="card recent-card"><span class="event-meta">${esc(e.id)} · ${date(e.date)}</span><h3>${esc(e.title)}</h3><p>${esc(e.summary)}</p><span class="small-proof">${e.checks?.length||0} verification ${e.checks?.length===1?'note':'notes'}</span></article>`).join('')}</div></section>`;
}
function timeline() {
  $('#content').innerHTML = `<div class="section-title row"><div><p class="eyebrow">OUTCOMES, NOT A TOOL TRANSCRIPT</p><h2>The work log.</h2></div><a class="inline-link" href="/project/doc/JOURNAL.md" target="_blank" rel="noopener">Read as Markdown ↗</a></div><div class="log-controls"><div class="filter-buttons">${[['all','Everything'],['done','Completed'],['decision','Decisions'],['note','Notes']].map(([value,label])=>`<button data-filter="${value}" class="${filter===value?'selected':''}" aria-pressed="${filter===value}">${label}</button>`).join('')}</div><label class="search-label"><span class="sr-only">Search project history</span><input id="log-search" type="search" placeholder="Find a change or decision…" value="${esc(query)}"></label></div><div id="events"></div>`;
  renderEvents();
}
function renderEvents() {
  const entries = data.events.filter(e => (filter==='all'||e.kind===filter) && JSON.stringify(e).toLowerCase().includes(query.toLowerCase())).reverse();
  $('#events').innerHTML = entries.length ? entries.map(e=>eventCard(e)).join('') : '<div class="empty">Nothing matches yet. Try another word or filter.</div>';
}
function architecture() {
  const selected = data.project.architecture.lanes.find(l=>l.id===lane);
  $('#content').innerHTML = `<div class="section-title row"><div><p class="eyebrow">FOLLOW THE DATA</p><h2>Two surfaces. Clear boundaries.</h2></div><a class="inline-link" href="/project/doc/ARCHITECTURE.md" target="_blank" rel="noopener">Mermaid diagrams & sources ↗</a></div><div class="lane-switch" role="group" aria-label="Architecture surface">${data.project.architecture.lanes.map(l=>`<button data-lane="${esc(l.id)}" class="${l.id===lane?'selected':''}" aria-pressed="${l.id===lane}">${esc(l.title)}</button>`).join('')}</div><section class="card architecture-card"><div class="architecture-heading"><div><h3>${esc(selected.title)}</h3><p>${esc(selected.summary)}</p></div>${badge('state','OFFLINE FIRST')}</div><div class="flow-grid">${selected.nodes.map(n=>`<article class="node ${esc(n.role)}"><span class="node-role">${esc(n.role==='external'?'ONLINE SERVICE':n.role.toUpperCase())}</span><h4>${esc(n.title)}</h4><p>${esc(n.detail)}</p></article>`).join('')}</div><div class="connections"><p class="eyebrow">HOW THEY CONNECT</p>${selected.edges.map(edge=>`<div><strong>${esc(selected.nodes.find(n=>n.id===edge.from).title)}</strong><span class="edge-line"><i></i><small>${esc(edge.label)}</small>→</span><strong>${esc(selected.nodes.find(n=>n.id===edge.to).title)}</strong></div>`).join('')}</div></section><section class="boundaries"><h3>The lines we don’t blur.</h3><div>${data.project.architecture.boundaries.map((text,i)=>`<article><span>0${i+1}</span><p>${esc(text)}</p></article>`).join('')}</div></section>`;
}
function decisionList() {
  const entries = data.events.filter(e=>e.kind==='decision').reverse();
  $('#content').innerHTML = `<div class="section-title"><p class="eyebrow">SAVE THE WHY, NOT JUST THE WHAT</p><h2>Decisions that shaped the build.</h2><p class="section-copy">A record of the context, alternatives and tradeoffs. A changed decision gets a new record; the old reasoning stays visible.</p></div><div class="decisions">${entries.map(e=>eventCard(e,true)).join('')}</div>`;
}
function workflow() {
  $('#content').innerHTML = `<div class="section-title"><p class="eyebrow">A SMALL HABIT. A BETTER HANDOFF.</p><h2>Both agents. One memory.</h2><p class="section-copy">The shared skill records meaningful outcomes after the work is verified. No session transcripts, no private reasoning, no credentials.</p></div><div class="workflow-grid"><section class="card"><span class="badge state">AUTOMATIC PROJECT CONTEXT</span><h3>The same rules on both sides.</h3><div class="agent-path"><strong>Codex</strong><code>AGENTS.md<br>.agents/skills/dukaan-workflow/</code></div><div class="agent-path"><strong>Claude Code</strong><code>CLAUDE.md → @AGENTS.md<br>.claude/skills/dukaan-workflow/ → shared skill</code></div><p>Invoke <code>$dukaan-workflow</code> in Codex or <code>/dukaan-workflow</code> in Claude Code. Normal skill selection remains enabled.</p><a class="inline-link" href="/project/doc/guide" target="_blank" rel="noopener">Read the project guide ↗</a><p class="note">The discovery paths and shared target are validated locally. Claude Code is not installed here, so an actual Claude session has not been tested.</p></section><section class="card"><span class="badge decision">ONE SOURCE OF HISTORY</span><h3>Easy to write. Easy to revisit.</h3><div class="file-chain"><code>docs/events.jsonl</code><span>↓ one render step ↓</span><code>JOURNAL.md + decisions/ADR-*.md</code><span>and this read-only notebook</span></div><p><code>docs/project.json</code> holds the current snapshot and architecture. Markdown views are generated, so the notebook and GitHub views stay aligned.</p><p class="note">The helper runs locally using Python’s standard library. Nothing is uploaded or committed by the logging command.</p></section></div><div class="command-grid">${[['done','Log a completed outcome','What changed, what was checked and the important remaining limits.'],['decision','Capture a design choice','The context, the choice, credible alternatives and accepted consequences.'],['note','Leave an honest handoff','Use this when work is partial or an investigation is still open.']].map(([type,title,description])=>`<section class="card command-card"><h3>${title}</h3><p>${description}</p><pre><code>${esc(commands[type])}</code></pre><button class="button subtle" data-copy="${type}">Copy command</button></section>`).join('')}</div>`;
}
async function load() {
  const response = await fetch('/project/data.json'); if (!response.ok) throw new Error('Could not read the project notebook.');
  data = await response.json(); render();
}
function toast(message) { $('#toast').textContent=message; $('#toast').classList.add('show'); setTimeout(()=>$('#toast').classList.remove('show'),3000); }
document.addEventListener('click', async event=>{
  const button=event.target.closest('button'); if(!button)return;
  if(button.dataset.tab){active=button.dataset.tab;render();}
  if(button.dataset.filter){filter=button.dataset.filter;timeline();}
  if(button.dataset.lane){lane=button.dataset.lane;architecture();}
  if(button.dataset.copy){try{await navigator.clipboard.writeText(commands[button.dataset.copy]);toast('Command copied. Run it from the project root.');}catch{toast('Select and copy the command text instead.');}}
  if(button.id==='refresh'){try{await load();toast('Notebook refreshed from repository files.');}catch(error){toast(error.message);}}
});
document.addEventListener('input',event=>{if(event.target.id==='log-search'){query=event.target.value;renderEvents();}});
try{await load();}catch(error){$('#main').innerHTML=`<section class="card error"><h1>The notebook couldn’t open.</h1><p>${esc(error.message)}</p><p>Check that the local server is running, then refresh.</p></section>`;}
