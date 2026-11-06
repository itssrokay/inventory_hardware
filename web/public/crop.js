import { cropRegion } from './domain.js';

/** Local canvas editing only. Nothing is uploaded until the user requests AI review. */
export async function cropImage(source) {
  if (source instanceof File && (source.size > 20_000_000 || !source.type.startsWith('image/'))) throw new Error('Choose an image smaller than 20 MB.');
  let bitmap;
  try {
    if (typeof source === 'string') {
      const image = new Image(); image.src = source; await image.decode(); bitmap = await createImageBitmap(image);
    } else bitmap = await createImageBitmap(source, {imageOrientation:'from-image'});
  } catch { throw new Error('This image could not be opened. Try a JPEG, PNG or WebP photo.'); }
  return new Promise(resolve => {
    let zoom=1,panX=0,panY=0,aspect='original',drag=null,closed=false,rotating=false;
    const dialog=document.createElement('dialog');dialog.className='crop-dialog';
    dialog.setAttribute('aria-label','Crop image');
    dialog.innerHTML=`<div class="dialog-heading"><div><p class="eyebrow">MAKE THE IMPORTANT PART CLEAR</p><h2>Crop your image</h2></div><button class="icon-button" id="crop-cancel" aria-label="Cancel crop">×</button></div><p>Drag to position. Zoom or rotate to make the label, item or price list easy to read.</p><div class="crop-stage"><canvas id="crop-canvas" aria-label="Image crop preview"></canvas></div><div class="crop-controls"><label>Shape<select id="crop-aspect"><option value="original">Original</option><option value="1">Square</option><option value="1.333333333333">Landscape 4:3</option><option value="0.75">Portrait 3:4</option></select></label><label>Zoom <span id="crop-zoom-value">1.0×</span><input id="crop-zoom" type="range" min="1" max="5" step="0.05" value="1"></label></div><div class="crop-tools"><button id="crop-rotate" class="button secondary">Rotate 90°</button><button id="crop-reset" class="text-button">Reset crop</button></div><p id="crop-error" class="form-error" role="alert"></p><div class="dialog-actions"><button id="crop-back" class="button secondary">Cancel</button><button id="crop-use" class="button primary">Use crop</button></div>`;
    document.body.append(dialog);
    const q=s=>dialog.querySelector(s),canvas=q('#crop-canvas'),context=canvas.getContext('2d');
    function setup(){const ratio=aspect==='original'?bitmap.width/bitmap.height:Number(aspect);canvas.width=800;canvas.height=Math.max(1,Math.round(800/ratio));reset();}
    function clamp(){const scale=Math.max(canvas.width/bitmap.width,canvas.height/bitmap.height)*zoom;const x=Math.max(0,(bitmap.width*scale-canvas.width)/2),y=Math.max(0,(bitmap.height*scale-canvas.height)/2);panX=Math.max(-x,Math.min(x,panX));panY=Math.max(-y,Math.min(y,panY));}
    function draw(){
      clamp();const scale=Math.max(canvas.width/bitmap.width,canvas.height/bitmap.height)*zoom;
      context.fillStyle='#fff';context.fillRect(0,0,canvas.width,canvas.height);
      context.drawImage(bitmap,(canvas.width-bitmap.width*scale)/2+panX,(canvas.height-bitmap.height*scale)/2+panY,bitmap.width*scale,bitmap.height*scale);
      context.strokeStyle='rgba(255,255,255,.65)';context.lineWidth=Math.max(1,canvas.width/500);context.beginPath();
      for(let i=1;i<3;i++){context.moveTo(canvas.width*i/3,0);context.lineTo(canvas.width*i/3,canvas.height);context.moveTo(0,canvas.height*i/3);context.lineTo(canvas.width,canvas.height*i/3);}context.stroke();
      q('#crop-zoom-value').textContent=zoom.toFixed(1)+'×';
    }
    function reset(){zoom=1;panX=panY=0;q('#crop-zoom').value='1';draw();}
    function finish(value){if(closed)return;closed=true;bitmap.close();dialog.close();dialog.remove();resolve(value);}
    dialog.addEventListener('cancel',event=>{event.preventDefault();finish(null);});
    q('#crop-cancel').onclick=q('#crop-back').onclick=()=>finish(null);
    q('#crop-aspect').onchange=event=>{aspect=event.target.value;setup();};
    q('#crop-zoom').oninput=event=>{zoom=Number(event.target.value);draw();};
    q('#crop-reset').onclick=reset;
    canvas.addEventListener('pointerdown',event=>{drag={x:event.clientX,y:event.clientY};canvas.setPointerCapture(event.pointerId);});
    canvas.addEventListener('pointermove',event=>{if(!drag)return;const rect=canvas.getBoundingClientRect();panX+=(event.clientX-drag.x)*canvas.width/rect.width;panY+=(event.clientY-drag.y)*canvas.height/rect.height;drag={x:event.clientX,y:event.clientY};draw();});
    canvas.addEventListener('pointerup',()=>{drag=null;});canvas.addEventListener('pointercancel',()=>{drag=null;});
    q('#crop-rotate').onclick=async()=>{
      if(rotating)return;rotating=true;q('#crop-use').disabled=true;q('#crop-rotate').disabled=true;
      try{const rotated=document.createElement('canvas');rotated.width=bitmap.height;rotated.height=bitmap.width;const c=rotated.getContext('2d');c.translate(rotated.width/2,rotated.height/2);c.rotate(Math.PI/2);c.drawImage(bitmap,-bitmap.width/2,-bitmap.height/2);const next=await createImageBitmap(rotated);if(closed){next.close();return;}bitmap.close();bitmap=next;setup();}
      catch{q('#crop-error').textContent='Could not rotate this image. Try another photo.';}
      finally{rotating=false;if(!closed){q('#crop-use').disabled=false;q('#crop-rotate').disabled=false;}}
    };
    q('#crop-use').onclick=()=>{
      if(rotating)return;
      const region=cropRegion(bitmap.width,bitmap.height,canvas.width,canvas.height,zoom,panX,panY),factor=Math.min(1,1600/Math.max(region.width,region.height));
      const output=document.createElement('canvas');output.width=Math.max(1,Math.round(region.width*factor));output.height=Math.max(1,Math.round(region.height*factor));const c=output.getContext('2d');c.fillStyle='#fff';c.fillRect(0,0,output.width,output.height);c.drawImage(bitmap,region.left,region.top,region.width,region.height,0,0,output.width,output.height);
      const image=output.toDataURL('image/jpeg',.85);if(image.length>2_700_000){q('#crop-error').textContent='This image is too large. Crop a smaller area.';return;}finish(image);
    };
    setup();dialog.showModal();
  });
}
