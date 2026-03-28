package com.serverswitcher;

import com.serverswitcher.config.ModSettings;
import com.serverswitcher.screen.ServerSwitchScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Клиентская инициализация: горячая клавиша и перезагрузка настроек при старте.
 */
public final class ServerSwitcherClient implements ClientModInitializer {

    public static final String MOD_ID = "serverswitcher";

    private static KeyBinding openSwitcherKey;

    @Override
    public void onInitializeClient() {
        ModSettings.reload();
        openSwitcherKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.serverswitcher.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "category.serverswitcher"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(ServerSwitcherClient::onEndClientTick);
    }

    private static void onEndClientTick(MinecraftClient client) {
        while (openSwitcherKey.wasPressed()) {
            if (client.world == null || client.player == null) {
                continue;
            }
            Screen current = client.currentScreen;
            if (current instanceof GameMenuScreen || current == null) {
                client.setScreen(new ServerSwitchScreen(current));
            }
        }
    }
}
