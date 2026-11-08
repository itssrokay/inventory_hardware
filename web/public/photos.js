export const MAX_PHOTOS=6;
export function productPhotos(item) {return [...new Set([item.photo,...(item.extraPhotos||[])].filter(Boolean))];}
export function setProductPhotos(item,images) {const album=[...new Set(images)];if(album.length>MAX_PHOTOS)throw new Error('Choose up to 6 photos per album.');item.photo=album[0]||null;item.extraPhotos=album.slice(1);}
export function validPhoto(photo) {
  if(typeof photo!=='string'||!/^data:image\/(?:jpeg|png|webp);base64,[A-Za-z0-9+/]+={0,2}$/.test(photo))return false;
  const value=photo.slice(photo.indexOf(',')+1);return value.length%4===0&&value.length/4*3-(value.endsWith('==')?2:value.endsWith('=')?1:0)<=2_000_000;
}
export function validatePhotoAlbums(item) {
  if(item.extraPhotos!=null&&!Array.isArray(item.extraPhotos)||item.shelfPhotos!=null&&!Array.isArray(item.shelfPhotos))return false;
  if(item.photo!=null&&item.photo!==''&&!validPhoto(item.photo))return false;
  const extra=item.extraPhotos||[],shelf=item.shelfPhotos||[];
  return extra.length<=5&&shelf.length<=6&&(!extra.length||Boolean(item.photo))&&[...productPhotos(item),...shelf].every(validPhoto);
}
