# Browser Mod (Fabric, Minecraft 1.21.11)

Ein Browser im Spiel. Mit `/browser` oder der Taste **B** (in den Steuerungs-Einstellungen änderbar)
öffnet sich ein Menü, in dem **Chrome, Edge, Opera, Brave und Firefox im Kreis laufen**.
Klick auf einen Browser öffnet ihn.

## Stand

**Phase 1 (dieses ZIP)**
- `/browser` und Keybind
- Menü mit den kreisenden Browser-Kacheln (beim Drüberfahren langsamer, Klick öffnet)
- Browser-Bildschirm mit Zurück, Vor, Neu laden, Adressleiste (Adresse oder Suchbegriff), Go,
  ChatGPT-Schnellbutton und „Wechseln“
- Bau per GitHub Actions

**Phase 2 (kommt als Nächstes)**
- Anbindung der Chromium-Engine über [MCEF Modern](https://modrinth.com/mod/mcef-modern),
  damit die Seitenfläche echte Webseiten anzeigt (Suchen, ChatGPT usw.), inklusive Maus und Tastatur.
- Die passende Dependency steht schon auskommentiert in `build.gradle`.

## Hinweise

- Chrome, Edge, Opera und Brave basieren alle auf Chromium. Firefox nutzt eine andere Engine, die man
  nicht einbetten kann. Die Auswahl im Menü unterscheidet sich deshalb nur durch Name, Farbe, Startseite
  und Suchmaschine, hinter allen läuft später dieselbe Chromium-Engine.
- Die Kacheln sind bewusst selbst gezeichnet, ohne Original-Logos der Hersteller (Markenrechte).
- Mod läuft nur auf dem Client.

## Bauen

**GitHub Actions (empfohlen):** Dateien ins Repo hochladen (der Ordner `.github` muss im Hauptverzeichnis
liegen), dann Reiter **Actions** → letzter Lauf → **Artifacts** → `browsermod` herunterladen.
Die fertige `.jar` kommt in den `mods`-Ordner, zusammen mit dem Fabric API.

**Lokal:** Java 21 und Gradle 9.2 oder neuer, dann `gradle build`. Die Jar liegt in `build/libs/`.

## Lizenz

MIT, siehe `LICENSE`.
