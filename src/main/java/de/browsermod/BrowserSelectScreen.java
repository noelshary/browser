package de.browsermod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Das Auswahl-Menue: Die Browser kreisen auf einer Ellipse, Klick oeffnet den Browser. */
public class BrowserSelectScreen extends Screen {
    private static final BrowserProfile[] PROFILES = BrowserProfile.values();

    private double phase = 0.0;
    private long lastNanos = System.nanoTime();
    private BrowserProfile hovered = null;

    public BrowserSelectScreen() {
        super(Component.literal("Browser wählen"));
    }

    private record Slot(BrowserProfile profile, int x, int y, int size, double depth) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + size && my >= y && my < y + size;
        }
    }

    /** Liefert alle Kacheln, von hinten nach vorne sortiert (zuletzt = ganz vorne). */
    private List<Slot> computeSlots() {
        int cx = this.width / 2;
        int cy = this.height / 2 + 4;
        double rx = Math.min(this.width * 0.36, 280.0);
        double ry = Math.min(this.height * 0.16, 64.0);

        List<Slot> slots = new ArrayList<>();
        for (int i = 0; i < PROFILES.length; i++) {
            double angle = phase + i * (2.0 * Math.PI / PROFILES.length);
            double depth = (Math.sin(angle) + 1.0) / 2.0; // 0 = hinten, 1 = vorne
            int size = (int) (50 + depth * 46);
            int x = (int) (cx + Math.cos(angle) * rx - size / 2.0);
            int y = (int) (cy + Math.sin(angle) * ry - size / 2.0);
            slots.add(new Slot(PROFILES[i], x, y, size, depth));
        }
        slots.sort(Comparator.comparingDouble(Slot::depth));
        return slots;
    }

    private BrowserProfile pick(double mx, double my) {
        List<Slot> slots = computeSlots();
        for (int i = slots.size() - 1; i >= 0; i--) { // vorderste zuerst
            Slot s = slots.get(i);
            if (s.contains(mx, my)) {
                return s.profile();
            }
        }
        return null;
    }

    private static int withAlpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

    private void drawTile(GuiGraphics g, Slot s, boolean hover) {
        int x = s.x();
        int y = s.y();
        int sz = s.size();
        int alpha = (int) (120 + s.depth() * 135);
        BrowserProfile p = s.profile();

        g.fill(x + 4, y + 4, x + sz + 4, y + sz + 4, 0x66000000); // Schatten
        g.fill(x, y, x + sz, y + sz, withAlpha(p.color(), alpha));
        int inset = sz / 5;
        g.fill(x + inset, y + inset, x + sz - inset, y + sz - inset, withAlpha(p.accent(), alpha));
        int inset2 = sz * 2 / 5;
        g.fill(x + inset2, y + inset2, x + sz - inset2, y + sz - inset2, withAlpha(0xFFFFFFFF, alpha));

        if (hover) {
            int c = 0xFFFFFFFF;
            g.fill(x - 2, y - 2, x + sz + 2, y, c);
            g.fill(x - 2, y + sz, x + sz + 2, y + sz + 2, c);
            g.fill(x - 2, y, x, y + sz, c);
            g.fill(x + sz, y, x + sz + 2, y + sz, c);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // bewusst leer: der Hintergrund wird in render() selbst gezeichnet (kein Weichzeichner)
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        long now = System.nanoTime();
        double dt = Math.min((now - lastNanos) / 1_000_000_000.0, 0.1);
        lastNanos = now;
        double speed = hovered != null ? 0.12 : 0.6; // beim Drueberfahren langsamer
        phase += speed * dt;

        hovered = pick(mouseX, mouseY);

        g.fillGradient(0, 0, this.width, this.height, 0xF00B0B14, 0xF01B1B33);

        g.drawCenteredString(this.font, "Wähle deinen Browser", this.width / 2, 16, 0xFFFFFFFF);
        g.drawCenteredString(this.font, "Klick auf einen Browser zum Öffnen  |  Esc = Schließen",
                this.width / 2, 30, 0xFFAAAAAA);

        for (Slot s : computeSlots()) {
            drawTile(g, s, s.profile() == hovered);
            if (s.depth() > 0.55) {
                int labelColor = s.profile() == hovered ? 0xFFFFFFFF : 0xFFCCCCCC;
                g.drawCenteredString(this.font, s.profile().displayName(),
                        s.x() + s.size() / 2, s.y() + s.size() + 5, labelColor);
            }
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            BrowserProfile hit = pick(event.x(), event.y());
            if (hit != null) {
                this.minecraft.setScreen(new BrowserScreen(hit));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
