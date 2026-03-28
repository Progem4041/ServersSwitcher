package com.serverswitcher.screen;

import com.serverswitcher.config.ModConfigScreen;
import com.serverswitcher.config.ModSettings;
import com.serverswitcher.network.ServerConnectionHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Список серверов из {@code servers.dat}: строки рисуются вручную в области панели (без EntryListWidget),
 * чтобы текст гарантированно был виден при любых настройках клипа/фона виджета.
 */
public class ServerSwitchScreen extends Screen {

    private final Screen parent;
    private final List<ServerInfo> servers = new ArrayList<>();

    private ButtonWidget connectButton;
    private ButtonWidget reconnectButton;
    private ButtonWidget settingsButton;

    private int panelLeft;
    private int panelTop;
    private int panelRight;
    private int panelBottom;
    /** Область списка (координаты экрана). */
    private int listLeft;
    private int listRight;
    private int listTop;
    private int listBottom;
    private int rowHeight;

    private int selectedIndex = -1;
    private double scrollAmount;
    private long lastClickTime;
    private int lastClickedIndex = -1;

    public ServerSwitchScreen(@Nullable Screen parent) {
        super(Text.translatable("serverswitcher.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ModSettings s = ModSettings.get();
        int pw = s.panelWidth;
        int ph = s.panelHeight;
        panelLeft = (this.width - pw) / 2;
        panelTop = (this.height - ph) / 2;
        panelRight = panelLeft + pw;
        panelBottom = panelTop + ph;

        listTop = panelTop + 28;
        listBottom = panelBottom - 52;
        listLeft = panelLeft + 8;
        listRight = panelRight - 8;
        rowHeight = s.rowHeight;

        reloadServersFromFile();

        int bw = (pw - 24) / 3;
        int bx = panelLeft + 8;
        int by = panelBottom - 44;

        reconnectButton = ButtonWidget.builder(Text.translatable("serverswitcher.button.reconnect"), b -> {
            if (!ServerConnectionHelper.reconnectToCurrent(client, parent != null ? parent : this)
                    && client.player != null) {
                client.player.sendMessage(Text.translatable("serverswitcher.message.no_current_server"), false);
            }
        }).dimensions(bx, by, bw, 20).build();

        connectButton = ButtonWidget.builder(Text.translatable("serverswitcher.button.connect"), b -> {
            ServerInfo sel = getSelectedServer();
            if (sel != null) {
                Screen back = parent != null ? parent : this;
                ServerConnectionHelper.switchToServer(client, back, sel);
            }
        }).dimensions(bx + bw + 4, by, bw, 20).build();

        settingsButton = ButtonWidget.builder(Text.translatable("serverswitcher.button.settings"), b ->
                client.setScreen(ModConfigScreen.create(this))
        ).dimensions(bx + (bw + 4) * 2, by, bw, 20).build();

        this.addDrawableChild(reconnectButton);
        this.addDrawableChild(connectButton);
        this.addDrawableChild(settingsButton);

        boolean multiplayer = client.getCurrentServerEntry() != null;
        reconnectButton.active = multiplayer;
        connectButton.active = getSelectedServer() != null;

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.back"), b -> this.close())
                .dimensions(panelLeft + pw / 2 - 50, panelBottom - 22, 100, 20).build());
    }

    private void reloadServersFromFile() {
        servers.clear();
        ServerList vanilla = new ServerList(client);
        vanilla.loadFile();
        for (int i = 0; i < vanilla.size(); i++) {
            ServerInfo fromFile = vanilla.get(i);
            ServerInfo copy = new ServerInfo(fromFile.name, fromFile.address, fromFile.getServerType());
            copy.copyWithSettingsFrom(fromFile);
            servers.add(copy);
        }
        scrollAmount = 0;
        if (selectedIndex >= servers.size()) {
            selectedIndex = servers.isEmpty() ? -1 : servers.size() - 1;
        }
    }

    @Nullable
    private ServerInfo getSelectedServer() {
        if (selectedIndex < 0 || selectedIndex >= servers.size()) {
            return null;
        }
        return servers.get(selectedIndex);
    }

    private int maxScroll() {
        int viewH = listBottom - listTop;
        int totalH = servers.size() * rowHeight;
        return Math.max(0, totalH - viewH);
    }

    private int rowIndexAt(double mouseY) {
        if (mouseY < listTop || mouseY >= listBottom) {
            return -1;
        }
        int rel = (int) (mouseY - listTop + scrollAmount);
        return rel / rowHeight;
    }

    private boolean isOverList(double mouseX, double mouseY) {
        return mouseX >= listLeft && mouseX < listRight && mouseY >= listTop && mouseY < listBottom;
    }

    private void connectToSelectedServer(ServerInfo info) {
        Screen back = parent != null ? parent : this;
        ServerConnectionHelper.switchToServer(client, back, info);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    /**
     * Без размытия снимка мира: при длинном списке серверов поверх размытого фона текст и строки
     * визуально «плывут». Оставляем только затемнение из {@link #renderBackground}.
     */
    @Override
    public void blur() {
    }

    @Override
    protected void applyBlur() {
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.fill(panelLeft, panelTop, panelRight, panelBottom, 0xC0101010);
        context.drawBorder(panelLeft, panelTop, panelRight - panelLeft, panelBottom - panelTop, 0xFF404040);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, panelTop + 10, 0xFFFFFFFF);

        /* Рамка области списка */
        context.fill(listLeft, listTop, listRight, listBottom, 0x60000000);
        context.drawBorder(listLeft, listTop, listRight - listLeft, listBottom - listTop, 0xFF606060);

        if (!servers.isEmpty()) {
            int viewH = listBottom - listTop;
            for (int i = 0; i < servers.size(); i++) {
                int rowY = listTop + i * rowHeight - (int) scrollAmount;
                if (rowY + rowHeight <= listTop || rowY >= listBottom) {
                    continue;
                }
                ServerInfo info = servers.get(i);
                boolean hovered = isOverList(mouseX, mouseY)
                        && mouseY >= rowY && mouseY < rowY + rowHeight
                        && mouseX >= listLeft && mouseX < listRight;
                boolean selected = i == selectedIndex;

                if (selected) {
                    context.fill(listLeft + 1, Math.max(listTop, rowY), listRight - 1, Math.min(listBottom, rowY + rowHeight), 0x55FFFFFF);
                } else if (hovered) {
                    context.fill(listLeft + 1, Math.max(listTop, rowY), listRight - 1, Math.min(listBottom, rowY + rowHeight), 0x33FFFFFF);
                }

                int color = (hovered || selected) ? 0xFFFFFFA0 : 0xFFE0E0E0;
                int ty = rowY + Math.max(2, (rowHeight - (rowHeight >= 20 ? 18 : 9)) / 2);
                /* Обрезаем Y по видимой области, чтобы текст не рисовался на рамке */
                int textY = Math.max(listTop + 2, Math.min(ty, listBottom - 10));
                context.drawTextWithShadow(this.textRenderer, info.name, listLeft + 4, textY, color);
                if (rowHeight >= 20 && textY + 20 <= listBottom) {
                    context.drawTextWithShadow(this.textRenderer, info.address, listLeft + 4, textY + 10, 0xFFAAAAAA);
                }
            }

            /* Полоса прокрутки */
            int max = maxScroll();
            if (max > 0) {
                int trackW = 4;
                int trackX = listRight - trackW - 2;
                int trackTop = listTop + 2;
                int trackBottom = listBottom - 2;
                context.fill(trackX, trackTop, trackX + trackW, trackBottom, 0xFF303030);
                int thumbH = Math.max(12, (int) ((long) (trackBottom - trackTop) * viewH / (servers.size() * rowHeight)));
                int thumbTravel = trackBottom - trackTop - thumbH;
                int thumbY = trackTop + (int) (thumbTravel * scrollAmount / max);
                context.fill(trackX, thumbY, trackX + trackW, thumbY + thumbH, 0xFF909090);
            }
        }

        super.render(context, mouseX, mouseY, delta);

        if (servers.isEmpty()) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.translatable("serverswitcher.empty"),
                    this.width / 2,
                    listTop + (listBottom - listTop) / 2 - 4,
                    0xFFA0A0A0
            );
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isOverList(mouseX, mouseY)) {
            int idx = rowIndexAt(mouseY);
            if (idx >= 0 && idx < servers.size()) {
                long now = System.currentTimeMillis();
                if (idx == lastClickedIndex && now - lastClickTime < 350) {
                    connectToSelectedServer(servers.get(idx));
                    lastClickTime = 0;
                    lastClickedIndex = -1;
                } else {
                    selectedIndex = idx;
                    lastClickTime = now;
                    lastClickedIndex = idx;
                    connectButton.active = true;
                }
                return true;
            }
        }
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (handled) {
            connectButton.active = getSelectedServer() != null;
        }
        return handled;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isOverList(mouseX, mouseY) && !servers.isEmpty()) {
            scrollAmount = MathHelper.clamp(scrollAmount - verticalAmount * rowHeight, 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
}
