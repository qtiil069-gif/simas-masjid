#!/bin/bash
set -e
cd "$(dirname "$0")/.."
TOOLS="$PWD/sdk/android-15"
JAR="$PWD/sdk/android-35/android.jar"
rm -rf tests/classes tests/dex
mkdir -p tests/classes tests/dex
java -jar ecj.jar -source 8 -target 8 -bootclasspath "$JAR:$TOOLS/core-lambda-stubs.jar" -cp out/classes.jar -d tests/classes tests/NativeTests.java
(cd tests/classes && zip -qr ../tests.jar .)
"$TOOLS/d8" --lib "$JAR" --min-api 23 --classpath out/classes.jar --output tests/dex tests/tests.jar
"$TOOLS/aapt" package -f -M tests/AndroidManifest.xml -I "$JAR" -F tests/unsigned.apk
(cd tests/dex && zip -q -u ../unsigned.apk classes.dex)
"$TOOLS/zipalign" -f 4 tests/unsigned.apk tests/aligned.apk
"$TOOLS/apksigner" sign --ks signing.p12 --ks-pass file:signing-password.txt --out tests/tests.apk tests/aligned.apk
