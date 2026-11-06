#!/usr/bin/env python3
"""Transfer the local key to a selected debug installation, only after interactive confirmation."""
import argparse
from pathlib import Path
import shutil
import subprocess
import sys
from smoke_openai import local_key

parser = argparse.ArgumentParser()
parser.add_argument('--serial', help='adb device serial; required if more than one device is connected')
args = parser.parse_args()
adb = shutil.which('adb') or str(Path.home() / 'Library/Android/sdk/platform-tools/adb')
base = [adb] + (['-s', args.serial] if args.serial else [])
package = 'com.shopkhata.inventory'
key = local_key()
if not key:
    sys.exit('No key found in the environment or project .env.local.')
state = subprocess.run(base + ['get-serialno'], capture_output=True, text=True)
if state.returncode:
    sys.exit('Select one connected Android device with --serial.')
serial = state.stdout.strip()
answer = input(f'Store the existing local key in the Dukaan debug app on {serial}? It will be encrypted by Android Keystore. [y/N] ')
if answer.strip().lower() not in ('y', 'yes'):
    sys.exit('Canceled; no key was transferred.')
# The key is passed over stdin, never in shell arguments or tool output.
result = subprocess.run(base + ['shell', 'run-as', package, 'sh', '-c', "'umask 077; cat > files/openai-key-import'"],
                        input=key.encode(), stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
if result.returncode:
    sys.exit('Could not transfer the key. Install and open the debug app once, then try again.')
subprocess.run(base + ['shell', 'am', 'force-stop', package], stdout=subprocess.DEVNULL, check=True)
subprocess.run(base + ['shell', 'am', 'start', '-n', package + '/.MainActivity'], stdout=subprocess.DEVNULL, check=True)
print('Key transferred. The app encrypts it and deletes the temporary import on startup.')
