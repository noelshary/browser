package de.browsermod;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class BrowserModClient implements ClientModInitializer {
    public static final String MOD_ID = "browsermod";

    private static KeyMapping openKey;
    private static volatile boolean openRequested;

    @Override
    public void onInitializeClient() {
        // Keybind (Standard: B, in den Steuerungs-Einstellungen aenderbar)
        openKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.browsermod.open",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                KeyMapping.Category.MISC));

        // Befehl /browser
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("browser").executes(context -> {
                    openRequested = true;
                    return 1;
                })));

        // Das Menue wird erst im naechsten Tick geoeffnet, weil sich der Chat nach dem
        // Befehl sonst sofort wieder ueber das Menue schliessen wuerde.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.consumeClick()) {
                openRequested = true;
            }
            if (openRequested && client.screen == null) {
                openRequested = false;
                client.setScreen(new BrowserSelectScreen());
            }
        });
    }
}
