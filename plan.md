# Shop inventory app — v1 plan

Last updated: Nov 6, 2026

Implementation has started. See `README.md` for the working features, build commands and remaining shop-readiness work. OpenAI replaces Claude by decision on Nov 6, 2026.

## Goal and scope of v1

v1 is an Android app where you speak or type items and prices in plain language, and find any item's price in a few seconds. It runs on your own Android phone only, with all data stored there: about 500–1,000 items, one selling price per size, and a photo per product. Location fields exist but are optional; price lookup and easy listing come first.

v1 is done when:

- A known item's price shows within 5 seconds of starting a search, by voice or text.
- One sentence like "Godrej tala 50mm 450, 65mm 600" adds an item with two sizes and their prices.
- Search works with no internet. Only the language-understanding steps need a connection.

## Knowledge base or embeddings?

No separate knowledge base: your item catalog is the knowledge base. Leave embeddings out of v1 and add them later only if real searches keep missing.

Your catalog is small (about 500–1,000 items) and structured, so RAG adds nothing. Most "different name for the same item" problems are solved by stored aliases plus typo-tolerant matching. Full sentences are handled by an LLM that rewrites the query into keywords. Embeddings only help when a query shares no words with the item, like "something to stop a tap leaking" finding a washer. Doing that on the phone means bundling a model of tens of MB, so it waits for evidence from the miss log.

| Approach | What it catches | In v1? |
| --- | --- | --- |
| Aliases (stored other names) | Hindi or local names, brand used as generic ("pench", "Fevicol", "L key") | Yes |
| Full-text search (SQLite FTS) | Word and prefix matches across name, aliases, size | Yes |
| Fuzzy matching | Typos and spelling variants ("alen key", "hinj") | Yes |
| LLM query rewrite | Full sentences and descriptions | Yes, online only |
| On-device embeddings | Meaning with no shared words | Later, if the miss log shows a need |
| RAG / knowledge base | Nothing extra for a small structured catalog | No |

Design the database so an embedding column can be added later. At 1,000 items, a plain loop comparing vectors is fast on a phone, so no vector database is needed even then.

## What you asked for

- **Natural-language listing:** add items and prices by typing or speaking normally, several at once.
- **Natural-language search:** ask the way a customer would, and the price is the first thing you see.
- **Hindi and English:** for listing, search and voice.
- **One price per size:** the selling price you set. No cost or bulk prices.
- **Product photo:** one per item, shown on the result.
- **Easy upkeep:** changing a price or adding an item takes seconds.
- **Your phone only:** Android, local storage. Other phones come later if this works.

## What you'll also want

The three that matter most are sizes with their own prices, price per unit, and backup. Without them, a hardware price app gives wrong answers or loses its data.

| Feature | Why it matters in a hardware shop | When |
| --- | --- | --- |
| Sizes and variants under one item | One "wood screw" has many sizes, each with its own price | v1 |
| Price per unit (piece, kg, ft, metre, box, dozen, packet) | Nails sell by kg and pipe by foot, so a bare number is ambiguous | v1 |
| Backup and export (ZIP of data and photos via WhatsApp or Drive) | With data on one phone only, a lost or broken phone loses everything | v1 |
| Voice input in Hindi, English and Hinglish | At the counter, speaking is faster than typing | v1 |
| Aliases in both Devanagari and Roman ("ताला", "tala") | Hindi gets typed both ways; each should find the item | v1 |
| Confirm card before saving what the LLM parsed | It can misread "1.5 inch" or a price; you check, then save | v1 |
| Product photo on every result | Confirms you've got the right item at a glance | v1 |
| "Price updated" date and price history | Rates change often; a stale price shows up as stale | v1 |
| Duplicate check when adding | Stops three entries for the same item | v1 |
| Edit price straight from the search result | The edit you'll make most often | v1 |
| Brand field | The same item costs different amounts by brand | v1 |
| Bulk add (many items in one message) | First setup means entering 500–1,000 items | v1 |
| Search-miss log with one-tap "save as alias" | The catalog learns the words your customers use | v1 |
| LLM-suggested aliases when listing | Hindi and common alternate names get added automatically | v1 |
| Optional zone and location note | Your original "where is it" goal, nearly free to add now | v1, optional field |
| Import items and prices from Vyapar | Prices are already in Vyapar, so no typing twice | Later |
| Supplier bill photo to items | Links to your purchase-bill automation | Later |
| Other phones and sync | If the app works, staff can use it too | Later |
| Quick quote for a customer | Add up a few items on the spot | Later |

## Data model

Prices belong to variants, not items. An item with no sizes still gets exactly one variant, so search and the price card work the same way for everything.

```sql
CREATE TABLE item (
  id INTEGER PRIMARY KEY,
  name TEXT NOT NULL,          -- canonical: "Wood screw"
  category TEXT,               -- "Fasteners"
  brand TEXT,
  photo_path TEXT,             -- one product photo, compressed
  zone TEXT,                   -- optional: "Stairs"
  location_note TEXT,          -- optional: "blue crate, 2nd from top"
  created_at INTEGER,
  updated_at INTEGER
);

CREATE TABLE variant (         -- one row per size/spec; price lives here
  id INTEGER PRIMARY KEY,
  item_id INTEGER NOT NULL REFERENCES item(id),
  spec TEXT,                   -- "1.5 inch, steel"
  unit TEXT NOT NULL,          -- piece | kg | ft | metre | box | dozen | packet
  sell_price REAL NOT NULL,    -- rupees per unit, the price you set
  price_updated_at INTEGER
);

CREATE TABLE alias (
  item_id INTEGER NOT NULL REFERENCES item(id),
  text TEXT NOT NULL           -- "pench", "पेंच", "L key", "Fevicol"
);

CREATE TABLE price_history (
  variant_id INTEGER, old_price REAL, new_price REAL, changed_at INTEGER
);

CREATE TABLE search_miss (
  query TEXT, searched_at INTEGER, resolved_item_id INTEGER  -- filled when someone picks the right item
);

-- Full-text index over the searchable text (Room supports FTS4 entities;
-- use the unicode61 tokenizer so Devanagari words index correctly)
CREATE VIRTUAL TABLE item_fts USING fts4(name, aliases, category, brand, specs, tokenize=unicode61);
```

The one open choice is whether prices include GST. Store one convention and label it on the price card (see open questions).

## Search design

Search runs locally first and calls the LLM only when local results are weak. Most searches finish offline in under 100 ms.

1. **Normalize the query.** Lowercase it, drop filler words ("ka rate", "price of", "hai kya"), and make sizes consistent (1.5", 1.5 in and 1½ inch all become "1.5 inch").
2. **Search locally.** Run a full-text prefix match over name, aliases, brand and spec. Add fuzzy matching (Jaro-Winkler or Levenshtein over names and aliases, in memory). Rank exact alias matches first, then prefix matches, then fuzzy ones, and show the top 10.
3. **Escalate if weak.** With no good match and a connection, send the query and your category list to the LLM. It returns keywords, likely aliases and a size filter as JSON, and step 2 runs again with those.
4. **Show price first.** The result card leads with price and unit, next to the product photo. Below that come other sizes, brand, zone and "updated N days ago". One tap edits the price.
5. **Learn from misses.** When someone finds the item after a miss, offer "Save 'kabja' as another name for Hinge?" in one tap. Every miss goes into `search_miss` for review.

If a query names a size ("2 inch screw ka rate"), jump straight to that variant's price instead of the item's list of sizes.

Hindi works because every item stores aliases in both scripts, such as "ताला" and "tala". Search checks both, so a query typed either way, or spoken, finds the same item.

## Natural-language listing

You say or type it, the LLM turns it into structured data, and you confirm before anything is saved.

1. Tap the mic or type, for example: "Godrej tala 50mm 450, 65mm 600. Wood screw 1 inch 90 rupay per packet".
2. The app sends the text to the LLM and asks for JSON in a fixed format: items, variants, units, prices and suggested aliases.
3. Each parsed item is fuzzy-matched against the catalog and marked **New** or **Update existing** ("Padlock – Godrej already exists; update prices?").
4. A confirm card shows every field, editable inline. Add a photo from the camera or gallery, then Save writes the item, updates the search index and logs any price change in `price_history`.
5. Offline, the app falls back to a plain form with autocomplete, so listing never stops.

Expected LLM output for step 2:

```json
{
  "items": [
    {
      "name": "Padlock",
      "brand": "Godrej",
      "category": "Locks",
      "aliases": ["tala", "ताला", "lock"],
      "variants": [
        {"spec": "50 mm", "unit": "piece", "sell_price": 450},
        {"spec": "65 mm", "unit": "piece", "sell_price": 600}
      ]
    },
    {
      "name": "Wood screw",
      "category": "Fasteners",
      "aliases": ["pench", "पेंच", "screw"],
      "variants": [{"spec": "1 inch", "unit": "packet", "sell_price": 90}]
    }
  ]
}
```

## Tech stack

Use native Kotlin with Jetpack Compose and Room. Android is the only target, and Room gives you SQLite full-text search with no extra setup.

| Layer | Choice | Why |
| --- | --- | --- |
| UI | Kotlin + Jetpack Compose | Native, fast, easy to build one-screen flows |
| Storage | Room (SQLite) with an FTS4 entity | Local, offline, full-text search included |
| Fuzzy match | Jaro-Winkler or Levenshtein in Kotlin, in memory | A few thousand names is cheap; no library needed |
| Voice | Android `SpeechRecognizer` (hi-IN / en-IN) | Free and built in; handles Hinglish reasonably |
| LLM | OpenAI Responses API over HTTPS, configurable model (initially gpt-4.1-mini) | Structured Outputs for parsing and query rewriting |
| API key | Encrypted on device for now; a small proxy later | Fine on your own phone; a proxy matters only once other phones join |
| Photos | CameraX, saved in app storage | One product photo per item, compressed to about 150 KB, so 1,000 photos take about 150 MB |
| Backup | Export a ZIP (JSON + photos) via the share sheet; WorkManager auto-backup later | Protects against phone loss |
| Embeddings (later) | On-device text embedder (MediaPipe or ONNX Runtime), vectors as BLOB | Add only if the miss log shows a need |

If iOS ever matters, Flutter with sqflite is the alternative. The data model and flows stay the same.

## Build phases

After phase 1 the app already answers "what's the price?" offline. Each later phase cuts more typing.

Each phase works in the shop before the next begins.

1. **Core catalog:** Room DB + FTS search, manual add and edit, price-first results, sizes, units and photo, backup export (ZIP).
2. **Smart search:** aliases + fuzzy match, size-aware results, search-miss log, save a miss as an alias, edit price from the result.
3. **Natural listing:** voice input (Hindi/English), LLM parse to JSON, confirm card, duplicate check, bulk add.
4. **Shop-ready:** LLM query rewrite, Hindi + Roman aliases, price history view, optional zone field, weekly backup reminder.

**Gate:** used daily in the shop and the miss log reviewed.

**Later, only if the gate shows a need:** on-device embeddings, Vyapar import, supplier bill photo to items, other phones and sync.

At the gate, the miss log shows whether embeddings or Vyapar import should come next.

## Not in v1

- **Billing and invoicing:** Vyapar already does this, so this app doesn't duplicate it.
- **Stock quantity tracking:** it needs every sale recorded, which Vyapar already does.
- **Other phones, sync, a web version, iOS:** only once v1 is working well on your phone.
- **Vyapar import and supplier-bill-to-items:** both fit once v1 is in daily use.
- **3D or panorama maps:** zone and photo fields cover findability for now.

## Open questions

Decided: your phone only, about 500–1,000 items, Hindi and English, the selling price only, and a photo per product.

- [ ] Do the prices you set include GST? The price card will say "incl. GST" or "excl. GST".
- [ ] Is internet reliable at the counter? If not, the LLM steps should queue and retry instead of blocking.
