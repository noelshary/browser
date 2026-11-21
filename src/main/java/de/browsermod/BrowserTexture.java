package de.browsermod;

import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.dimaskama.mcef.api.MCEFBrowser;
import net.minecraft.client.renderer.texture.AbstractTexture;

/**
 * Bruecke: Macht die Chromium-Textur von MCEF fuer Minecraft zeichenbar.
 * Die Textur gehoert MCEF; wir geben sie hier nur weiter und schliessen sie nie selbst.
 */
public final class BrowserTexture extends AbstractTexture {
    private final MCEFBrowser browser;

    public BrowserTexture(MCEFBrowser browser) {
        this.browser = browser;
    }

    @Override
    public GpuTexture getTexture() {
        return browser.getTexture();
    }

    @Override
    public GpuTextureView getTextureView() {
        return browser.getTextureView();
    }

    @Override
    public void close() {
        // bewusst leer: MCEF verwaltet die Textur
    }
}
