# IntentLock

A forced pause in front of the apps that eat your time.

Open **TikTok** or **Instagram** and a full-screen countdown blocks the app for
**10 minutes**. Open **X** and it blocks for **30 minutes**. If you leave before
the countdown ends, it **resets** — so you can't background-and-return to cheat
it. The goal is friction high enough that you stop opening these apps on reflex.

This is a self-control tool for *your own* phone. It is **not** malware and it is
**not** a permanent trap — see *Honest limitations* below.

## How it works

| Piece | What it does |
|-------|--------------|
| `AppMonitorService` (AccessibilityService) | Notices which app is in the foreground. Reads the package name only — never your screen content or messages. |
| `LockOverlayController` | Draws the full-screen countdown over a guarded app and removes it when the pause is served or you leave. |
| `AdminReceiver` (Device Admin) | Optional. Makes the app impossible to uninstall *until you deactivate admin first* — friction against an impulsive delete. |
| 24-hour off-switch | Turning protection off requires a deliberate 24-hour wait, so quitting is a slept-on decision, not an urge. |

## Get the app on your phone (no computer needed)

1. Push this project to a GitHub repo (see below).
2. Go to the repo's **Actions** tab → the latest **Build APK** run.
3. Download the **IntentLock-debug-apk** artifact (a zip) and open the `.apk`.
4. Allow "install unknown apps" when your phone asks, then install.

## First-time setup (in the app)

Grant three things from the dashboard:

1. **Draw over other apps** — lets the pause screen appear.
2. **Accessibility → IntentLock app guard** — lets it detect app launches.
3. **Delete protection** (optional) — device-admin so it can't be deleted on impulse.

When the top banner turns green, it's active.

## Turning it off

Open the app → **Turn protection off…** → start the 24-hour wait → come back
after 24 hours to finish. This removes device-admin and lifts the pauses. It
does not delete the app.

## Honest limitations (please read)

On a normal, non-rooted phone **you are the owner of the device**, and that's a
good thing. This app raises the cost of opening distracting apps and the cost of
deleting itself, but a determined person can still remove it via Settings →
Accessibility (switch it off), Device admin (deactivate), Safe Mode, or a factory
reset. That escape hatch is *intentional* — a tool with no way out turns your
phone into a liability the day you actually need it.

If after trying this you still want the stricter, no-casual-escape version, that
requires provisioning the phone as a managed **device-owner** (a factory reset +
ADB setup). That's the "your way" path — it's a bigger, irreversible step, and
it's a separate build.

## Build it yourself

Open in Android Studio (Giraffe+), or from a machine with the Android SDK:

```bash
./gradlew assembleDebug
# APK at app/build/outputs/apk/debug/app-debug.apk
```

## Guarded apps & durations

Defaults live in `TargetApps.kt`; per-app minutes are adjustable and stored in
`Prefs`. Out of the box: TikTok 10 min, Instagram 10 min, X 30 min.
