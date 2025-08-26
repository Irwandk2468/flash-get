# Firebase Functions (v2)
- `rollupDailySummary` — rekap harian (Asia/Jakarta) untuk hari sebelumnya: `notif`, `topApps`, dan **`hours`** (histogram 24 jam untuk `app_foreground`).
- `aiSummarizeDaily` — opsional; tulis ringkasan AI singkat (3 kalimat) ke `summaries/{day}.ai`. Pakai `OPENAI_API_KEY` env atau `functions.config().openai.key`.
- `purgeOldEvents` — hapus event lebih dari 30 hari.

## Deploy via GitHub Actions
Secrets yang dibutuhkan:
- `FIREBASE_SERVICE_ACCOUNT` — JSON Service Account
- `PROJECT_ID` — Firebase Project ID
- (opsional) `OPENAI_API_KEY` — untuk `aiSummarizeDaily`
