package com.serverswitcher.mixin;

import com.serverswitcher.config.ModSettings;
import com.serverswitcher.screen.ServerSwitchScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Добавляет кнопку переключения сервера рядом с «Отключиться», не удаляя ванильные виджеты.
 * Поиск кнопки по ключу перевода {@code menu.disconnect} устойчивее при смене языка, чем сравнение текста.
 * Инжект в конец {@code init} и низкий приоритет миксина — после большинства модов меню (в т.ч. FancyMenu),
 * чтобы корректно найти уже созданную кнопку отключения.
 */
@Mixin(value = GameMenuScreen.class, priority = 1100)
public abstract class GameMenuScreenMixin {

    @Inject(method = "init", at = @At("TAIL"))
    private void serverswitcher$addSwitchServerButton(CallbackInfo ci) {
        if (!ModSettings.get().showPauseMenuButton) {
            return;
        }
        GameMenuScreen self = (GameMenuScreen) (Object) this;
        MinecraftClient client = MinecraftClient.getInstance();

        for (Element child : self.children()) {
            if (!(child instanceof ButtonWidget button)) {
                continue;
            }
            if (!isDisconnectButton(button)) {
                continue;
            }
            int bx = button.getX();
            int by = button.getY();
            int half = (button.getWidth() - 2) / 2;
            button.setWidth(half);
            self.addDrawableChild(ButtonWidget.builder(
                    Text.translatable("serverswitcher.button.switch_server"),
                    b -> {
                        if (client.world != null) {
                            client.setScreen(new ServerSwitchScreen(self));
                        }
                    }
            ).dimensions(bx + half + 2, by, half, 20).build());
            return;
        }
    }

    private static boolean isDisconnectButton(ButtonWidget button) {
        Text message = button.getMessage();
        if (message.getContent() instanceof TranslatableTextContent translatable) {
            return "menu.disconnect".equals(translatable.getKey());
        }
        return false;
    }
}
