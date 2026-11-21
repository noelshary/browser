# Browser Mod

Fabric-Mod für Minecraft 1.21.11: Ein echter Chromium-Browser im Spiel (über MCEF Modern).

- `/browser` oder Taste **B** öffnet das Kreis-Menü
- Klick auf Chrome / Edge / Opera / Brave / Firefox öffnet den Browser
- Adressleiste, Zurück, Vor, Neu laden, ChatGPT-Schnellbutton

## Installation
In den `mods`-Ordner: diese Mod, **Fabric API** und **MCEF Modern 0.3.3** (Modrinth).
Beim ersten Start lädt MCEF die Chromium-Dateien herunter (dauert kurz).

## Hinweis
Alle "Browser" nutzen dieselbe Chromium-Engine; sie unterscheiden sich in Name, Farbe, Startseite und Suchmaschine.

## Videos wie Twitch / TikTok (Button "Video")
Chromium in MCEF kann kein H.264. Der Button "Video" wandelt die aktuelle Seite mit
**yt-dlp** und **ffmpeg** live in WebM um und spielt sie im Spiel ab.
Beide Programme muessen installiert und im PATH sein (Windows: `winget install yt-dlp.yt-dlp` und
`winget install Gyan.FFmpeg`, danach Minecraft/Launcher neu starten).
