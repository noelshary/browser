package de.browsermod;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import net.minecraft.resources.Identifier;

/**
 * Die Browser, die im Kreis-Menue angezeigt werden.
 * Hinter allen steckt spaeter dieselbe Chromium-Engine (MCEF); sie unterscheiden sich
 * durch Name, Farbe, Startseite und Suchmaschine.
 */
public enum BrowserProfile {
    CHROME("Chrome", 0xFF4285F4, 0xFFEA4335, "https://www.google.com", "https://www.google.com/search?q=%s"),
    EDGE("Edge", 0xFF0A84D6, 0xFF2BD39A, "https://www.bing.com", "https://www.bing.com/search?q=%s"),
    OPERA("Opera", 0xFFE0112B, 0xFFFF6A7A, "https://www.google.com", "https://www.google.com/search?q=%s"),
    BRAVE("Brave", 0xFFFB542B, 0xFFFFA07A, "https://search.brave.com", "https://search.brave.com/search?q=%s"),
    FIREFOX("Firefox", 0xFFFF7139, 0xFF6A2FB5, "https://duckduckgo.com", "https://duckduckgo.com/?q=%s");

    private final String displayName;
    private final int color;
    private final int accent;
    private final String homeUrl;
    private final String searchTemplate;

    BrowserProfile(String displayName, int color, int accent, String homeUrl, String searchTemplate) {
        this.displayName = displayName;
        this.color = color;
        this.accent = accent;
        this.homeUrl = homeUrl;
        this.searchTemplate = searchTemplate;
    }

    public String displayName() {
        return displayName;
    }

    /** Hauptfarbe (ARGB). */
    public int color() {
        return color;
    }

    /** Zweitfarbe (ARGB). */
    public int accent() {
        return accent;
    }

    /** Pfad zum Logo: assets/browsermod/textures/gui/browsers/<name>.png (256x256, transparent). */
    public Identifier iconId() {
        return Identifier.fromNamespaceAndPath("browsermod", "textures/gui/browsers/" + name().toLowerCase() + ".png");
    }

    public String homeUrl() {
        return homeUrl;
    }

    /**
     * Macht aus der Eingabe in der Adressleiste eine URL:
     * vollstaendige Adresse bleibt, "beispiel.de" bekommt https://, alles andere wird gesucht.
     */
    public String toUrl(String input) {
        String s = input == null ? "" : input.trim();
        if (s.isEmpty()) {
            return homeUrl;
        }
        if (s.matches("(?i)^[a-z][a-z0-9+.-]*://.*")) {
            return s;
        }
        if (!s.contains(" ") && (s.contains(".") || s.startsWith("localhost"))) {
            return "https://" + s;
        }
        return searchTemplate.formatted(URLEncoder.encode(s, StandardCharsets.UTF_8));
    }
}
