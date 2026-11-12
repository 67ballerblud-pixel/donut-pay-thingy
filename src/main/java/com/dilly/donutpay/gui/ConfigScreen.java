package com.dilly.donutpay.gui;

import com.dilly.donutpay.Config;
import com.dilly.donutpay.PaymentHud;
import com.dilly.donutpay.Sounds;
import com.dilly.donutpay.Stats;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.Supplier;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private static final long[] THRESHOLDS = {100_000L, 500_000L, 1_000_000L, 5_000_000L, 10_000_000L, 100_000_000L, 1_000_000_000L};
    private static final int[] DURATIONS = {2000, 3000, 3500, 5000, 7000};

    public ConfigScreen(Screen parent) {
        super(Text.literal("DonutPay Settings"));
        this.parent = parent;
    }

    private void cycle(int x, int y, Supplier<String> label, Runnable action) {
        ButtonWidget[] ref = new ButtonWidget[1];
        ref[0] = ButtonWidget.builder(Text.literal(label.get()), b -> {
            action.run(); b.setMessage(Text.literal(label.get())); Config.save();
        }).dimensions(x, y, 150, 20).build();
        addDrawableChild(ref[0]);
    }

    private static <E extends Enum<E>> E next(E cur, E[] all) { return all[(cur.ordinal() + 1) % all.length]; }

    private static String nextSound(String cur) {
        for (int i = 0; i < Sounds.PRESETS.length; i++)
            if (Sounds.PRESETS[i].equals(cur)) return Sounds.PRESETS[(i + 1) % Sounds.PRESETS.length];
        return Sounds.PRESETS[0];
    }

    @Override
    protected void init() {
        Config c = Config.get();
        int lx = width / 2 - 155, rx = width / 2 + 5, y = 36, dy = 23;

        // ----- left column: notifications + sound
        cycle(lx, y, () -> "Notifications: " + (c.notifEnabled ? "ON" : "OFF"), () -> c.notifEnabled = !c.notifEnabled);
        cycle(lx, y + dy, () -> "Style: " + c.style, () -> c.style = next(c.style, Config.Style.values()));
        cycle(lx, y + dy * 2, () -> "Animation: " + c.animation, () -> c.animation = next(c.animation, Config.Anim.values()));
        cycle(lx, y + dy * 3, () -> "Duration: " + c.notifMs / 1000.0 + "s", () -> {
            int idx = 0;
            for (int i = 0; i < DURATIONS.length; i++) if (DURATIONS[i] == c.notifMs) idx = i;
            c.notifMs = DURATIONS[(idx + 1) % DURATIONS.length];
        });
        cycle(lx, y + dy * 4, () -> "Sound: " + (c.soundEnabled ? "ON" : "OFF"), () -> c.soundEnabled = !c.soundEnabled);
        cycle(lx, y + dy * 5, () -> "Pay sound: " + Sounds.shortName(c.soundId), () -> {
            c.soundId = nextSound(c.soundId);
            Sounds.play(c.soundId, c.volume, c.pitch);
        });
        addDrawableChild(new Slider(lx, y + dy * 6, "Volume", 0f, 1f, c.volume, v -> c.volume = v));
        addDrawableChild(new Slider(lx, y + dy * 7, "Pitch", 0.5f, 2f, c.pitch, v -> c.pitch = v));

        // ----- right column: HUD + celebration
        cycle(rx, y, () -> "History HUD: " + (c.historyHud ? "ON" : "OFF"), () -> c.historyHud = !c.historyHud);
        cycle(rx, y + dy, () -> "History lines: " + c.historyLines, () -> c.historyLines = c.historyLines >= 10 ? 3 : c.historyLines + 1);
        cycle(rx, y + dy * 2, () -> "HUD corner: " + c.hudCorner, () -> c.hudCorner = next(c.hudCorner, Config.Corner.values()));
        cycle(rx, y + dy * 3, () -> "Session panel: " + (c.sessionPanel ? "ON" : "OFF"), () -> c.sessionPanel = !c.sessionPanel);
        cycle(rx, y + dy * 4, () -> "Big animation: " + (c.celebrationEnabled ? "ON" : "OFF"), () -> c.celebrationEnabled = !c.celebrationEnabled);
        cycle(rx, y + dy * 5, () -> "Big at: " + Stats.fmt(c.bigThreshold) + " and up", () -> {
            int idx = 0;
            for (int i = 0; i < THRESHOLDS.length; i++) if (THRESHOLDS[i] == c.bigThreshold) idx = i;
            c.bigThreshold = THRESHOLDS[(idx + 1) % THRESHOLDS.length];
        });
        cycle(rx, y + dy * 6, () -> "Big sound: " + Sounds.shortName(c.bigSoundId), () -> {
            c.bigSoundId = nextSound(c.bigSoundId);
            Sounds.play(c.bigSoundId, c.volume, 1f);
        });
        cycle(rx, y + dy * 7, () -> "Only on DonutSMP: " + (c.onlyOnDonutSMP ? "ON" : "OFF"), () -> c.onlyOnDonutSMP = !c.onlyOnDonutSMP);

        // ----- bottom row
        int by = y + dy * 8 + 6;
        addDrawableChild(ButtonWidget.builder(Text.literal("Preview normal"), b ->
            PaymentHud.onPayment("PreviewPlayer", 250_000, true)).dimensions(lx, by, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Preview big"), b ->
            PaymentHud.onPayment("PreviewPlayer", 1_500_000_000L, true)).dimensions(lx + 104, by, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Stats"), b ->
            client.setScreen(new StatsScreen(this))).dimensions(lx + 208, by, 102, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
            .dimensions(width / 2 - 100, by + 24, 200, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFF);
        String me = client.getSession().getUsername();
        boolean ok = me.equalsIgnoreCase(Config.get().ownerName);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("Tracking for: " + Config.get().ownerName + (ok ? "" : "  (logged in as " + me + " - tracking inactive)")),
            width / 2, 24, ok ? 0x55FF55 : 0xFF5555);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("Previews are labelled and never saved. Real payments come only from server messages."),
            width / 2, height - 12, 0x888888);
    }

    @Override
    public void close() { Config.save(); client.setScreen(parent); }

    private static class Slider extends SliderWidget {
        private final String name; private final float min, max;
        private final java.util.function.Consumer<Float> setter;
        Slider(int x, int y, String name, float min, float max, float cur, java.util.function.Consumer<Float> setter) {
            super(x, y, 150, 20, Text.empty(), (cur - min) / (max - min));
            this.name = name; this.min = min; this.max = max; this.setter = setter;
            updateMessage();
        }
        private float val() { return (float) (min + value * (max - min)); }
        @Override protected void updateMessage() { setMessage(Text.literal(name + ": " + String.format("%.2f", val()))); }
        @Override protected void applyValue() { setter.accept(val()); Config.save(); }
    }
}
