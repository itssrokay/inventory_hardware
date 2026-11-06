#!/usr/bin/env python3
"""Deterministic, offline import of the supplied Prince PDF. Never writes shop data.

Requires pdfplumber. Source tables and page text remain alongside normalized entries;
ambiguous fields are retained verbatim rather than interpreted as selling prices.
"""
import argparse
from collections import Counter, defaultdict
from decimal import Decimal, InvalidOperation
import hashlib
import json
from pathlib import Path
import re

SKU = re.compile(r'\b(?:SIM|IM|IT|IP)\d{5,8}\b', re.I)
ROOT = Path(__file__).resolve().parents[1]
RANGES = [(7,14,'Smartfit Plus · CPVC'),(15,22,'Easyfit · UPVC'),(23,31,'Greenfit · PPR'),
          (32,45,'Ultrafit · SWR'),(46,50,'Silentfit · Low-noise SWR'),(51,54,'Corfit · DWC'),
          (55,58,'Drainfit · Underground'),(59,60,'Foamfit · Multilayer'),(61,64,'PEFit Aqua · HDPE'),
          (65,77,'Aquafit · Agriculture'),(78,79,'Safefit · Borewell'),(80,81,'Rainfit · Roofwater'),
          (82,84,'Wirefit · Electrical'),(85,86,'Durafit · FRP covers'),(87,88,'Storefit · Water tanks'),
          (89,89,'Cablefit · Cable ducting')]

def clean(s):
    return re.sub(r'\s+', ' ', s or '').strip()

def amount(s):
    s = re.sub(r'\s+', '', s or '').replace(',', '')
    if not re.fullmatch(r'\d+(?:\.\d{1,2})?', s): return None
    try: return int(Decimal(s) * 100)
    except InvalidOperation: return None

def normalize_numbers(result):
    """Remove PDF kerning spaces in wholly numeric fields; keep the raw spelling."""
    for group in result['products']:
        for variant in group['variants']:
            for field in variant['fields']+variant['sourcePrices']+variant['rowPrices']:
                value=field['text']
                if amount(value) is not None and re.search(r'\d\s+\d',value):
                    field['rawText']=value
                    field['text']=re.sub(r'\s+','',value)
    return result

def cluster(values, tolerance=1.2):
    groups=[]
    for v in sorted(values):
        if groups and v-groups[-1][-1] < tolerance: groups[-1].append(v)
        else: groups.append([v])
    return [sum(g)/len(g) for g in groups]

def extract_table(page, table, pn=None, ti=None):
    """Recover rows whose PDF borders merge all cells into one pdfplumber cell.

    Reuse the physical column boundaries of the other rows. Preserve the original
    table too; no guessed delimiter splitting, OCR or model rewriting is involved.
    """
    from pdfplumber.utils import extract_text
    xs=cluster([v for c in table.cells for v in (c[0],c[2])])
    # Hairline slivers from image borders are not real data columns.
    xs=[x for i,x in enumerate(xs) if i==0 or x-xs[i-1]>3]
    words=page.extract_words(x_tolerance=1,y_tolerance=2)
    code_words=[w for w in words if SKU.fullmatch(w['text']) and table.bbox[1]<=w['top']<=table.bbox[3] and table.bbox[0]<=w['x0']<=table.bbox[2]]
    xs=[x for x in xs if not any(w['x0']+1<x<w['x1']-1 for w in code_words)]
    # Page 82's first table has a misplaced vertical rule through the printed
    # second size field. Boundaries below are the observed gaps between its six
    # printed columns; source labels are retained, including the erroneous inch label.
    wirefit_fix=pn==82 and ti in (0,3)
    if wirefit_fix: xs=[table.bbox[0],54.0,74.0,111.5,133.5,171.0,table.bbox[2]]
    all_chars=[c for c in page.chars if c.get('upright',True)]
    rows=[]; bounds=[]
    for r in table.rows:
        y0=r.bbox[1]
        # A vertically merged size cell can extend across multiple data rows.
        # Its bottom is not the current row's bottom; use the nearest cell edge.
        y1=min(c[3] for c in r.cells if c and c[3]>y0+.5)
        chars=[c for c in all_chars if y0 <= (c['top']+c['bottom'])/2 < y1]
        values=[]
        for left,right in zip(xs,xs[1:]):
            cc=[c for c in chars if left <= (c['x0']+c['x1'])/2 < right]
            values.append(clean(extract_text(cc,x_tolerance=1,y_tolerance=2)))
        rows.append(values);bounds.append((y0,y1))
    # Build paths from actual merged header cells, not from blank-cell forward fill.
    originals=table.extract()
    first=next((i for i,r in enumerate(originals) if any(SKU.search(v or '') for v in r) or
                (re.match(r'^\d',r[0] or '') and (any(re.search(r'\d|^-+$',v or '') for v in r[1:]) or len(re.findall(r'\d+(?:\.\d+)?',r[0] or ''))>1))),len(rows))
    title=clean(' '.join(v or '' for v in table.extract()[0]))
    title_row=not re.search(r'\b(?:Size|Item\s*Code|Rate\s*Per|Capacity)\b',title,re.I) and not SKU.search(title)
    headers=[]
    for col,(left,right) in enumerate(zip(xs,xs[1:])):
        center=(left+right)/2; path=[]
        for ri in range(1 if title_row else 0,first):
            candidates=[c for c in table.rows[ri].cells if c and min(right,c[2])-max(left,c[0])>2]
            for c in candidates:
                if c and c[0]-.6 <= center <= c[2]+.6:
                    cc=[ch for ch in all_chars if c[0] <= (ch['x0']+ch['x1'])/2 < c[2] and c[1] <= (ch['top']+ch['bottom'])/2 < c[3]]
                    value=clean(extract_text(cc,x_tolerance=1,y_tolerance=2))
                    if value and value not in path: path.append(value)
                    break
            if len(candidates)>1 and all(c[2]-c[0] < right-left+2 for c in candidates):
                y0=min(c[1] for c in candidates); y1=max(c[3] for c in candidates)
                cc=[ch for ch in all_chars if left <= (ch['x0']+ch['x1'])/2 < right and y0 <= (ch['top']+ch['bottom'])/2 < y1]
                value=clean(extract_text(cc,x_tolerance=1,y_tolerance=2))
                if path: path[-1]=value
                elif value: path.append(value)
        headers.append(' / '.join(path) or f'Column {col+1}')
    # Decorative vertical seams can divide a real column (even through a SKU).
    # Adjacent slots belonging to exactly the same merged header are one column.
    if wirefit_fix: headers=['Size in mm','Size in inch','Black / Item Code','Black / Unit Price','Ivory / Item Code','Ivory / Unit Price']
    merged_xs=[xs[0]]; merged_headers=[]
    for i,h in enumerate(headers):
        if merged_headers and h==merged_headers[-1]:
            merged_xs[-1]=xs[i+1]
        else:
            merged_headers.append(h);merged_xs.append(xs[i+1])
    if len(merged_headers)!=len(headers):
        headers=merged_headers;xs=merged_xs;rows=[]
        for y0,y1 in bounds:
            chars=[c for c in all_chars if y0 <= (c['top']+c['bottom'])/2 < y1]
            rows.append([clean(extract_text([c for c in chars if l <= (c['x0']+c['x1'])/2 < r],x_tolerance=1,y_tolerance=2)) for l,r in zip(xs,xs[1:])])
    if wirefit_fix: headers=['Size in mm','Size in inch','Black / Item Code','Black / Unit Price','Ivory / Item Code','Ivory / Unit Price']
    return title if title_row else 'Product table',headers,rows,first,xs,bounds

def build(pdf_path):
    import pdfplumber
    digest=hashlib.sha256(pdf_path.read_bytes()).hexdigest()
    source_id='prince-'+digest[:16]
    result={'version':1,'source':{'id':source_id,'sha256':digest,'filename':pdf_path.name,
        'title':'Prince Pipes · Product Price List 2026','effectiveDate':'2026-01-01',
        'filenameDate':'2026-01-30','dateEvidence':'Cover: W.E.F - 01 JAN 26',
        'taxBasis':'GST extra (terms on PDF page 92); not the shop tax setting',
        'stockStatus':'unconfirmed','shopPricePaise':None},'products':[],'pages':[]}
    coded_occurrences=0; captured=0; coverage=[];list_labels={}
    with pdfplumber.open(pdf_path) as pdf:
        result['source']['pageCount']=len(pdf.pages)
        for pn,page in enumerate(pdf.pages,1):
            text=page.extract_text() or ''
            family=next((name for start,end,name in RANGES if start<=pn<=end),None)
            label=re.search(r'Pricelist(?:\s*Code)?\s*:\s*([^\n]+)',text,re.I)
            if family and label: list_labels[family]=clean(label.group(0))
            page_record={'page':pn,'text':text,'tables':[]}
            source_codes=Counter(m.upper() for m in SKU.findall(text))
            found_codes=Counter()
            if family:
                from types import SimpleNamespace
                tables=page.find_tables(); joined=[]
                for table in tables:
                    if joined and not SKU.search(str(joined[-1].extract())) and SKU.search(str(table.extract()[0])) and abs(joined[-1].bbox[0]-table.bbox[0])<2 and abs(joined[-1].bbox[2]-table.bbox[2])<2 and 0<table.bbox[1]-joined[-1].bbox[3]<25:
                        prev=joined.pop();gap=(prev.bbox[0],prev.bbox[3],table.bbox[2],table.bbox[1])
                        gaprow=SimpleNamespace(bbox=gap,cells=[gap]);original=prev.extract()+[[clean(page.crop(gap).extract_text())]]+table.extract()
                        joined.append(SimpleNamespace(bbox=(prev.bbox[0],prev.bbox[1],table.bbox[2],table.bbox[3]),cells=prev.cells+[gap]+table.cells,rows=prev.rows+[gaprow]+table.rows,extract=lambda r=original:r))
                    else: joined.append(table)
                for ti,table in enumerate(joined):
                    parsed=extract_table(page,table,pn,ti)
                    raw={'index':ti,'bbox':list(table.bbox),'originalRows':table.extract()}
                    page_record['tables'].append(raw)
                    title,headers,rows,first,xs,bounds=parsed
                    raw.update({'title':title,'headers':headers,'rows':rows,'dataStartsAt':first})
                    code_cols=[i for i,h in enumerate(headers) if any(SKU.search(r[i]) for r in rows[first:]) or
                               (re.search(r'item\s*code',h,re.I) and not re.search(r'price|rate',h,re.I))]
                    price_cols=[i for i,h in enumerate(headers) if re.search(r'\b(?:rate|price)\b',h,re.I) and any(amount(r[i]) is not None for r in rows[first:])]
                    price_for={c:next((p for p in price_cols if p>c),None) for c in code_cols}
                    name=title
                    if pn==88 and title.startswith('WATER CONTROL VALVE'):
                        name += ' · '+('Blue' if table.bbox[0]<250 else 'White')
                    group={'id':f'{source_id}:p{pn}:t{ti}','name':name,'brand':'Prince',
                           'category':family,'page':pn,'table':ti,'priceListLabel':list_labels.get(family,''),'variants':[]}
                    # Repeat only genuine vertically merged dimensions, not empty data cells.
                    for ri in range(first,len(rows)):
                        row=rows[ri].copy()
                        for ci,h in enumerate(headers):
                            if row[ci] or ci in code_cols or ci in price_cols: continue
                            if h.lower()=='shape':
                                row[ci]=next((rows[prev][ci] for prev in range(ri-1,first-1,-1) if rows[prev][ci]),'')
                                continue
                            center=(xs[ci]+xs[ci+1])/2; yc=sum(bounds[ri])/2
                            merged=next((c for c in table.cells if c[0] <= center < c[2] and c[1] <= yc < c[3] and c[3]-c[1] > bounds[ri][1]-bounds[ri][0]+2),None)
                            if merged:
                                from pdfplumber.utils import extract_text
                                cc=[ch for ch in page.chars if merged[0] <= (ch['x0']+ch['x1'])/2 < merged[2] and merged[1] <= (ch['top']+ch['bottom'])/2 < merged[3]]
                                row[ci]=clean(extract_text(cc,x_tolerance=1,y_tolerance=2))
                        found_codes.update(m.upper() for m in SKU.findall(' '.join(row)))
                        dimensions=[f'{headers[i]}: {v}' for i,v in enumerate(row) if v and v!='-' and i not in code_cols and i not in price_cols]
                        observations=[{'label':headers[i],'text':row[i],'paise':amount(row[i])} for i in price_cols if amount(row[i]) is not None]
                        for ci in code_cols or [None]:
                            code_matches=SKU.findall(row[ci]) if ci is not None else []
                            next_code=next((c for c in code_cols if c>ci),len(headers)) if ci is not None else len(headers)
                            local_prices=[price_for[ci]] if ci is not None and price_for[ci] is not None else []
                            exclusive_prices=local_prices.copy()
                            if len(price_cols)==1: local_prices=price_cols
                            prices=[{'label':headers[p],'text':row[p],'paise':amount(row[p])} for p in local_prices if amount(row[p]) is not None]
                            # Empty class/length cells mean not listed, not a fabricated SKU.
                            peers=[c for c in code_cols if price_for[c]==price_for.get(ci)]
                            shared_missing=not code_matches and len(peers)>1
                            if not code_matches and (not prices or (shared_missing and (ci!=peers[-1] or any(SKU.search(row[c]) for c in peers)))): continue
                            for code_index,code in enumerate(code_matches or [None]):
                                issues=[]
                                if code is None: issues.append('No item code printed for this priced entry.')
                                if code and not re.fullmatch(r'(?:SIM|IM|IT|IP)\d{6}',code,re.I): issues.append('Unusual item code length is printed in the source; retained without correction.')
                                if len(code_matches)>1: issues.append('Merged code field; verify the original table before activation.')
                                if not prices: issues.append('No unambiguous price mapped to this item code; inspect the source row.')
                                if any(re.search(r'unit\s*price|^price',p['label'],re.I) for p in prices): issues.append('Source does not specify a selling unit in the price heading. Confirm it separately.')
                                if any(re.search(r'per\s*pc',p['label'],re.I) and re.search(r'mtr',p['label'],re.I) for p in prices): issues.append('The printed price heading mentions both PC and MTR. Confirm the source rate basis before using it.')
                                if 82<=pn<=84: issues.append('Source column says size in inch but contains values such as 100 or 20. Retained verbatim; do not treat as an inch dimension.')
                                if any(re.search(r'pack|pkg',h,re.I) and re.search(r'\d\s*mm',v,re.I) for h,v in zip(headers,row)): issues.append('The source packaging column contains dimensions in mm. Retained under its printed heading; verify the size and pack details in the PDF.')
                                if pn==86 and any(p['paise']==8586000 for p in prices): issues.append('Source prints 85860.00 for this cover. Possible source typo; retained exactly and never used as a shop price automatically.')
                                if pn==88 and title=='AIR VENT': issues.append('104.55 appears under Item Code without an actual item code or price heading. Kept as an ambiguous source field.')
                                context=headers[ci] if ci is not None else ''
                                if shared_missing: context='Shared rate; source code and length not listed'
                                # Full context is retained, even when it is too long for a shop label.
                                label=' · '.join(dimensions+[context,code or 'Code not listed'])
                                group['variants'].append({'id':f'{group["id"]}:r{ri}:c{ci}:{code_index}',
                                    'code':code,'label':label,'context':context,'sourcePrices':prices,
                                    'rowPrices':observations,'fields':[{'label':h,'text':v} for h,v in zip(headers,row)],
                                    'rawRow':' | '.join(row),'row':ri,'issues':issues,'stockStatus':'unconfirmed',
                                    'shopPricePaise':None})
                    if group['variants']: result['products'].append(group)
                    if pn==88 and title=='AIR VENT':
                        # The PDF labels 104.55 as Item Code. It is not a valid SKU,
                        # and we must not silently turn it into a confirmed price.
                        result['products'].append({'id':group['id'],'name':title,'brand':'Prince','category':family,'page':pn,'table':ti,'variants':[
                            {'id':f'{group["id"]}:ambiguous','code':None,'label':'1 inch · ambiguous source field 104.55','context':'Size in inch: 1',
                             'fields':[{'label':h,'text':v} for h,v in zip(headers,rows[first])],'sourcePrices':[],'rowPrices':[],
                             'rawRow':' | '.join(rows[first]),'row':first,'issues':['104.55 is printed under Item Code. No actual SKU or explicit price heading is provided. Verify the source before entering a selling price.'],
                             'stockStatus':'unconfirmed','shopPricePaise':None}]})
            # The page-wide extractor can swallow a leading S when text touches
            # a border. Reconcile by six-digit code suffix, retain exact per-cell
            # code spelling, and retain unmodified page text for comparison.
            remaining=Counter(found_codes)
            missing=Counter()
            for code,count in source_codes.items():
                for _ in range(count):
                    match=next((c for c in remaining if remaining[c] and c[-6:]==code[-6:]),None)
                    if match: remaining[match]-=1
                    else: missing[code]+=1
            # Every code occurrence must be retained either in a variant or in an explicit
            # unresolved source record. Unresolved codes are selectable only after review.
            if missing and family:
                group={'id':f'{source_id}:p{pn}:unresolved','name':'Source entries requiring table review',
                       'brand':'Prince','category':family,'page':pn,'table':None,'variants':[]}
                for code,count in sorted(missing.items()):
                    for occurrence in range(count):
                        group['variants'].append({'id':f'{group["id"]}:{code}:{occurrence}','code':code,'label':code,
                            'context':'See complete source page','sourcePrices':[],'rowPrices':[],
                            'fields':[],'rawRow':'\n'.join(line for line in text.splitlines() if code.lower() in line.lower()),
                            'row':None,'issues':['Table extraction did not resolve this code. Read the PDF page and enter its exact specification before activation.'],
                            'stockStatus':'unconfirmed','shopPricePaise':None})
                result['products'].append(group)
            coverage.append({'page':pn,'codeOccurrences':sum(source_codes.values()),'tableCaptured':sum(source_codes.values())-sum(missing.values()),'unresolvedCodes':dict(missing)})
            coded_occurrences+=sum(source_codes.values());captured+=sum(source_codes.values())-sum(missing.values())
            result['pages'].append(page_record)
            print(f'Page {pn}: {len(page_record["tables"])} tables, {sum(source_codes.values())} code occurrences, {sum(missing.values())} unresolved',flush=True)
    # The unpriced BIOFIT ad has explicit capacities, but no SKUs or prices.
    result['products'].append({'id':f'{source_id}:p91:biofit','name':'BIOFIT Septic Tanks for Wastewater Management',
        'brand':'Prince','category':'BIOFIT · Septic tanks','page':91,'table':None,'variants':[
            {'id':f'{source_id}:p91:biofit:{litres}','code':None,'label':f'{litres} Litres','context':'Advertised capacity',
             'sourcePrices':[],'rowPrices':[],'fields':[{'label':'Available Sizes','text':f'{litres} Litres'}],
             'rawRow':'Available Sizes: 1000, 1500, 2000 Litres','row':None,'issues':['Advertisement only: no item code or price printed.'],
             'stockStatus':'unconfirmed','shopPricePaise':None} for litres in [1000,1500,2000]]})
    by_code=defaultdict(list)
    for group in result['products']:
        for variant in group['variants']:
            if variant['code']: by_code[variant['code'].upper()].append(variant)
    for code,variants in by_code.items():
        if len(variants)>1:
            for v in variants: v['issues'].append('Item code appears in multiple source contexts. Entries are kept separate; verify size, class, colour and price.')
    report={'sourceSha256':digest,'pages':len(result['pages']),'productGroups':len(result['products']),
        'referenceVariants':sum(len(p['variants']) for p in result['products']),
        'sourceCodeOccurrences':coded_occurrences,'tableCapturedCodeOccurrences':captured,
        'unresolvedCodeOccurrences':coded_occurrences-captured,'coverage':coverage,
        'duplicateCodeContexts':{c:len(v) for c,v in by_code.items() if len(v)>1}}
    result['audit']={k:v for k,v in report.items() if k not in ['coverage','duplicateCodeContexts']}
    return normalize_numbers(result),report

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('pdf',type=Path)
    parser.add_argument('--output',type=Path,default=ROOT/'app/src/main/assets/reference/prince-2026.json')
    parser.add_argument('--report',type=Path,default=ROOT/'docs/reference-import-audit.json')
    parser.add_argument('--render',action='store_true',help='Copy the original PDF locally and render browser source pages (requires pypdfium2).')
    args=parser.parse_args()
    if args.output.exists():
        existing=json.loads(args.output.read_text())
        if existing.get('source',{}).get('sha256')!=hashlib.sha256(args.pdf.read_bytes()).hexdigest():
            parser.error('Destination contains a different source. Choose a new --output; do not overwrite linked reference data.')
    data,report=build(args.pdf)
    for path,value in [(args.output,data),(args.report,report)]:
        path.parent.mkdir(parents=True,exist_ok=True)
        path.write_text(json.dumps(value,ensure_ascii=False,separators=(',',':'))+'\n')
    if args.render:
        render_sources(args.pdf)
    print(json.dumps(data['audit'],indent=2))

def render_sources(pdf_path):
    import shutil
    import pypdfium2 as pdfium
    target=ROOT/'app/src/main/assets/reference/prince-2026.pdf'
    reference=ROOT/'app/src/main/assets/reference/prince-2026.json'
    if reference.exists() and json.loads(reference.read_text())['source']['sha256']!=hashlib.sha256(pdf_path.read_bytes()).hexdigest():
        raise ValueError('Source viewer belongs to the current reference library. Refusing to replace it with a different PDF.')
    target.parent.mkdir(parents=True,exist_ok=True)
    if pdf_path.resolve()!=target.resolve(): shutil.copy2(pdf_path,target)
    directory=ROOT/'build/reference-pages/prince-2026';directory.mkdir(parents=True,exist_ok=True)
    pdf=pdfium.PdfDocument(pdf_path)
    for n in range(len(pdf)):
        pdf[n].render(scale=2).to_pil().convert('RGB').save(directory/f'page-{n+1:02}.webp',quality=90)
    print(f'Rendered {len(pdf)} source pages. Original PDF and page images remain local and ignored by Git.')

if __name__=='__main__': main()
