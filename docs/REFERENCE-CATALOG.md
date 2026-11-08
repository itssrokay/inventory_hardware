# Reference library and carried products

The Prince library is separate from the main shop catalog in Android and the local browser preview. All **4,105 reference variants in 702 product groups** start **stock unconfirmed**. Loading or browsing the library adds no shop items and creates no stock quantities.

## Select the products you carry

1. Open **Reference** on Android or **Reference library** in the web preview.
2. Search a name, size or exact printed item code; optionally choose a product family. Expand a group to choose individual variants.
3. Select only variants you carry, then **Review carried products**.
4. Check the name/specification, enter your **selling price** and explicitly choose your **selling unit**. Both fields start blank. Check the shop tax label separately in Settings. The source rates, printed rate basis and PDF page remain visible alongside the review.
5. Save. Only the reviewed variants enter the main catalog. Later selections add remaining sizes without replacing existing sizes. Linked variants show **Carried · shop price confirmed · quantity unknown** and cannot be activated again.

“Carried” is the owner's selection, not a live stock count or promise that an item is currently available. No quantities are imported. Discarding a review changes no shop data. Existing same-name/brand matches can be merged from review; differently named existing items need an explicit owner match rather than a guessed merge.

## What the import preserves

- The supplied PDF has **92 pages**. Its cover says **W.E.F. 01 JAN 26**, so the cover effective date is **2026-01-01**. The filename date **2026-01-30** is stored separately. Section-specific pricelist codes/dates remain visible; the cover date does not overwrite them.
- Product families, table headings, codes, sizes, lengths, classes, colour contexts, manufacturer pack details, printed price bases and integer-paise source rates remain separate from shop prices.
- Exact source fields, original extracted tables, complete page text, page/table/row provenance and the PDF SHA-256 are retained. Kerning spaces inside wholly numeric fields are removed for reading; `rawText` and the source row remain available.
- Every one of **3,940 code occurrences detected in the page-wide text** is reconciled to table content. The cell extraction also recovers codes that the page-wide text misses. This is an extraction audit, not a claim that the manufacturer's data is internally consistent or that every rate has been manually checked.
- **39 variants have no resolved source price**, including unpriced entries and the three BIOFIT capacities advertised on page 91. They stay selectable for manual review, with no invented price. The air-vent value `104.55` under an item-code heading is retained as ambiguous, not converted into a SKU or price.
- Marketing-only ranges without individual item details stay available in the original source pages; product SKUs and prices are not invented from promotional images.
- Repeated source codes remain separate where their context differs. Source inconsistencies, unit ambiguity, questionable inch headings, unusual code lengths and the printed cover rate `85860.00` are flagged instead of silently corrected.

The standalone original PDF and rendered browser page images are excluded from Git. The Android APK bundles a copy of the original PDF for offline browsing and zoom; publishing the APK also distributes that copy. The web source viewer displays pages rendered directly from that PDF and also offers the original download. None of this import uses OpenAI or sends the PDF to a service.

## Product pictures and language modes

A source-hash-bound `reference/images.json` manifest maps 520 product-table illustrations to 2,404 variants. Pictures are extracted only when they belong to the matching table; no nearby or unrelated picture is borrowed. Where no picture can be matched, the interface says so. Images illustrate the product family and may differ in size or colour from an individual variant.

Reference review uses the matched image if the existing shop item has no custom photo. Reviewed pictures are copied into ordinary photo storage and back up with the shop item; older linked items can also display a bundled fallback. Owner photos take priority. None of this selects stock or saves a selling price.

English, हिन्दी and Hinglish modes change controls and familiar product names locally. Original names and source fields remain visible; codes, dimensions, stored selling units and prices do not change. Display and voice languages are separate preferences.

## Reproduce the import

The importer never reads or writes the Room or IndexedDB shop catalog. Running it again with the same source produces the same IDs and bytes. It refuses to overwrite a destination associated with a different PDF hash; choose a separate output for another source.

```sh
python3 -m venv build/reference-import-env
build/reference-import-env/bin/pip install -r scripts/reference-import-requirements.txt
build/reference-import-env/bin/python scripts/import_prince_reference.py '/path/to/original.pdf' --render
```

Outputs:

- `app/src/main/assets/reference/prince-2026.json`: shared, versioned reference data.
- `docs/reference-import-audit.json`: code coverage by page and duplicate-code contexts.
- `app/src/main/assets/reference/images.json` and `products/`: matched source picture provenance and thumbnails, created by `scripts/extract_reference_images.py`.
- `app/src/main/assets/reference/prince-2026.pdf`: local original, ignored by Git.
- `build/reference-pages/prince-2026/`: local browser page images, ignored by Git.

A checkout without the original PDF still has the JSON library and source rows. Recreate the local PDF/page assets before building an APK that includes the original source viewer. New document sources need their own library entry and review; do not replace old linked source data.

## Persistence and verification

Room schema 2 adds a nullable, unique reference ID to shop variants. The 1→2 migration preserves existing items and prices and seeds no reference stock. Current Room schema 3 preserves these links while adding product/shelf albums. Android ZIP and web JSON backup version 3 retain links and both albums; each restores its own formats 1–3. Android/browser catalogs and backups remain independent.

Focused checks cover source examples and anomalies, vertically merged rows, shared rates across pipe lengths/classes, exact-code searching, partial activation, repeated activation, source-price separation, backup links and migration from the actual exported version 1 schema. The shop counter still needs an owner-led selection and a real-phone trial.

[Architecture decision](decisions/ADR-007.md) · [Machine-readable audit](reference-import-audit.json)
