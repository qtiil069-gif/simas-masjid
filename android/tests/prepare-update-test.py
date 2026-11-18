"""Compile instrumentation matching the fixed SIMASJID 2.4 update package."""
from pathlib import Path
import argparse, shutil, subprocess
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--signing-dir',required=True,type=Path)
a=p.parse_args()
root=Path(__file__).resolve().parent.parent
target=root.parent/'android-update-test'
if target.exists():raise SystemExit('Use a fresh android-update-test directory.')
target.mkdir()
for name in ('app','tests','avd'):shutil.copytree(root/name,target/name)
for name in ('sdk','ecj.jar'):(target/name).symlink_to((root/name).resolve())
for name in ('signing.p12','signing-password.txt'):(target/name).symlink_to((a.signing_dir/name).resolve())
for java in (target/'app/src/id/simas/mobile').glob('*.java'):
 java.write_text(java.read_text().replace('package id.simas.mobile;', 'package id.simas.mobile.modernpreview;'))
f=target/'app/AndroidManifest.xml';f.write_text(f.read_text().replace('package="id.simas.mobile"','package="id.simas.mobile.modernpreview"'))
f=target/'tests/NativeTests.java';f.write_text(f.read_text().replace('package id.simas.mobile;','package id.simas.mobile.modernpreview;'))
f=target/'tests/AndroidManifest.xml';f.write_text(f.read_text().replace('id.simas.mobile.tests','id.simas.mobile.modernpreview.tests').replace('id.simas.mobile.NativeTests','id.simas.mobile.modernpreview.NativeTests').replace('targetPackage="id.simas.mobile"','targetPackage="id.simas.mobile.modernpreview"'))
build=(root/'build.sh').read_text().split('cp out/aligned.apk')[0].replace('out/generated/id/simas/mobile/R.java','out/generated/id/simas/mobile/modernpreview/R.java')
(target/'build.sh').write_text(build)
subprocess.run(['bash',str(target/'build.sh')],check=True)
subprocess.run(['bash',str(target/'tests/build-test.sh')],check=True)
