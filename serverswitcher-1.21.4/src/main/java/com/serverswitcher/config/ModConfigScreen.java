package com.serverswitcher.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Экран настроек на базе Cloth Config (без AutoConfig — меньше зависимостей и проще сборка).
 */
public final class ModConfigScreen {

    private ModConfigScreen() {
    }

    public static Screen create(Screen parent) {
        ModSettings s = ModSettings.get();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("serverswitcher.config.title"))
                .transparentBackground()
                .setSavingRunnable(() -> {
                    s.save();
                    ModSettings.reload();
                });

        ConfigEntryBuilder eb = builder.entryBuilder();
        ConfigCategory ui = builder.getOrCreateCategory(Text.translatable("serverswitcher.config.category.ui"));

        ui.addEntry(eb.startIntSlider(Text.translatable("serverswitcher.config.panel_width"), s.panelWidth, 200, 400)
                .setDefaultValue(280)
                .setTooltip(Text.translatable("serverswitcher.config.panel_width.tooltip"))
                .setSaveConsumer(v -> s.panelWidth = v)
                .build());

        ui.addEntry(eb.startIntSlider(Text.translatable("serverswitcher.config.panel_height"), s.panelHeight, 140, 320)
                .setDefaultValue(200)
                .setTooltip(Text.translatable("serverswitcher.config.panel_height.tooltip"))
                .setSaveConsumer(v -> s.panelHeight = v)
                .build());

        ui.addEntry(eb.startIntSlider(Text.translatable("serverswitcher.config.row_height"), s.rowHeight, 18, 32)
                .setDefaultValue(22)
                .setTooltip(Text.translatable("serverswitcher.config.row_height.tooltip"))
                .setSaveConsumer(v -> s.rowHeight = v)
                .build());

        ui.addEntry(eb.startBooleanToggle(Text.translatable("serverswitcher.config.show_pause_button"), s.showPauseMenuButton)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("serverswitcher.config.show_pause_button.tooltip"))
                .setSaveConsumer(v -> s.showPauseMenuButton = v)
                .build());

        ConfigCategory keys = builder.getOrCreateCategory(Text.translatable("serverswitcher.config.category.keys"));
        keys.addEntry(eb.startTextDescription(Text.translatable("serverswitcher.config.keys.hint")).build());

        return builder.build();
    }
}
