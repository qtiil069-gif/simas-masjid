#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
TOOLS="$PWD/sdk/android-15"
JAR="$PWD/sdk/android-35/android.jar"
rm -rf out/classes out/generated out/dex out/classes.jar
mkdir -p out/classes out/generated out/dex ../deliverables/android
"$TOOLS/aapt" package -f -m -J out/generated -M app/AndroidManifest.xml -S app/res -I "$JAR"
java -jar ecj.jar -warn:none -source 8 -target 8 -bootclasspath "$JAR:$TOOLS/core-lambda-stubs.jar" -d out/classes app/src/id/simas/mobile/*.java out/generated/id/simas/mobile/R.java
(cd out/classes && zip -qr ../classes.jar .)
"$TOOLS/d8" --lib "$JAR" --min-api 23 --output out/dex out/classes.jar
"$TOOLS/aapt" package -f -M app/AndroidManifest.xml -S app/res -0 arsc -I "$JAR" -F out/unsigned.apk
(cd out/dex && zip -q -u ../unsigned.apk classes.dex)
"$TOOLS/zipalign" -f 4 out/unsigned.apk out/aligned.apk
cp out/aligned.apk ../deliverables/android/SIMASJID-2.4-unsigned.apk
echo "Unsigned APK ready; sign with the existing SIMAS key before installation."
