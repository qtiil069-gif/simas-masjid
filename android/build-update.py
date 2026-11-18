"""Build updates compatible with the owner's installed SIMASJID 2.4 APK."""
import argparse
import re
import shutil
import subprocess
import tempfile
from pathlib import Path
from xml.sax.saxutils import quoteattr

PACKAGE = 'id.simas.mobile.modernpreview'
CERT = '28240dd1adaa15d784db2bbc51cc1e6883c417612c9c825bfafac035bc7bb9f2'
ROOT = Path(__file__).resolve().parent
TOOLS = ROOT / 'sdk/android-15'


def run(args):
    return subprocess.check_output([str(x) for x in args], text=True)


def apk_info(apk):
    verified = run([TOOLS / 'apksigner', 'verify', '--print-certs', apk])
    fingerprints = re.findall(r'certificate SHA-256 digest: ([0-9a-f]+)', verified)
    if fingerprints != [CERT]:
        raise ValueError('APK certificate does not match the installed 2.4 baseline.')
    badging = run([TOOLS / 'aapt', 'dump', 'badging', apk])
    match = re.search(r"package: name='([^']+)' versionCode='([0-9]+)'", badging)
    if not match or match[1] != PACKAGE:
        raise ValueError('APK package does not match the installed 2.4 baseline.')
    return int(match[2])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--signing-dir', required=True, type=Path)
    parser.add_argument('--previous-apk', required=True, type=Path)
    parser.add_argument('--version-code', required=True, type=int)
    parser.add_argument('--version-name', required=True)
    parser.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    previous = apk_info(args.previous_apk.resolve())
    if args.version_code <= max(previous, 7):
        parser.error('version-code must exceed the previous APK and baseline code 7.')
    if not re.fullmatch(r'[A-Za-z0-9._-]{1,50}', args.version_name):
        parser.error('Use a short version name containing letters, digits, dot, dash or underscore.')
    signing = args.signing_dir.resolve()
    key, password = signing / 'signing.p12', signing / 'signing-password.txt'
    if not key.is_file() or not password.is_file():
        parser.error('Restore the private 2.4 signing backup first. No new keys are generated.')
    output = args.output.resolve()
    if output.exists():
        parser.error('Output exists; choose a new output path.')
    output.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='simasjid-update-') as folder:
        target = Path(folder) / 'android'
        target.mkdir()
        shutil.copytree(ROOT / 'app', target / 'app')
        for name in ('sdk', 'ecj.jar'):
            (target / name).symlink_to((ROOT / name).resolve())
        for java in (target / 'app/src/id/simas/mobile').glob('*.java'):
            java.write_text(java.read_text().replace('package id.simas.mobile;', 'package ' + PACKAGE + ';'))
        manifest = target / 'app/AndroidManifest.xml'
        source = manifest.read_text().replace('package="id.simas.mobile"', 'package="' + PACKAGE + '"')
        source = re.sub(r'android:versionCode="[^"]+"', 'android:versionCode=' + quoteattr(str(args.version_code)), source)
        source = re.sub(r'android:versionName="[^"]+"', 'android:versionName=' + quoteattr(args.version_name), source)
        manifest.write_text(source)
        build = (ROOT / 'build.sh').read_text().split('cp out/aligned.apk')[0]
        build = build.replace('out/generated/id/simas/mobile/R.java', 'out/generated/id/simas/mobile/modernpreview/R.java')
        (target / 'build.sh').write_text(build)
        subprocess.run(['bash', str(target / 'build.sh')], check=True)
        signed = target / 'signed.apk'
        subprocess.run([str(TOOLS / 'apksigner'), 'sign', '--ks', str(key), '--ks-key-alias', 'simas', '--ks-pass', 'file:' + str(password), '--out', str(signed), str(target / 'out/aligned.apk')], check=True)
        if apk_info(signed) != args.version_code:
            raise ValueError('Unexpected version code after signing.')
        subprocess.run([str(TOOLS / 'zipalign'), '-c', '4', str(signed)], check=True)
        shutil.copyfile(signed, output)
    print('Verified update package, signing certificate and increasing version:', output)


if __name__ == '__main__':
    main()
