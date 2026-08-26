<div align="center">

<img src="app/src/main/res/drawable/LogoGymerasinfondo.png" alt="Gymera Logo" width="220"/>

# **GYMERA**

### Tu entrenador personal con IA
### Your AI-powered personal trainer

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue?logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024-4285F4?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![API](https://img.shields.io/badge/Min%20SDK-26-green)](https://developer.android.com/about/versions/oreo)
[![Architecture](https://img.shields.io/badge/MVVM%20+%20Hilt-purple)](https://developer.android.com/topic/architecture)
[![Firebase](https://img.shields.io/badge/Firebase-Auth%20%2F%20Firestore-FFCA28?logo=firebase)](https://firebase.google.com)

</div>

---

## Capturas / Screenshots

<div align="center">

| Pantalla de Rutina | Detalle de Ejercicio | Perfil de Usuario |
|:---:|:---:|:---:|
| ![Rutina](docs/screenshots/routine.png) | ![Detalle](docs/screenshots/exercise_detail.png) | ![Perfil](docs/screenshots/profile.png) |

| Formulario IA | Búsqueda de Ejercicios | Generando Rutina |
|:---:|:---:|:---:|
| ![Formulario](docs/screenshots/form.png) | ![Búsqueda](docs/screenshots/search.png) | ![Loading](docs/screenshots/loading.png) |

</div>

> **Nota:** Agregá tus capturas en `docs/screenshots/` con los nombres indicados arriba.

---

## Descripción General

**Gymera** es una aplicación Android que genera rutinas de entrenamiento personalizadas utilizando inteligencia artificial. El usuario completa un formulario con sus datos físicos y preferencias de entrenamiento; la app consulta una cadena de modelos de IA (Groq y Gemini) que genera una rutina de 7 días adaptada a su perfil, respetando lesiones, nivel de experiencia y objetivos.

La rutina se guarda localmente en Room Database y se sincroniza en la nube con Firebase Firestore, permitiendo acceso multi-dispositivo. Incluye una base de datos de **873 ejercicios** con imágenes, instrucciones en español e inglés, y búsqueda inteligente con fuzzy matching.

## General Description

**Gymera** is an Android application that generates personalized workout routines using artificial intelligence. The user fills out a form with their physical data and training preferences; the app queries a chain of AI models (Groq and Gemini) that generates a 7-day routine tailored to their profile, respecting injuries, experience level, and goals.

The routine is stored locally in Room Database and synced to the cloud via Firebase Firestore, enabling multi-device access. It includes a database of **873 exercises** with images, instructions in Spanish and English, and smart search with fuzzy matching.

---

## Características Principales / Key Features

### Generación de Rutinas con IA / AI Routine Generation
- Formulario de 8 pasos: género, fecha de nacimiento, peso, altura, objetivo, frecuencia, duración y nivel
- Cadena de **5 modelos de IA** con failover automático (ver [Sistema de IA](#-sistema-de-ia--ai-system))
- Prompt que respeta lesiones, preferencias y perfil físico del usuario
- Regeneración rápida sin volver a completar todos los datos

### Base de Datos de Ejercicios / Exercise Database
- **873 ejercicios** bundleados en el APK (sin conexión a internet necesaria)
- Imágenes animadas (2 por ejercicio) cargadas desde [free-exercise-db](https://github.com/yuhonas/free-exercise-db)
- Instrucciones paso a paso traducidas al español con toggle EN/ES
- Cache de 3 niveles: RAM → Room → Asset (carga instantánea en uso diario)

### Búsqueda Inteligente / Smart Search
- Fuzzy matching con stemming inglés (resuelve plurales: "flyes" → "fly")
- Búsqueda en español: mapea nombres de músculos y equipos del español al inglés
- Filtro por grupo muscular con debounce de 300ms

### Gestión de Rutina / Routine Management
- Vista semanal de 7 días con swipe para activar/desactivar días de descanso
- Arrastrar para reordenar ejercicios dentro de un día
- Mover ejercicios entre días preservando la descripción del entrenamiento
- Descripción editable por día (ej: "Pecho y Tríceps")

### Perfil y Físico / Profile & Physical Data
- Inicio de sesión con Google (Firebase Auth)
- Cálculo de IMC con categoría (Bajo peso / Normal / Sobrepeso / Obesidad)
- Recomendaciones de carga por grupo muscular basadas en peso, altura, objetivo y nivel
- Sincronización de datos físicos con Firestore

---

## Arquitectura / Architecture

```
┌─────────────────────────────────────────────────────────┐
│                        UI Layer                          │
│  ┌─────────┐ ┌──────────┐ ┌────────┐ ┌──────────────┐  │
│  │ Screens  │ │ViewModels│ │Compose │ │ SharedVM     │  │
│  │ (10)     │ │ (11)     │ │Components│ │ (NavGraph)  │  │
│  └────┬─────┘ └────┬─────┘ └────────┘ └──────┬───────┘  │
│       │             │                          │          │
├───────┼─────────────┼──────────────────────────┼──────────┤
│       │       Domain Layer                     │          │
│  ┌────┴──────────────┴──────────────────────────┴──────┐  │
│  │  Models (Routine, WorkoutDay, Exercise, Profile)    │  │
│  │  Repository Interfaces                             │  │
│  └────────────────────┬────────────────────────────────┘  │
│                       │                                   │
├───────────────────────┼───────────────────────────────────┤
│                  Data Layer                               │
│  ┌──────────┐  ┌──────┴──────┐  ┌────────────────────┐  │
│  │  Room DB  │  │Repositories │  │  Firebase          │  │
│  │ 5 entities│  │Impl (4)     │  │  Auth + Firestore  │  │
│  │ 3 DAOs    │  │             │  │                    │  │
│  └──────────┘  └──────┬──────┘  └────────────────────┘  │
│                       │                                   │
│              ┌────────┴────────┐                          │
│              │  AI Providers   │                          │
│              │  Failover Chain │                          │
│              │  Groq → Gemini  │                          │
│              └─────────────────┘                          │
└─────────────────────────────────────────────────────────┘
```

### Patrones / Patterns

| Patrón | Uso |
|--------|-----|
| **MVVM** | ViewModel + StateFlow + @Composable en todas las pantallas |
| **Clean Architecture (lite)** | `domain/` (modelos + interfaces), `data/` (implementaciones), `ui/` (pantallas + componentes) |
| **Repository Pattern** | Interfaces en `domain/repository`, impl en `data/repository`, vinculadas con Hilt `@Binds` |
| **Hilt DI** | `@HiltAndroidApp`, `@HiltViewModel`, `@Module` con `@Provides` y `@Binds` |
| **Single Activity** | Una sola `MainActivity` con Navigation Compose |
| **Offline-First** | Room como fuente de verdad local; Firestore sync en background |

### Stack Tecnológico / Tech Stack

| Categoría | Tecnología | Versión |
|-----------|-----------|---------|
| Lenguaje | Kotlin | 2.0.21 |
| UI | Jetpack Compose + Material3 | BOM 2024.09 |
| Navegación | Navigation Compose | 2.7.7 |
| DI | Hilt (Dagger) | 2.51.1 |
| Base de datos local | Room | 2.6.1 |
| Base de datos nube | Firebase Firestore | BOM 32.8 |
| Autenticación | Firebase Auth + Google Sign-In | — |
| Red | Retrofit + OkHttp | 2.9.0 / 4.12.0 |
| Imágenes | Glide Compose | 1.0.0-beta01 |
| IA | Groq API + Gemini API | REST |
| Reorder | reorderable | 2.4.0 |
| Testing | JUnit 4 + MockK + Coroutines Test | 4.13.2 / 1.13.10 |

---

## Flujo de Navegación / Navigation Flow

```
                    ┌─────────┐
                    │ Splash  │
                    └────┬────┘
                         │
            ┌────────────┼────────────┐
            │            │            │
       Sin usuario  Sin rutina   Rutina activa
            │            │            │
            ▼            ▼            │
        ┌───────┐   ┌────────┐       │
        │ Login │   │  Form  │       │
        └───┬───┘   └───┬────┘       │
            │           │            │
            ▼           ▼            │
        ┌─────────────────┐          │
        │     Loading     │◄─────────┘
        │  (Generando IA) │
        └────────┬────────┘
                 │
                 ▼
         ┌──────────────┐     ┌──────────────┐
         │   Rutina     │────▶│  Day Detail  │
         │  (7 días)    │     │ (ejercicios) │
         └──────┬───────┘     └──────┬───────┘
                │                    │
                │              ┌─────┴──────┐
                │              │  Exercise  │
                │              │   Detail   │
                │              └────────────┘
                │
         ┌──────┴───────┐     ┌──────────────┐
         │  Búsqueda    │     │   Perfil     │
         │ (873 ejerc.) │     │  (IMC, datos)│
         └──────────────┘     └──────────────┘

         ┌─────────────────────────────────────┐
         │  Bottom Nav: Rutina | Ejercicios | Perfil │
         └─────────────────────────────────────┘
```

---

## Sistema de IA / AI System

Gymera utiliza una **cadena de failover** de 5 modelos de IA. Si uno falla (rate limit, timeout, JSON inválido), pasa automáticamente al siguiente:

| Prioridad | Proveedor | Modelo | Tokens máx. |
|:---------:|-----------|--------|:-----------:|
| 1 | Groq | `gpt-oss-120b` | 5.000 |
| 2 | Groq | `llama-3.3-70b-versatile` | 5.000 |
| 3 | Gemini | `gemini-2.5-flash` | — |
| 4 | Groq | `gpt-oss-20b` | 5.000 |
| 5 | Groq | `llama-3.1-8b-instant` | 4.000 |

### Cómo funciona / How it works

1. El usuario completa el formulario con datos físicos y preferencias
2. Se construye un prompt detallado que incluye: objetivo, frecuencia, duración, nivel, lesiones, peso, altura, edad y género
3. El prompt se envía a la cadena de proveedores (en orden)
4. La IA devuelve un JSON con **7 días de entrenamiento** (Lun-Dom), cada uno con 3-8 ejercicios
5. Cada ejercicio tiene nombre en español (`name`) e inglés (`nameEn`) para lookup en la base de datos
6. Los ejercicios se validan contra el asset de 873 ejercicios — los que no matchean se eliminan antes de guardar
7. La rutina se guarda en Room y se sincroniza con Firestore

### Cómo funciona / How it works

1. The user completes the form with physical data and preferences
2. A detailed prompt is built including: goal, frequency, duration, level, injuries, weight, height, age, and gender
3. The prompt is sent to the provider chain (in order)
4. The AI returns a JSON with **7 training days** (Mon-Sun), each with 3-8 exercises
5. Each exercise has a Spanish name (`name`) and English name (`nameEn`) for lookup in the exercise database
6. Exercises are validated against the 873-exercise asset — unmatched ones are removed before saving
7. The routine is saved to Room and synced to Firestore

---

## Modelo de Datos / Data Model

```
┌──────────────┐       ┌──────────────────┐       ┌─────────────────────────┐
│    Routine    │──1:N──▶│   WorkoutDay     │──1:N──▶│  ExerciseAssignment     │
│              │       │                  │       │                         │
│ id           │       │ id               │       │ id                      │
│ userUid      │       │ routineId (FK)   │       │ workoutDayId (FK)       │
│ goal         │       │ dayName          │       │ nameEs / nameEn         │
│ daysPerWeek  │       │ dayOrder         │       │ muscleGroup             │
│ level        │       │ isRestDay        │       │ sets / reps / restSec   │
│ generatedAt  │       │ muscleFocus      │       │ orderInDay              │
│ isActive     │       └──────────────────┘       └─────────────────────────┘
└──────────────┘

┌──────────────────┐       ┌──────────────────┐
│  ExerciseCache    │       │  UserProfile     │
│                  │       │                  │
│ id               │       │ uid              │
│ name             │       │ age              │
│ primaryMuscles   │       │ weightKg         │
│ instructionsEs   │       │ heightCm         │
│ imageUrl/2       │       │ gender           │
│ cachedAt         │       │ birthDateMillis  │
└──────────────────┘       └──────────────────┘
```

---

## Testing / Pruebas

| Métrica | Valor |
|---------|:-----:|
| Tests unitarios | **105** |
| Archivos de test | **9** |
| Librerías | JUnit 4, MockK, Coroutines Test |

### Cobertura por módulo / Coverage by Module

| Módulo | Tests | Qué se valida |
|--------|:-----:|---------------|
| `RoutineRepositoryImpl` | 23 | Parsing JSON, CRUD de rutinas/días/ejercicios, move/clear/rest |
| `DayDetailViewModel` | 21 | Inicialización, reorder, add/remove, búsqueda, imágenes, removeUnmatched |
| `FormViewModel` | 15 | Wizard de 8 pasos, validación de métricas, persistencia Room |
| `SharedRoutineViewModel` | 14 | setRoutine inmediato, clearRoutine, edición de días, pending profile |
| `LoadingViewModel` | 10 | Generación + filtrado + guardado + sync Firestore |
| `SplashViewModel` | 7 | Lógica de routing: Login / Form / Routine |
| `RegenFormViewModel` | 7 | Pre-fill, validación, buildProfile |
| `FailoverRoutineGenerator` | 7 | Failover, JSON inválido, provider vacío, all-fail |

---

## Licencia / License

```
MIT License

Copyright (c) 2025 Ramiro De Biase

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

<div align="center">

Hecho con Kotlin, Compose y mucho entrenamiento.

Built with Kotlin, Compose, and a lot of training.

</div>
