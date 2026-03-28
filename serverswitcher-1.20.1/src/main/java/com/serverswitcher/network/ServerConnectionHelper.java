package com.serverswitcher.network;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

/**
 * Разрыв текущей сессии и запуск подключения к выбранному серверу (ванильная логика {@link ConnectScreen}).
 */
public final class ServerConnectionHelper {

    private ServerConnectionHelper() {
    }

    /**
     * После {@code disconnect} клиент вне мира — {@link GameMenuScreen} нельзя отдавать в {@link ConnectScreen}
     * как экран возврата при отмене / ошибке подключения.
     */
    private static Screen sanitizeReturnScreen(Screen requested) {
        if (requested instanceof GameMenuScreen) {
            return new MultiplayerScreen(new TitleScreen());
        }
        return requested != null ? requested : new TitleScreen();
    }

    public static void switchToServer(MinecraftClient client, Screen returnScreen, ServerInfo target) {
        Screen safeReturn = sanitizeReturnScreen(returnScreen);
        if (client.world != null) {
            client.world.disconnect();
        }
        ServerAddress address = ServerAddress.parse(target.address);
        ConnectScreen.connect(safeReturn, client, address, target, false);
    }

    public static boolean reconnectToCurrent(MinecraftClient client, Screen returnScreen) {
        ServerInfo current = client.getCurrentServerEntry();
        if (current == null) {
            return false;
        }
        switchToServer(client, returnScreen, current);
        return true;
    }
}
