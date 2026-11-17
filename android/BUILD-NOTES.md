Build continuation 17 November 2026

Source supplied by owner, based on native 2.0. No feature changes in this rebuild. Production versionCode increased to 4. build.sh outputs an unsigned production APK and never generates a replacement production key. Sign it locally using the original signing.p12.

The separately signed test APK uses package id.simas.mobile.test and label SIMAS Uji, so it installs alongside production. It uses the same live Supabase database: authorized edits affect real data. Runtime login and CRUD have not been tested in this session. VERIFIKASI.txt describes historical tests only.
