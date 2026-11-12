package com.dilly.donutpay.gui;

import com.dilly.donutpay.Stats;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class StatsScreen extends Screen {
    private final Screen parent;
    private int scroll = 0;
    private static final SimpleDateFormat DF = new SimpleDateFormat("MMM d HH:mm");

    public StatsScreen(Screen parent) { super(Text.literal("DonutPay Stats")); this.parent = parent; }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
            .dimensions(width / 2 - 100, height - 28, 95, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset ALL stats"), b ->
            client.setScreen(new ConfirmScreen(yes -> {
                if (yes) { Stats.resetAll(); scroll = 0; }
                client.setScreen(this);
            }, Text.literal("Reset all saved payment stats?"), Text.literal("This cannot be undone."))))
            .dimensions(width / 2 + 5, height - 28, 95, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        Stats st = Stats.get();
        int lx = width / 2 - 150, y = 12;
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, y, 0xFFFFFF); y += 16;

        ctx.drawText(textRenderer, "Session: " + Stats.fmt(st.sessionTotal) + " from " + st.sessionCount + " payments", lx, y, 0x55FF55, true); y += 11;
        ctx.drawText(textRenderer, "Lifetime: " + Stats.fmt(st.lifetimeTotal) + " (" + Stats.exact(st.lifetimeTotal) + ") from " + st.lifetimeCount + " payments", lx, y, 0xFFFFFF, true); y += 11;
        String big = st.largest > 0 ? Stats.fmt(st.largest) + " from " + st.largestFrom + " (" + DF.format(new Date(st.largestAt)) + ")" : "-";
        ctx.drawText(textRenderer, "Largest payment: " + big, lx, y, 0xFFD700, true); y += 16;

        ctx.drawText(textRenderer, "Biggest-paying players", lx, y, 0xFFAA00, true);
        ctx.drawText(textRenderer, "Recent payments (scroll)", lx + 160, y, 0xFFAA00, true); y += 12;

        int top = y;
        List<Map.Entry<String, Long>> tp = st.topPayers(10);
        for (int i = 0; i < tp.size(); i++) {
            var e = tp.get(i);
            String line = (i + 1) + ". " + textRenderer.trimToWidth(e.getKey(), 70) + "  " + Stats.fmt(e.getValue())
                + " (" + st.countByPlayer.getOrDefault(e.getKey(), 0) + "x)";
            ctx.drawText(textRenderer, line, lx, top + i * 11, 0xFFFFFF, true);
        }

        int rows = Math.max(1, (height - 40 - top) / 11);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, st.recent.size() - rows)));
        for (int i = 0; i < rows && i + scroll < st.recent.size(); i++) {
            var p = st.recent.get(i + scroll);
            ctx.drawText(textRenderer, DF.format(new Date(p.time)) + "  " + Stats.fmt(p.amount) + "  " + textRenderer.trimToWidth(p.from, 70),
                lx + 160, top + i * 11, 0xCCCCCC, true);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        scroll -= (int) Math.signum(v) * 2;
        return true;
    }

    @Override
    public void close() { client.setScreen(parent); }
}
