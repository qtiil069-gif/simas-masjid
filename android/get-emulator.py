import urllib.request,zipfile,concurrent.futures,pathlib
base=pathlib.Path(__file__).resolve().parent/'sdk'
base.mkdir(parents=True, exist_ok=True)
def get(item):
 url,name,dest=item
 p=base/name
 if not p.exists():urllib.request.urlretrieve(url,p)
 with zipfile.ZipFile(p) as z:
  target=(base/dest).resolve()
  for member in z.infolist():
   out=(target/member.filename).resolve()
   if not out.is_relative_to(target): raise ValueError('Unsafe archive path')
   z.extract(member,target)
   mode=member.external_attr>>16
   if mode and not member.is_dir():out.chmod(mode&0o777)
 return name
jobs=[('https://dl.google.com/android/repository/emulator-linux_x64-16740729.zip','emulator.zip',''),('https://dl.google.com/android/repository/sys-img/android/x86_64-29_r08-linux.zip','system29.zip','system-images/android-29/default'),('https://dl.google.com/android/repository/platform-tools-latest-linux.zip','platform-tools.zip','')]
with concurrent.futures.ThreadPoolExecutor(3) as pool:
 for name in pool.map(get,jobs):print(name,flush=True)
