#!/usr/bin/env python3
"""Resolve the website's pinned typeface at BUILD time, never at app runtime.

Binary resources are generated under build/ and excluded from the source bundle.
The upstream commit and Git blob hashes are pinned; a mismatched download fails.
"""
from pathlib import Path
import hashlib
import sys
import time
import urllib.request

COMMIT = '5c41199ea0024a9e0b2cb31735265056e5472d76'
BASE = f'https://raw.githubusercontent.com/orioncactus/pretendard/{COMMIT}/'
FILES = {
    'Regular': '08bf4cfc2164a0bff74a4bf844128d3b843bbf87',
    'Medium': '057506983f0a8b0438fc5f24382db64497bf6eae',
    'SemiBold': 'e7e36abc474796c8b6c383380ddd9f87655682ec',
    'Bold': '8e5e30a28b4435359945cf08b261c86d0de8da53',
}

def blob_hash(data: bytes) -> str:
    return hashlib.sha1(f'blob {len(data)}\0'.encode() + data).hexdigest()

def resolve(relative: str, destination: Path, expected: str) -> None:
    if destination.exists() and blob_hash(destination.read_bytes()) == expected:
        return
    for attempt in range(3):
        try:
            request = urllib.request.Request(BASE + relative, headers={'User-Agent': 'SAEON-reproducible-build'})
            with urllib.request.urlopen(request, timeout=45) as response:
                data = response.read()
            if blob_hash(data) != expected:
                raise ValueError(f'Integrity mismatch for {relative}')
            destination.parent.mkdir(parents=True, exist_ok=True)
            temp = destination.with_suffix('.tmp')
            temp.write_bytes(data)
            temp.replace(destination)
            return
        except Exception:
            if attempt == 2:
                raise
            time.sleep(2 * (attempt + 1))

root = Path(sys.argv[1])
for weight, digest in FILES.items():
    resolve(f'packages/pretendard/dist/public/static/Pretendard-{weight}.otf',
            root / 'font' / f'pretendard_{weight.lower()}.otf', digest)
resolve('LICENSE', root / 'raw' / 'pretendard_license.txt', 'c0592ca0072da4537bdc064eb935d88e9bb4361d')
(root / 'raw' / 'keep.xml').write_text('<resources xmlns:tools="http://schemas.android.com/tools" tools:keep="@raw/pretendard_license" />\n')
print('Pinned Pretendard resources verified for offline application use.')
