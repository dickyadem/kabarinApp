# Kabarin 💬

Aplikasi Android native untuk pasangan agar selalu terhubung lewat update status singkat. Cukup satu tap untuk kasih tahu pasangan kamu lagi ngapain — **Kerja, Di Jalan, Makan, Istirahat,** atau **Lainnya** — lengkap dengan catatan & lokasi opsional.

> Ringan, hemat baterai, tanpa server berbayar. Sinkronisasi real-time pakai Firebase Firestore (free Spark plan).

---

## ✨ Fitur Utama

| Fitur | Deskripsi |
|-------|-----------|
| **Quick Status Update** | Update status sekali tap (Kerja/Di Jalan/Makan/Istirahat/Lainnya) |
| **Catatan & Lokasi** | Tambah catatan singkat + tag lokasi (opt-in, hemat privasi) |
| **Status Pasangan** | Lihat status terbaru pasangan langsung di home (seperti WhatsApp/Telegram) |
| **Notifikasi** | Dapat notifikasi saat pasangan update status (via WorkManager, hemat baterai) |
| **Smart Reminder** | Pengingat berkala untuk update status |
| **Pairing 6-Digit** | Hubungkan dua device dengan kode pairing, tanpa daftar akun |
| **Nickname** | Atur panggilan untuk ditampilkan ke pasangan |
| **Riwayat Harian** | History status hanya untuk hari ini (auto-cleanup) |

---

## 🛠️ Tech Stack

### Bahasa & UI
- **Kotlin**
- **Jetpack Compose** + **Material 3** (Material Design 3)
- **Material Icons Extended** (style outlined yang konsisten)

### Arsitektur
- **MVVM** (Model–View–ViewModel)
- **StateFlow** untuk state management reaktif
- **Hilt** untuk Dependency Injection

### Data & Sinkronisasi
- **Firebase Firestore** — sinkronisasi status real-time antar device (free Spark plan)
- **Room Database** — penyimpanan riwayat lokal (today-only)
- **DataStore / SharedPreferences** — penyimpanan setting & profil ringan

### Background Work
- **WorkManager**
  - `ReminderWorker` — pengingat update status (interval 1 jam)
  - `PartnerCheckWorker` — cek update pasangan (interval 15 menit, minimum WorkManager)

### Lainnya
- **FusedLocationProvider** + **Geocoder** — tag lokasi (opt-in)
- **Jetpack Navigation Compose** — navigasi antar layar

---

## 📁 Struktur Project

```
app/src/main/java/com/kabarinpacar/app/
├── KabarinApplication.kt          # Entry point, schedule WorkManager
├── MainActivity.kt                # Host Compose, request izin notifikasi
│
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt         # Room database
│   │   └── StatusDao.kt           # DAO query status harian
│   ├── model/
│   │   ├── StatusUpdate.kt        # Entity + enum ActivityType
│   │   └── UserProfile.kt         # PartnerStatus data class
│   └── repository/
│       └── StatusRepository.kt    # Single source of truth (Room + Firestore + Prefs)
│
├── di/
│   └── AppModule.kt               # Hilt module
│
├── ui/
│   ├── navigation/NavGraph.kt     # Rute navigasi
│   ├── screen/                    # Splash, Onboarding, Home, Partner, History
│   ├── theme/                     # Color, Theme, Type
│   ├── util/ActivityIcon.kt       # Mapping ActivityType → icon + warna
│   └── viewmodel/                 # MainViewModel, OnboardingViewModel
│
└── worker/
    ├── ReminderWorker.kt          # Pengingat update status
    └── PartnerCheckWorker.kt      # Cek & notif update pasangan
```

---

## 🔄 Alur Kerja (Workflow)

### 1. Onboarding & Pairing

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   Splash    │ ──> │   Onboarding     │ ──> │      Home       │
│ (cek paired)│     │  (pairing code)  │     │ (kalau sudah    │
└─────────────┘     └──────────────────┘     │     paired)     │
                                              └─────────────────┘
```

**Proses Pairing (butuh 2 device):**

```
DEVICE A (pembuat)                    DEVICE B (penggabung)
─────────────────                     ─────────────────────
1. Generate kode 6-digit
2. Tulis ke Firestore:
   pairing_codes/{kode}
3. Pasang snapshot listener ──┐
   (menunggu...)              │
                              │       4. Input kode 6-digit
                              │       5. Baca pairing_codes/{kode}
                              │       6. Buat dokumen pairs/{pairId}
                              │       7. Set partnerId masing-masing
   8. Listener trigger! <─────┘
   9. Tersimpan: pairId,
      partnerId, isPaired=true
```

### 2. Update Status

```
User pilih activity (tap chip)
        │
        ├─> (opsional) ambil lokasi via FusedLocationProvider
        │
        v
  submitStatus()
        │
        ├─> Simpan ke Room (lokal, riwayat harian)
        │
        └─> Push ke Firestore:
            pairs/{pairId}/status/{userId}
            { activity, note, locationName, timestamp, userId, nickname }
```

### 3. Terima Status Pasangan

Ada **dua jalur** agar status pasangan selalu update:

```
JALUR A — Live (saat app dibuka)
────────────────────────────────
Firestore snapshot listener
        │
        v
MainViewModel.partnerStatus (StateFlow)
        │
        ├─> Tampil di Home (PartnerStatusCard)
        └─> Majukan partnerLastSeenTimestamp
            (cegah notif duplikat)

JALUR B — Background (app ditutup)
──────────────────────────────────
PartnerCheckWorker (tiap 15 menit)
        │
        ├─> fetchPartnerStatusOnce() dari Firestore
        ├─> Bandingkan timestamp > lastSeen?
        │       │
        │       ├─ Ya  ─> Kirim notifikasi + majukan lastSeen
        │       └─ Tidak ─> Skip
```

### 4. Reminder

```
ReminderWorker (tiap 1 jam)
        │
        └─> Kirim notifikasi pengingat untuk update status
```

---

## 🗄️ Struktur Data Firestore

```
pairing_codes/
  {6-digit-code}          # sementara, untuk proses pairing
    └── creatorId, pairId, timestamp

pairs/
  {pairId}/
    status/
      {userId}            # status terbaru tiap user
        ├── activity      # KERJA | DI_JALAN | MAKAN | ISTIRAHAT | LAINNYA
        ├── note          # catatan singkat
        ├── locationName  # nama area (opsional)
        ├── timestamp     # waktu update (epoch millis)
        ├── userId
        └── nickname      # panggilan untuk ditampilkan
```

---

## ⚙️ Setup & Build

### Prasyarat
- Android Studio (Hedgehog atau lebih baru)
- JDK 11+
- File `google-services.json` dari Firebase Console (sudah disertakan untuk project `kabarinapp-cd648`)

### Konfigurasi
- `applicationId` : `com.kabarinpacar.app`
- `minSdk` : 26 (Android 8.0)
- `targetSdk` / `compileSdk` : 35

### Build Debug APK

**Lewat Android Studio:**
```
Build → Generate App Bundles or APKs → Generate APKs
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

**Lewat terminal:**
```bash
./gradlew assembleDebug
```

### Build Release APK
```
Build → Generate Signed App Bundle or APK → APK
```
Perlu keystore (buat baru lewat tombol "Create new..." kalau belum ada).
Output: `app/build/outputs/apk/release/app-release.apk`

> ⚠️ Simpan keystore baik-baik — kalau hilang, app tidak bisa di-update di Play Store.

### Install ke HP
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔋 Catatan Desain

- **Kenapa WorkManager, bukan FCM?** FCM butuh server (Cloud Functions = paid plan). WorkManager gratis & cukup untuk kebutuhan ini. Trade-off: ada delay maksimal ~15 menit untuk notifikasi background.
- **Kenapa WorkManager, bukan Foreground Service?** Foreground service menampilkan notifikasi persisten di status bar dan lebih boros baterai. WorkManager lebih hemat.
- **Privasi lokasi:** tag lokasi bersifat **opt-in**. Kalau tidak diaktifkan, lokasi tidak pernah diambil/dikirim.
- **Riwayat harian saja:** data status lama auto-cleanup, hanya menyimpan hari ini.
