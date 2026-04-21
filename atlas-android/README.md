# Atlas — Android

Native Android app port of the Atlas UI design, built in Kotlin + Jetpack Compose.

## What this is

Atlas is a note-taking companion with two modes:

- **Ask** (Command mode, dark): a conversational prompt where you query your own
  notes. Atlas pulls threads across your vault and cites which notes it used.
- **Read** (Reading mode, paper): a library of your captured notes, filterable
  by vault (personal / business), with a full-screen detail view.

A floating capture button opens a bottom-sheet intake with dictation support.

## Stack

- Kotlin 2.0.20 + Compose compiler
- Jetpack Compose + Material 3
- Google Fonts downloadable fonts (Fraunces serif, Inter sans, JetBrains Mono)
- Room for note persistence
- Android `SpeechRecognizer` for voice input
- Gradle 8.9, AGP 8.5.2, min SDK 26, target SDK 34

## Running

Open the `atlas-android/` folder in Android Studio (Koala or newer). Let it sync
Gradle, then run the `app` configuration on an emulator or device.

First launch seeds the vault with a handful of example notes so both modes have
something to show.

## What was invented vs. ported

This project was built from a partial HTML/CSS design that stopped mid-CSS and
contained no JavaScript. The design was treated as the visual spec; behavior was
designed fresh:

- **Ask**: a local keyword-matching retriever over the note corpus. Queries are
  matched against title + body + tags; the top three notes are summarized as
  citations. (Swap in a real LLM later by replacing `synthesizeReply` in
  `AtlasSharedViewModel`.)
- **Voice**: `SpeechRecognizer` with `EXTRA_PARTIAL_RESULTS` for live transcript.
- **Capture**: vault toggle, optional dictation, first non-blank line becomes
  the title, `#tags` in the body are extracted.
- **Persistence**: Room, local-only. No backend.

## Structure

```
app/src/main/java/com/atlas/app/
├── AtlasApp.kt                  Application singleton, repository wiring
├── MainActivity.kt              Entry point
├── data/
│   ├── Models.kt                Note, Turn, Vault, ReadingFilter, Speaker
│   ├── NoteRepository.kt        Room facade + seed data
│   └── local/                   Room entity, DAO, database
├── voice/
│   └── SpeechRecognizer.kt      Compose wrapper around SpeechRecognizer
└── ui/
    ├── theme/                   AtlasTheme, palettes, typography, colors
    ├── common/                  Shared ViewModel, identity strip + mode switch
    ├── command/                 Dark Ask mode (greeting, suggestions, chat, prompt)
    ├── reading/                 Paper Read mode (filter strip, note cards, FAB)
    ├── capture/                 Modal bottom sheet for note intake
    └── detail/                  Full-screen article reader
```
