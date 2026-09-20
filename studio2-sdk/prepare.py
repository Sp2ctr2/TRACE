"""Exact Studio 2 exported source fixture; unpack data only, then normal Gradle builds it."""
from pathlib import Path
import base64, hashlib, json, lzma, shutil
base=Path(__file__).parent
encoded=''.join(p.read_text().strip() for p in sorted(base.glob('source.*.b64')))
raw=lzma.decompress(base64.b64decode(encoded,validate=True))
assert hashlib.sha256(raw).hexdigest()=='ef36b1cc39912fc1d14a7f8111e913e5b742f78dd4565354fda93a403a910192','source fixture checksum mismatch'
files=json.loads(raw)
root=Path('studio-sdk-project');root.mkdir(exist_ok=True)
for name,text in files.items():
 path=root/name
 assert root.resolve() in path.resolve().parents and '..' not in Path(name).parts
 path.parent.mkdir(parents=True,exist_ok=True);path.write_text(text)
shutil.copyfile('android-native/gradle/wrapper/gradle-wrapper.jar',root/'gradle/wrapper/gradle-wrapper.jar')
print('Exact exported project files:',len(files))
print('Source fixture SHA-256:',hashlib.sha256(raw).hexdigest())
