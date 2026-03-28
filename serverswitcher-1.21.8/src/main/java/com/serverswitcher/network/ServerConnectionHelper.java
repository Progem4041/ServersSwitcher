package com.serverswitcher.network;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

/**
 * Разрыв текущей сессии и запуск подключения к выбранному серверу (ванильная логика {@link ConnectScreen}).
 */
public final class ServerConnectionHelper {

    private ServerConnectionHelper() {
    }

    /**
     * После {@code disconnect} клиент вне мира — {@link GameMenuScreen} нельзя использовать как экран возврата
     * из {@link ConnectScreen} (отмена / ошибка), иначе краш или сломанное меню.
     */
    private static Screen sanitizeReturnScreen(Screen requested) {
        if (requested instanceof GameMenuScreen) {
            return new MultiplayerScreen(new TitleScreen());
        }
        return requested != null ? requested : new TitleScreen();
    }

    /**
     * Отключается от текущего мира/сервера и открывает экран подключения к {@code target}.
     */
    public static void switchToServer(MinecraftClient client, Screen returnScreen, ServerInfo target) {
        Screen safeReturn = sanitizeReturnScreen(returnScreen);
        if (client.world != null) {
            client.world.disconnect(Text.empty());
        }
        ServerAddress address = ServerAddress.parse(target.address);
        ConnectScreen.connect(safeReturn, client, address, target, false, null);
    }

    /**
     * Повторное подключение к серверу, на котором игрок сейчас играет (мультиплеер).
     *
     * @return {@code false}, если нет данных о текущем сервере (одиночка и т.п.)
     */
    public static boolean reconnectToCurrent(MinecraftClient client, Screen returnScreen) {
        ServerInfo current = client.getCurrentServerEntry();
        if (current == null) {
            return false;
        }
        switchToServer(client, returnScreen, current);
        return true;
    }
}
