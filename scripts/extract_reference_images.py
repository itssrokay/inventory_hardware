#!/usr/bin/env python3
"""Extract only pictures inside their own source product tables; no stock writes."""
import argparse
import hashlib
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]

def extract(pdf_path,output):
    import pdfplumber
    import pypdfium2 as pdfium
    catalog=json.loads((output/'prince-2026.json').read_text())
    digest=hashlib.sha256(pdf_path.read_bytes()).hexdigest()
    if catalog['source']['sha256']!=digest: raise ValueError('PDF does not match the reference library source hash.')
    directory=output/'products';directory.mkdir(parents=True,exist_ok=True)
    images={};missing=[]
    renderer=pdfium.PdfDocument(pdf_path)
    with pdfplumber.open(pdf_path) as pdf:
        for pn in sorted({p['page'] for p in catalog['products']}):
            page=pdf.pages[pn-1];rendered=None
            for group in (p for p in catalog['products'] if p['page']==pn):
                table=next((t for t in catalog['pages'][pn-1]['tables'] if t['index']==group['table']),None)
                if table is None:missing.append(group['id']);continue
                x0,y0,x1,y1=table['bbox']
                candidates=[]
                for image in page.images:
                    overlap=max(0,min(x1,image['x1'])-max(x0,image['x0']))*max(0,min(y1,image['bottom'])-max(y0,image['top']))
                    area=image['width']*image['height']
                    if (x0 < (image['x0']+image['x1'])/2 < x1 and image['top']>=y0-12 and image['bottom']<=min(y1+12,y0+110)
                        and image['width']>=12 and image['height']>=12 and image['width']<(x1-x0)*.95 and overlap/area>=.7):
                        candidates.append(image)
                if not candidates:missing.append(group['id']);continue
                # Some source pictures consist of multiple embedded image pieces.
                bounds=[min(i['x0'] for i in candidates),min(i['top'] for i in candidates),max(i['x1'] for i in candidates),max(i['bottom'] for i in candidates)]
                if rendered is None:rendered=renderer[pn-1].render(scale=3).to_pil().convert('RGB')
                box=(max(0,int(bounds[0]*3)-3),max(0,int(bounds[1]*3)-3),min(rendered.width,int(bounds[2]*3)+3),min(rendered.height,int(bounds[3]*3)+3))
                image=rendered.crop(box);image.thumbnail((320,320))
                name=hashlib.sha256(group['id'].encode()).hexdigest()[:24]+'.webp'
                image.save(directory/name,quality=90)
                images[group['id']]={'asset':'reference/products/'+name,'page':pn,'bbox':bounds,'kind':'source-table-illustration','sha256':hashlib.sha256((directory/name).read_bytes()).hexdigest()}
            if rendered:rendered.close()
    result={'version':1,'sourceSha256':digest,'images':images,'withoutMatchedPicture':missing,
            'picturedGroups':len(images),'picturedVariants':sum(len(p['variants']) for p in catalog['products'] if p['id'] in images)}
    (output/'images.json').write_text(json.dumps(result,ensure_ascii=False,separators=(',',':'))+'\n')
    print(json.dumps({k:v for k,v in result.items() if k not in ['images','withoutMatchedPicture']},indent=2))

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('pdf',type=Path);parser.add_argument('--output',type=Path,default=ROOT/'app/src/main/assets/reference')
    args=parser.parse_args();extract(args.pdf,args.output)
