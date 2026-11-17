"""Download pinned build dependencies; requires acceptance of Android SDK terms."""
from pathlib import Path
import urllib.request, hashlib, zipfile, os
root=Path(__file__).resolve().parent
items=[
 ('https://repo.maven.apache.org/maven2/org/eclipse/jdt/ecj/3.38.0/ecj-3.38.0.jar','ecj.jar','97c566b120009c203a2fc8b291f4a9adbc171cf1ccb70f06f6b4e1828c00ce8e'),
 ('https://dl.google.com/android/repository/platform-35_r02.zip','platform.zip','0988cacad01b38a18a47bac14a0695f246bc76c1b06c0eeb8eb0dc825ab0c8e0'),
 ('https://dl.google.com/android/repository/build-tools_r35_linux.zip','tools.zip','bd3a4966912eb8b30ed0d00b0cda6b6543b949d5ffe00bea54c04c81e1561d88')]
for url,name,digest in items:
 path=root/name
 if not path.exists():urllib.request.urlretrieve(url,path)
 if hashlib.sha256(path.read_bytes()).hexdigest()!=digest:raise SystemExit('Checksum mismatch: '+name)
 if name.endswith('.zip'):
  with zipfile.ZipFile(path) as z:
   for member in z.infolist():
    dest=(root/'sdk'/member.filename).resolve()
    if not dest.is_relative_to((root/'sdk').resolve()):raise SystemExit('Unsafe archive entry')
    z.extract(member,root/'sdk')
    mode=member.external_attr>>16
    if mode and not member.is_dir():os.chmod(dest,mode&0o777)
 print('Ready:',name)
