import {normalize} from './domain.js';
import {productSearchText,translate} from './i18n.js';

// Keep exact stored names as values: an empty name is Uncategorized, null is All.
export function categoryChoices(products, count = () => 1) {
  const counts = new Map();
  for (const product of products) {
    const value = product.category || '';
    counts.set(value, (counts.get(value) || 0) + count(product));
  }
  return [...counts].sort(([a], [b]) => a.localeCompare(b)).map(([value, count]) => ({value, count}));
}
export function matchingCategories(choices, query) {
  const terms = normalize(query).split(' ').filter(Boolean);
  return choices.filter(({value}) => {
    const label=value || 'Uncategorized';
    const words = normalize(productSearchText(label)+' '+translate(label,'hi')+' '+translate(label,'hinglish'));
    return terms.every(term => words.includes(term));
  });
}
export function inCategory(products, category) {
  return category === null ? products : products.filter(product => (product.category || '') === category);
}
