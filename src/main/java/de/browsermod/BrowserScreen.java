package de.browsermod;

import java.util.EnumMap;
import java.util.Map;
import net.dimaskama.mcef.api.MCEFApi;
import net.dimaskama.mcef.api.MCEFBrowser;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Der Browser-Bildschirm: Werkzeugleiste oben, darunter die echte Chromium-Seite (MCEF Modern).
 * Jeder Browser aus dem Kreis-Menue behaelt seine Sitzung, auch wenn man den Bildschirm schliesst.
 */
public class BrowserScreen extends Screen {
    private static final int MARGIN = 6;
    private static final int BAR_H = 20;

    /** Offene Browser-Sitzungen (bleiben beim Schliessen des Bildschirms erhalten). */
    private static final Map<BrowserProfile, MCEFBrowser> SESSIONS = new EnumMap<>(BrowserProfile.class);

    private final BrowserProfile profile;
    private MCEFBrowser browser;
    private boolean pageFocused = true;

    private EditBox urlBox;
    private Button backButton;
    private Button forwardButton;

    // Seitenflaeche in GUI-Koordinaten
    private int px, py, pw, ph;
    private int scale = 1;

    public BrowserScreen(BrowserProfile profile) {
        super(Component.literal(profile.displayName()));
        this.profile = profile;
    }

    @Override
    protected void init() {
        // Chromium-Dateien laden/starten (beim ersten Mal dauert das)
        MCEFApi.initialize();

        int y = MARGIN;
        int x = MARGIN;

        backButton = this.addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            if (browser != null) browser.getCefBrowser().goBack();
        }).bounds(x, y, 20, BAR_H).build());
        x += 23;
        forwardButton = this.addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            if (browser != null) browser.getCefBrowser().goForward();
        }).bounds(x, y, 20, BAR_H).build());
        x += 23;
        this.addRenderableWidget(Button.builder(Component.literal("R"), b -> {
            if (browser != null) browser.getCefBrowser().reload();
        }).bounds(x, y, 20, BAR_H).build());
        x += 23;

        int right = this.width - MARGIN;
        this.addRenderableWidget(Button.builder(Component.literal("Wechseln"),
                b -> this.minecraft.setScreen(new BrowserSelectScreen()))
                .bounds(right - 62, y, 62, BAR_H).build());
        right -= 65;
        this.addRenderableWidget(Button.builder(Component.literal("ChatGPT"),
                b -> navigate("https://chatgpt.com"))
                .bounds(right - 54, y, 54, BAR_H).build());
        right -= 57;
        this.addRenderableWidget(Button.builder(Component.literal("Go"),
                b -> navigate(urlBox.getValue()))
                .bounds(right - 28, y, 28, BAR_H).build());
        right -= 31;

        urlBox = new EditBox(this.font, x, y, Math.max(60, right - x), BAR_H, Component.literal("Adresse"));
        urlBox.setMaxLength(2048);
        urlBox.setHint(Component.literal("Suchen oder Adresse eingeben"));
        this.addRenderableWidget(urlBox);

        // Seitenflaeche
        px = MARGIN;
        py = MARGIN + BAR_H + 6;
        pw = this.width - 2 * MARGIN;
        ph = this.height - py - MARGIN;
        scale = Math.max(1, (int) this.minecraft.getWindow().getGuiScale());

        browser = SESSIONS.get(profile);
        if (browser != null) {
            browser.resize(pw * scale, ph * scale);
            browser.setFocus(true);
            pageFocused = true;
        }
    }

    /** Erstellt den Browser, sobald MCEF fertig initialisiert ist. */
    private void ensureBrowser() {
        if (browser != null) return;
        MCEFApi.Initialization init = MCEFApi.initialize();
        if (!init.isDone()) return;
        browser = MCEFApi.getInstance().createBrowser(profile.homeUrl(), false);
        SESSIONS.put(profile, browser);
        browser.resize(pw * scale, ph * scale);
        browser.setFocus(true);
        pageFocused = true;
    }

    private void navigate(String input) {
        if (browser == null) return;
        browser.getCefBrowser().loadURL(profile.toUrl(input));
        this.setFocused(null);
        urlBox.setFocused(false);
        pageFocused = true;
        browser.setFocus(true);
    }

    private boolean inPage(double mx, double my) {
        return mx >= px && mx < px + pw && my >= py && my < py + ph;
    }

    private int relX(double mx) {
        return (int) ((mx - px) * scale);
    }

    private int relY(double my) {
        return (int) ((my - py) * scale);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // bewusst leer: Hintergrund kommt aus render()
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0xFF14141C);

        ensureBrowser();

        g.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, profile.color());
        g.fill(px, py, px + pw, py + ph, 0xFF1E1E28);

        if (browser != null) {
            drawPage(g);
            var cef = browser.getCefBrowser();
            backButton.active = cef.canGoBack();
            forwardButton.active = cef.canGoForward();
            String url = cef.getURL();
            if (!urlBox.isFocused() && url != null && !url.isEmpty() && !url.equals(urlBox.getValue())) {
                urlBox.setValue(url);
            }
        } else {
            MCEFApi.Initialization init = MCEFApi.initialize();
            int cx = this.width / 2;
            int cy = py + ph / 2;
            g.drawCenteredString(this.font, "Chromium wird vorbereitet ...", cx, cy - 14, 0xFFFFFFFF);
            g.drawCenteredString(this.font,
                    init.getStage().name() + "  " + Math.round(init.getPercentage() * 100) + " %",
                    cx, cy, 0xFFCCCCCC);
            g.drawCenteredString(this.font, "Beim ersten Start dauert das etwas (Download).",
                    cx, cy + 16, 0xFF888899);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    /**
     * Zeichnet die Chromium-Textur in die Seitenflaeche.
     * Das ist die einzige Stelle, die von der genauen Render-API von 1.21.11 abhaengt.
     */
    private void drawPage(GuiGraphics g) {
        g.blit(browser.getTextureView(), px, py, px + pw, py + ph, 0f, 1f, 0f, 1f);
    }

    // ---------------------------------------------------------------- Maus

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (browser != null && inPage(event.x(), event.y())) {
            this.setFocused(null);
            urlBox.setFocused(false);
            pageFocused = true;
            browser.setFocus(true);
            browser.onMouseClicked(new MouseButtonEvent(relX(event.x()), relY(event.y()), event.buttonInfo()), doubleClick);
            return true;
        }
        pageFocused = false;
        if (browser != null) browser.setFocus(false);
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (browser != null && pageFocused) {
            browser.onMouseReleased(new MouseButtonEvent(relX(event.x()), relY(event.y()), event.buttonInfo()));
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (browser != null && pageFocused) {
            browser.onMouseMoved(relX(event.x()), relY(event.y()));
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (browser != null && inPage(mouseX, mouseY)) {
            browser.onMouseMoved(relX(mouseX), relY(mouseY));
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (browser != null && inPage(mouseX, mouseY)) {
            browser.onMouseScrolled(relX(mouseX), relY(mouseY), scrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ------------------------------------------------------------ Tastatur

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (urlBox != null && urlBox.isFocused()
                && (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)) {
            navigate(urlBox.getValue());
            return true;
        }
        if (browser != null && pageFocused && key != GLFW.GLFW_KEY_ESCAPE) {
            browser.onKeyPressed(event);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (browser != null && pageFocused && event.key() != GLFW.GLFW_KEY_ESCAPE) {
            browser.onKeyReleased(event);
            return true;
        }
        return super.keyReleased(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (browser != null && pageFocused) {
            browser.onCharTyped(event);
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public void removed() {
        if (browser != null) {
            browser.setFocus(false);
        }
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
