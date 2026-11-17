# Native Android fixture tests

The uploaded runtime-result.txt was historical evidence, not a new run.

From the android directory, run python3 setup-build.py then python3 get-emulator.py.
From repository root run python3 android/tests/prepare-test.py (requires a fresh android-test output directory). Then run bash android-test/tests/run-emulator.sh.

The preparation tool creates package id.simas.mobile.test and a separate ephemeral test signing key, and signs instrumentation with that key. It never reads the production key. Run with a single emulator attached. Android 10 software emulation is used when KVM is unavailable.

Fixtures exercise startup login, eight module renders, finance sums, Hijri year label, CSV formula escaping and totals, PDF header, date/zakat validation, zakat form rendering, and UI role checks. The runner disables mobile data and Wi-Fi before instrumentation. No real account login or production CRUD is exercised. These checks do not establish backend authorization correctness or complete PDF layout correctness.

Do not distribute the instrumentation APK. Screenshots use fictional fixtures. Test keystores, downloaded SDKs and emulator disks must stay out of GitHub.
