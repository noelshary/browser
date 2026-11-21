package de.browsermod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Der Browser-Bildschirm: Werkzeugleiste (Zurueck, Vor, Neu laden, Adressleiste, Go, ChatGPT,
 * Browser wechseln) und darunter die Seitenflaeche.
 *
 * PHASE 1: Die Seitenflaeche zeigt noch Platzhalter-Text. In Phase 2 rendert hier die
 * Chromium-Engine (MCEF Modern) die echte Webseite.
 */
public class BrowserScreen extends Screen {
    private static final int MARGIN = 6;
    private static final int BAR_H = 20;

    private final BrowserProfile profile;
    private final List<String> history = new ArrayList<>();
    private int historyIndex = 0;
    private String currentUrl;

    private EditBox urlBox;
    private Button backButton;
    private Button forwardButton;

    public BrowserScreen(BrowserProfile profile) {
        super(Component.literal(profile.displayName()));
        this.profile = profile;
        this.currentUrl = profile.homeUrl();
        this.history.add(currentUrl);
    }

    @Override
    protected void init() {
        int y = MARGIN;
        int x = MARGIN;

        backButton = this.addRenderableWidget(Button.builder(Component.literal("<"), b -> goBack())
                .bounds(x, y, 20, BAR_H).build());
        x += 23;
        forwardButton = this.addRenderableWidget(Button.builder(Component.literal(">"), b -> goForward())
                .bounds(x, y, 20, BAR_H).build());
        x += 23;
        this.addRenderableWidget(Button.builder(Component.literal("R"), b -> reload())
                .bounds(x, y, 20, BAR_H).build());
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
        urlBox.setValue(currentUrl);
        urlBox.setHint(Component.literal("Suchen oder Adresse eingeben"));
        this.addRenderableWidget(urlBox);
        this.setInitialFocus(urlBox);

        updateButtons();
    }

    private void updateButtons() {
        if (backButton != null) {
            backButton.active = historyIndex > 0;
        }
        if (forwardButton != null) {
            forwardButton.active = historyIndex < history.size() - 1;
        }
    }

    private void navigate(String input) {
        String url = profile.toUrl(input);
        while (history.size() > historyIndex + 1) {
            history.remove(history.size() - 1);
        }
        history.add(url);
        historyIndex = history.size() - 1;
        showUrl(url);
    }

    private void goBack() {
        if (historyIndex > 0) {
            historyIndex--;
            showUrl(history.get(historyIndex));
        }
    }

    private void goForward() {
        if (historyIndex < history.size() - 1) {
            historyIndex++;
            showUrl(history.get(historyIndex));
        }
    }

    private void reload() {
        showUrl(currentUrl);
    }

    private void showUrl(String url) {
        currentUrl = url;
        if (urlBox != null) {
            urlBox.setValue(url);
        }
        updateButtons();
        // Phase 2: hier wird die URL an die Chromium-Engine uebergeben (browser.loadURL(url)).
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // bewusst leer: Hintergrund kommt aus render()
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0xFF14141C);

        int px = MARGIN;
        int py = MARGIN + BAR_H + 6;
        int pw = this.width - 2 * MARGIN;
        int ph = this.height - py - MARGIN;
        g.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, profile.color());
        g.fill(px, py, px + pw, py + ph, 0xFF1E1E28);

        int cx = this.width / 2;
        int cy = py + ph / 2;
        g.drawCenteredString(this.font, profile.displayName(), cx, cy - 30, 0xFFFFFFFF);
        g.drawCenteredString(this.font, "Seite: " + currentUrl, cx, cy - 14, 0xFFCCCCCC);
        g.drawCenteredString(this.font, "Die Chromium-Engine wird in Phase 2 angebunden,", cx, cy + 6, 0xFF888899);
        g.drawCenteredString(this.font, "dann erscheint hier die echte Webseite.", cx, cy + 18, 0xFF888899);

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (urlBox != null && urlBox.isFocused()
                && (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)) {
            navigate(urlBox.getValue());
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
