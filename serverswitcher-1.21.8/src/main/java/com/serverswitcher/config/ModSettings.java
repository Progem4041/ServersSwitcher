package com.serverswitcher.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Настройки мода: хранятся в {@code config/serverswitcher.json}.
 * Экран настроек строится через Cloth Config ({@link com.serverswitcher.config.ModConfigScreen}).
 */
public final class ModSettings {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("serverswitcher.json");

    private static ModSettings instance = load();

    /** Ширина панели со списком серверов (пиксели). */
    public int panelWidth = 280;
    /** Высота панели (пиксели). */
    public int panelHeight = 200;
    /** Высота одной строки в списке. */
    public int rowHeight = 22;
    /** Показывать кнопку переключения сервера в меню паузы. */
    public boolean showPauseMenuButton = true;

    public static ModSettings get() {
        return instance;
    }

    public static void reload() {
        instance = load();
    }

    private static ModSettings load() {
        if (!Files.isRegularFile(PATH)) {
            return new ModSettings();
        }
        try (Reader reader = Files.newBufferedReader(PATH)) {
            ModSettings loaded = GSON.fromJson(reader, ModSettings.class);
            return loaded != null ? loaded.clamp() : new ModSettings();
        } catch (IOException | RuntimeException ignored) {
            return new ModSettings();
        }
    }

    public void save() {
        clamp();
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException ignored) {
            // без логгера в релизе — тихий сбой сохранения
        }
        instance = this;
    }

    private ModSettings clamp() {
        panelWidth = Math.min(400, Math.max(200, panelWidth));
        panelHeight = Math.min(320, Math.max(140, panelHeight));
        rowHeight = Math.min(32, Math.max(18, rowHeight));
        return this;
    }
}
