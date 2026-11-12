package com.dilly.donutpay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

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
        MinecraftClient mc = MinecraftClient.getInstance();
        if (ident == null || mc.player == null) return;
        // Played at the player's own position so it is always heard at full volume.
        mc.getSoundManager().play(new PositionedSoundInstance(
            SoundEvent.of(ident), SoundCategory.MASTER, volume, pitch, Random.create(),
            mc.player.getX(), mc.player.getY(), mc.player.getZ()));
    }

    public static String shortName(String id) {
        int i = id.lastIndexOf('.');
        String s = i >= 0 ? id.substring(i + 1) : id;
        return s.replace('_', ' ');
    }
}
