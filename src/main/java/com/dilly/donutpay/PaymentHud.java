package com.dilly.donutpay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;
import net.minecraft.text.Text;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Random;

public final class PaymentHud {
    private record Note(String name, long amount, long start, boolean big, boolean preview) {}

    private static final ArrayDeque<Note> queue = new ArrayDeque<>();
    private static Note current;
    private static long lastPaymentAt = 0;

    private static final int GOLD = 0xFFD700, GREEN = 0x55FF55, GRAY = 0xAAAAAA, WHITE = 0xFFFFFF;

    /** preview=true is used ONLY by the settings GUI and is never saved or counted. */
    public static void onPayment(String name, long amount, boolean preview) {
        Config cfg = Config.get();
        boolean big = cfg.celebrationEnabled && amount >= cfg.bigThreshold;
        if (!preview) lastPaymentAt = System.currentTimeMillis();

        if (cfg.soundEnabled) {
            if (big) Sounds.play(cfg.bigSoundId, cfg.volume, 1.0f);
            else Sounds.play(cfg.soundId, cfg.volume, cfg.pitch);
        }
        if (cfg.notifEnabled || big) {
            queue.add(new Note(name, amount, 0, big, preview));
        }
    }

    // ---------------------------------------------------------------- render
    public static void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.player == null) return;
        Config cfg = Config.get();
        TextRenderer tr = mc.textRenderer;
        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        long now = System.currentTimeMillis();

        if (cfg.historyHud || cfg.sessionPanel) drawPanel(ctx, tr, cfg, w, h, now);

        // advance notification queue
        if (current != null) {
            int dur = current.big ? cfg.celebrationMs : cfg.notifMs;
            if (queue.size() > 3) dur = Math.min(dur, 1200);
            if (now - current.start > dur) current = null;
        }
        if (current == null && !queue.isEmpty()) {
            Note n = queue.poll();
            current = new Note(n.name, n.amount, now, n.big, n.preview);
        }
        if (current == null) return;

        if (current.big) drawCelebration(ctx, tr, cfg, current, now, w, h);
        else drawToast(ctx, tr, cfg, current, now, w);
    }

    // ---------------------------------------------------------------- history + session panel
    private static void drawPanel(DrawContext ctx, TextRenderer tr, Config cfg, int w, int h, long now) {
        Stats st = Stats.get();
        final int LH = 12, PAD = 4, WIDTH = 150;
        int lines = Math.max(1, cfg.historyLines);
        int rows = (cfg.historyHud ? lines : 0);
        int header = cfg.sessionPanel ? 3 : 0;
        int height = (header + rows) * LH + PAD * 2;
        boolean right = cfg.hudCorner == Config.Corner.TOP_RIGHT || cfg.hudCorner == Config.Corner.BOTTOM_RIGHT;
        boolean top = cfg.hudCorner == Config.Corner.TOP_RIGHT || cfg.hudCorner == Config.Corner.TOP_LEFT;
        int x = right ? w - WIDTH - 6 : 6;
        int y = top ? 6 : h - height - 30;

        ctx.fill(x, y, x + WIDTH, y + height, 0x88000000);
        ctx.fill(x, y, x + 2, y + height, 0xFF000000 | GOLD);

        int cy = y + PAD;
        if (cfg.sessionPanel) {
            ctx.drawText(tr, "Session: " + Stats.fmt(st.sessionTotal) + " (" + st.sessionCount + ")", x + 6, cy, 0xFF000000 | GREEN, true);
            cy += LH;
            String best = st.sessionLargest > 0 ? Stats.fmt(st.sessionLargest) + " from " + st.sessionLargestFrom : "-";
            ctx.drawText(tr, "Best: " + tr.trimToWidth(best, WIDTH - 40), x + 6, cy, 0xFF000000 | GOLD, true);
            cy += LH;
            var top1 = st.topPayers(1);
            String tp = top1.isEmpty() ? "-" : top1.get(0).getKey() + " " + Stats.fmt(top1.get(0).getValue());
            ctx.drawText(tr, "Top payer: " + tr.trimToWidth(tp, WIDTH - 62), x + 6, cy, 0xFF000000 | WHITE, true);
            cy += LH;
        }
        if (!cfg.historyHud) return;

        // scrolling list: every entry slides down one slot when a new payment arrives
        double e = ease(Math.min(1.0, (now - lastPaymentAt) / 250.0));
        List<Stats.Payment> rec = st.recent;
        for (int i = 0; i < Math.min(rec.size(), lines + 1); i++) {
            Stats.Payment p = rec.get(i);
            double slot = i - (1 - e);
            if (lastPaymentAt == 0 || now - lastPaymentAt > 250) slot = i;
            int ey = cy + (int) Math.round(slot * LH);
            if (ey < cy - 1 || ey > cy + (lines - 1) * LH + 1) continue;
            int alpha = 255;
            if (i == 0 && now - lastPaymentAt < 250) alpha = clampA(e);
            int col = (alpha << 24) | (p.amount >= cfg.bigThreshold ? GOLD : GREEN);
            String text = "+" + Stats.fmt(p.amount);
            ctx.drawText(tr, text, x + 6, ey, col, true);
            ctx.drawText(tr, tr.trimToWidth(p.from, WIDTH - 12 - tr.getWidth(text) - 8), x + 6 + tr.getWidth(text) + 6, ey,
                (alpha << 24) | WHITE, true);
        }
    }

    // ---------------------------------------------------------------- toast
    private static void drawToast(DrawContext ctx, TextRenderer tr, Config cfg, Note n, long now, int w) {
        long el = now - n.start;
        int dur = cfg.notifMs;
        double p = Math.min(Math.min(1, el / 300.0), Math.min(1, (dur - el) / 300.0));
        p = Math.max(0, p);
        double e = ease(p);
        int a = clampA(cfg.animation == Config.Anim.NONE ? (p > 0 ? 1 : 0) : e);

        Matrix3x2fStack ms = ctx.getMatrices();
        ms.pushMatrix();
        double ty = 0, sc = 1;
        switch (cfg.animation) {
            case SLIDE -> ty = -(1 - e) * 40;
            case POP -> sc = 0.6 + 0.4 * easeOutBack(p);
            default -> {}
        }
        int cx = w / 2;
        int baseY = 22;
        ms.translate((float) (cx), (float) (baseY + ty));
        ms.scale((float) sc, (float) sc);

        String amt = "+" + Stats.fmt(n.amount);
        String from = "from " + n.name + (n.preview ? "  [PREVIEW]" : "");
        int accent = n.preview ? 0x55AAFF : GREEN;

        switch (cfg.style) {
            case MINIMAL -> {
                String line = amt + "  " + from;
                ctx.drawCenteredTextWithShadow(tr, Text.literal(line), 0, 0, (a << 24) | accent);
            }
            case BANNER -> {
                int bw = 200, bh = 22;
                ctx.fillGradient(-bw / 2, 0, bw / 2, bh, ((a * 170 / 255) << 24) | 0x103010, ((a * 170 / 255) << 24) | 0x000000);
                ctx.fill(-bw / 2, bh - 2, bw / 2, bh, (a << 24) | accent);
                ctx.drawCenteredTextWithShadow(tr, Text.literal(amt + "  " + from), 0, 7, (a << 24) | WHITE);
            }
            default -> { // CARD
                int bw = 176, bh = 38;
                ctx.fill(-bw / 2, 0, bw / 2, bh, ((a * 200 / 255) << 24) | 0x101010);
                ctx.fill(-bw / 2, 0, -bw / 2 + 3, bh, (a << 24) | accent);
                ms.pushMatrix();
                ms.translate((float) (-bw / 2 + 10), (float) (5));
                ms.scale(1.6f, 1.6f);
                ctx.drawText(tr, amt, 0, 0, (a << 24) | accent, true);
                ms.popMatrix();
                ctx.drawText(tr, tr.trimToWidth(from, bw - 16), -bw / 2 + 10, 22, (a << 24) | WHITE, true);
                ctx.drawText(tr, Stats.exact(n.amount), -bw / 2 + 10, 29, (a << 24) | GRAY, true);
            }
        }
        ms.popMatrix();
    }

    // ---------------------------------------------------------------- celebration ($1M+ by default)
    private static void drawCelebration(DrawContext ctx, TextRenderer tr, Config cfg, Note n, long now, int w, int h) {
        long el = now - n.start;
        int dur = cfg.celebrationMs;
        double fade = Math.max(0, Math.min(1, Math.min(el / 300.0, (dur - el) / 500.0)));
        int a = (int) (110 * fade);
        // golden screen-edge glow
        ctx.fillGradient(0, 0, w, h / 3, (a << 24) | 0xFFB000, 0x00FFB000);
        ctx.fillGradient(0, h - h / 3, w, h, 0x00FFB000, (a << 24) | 0xFFB000);

        int cx = w / 2, cy = h / 3;
        double pop = easeOutBack(Math.min(1, el / 450.0));
        double pulse = 1 + 0.04 * Math.sin(el / 90.0);
        int ta = clampA(fade);

        Matrix3x2fStack ms = ctx.getMatrices();
        ms.pushMatrix();
        ms.translate((float) (cx), (float) (cy));
        ms.pushMatrix();
        float s = (float) (4.0 * pop * pulse);
        ms.scale(s, s);
        ctx.drawCenteredTextWithShadow(tr, Text.literal("+" + Stats.fmt(n.amount)), 0, -4, (ta << 24) | GOLD);
        ms.popMatrix();
        ms.pushMatrix();
        ms.scale(1.6f, 1.6f);
        ctx.drawCenteredTextWithShadow(tr, Text.literal("from " + n.name + (n.preview ? " [PREVIEW]" : "")), 0, 14, (ta << 24) | WHITE);
        ms.popMatrix();
        ctx.drawCenteredTextWithShadow(tr, Text.literal("\u2605 MEGA PAYMENT \u2605"), 0, -34, (ta << 24) | 0xFFAA00);
        ctx.drawCenteredTextWithShadow(tr, Text.literal(Stats.exact(n.amount)), 0, 40, (ta << 24) | GRAY);
        ms.popMatrix();

        // sparkles
        Random r = new Random(n.start);
        for (int i = 0; i < 56; i++) {
            double ang = r.nextDouble() * Math.PI * 2;
            double spd = 40 + r.nextDouble() * 140;
            double life = 900 + r.nextDouble() * 1800;
            double delay = r.nextDouble() * 500;
            double t = el - delay;
            if (t < 0 || t > life) continue;
            double k = t / life;
            int px = (int) (cx + Math.cos(ang) * spd * (t / 1000.0) * 1.4);
            int py = (int) (cy + Math.sin(ang) * spd * (t / 1000.0) * 1.4 + 60 * k * k);
            int sa = clampA((1 - k) * fade);
            int size = 1 + r.nextInt(3);
            int col = (sa << 24) | (r.nextBoolean() ? GOLD : 0xFFFFFF);
            ctx.fill(px, py, px + size, py + size, col);
        }
    }

    // ---------------------------------------------------------------- helpers
    private static int clampA(double v) { return Math.max(5, Math.min(255, (int) (v * 255))); }
    private static double ease(double t) { return 1 - Math.pow(1 - t, 3); }
    private static double easeOutBack(double t) {
        double c1 = 1.70158, c3 = c1 + 1;
        return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
    }
}
