# Namaaz Rakah Tracker — Phase 1 Plan

## Problem Statement

People frequently lose count of their Rakah during Salah, leading to repeating the prayer or completing an incorrect number of Rakat. This app silently tracks postures using the phone camera and maintains an accurate Rakah count in real time — no tapping, no interruption to prayer.

---

## System Overview

```
Phone Camera
     │
     ▼
Android App (CameraX)
     │  captures frames
     ▼
Posture API (FastAPI + MediaPipe + body_language.pkl)
     │  returns posture label + confidence
     ▼
State Machine (Rakah Counter)
     │
     ▼
Compose UI (current Rakah, posture, progress)
```

---

## Phase 1 — Two Workstreams

### Workstream A — Posture Identification API
### Workstream B — Android Application

Both run in parallel; the API is a dependency the Android app consumes over HTTP.

---

## Workstream A: Posture Identification API

### Goal
Wrap the existing `Namaz-Posture-Identification/AI project/AI Prayer Posture Identification/body_language.pkl` Random Forest model behind a FastAPI REST endpoint. The Android app sends JPEG frames, the server returns the classified posture.

### Tech Stack
- **Python 3.11**
- **FastAPI** — REST framework
- **MediaPipe** — holistic pose extraction (same pipeline as the notebook)
- **scikit-learn** — loads and runs `body_language.pkl`
- **Uvicorn** — ASGI server
- **Docker** — containerised for portability

### API Contract

#### `POST /predict`
```
Request:
  Content-Type: multipart/form-data
  field "frame": JPEG image bytes

Response 200:
{
  "posture": "qiam" | "rukooh" | "sajdah" | "julsa" | "unknown",
  "confidence": 0.0–1.0
}

Response 422: validation error (bad image)
Response 500: mediapipe or model failure
```

#### `GET /health`
```
Response 200: { "status": "ok" }
```

### Server Directory Structure
```
api-server/
├── app/
│   ├── main.py             # FastAPI app, route registration
│   ├── routers/
│   │   └── predict.py      # /predict and /health endpoints
│   ├── services/
│   │   ├── mediapipe_service.py   # holistic pose extraction
│   │   └── posture_service.py     # loads pkl, runs prediction
│   └── schemas/
│       └── prediction.py   # Pydantic response models
├── ml/
│   └── body_language.pkl   # copied from Namaz-Posture-Identification
├── requirements.txt
├── Dockerfile
└── .env.example
```

### Posture Normalisation
The notebook labels are inconsistent (`Sajdah` vs `sajdah`, etc.). The API service normalises all labels to lowercase before returning them to the Android app.

### Feature Extraction Detail
The notebook's final model (test cell) uses **only pose landmarks** (33 landmarks × 4 values = 132 features), not face landmarks. The API service must replicate this exact extraction to match what `body_language.pkl` was trained on.

### Confidence Threshold
Responses with `confidence < 0.5` are returned as `"unknown"`. The Android state machine ignores `unknown` postures.

### Local Development
```bash
cd api-server
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

---

## Workstream B: Android Application

### Goal
An enterprise-grade Android application that:
1. Lets the user select Namaaz type (Fajr/Zuhr/Asr/Maghrib/Isha) to know the target Rakah count
2. Streams camera frames to the Posture API
3. Runs a Rakah-counting state machine on posture responses
4. Shows the user their current Rakah in a clean, distraction-free UI
5. Notifies (vibration only, no sound) when a Rakah is completed or the prayer is finished

### Tech Stack
| Concern | Library |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | Clean Architecture (Data / Domain / Presentation) |
| DI | Hilt |
| Camera | CameraX |
| Networking | Retrofit 2 + OkHttp |
| Async | Coroutines + Flow |
| State | ViewModel + StateFlow |
| Navigation | Navigation Compose |
| Build | Gradle (Kotlin DSL) |

### Module / Package Structure
```
com.namaaztracker/
├── data/
│   ├── remote/
│   │   ├── api/
│   │   │   └── PostureApi.kt          # Retrofit interface
│   │   ├── dto/
│   │   │   └── PosturePredictionDto.kt
│   │   └── datasource/
│   │       └── PostureRemoteDataSource.kt
│   └── repository/
│       └── PostureRepositoryImpl.kt
│
├── domain/
│   ├── model/
│   │   ├── Posture.kt                 # enum: QIAM, RUKOOH, SAJDAH, JULSA, UNKNOWN
│   │   ├── NamaazType.kt             # enum: FAJR(2), ZUHR(4), ASR(4), MAGHRIB(3), ISHA(4)
│   │   └── NamaazSession.kt          # current state of an active prayer session
│   ├── repository/
│   │   └── PostureRepository.kt      # interface
│   └── usecase/
│       ├── PredictPostureUseCase.kt
│       └── UpdateRakahStateUseCase.kt
│
├── presentation/
│   ├── navigation/
│   │   └── NavGraph.kt
│   ├── screens/
│   │   ├── home/
│   │   │   ├── HomeScreen.kt          # Namaaz type selection
│   │   │   └── HomeViewModel.kt
│   │   └── tracker/
│   │       ├── TrackerScreen.kt       # Live tracking UI
│   │       └── TrackerViewModel.kt
│   └── components/
│       ├── RakahIndicator.kt          # Rakah count display
│       ├── PostureBadge.kt            # Current posture pill
│       └── ProgressDots.kt           # Visual Rakah progress
│
├── di/
│   ├── NetworkModule.kt               # Hilt: provides Retrofit, OkHttp
│   └── RepositoryModule.kt            # Hilt: binds repo impl
│
└── util/
    ├── CameraFrameAnalyzer.kt         # ImageAnalysis.Analyzer impl
    └── ImageUtils.kt                  # Bitmap → JPEG bytes
```

### Rakah State Machine

A Rakah follows this posture sequence:
```
QIAM → RUKOOH → QIAM (i'tidal) → SAJDAH → JULSA → SAJDAH
```
After the 2nd Sajdah: Rakah count increments.

```
States: IDLE | QIAM | RUKOOH | ITIDAL | SAJDAH_1 | JULSA | SAJDAH_2 | TASHAHHUD | COMPLETE

Transitions (triggered by posture detection):
  IDLE       + start()    → QIAM          (rakah = 1)
  QIAM       + RUKOOH     → RUKOOH
  RUKOOH     + QIAM       → ITIDAL
  ITIDAL     + SAJDAH     → SAJDAH_1
  SAJDAH_1   + JULSA      → JULSA
  JULSA      + SAJDAH     → SAJDAH_2
  SAJDAH_2   + (auto)     → rakah++
                           → if rakah < total AND rakah not at tashahhud point → QIAM
                           → if tashahhud point (2nd rakah of 4-rakah) → TASHAHHUD
                           → if rakah == total → COMPLETE
  TASHAHHUD  + QIAM       → QIAM (continuing)

Tashahhud points:
  Fajr    (2):  after rakah 2 → COMPLETE
  Maghrib (3):  after rakah 2 → TASHAHHUD, after rakah 3 → COMPLETE
  Zuhr/Asr/Isha (4): after rakah 2 → TASHAHHUD, after rakah 4 → COMPLETE
```

Debounce rule: a posture must be held for **≥ 0.8 seconds** before a state transition fires. This prevents false triggers from transition frames.

### Camera Integration
- Uses `CameraX ImageAnalysis` use case
- Extracts frames at **4 fps** (sufficient for posture; avoids API overload)
- `CameraFrameAnalyzer` converts `ImageProxy → JPEG bytes` and sends to `PostureRemoteDataSource`
- Camera preview is hidden (or shown minimised); the UI focus is the Rakah counter

### UI Screens

#### Home Screen
- Clean card grid: Fajr / Zuhr / Asr / Maghrib / Isha
- Each card shows Namaaz name + Rakah count (e.g. "2 Rakat")
- Tapping a card requests camera permission then navigates to Tracker

#### Tracker Screen
- Large centred Rakah display: **"Rakah 2 of 4"**
- Posture badge (bottom): shows current detected posture
- Progress dots (top): filled dots for completed Rakat
- On completion: full-screen "Alhamdulillah" with a gentle fade and haptic
- Subtle vibration on each Rakah completion

### Haptics (no audio)
- Short vibration (50ms) on Rakah complete
- Long vibration (200ms) on full Namaaz complete
- No sound — the app operates silently during prayer

---

## Sequence Diagram

```
User                 Android App              Posture API
 │                       │                        │
 │  select Zuhr (4)      │                        │
 │──────────────────────►│                        │
 │                       │  start camera           │
 │                       │  start session (4 rakat)│
 │                       │                        │
 │                       │  [every 250ms]          │
 │                       │  POST /predict (frame) ─►│
 │                       │◄── {posture, confidence} │
 │                       │                        │
 │                       │  StateMachine.process(posture)
 │                       │  → transition if debounce passed
 │                       │  → if SAJDAH_2: rakah++  │
 │                       │                        │
 │◄── UI: "Rakah 2 of 4" │                        │
 │◄── haptic             │                        │
```

---

## Implementation Steps

### Step 1 — API Server Setup
1. Create `api-server/` directory
2. Write `requirements.txt` (fastapi, uvicorn, mediapipe, scikit-learn, opencv-python-headless, numpy, pandas, python-multipart)
3. Copy `body_language.pkl` into `api-server/ml/`
4. Implement `mediapipe_service.py` — replicates the notebook's pose extraction (pose landmarks only, 132 features)
5. Implement `posture_service.py` — loads pkl, normalises labels, applies confidence threshold
6. Implement `predict.py` router
7. Write `main.py` — FastAPI app with CORS enabled (Android emulator needs `10.0.2.2`)
8. Test locally: `curl -F "frame=@test.jpg" http://localhost:8000/predict`
9. Write `Dockerfile`

### Step 2 — Android Project Bootstrapping
1. Create new Android project in Android Studio: **Empty Activity**, Kotlin, min SDK 26
2. Set up Gradle (Kotlin DSL): add Compose, Hilt, CameraX, Retrofit, Coroutines dependencies
3. Create package structure as defined above
4. Set up Hilt application class and manifest entries
5. Implement `NetworkModule` pointing to `http://10.0.2.2:8000` (emulator) / configurable base URL

### Step 3 — Data Layer
1. Define `PostureApi.kt` Retrofit interface (`@Multipart POST /predict`)
2. Implement `PostureRemoteDataSource.kt` — suspending function, converts bitmap to multipart
3. Implement `PostureRepositoryImpl.kt` — wraps data source, maps DTO to domain model

### Step 4 — Domain Layer
1. Define `Posture`, `NamaazType`, `NamaazSession` models
2. Implement `UpdateRakahStateUseCase` — the full state machine logic (pure Kotlin, no Android deps, unit-testable)
3. Implement `PredictPostureUseCase`

### Step 5 — Camera Integration
1. Implement `CameraFrameAnalyzer` — `ImageAnalysis.Analyzer`, throttles to 4fps, publishes frame bytes via `StateFlow`
2. Implement `ImageUtils.kt` — `ImageProxy → Bitmap → JPEG ByteArray`

### Step 6 — Presentation Layer
1. `HomeViewModel` — holds list of `NamaazType`, emits selection event
2. `HomeScreen` — Compose grid of Namaaz cards
3. `TrackerViewModel` — combines camera frames + API calls + state machine; exposes `TrackerUiState`
4. `TrackerScreen` — Compose screen observing `TrackerUiState`
5. `RakahIndicator`, `PostureBadge`, `ProgressDots` — reusable composables

### Step 7 — Navigation
1. `NavGraph.kt` — two destinations: `home` and `tracker/{namaazType}`
2. Back navigation from tracker stops camera and resets session

### Step 8 — Integration Testing
1. Run API server locally
2. Run Android app on emulator or physical device
3. Walk through each posture manually in front of camera
4. Verify state transitions and Rakah count increments correctly
5. Test all 5 Namaaz types end-to-end

---

## Configuration

### API Base URL
Stored in `local.properties` (not committed):
```
API_BASE_URL=http://10.0.2.2:8000/   # emulator default
```
Injected at build time via `BuildConfig`. Real device on same network would use the Mac's LAN IP.

### Permissions (AndroidManifest.xml)
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.VIBRATE" />
```

---

## What Phase 1 Does NOT Include
- User accounts or login
- Prayer history storage
- Qibla direction
- Azan / prayer times
- Audio feedback
- On-device ML inference (deferred: would remove API dependency)
- Sunnah/Nafl Namaaz tracking
- Error recovery UI for lost API connection

These are explicitly Phase 2+ to keep Phase 1 focused and shippable.

---

## Definition of Done — Phase 1

- [ ] `POST /predict` returns correct posture for Sajdah, Rukooh, Qiam, Julsa test images
- [ ] Android app builds without warnings on API 26+ target
- [ ] Selecting a Namaaz type and walking through all postures increments Rakah count correctly
- [ ] Completion screen appears after final Rakah with haptic
- [ ] State machine unit tests pass for all Namaaz types
- [ ] Code follows Clean Architecture boundaries (domain layer has zero Android imports)
