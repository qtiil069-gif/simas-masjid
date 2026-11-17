"""Create isolated package and ephemeral signing key for instrumentation."""
from pathlib import Path
import shutil, subprocess, secrets
root=Path(__file__).resolve().parent.parent
target=root.parent/'android-test'
if target.exists():
 raise SystemExit('android-test already exists; use a fresh checkout to avoid replacing work.')
target.mkdir()
for name in ('app','tests','avd'):
 shutil.copytree(root/name,target/name)
for name in ('sdk','ecj.jar'):
 (target/name).symlink_to((root/name).resolve())
for f in (target/'app/src/id/simas/mobile').glob('*.java'):
 f.write_text(f.read_text().replace('package id.simas.mobile;', 'package id.simas.mobile.test;'))
m=target/'app/AndroidManifest.xml'
m.write_text(m.read_text().replace('package="id.simas.mobile"', 'package="id.simas.mobile.test"').replace('android:label="SIMAS"', 'android:label="SIMAS Uji"'))
t=target/'tests/NativeTests.java'
t.write_text(t.read_text().replace('package id.simas.mobile;', 'package id.simas.mobile.test;'))
m=target/'tests/AndroidManifest.xml'
m.write_text(m.read_text().replace('id.simas.mobile.tests','id.simas.mobile.test.tests').replace('id.simas.mobile.NativeTests','id.simas.mobile.test.NativeTests').replace('targetPackage="id.simas.mobile"','targetPackage="id.simas.mobile.test"'))
s=(root/'build.sh').read_text().replace('out/generated/id/simas/mobile/R.java','out/generated/id/simas/mobile/test/R.java')
s=s[:s.index('cp out/aligned.apk')]
(target/'build.sh').write_text(s)
password=target/'signing-password.txt'
password.write_text(secrets.token_urlsafe(32))
password.chmod(0o600)
subprocess.run(['keytool','-genkeypair','-keystore',str(target/'signing.p12'),'-storepass:file',str(password),'-alias','simas','-keyalg','RSA','-keysize','3072','-validity','365','-dname','CN=SIMAS Test'],check=True)
subprocess.run(['bash',str(target/'build.sh')],check=True)
subprocess.run([str(target/'sdk/android-15/apksigner'),'sign','--ks',str(target/'signing.p12'),'--ks-pass','file:'+str(password),'--out',str(root.parent/'deliverables/android/SIMAS-2.1-Uji.apk'),str(target/'out/aligned.apk')],check=True)
subprocess.run(['bash',str(target/'tests/build-test.sh')],check=True)
print('Run bash android-test/tests/run-emulator.sh from the repository root.')
