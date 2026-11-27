# Shaxsiy Pleyer

Oflayn musiqa pleyeri: izolyatsiyalangan **Guruhlar**, **-10s / +10s**, uyqu taymeri, ijro tezligi, JSON zaxira nusxa va **Yandex Music** (Media3 orqali).
Tablar: Guruhlar · Musiqalar · Yandex. (Spotify olib tashlangan.)

## APK olish (Android Studio'siz)
1. Bu papkani GitHub'dagi yangi repo'ga yuklang (`.github` papkasi ham bilan).
2. Repo → **Actions** → **Build APK** → ish tugagach **Artifacts** ichidan `ShaxsiyPlayer-debug-apk` ni yuklab oling.
3. Ichidagi `app-debug.apk` ni telefonga o‘rnating.

Lokal: `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`

## Yandex Music ulash
1. Ilovada **Yandex** tabini oching → **Brauzerda Token olish**.
2. Yandex hisobiga kiring; manzil satridagi to‘liq havolani (yoki `access_token=...`) nusxalang.
3. Ilovadagi maydonga joylab, **Yandex Music ulash** ni bosing.
