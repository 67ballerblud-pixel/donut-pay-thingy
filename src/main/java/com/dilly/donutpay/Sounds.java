package com.dilly.donutpay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class Sounds {
    public static final String[] PRESETS = {
        "minecraft:entity.experience_orb.pickup",
        "minecraft:block.note_block.pling",
        "minecraft:entity.player.levelup",
        "minecraft:block.amethyst_block.chime",
        "minecraft:entity.arrow.hit_player",
        "minecraft:block.bell.use",
        "minecraft:entity.firework_rocket.twinkle",
        "minecraft:ui.toast.challenge_complete",
        "minecraft:entity.villager.celebrate"
    };

    public static void play(String id, float volume, float pitch) {
        Identifier ident = Identifier.tryParse(id);
        if (ident == null) return;
        MinecraftClient.getInstance().getSoundManager()
            .play(PositionedSoundInstance.master(SoundEvent.of(ident), pitch, volume));
    }

    public static String shortName(String id) {
        int i = id.lastIndexOf('.');
        String s = i >= 0 ? id.substring(i + 1) : id;
        return s.replace('_', ' ');
    }
}
