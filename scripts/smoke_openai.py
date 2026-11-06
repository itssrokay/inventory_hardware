#!/usr/bin/env python3
"""One live request using the same instructions/schema as the Android app. Never prints credentials."""
import json
import os
from pathlib import Path
import re
import sys
import urllib.request
import urllib.error

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'app/src/main/assets'

def local_key():
    key = os.environ.get('OPENAI_API_KEY', '').strip()
    path = ROOT / '.env.local'
    if not key and path.is_file():
        for line in path.read_text().splitlines():
            match = re.match(r'\s*(?:export\s+)?OPENAI_API_KEY\s*=\s*(.*?)\s*$', line)
            if match:
                key = match.group(1).strip('\"\' ')
    return key

def main():
    key = local_key()
    if not key:
        print('No local OpenAI key configured.', file=sys.stderr)
        return 1
    body = {
        'model': os.environ.get('OPENAI_MODEL', 'gpt-4.1-mini'),
        'store': False,
        'instructions': (ASSETS / 'listing-instructions.txt').read_text(),
        'input': 'Godrej tala 50mm 450, 65mm 600. Wood screw 1 inch 90 rupay per packet',
        'max_output_tokens': 2000,
        'text': {'format': {'type': 'json_schema', 'name': 'catalog_listing', 'strict': True,
                            'schema': json.loads((ASSETS / 'listing-schema.json').read_text())}}
    }
    request = urllib.request.Request('https://api.openai.com/v1/responses', data=json.dumps(body).encode(),
                                     headers={'Authorization': 'Bearer ' + key, 'Content-Type': 'application/json'})
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            result = json.load(response)
    except urllib.error.HTTPError as error:
        print(f'OpenAI smoke check failed: HTTP {error.code}. No credentials were printed.', file=sys.stderr)
        return 1
    except (urllib.error.URLError, TimeoutError):
        print('OpenAI could not be reached.', file=sys.stderr)
        return 1
    if result.get('status') != 'completed':
        print('OpenAI returned an incomplete response.', file=sys.stderr)
        return 1
    output = ''.join(c['text'] for o in result.get('output', []) for c in o.get('content', []) if c.get('type') == 'output_text')
    parsed = json.loads(output)
    assert len(parsed['items']) == 2, 'Expected two products'
    locks = next(i for i in parsed['items'] if i['brand'].lower() == 'godrej')
    assert sorted(v['sell_price'] for v in locks['variants']) == [450, 600], 'Lock prices changed'
    assert [v['spec'].replace(' ', '') for v in locks['variants']] == ['50mm', '65mm'], 'Lock sizes changed'
    screw = next(i for i in parsed['items'] if 'screw' in i['name'].lower())
    assert screw['variants'][0]['sell_price'] == 90 and screw['variants'][0]['unit'] == 'packet', 'Screw selling unit or price changed'
    destination = ROOT / 'build/verification'
    destination.mkdir(parents=True, exist_ok=True)
    (destination / 'openai-listing.json').write_text(json.dumps(parsed, ensure_ascii=False, indent=2))
    print('PASS: Live OpenAI parsing returned two items, correct lock sizes/prices, and the per-packet screw price.')
    print('Result saved under build/verification/openai-listing.json (contains sample catalog data only).')
    return 0

if __name__ == '__main__':
    sys.exit(main())
