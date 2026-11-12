package com.dilly.donutpay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    public enum Style { MINIMAL, CARD, BANNER }
    public enum Anim { SLIDE, FADE, POP, NONE }
    public enum Corner { TOP_RIGHT, TOP_LEFT, BOTTOM_RIGHT, BOTTOM_LEFT }

    // identity / safety
    public String ownerName = "DILLYY_MC";
    public boolean onlyOnDonutSMP = true;

    // notifications
    public boolean notifEnabled = true;
    public Style style = Style.CARD;
    public Anim animation = Anim.SLIDE;
    public int notifMs = 3500;

    // HUD
    public boolean historyHud = true;
    public int historyLines = 6;
    public Corner hudCorner = Corner.TOP_RIGHT;
    public boolean sessionPanel = true;

    // sounds
    public boolean soundEnabled = true;
    public String soundId = "minecraft:entity.experience_orb.pickup";
    public float volume = 1.0f;
    public float pitch = 1.0f;

    // big payment celebration (default: $1,000,000 and up)
    public boolean celebrationEnabled = true;
    public long bigThreshold = 1_000_000L;
    public int celebrationMs = 3500;
    public String bigSoundId = "minecraft:ui.toast.challenge_complete";

    // ---- persistence ----
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Config INSTANCE;

    static Path dir() {
        Path d = FabricLoader.getInstance().getConfigDir().resolve("donutpay");
        try { Files.createDirectories(d); } catch (Exception ignored) {}
        return d;
    }

    public static Config get() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }

    public static void load() {
        Path f = dir().resolve("config.json");
        try (Reader r = Files.newBufferedReader(f)) {
            INSTANCE = GSON.fromJson(r, Config.class);
        } catch (Exception e) {
            INSTANCE = null;
        }
        if (INSTANCE == null) { INSTANCE = new Config(); save(); }
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(dir().resolve("config.json"))) {
            GSON.toJson(get(), w);
        } catch (Exception e) {
            DonutPayClient.LOG.error("Could not save config", e);
        }
    }
}
