package com.dilly.donutpay;

import com.dilly.donutpay.gui.ConfigScreen;
import com.dilly.donutpay.gui.StatsScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DonutPay Tracker - purely passive and client side.
 * It only READS system/game messages sent by the server. It never sends chat,
 * commands (other than local client commands), payments, or touches session tokens.
 */
public class DonutPayClient implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("donutpay");

    /**
     * Matches the DonutSMP format seen in the reference video:
     *   "<name> paid you $10M"   /   "Sebas04132397 paid you $1"
     * Anchored at the START of the message so a player cannot fake it with chat text
     * like "<Bob> X paid you $1B" (that begins with "<Bob>" and will not match).
     */
    static final Pattern PAID_YOU = Pattern.compile(
        "^([A-Za-z0-9_.]{3,17}) paid you \\$\\s?([0-9][0-9,]*(?:\\.[0-9]+)?)\\s?([kKmMbBtT])?(?![A-Za-z0-9]).*$");

    private static KeyBinding openConfig, openStats;
    private static final KeyBinding.Category CATEGORY =
        KeyBinding.Category.create(Identifier.of("donutpay", "main"));

    @Override
    public void onInitializeClient() {
        Config.load();
        Stats.load();

        openConfig = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.donutpay.config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY));
        openStats = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.donutpay.stats", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, CATEGORY));

        // GAME = system messages from the server. Signed player chat arrives on a different
        // event (CHAT), which we deliberately do NOT listen to.
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            handleServerMessage(message.getString());
        });

        HudElementRegistry.addLast(Identifier.of("donutpay", "payment_hud"), (ctx, tick) -> PaymentHud.render(ctx));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openConfig.wasPressed()) mc.setScreen(new ConfigScreen(null));
            while (openStats.wasPressed()) mc.setScreen(new StatsScreen(null));
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> { Stats.get().save(); Config.save(); });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> {
            dispatcher.register(ClientCommandManager.literal("donutpay")
                .executes(c -> { MinecraftClient.getInstance().send(() ->
                    MinecraftClient.getInstance().setScreen(new ConfigScreen(null))); return 1; })
                .then(ClientCommandManager.literal("stats").executes(c -> {
                    MinecraftClient.getInstance().send(() ->
                        MinecraftClient.getInstance().setScreen(new StatsScreen(null))); return 1; }))
                .then(ClientCommandManager.literal("resetsession").executes(c -> {
                    Stats.get().resetSession();
                    c.getSource().sendFeedback(Text.literal("[DonutPay] Session reset.")); return 1; })));
        });
    }

    static void handleServerMessage(String raw) {
        Config cfg = Config.get();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        // Only count payments for the configured account.
        String me = mc.getSession().getUsername();
        if (!me.equalsIgnoreCase(cfg.ownerName)) return;

        // Only on DonutSMP (unless disabled in settings).
        if (cfg.onlyOnDonutSMP) {
            var entry = mc.getCurrentServerEntry();
            if (entry == null || !entry.address.toLowerCase(Locale.ROOT).contains("donutsmp")) return;
        }

        Matcher m = PAID_YOU.matcher(raw.trim());
        if (!m.matches()) return;

        String sender = m.group(1);
        long amount;
        try { amount = parse(m.group(2), m.group(3)); } catch (NumberFormatException e) { return; }
        if (amount <= 0 || sender.equalsIgnoreCase(me)) return;

        mc.execute(() -> {
            Stats.get().record(sender, amount);
            PaymentHud.onPayment(sender, amount, false);
        });
    }

    static long parse(String num, String suffix) {
        double v = Double.parseDouble(num.replace(",", ""));
        double mult = 1;
        if (suffix != null) {
            switch (Character.toLowerCase(suffix.charAt(0))) {
                case 'k' -> mult = 1e3;
                case 'm' -> mult = 1e6;
                case 'b' -> mult = 1e9;
                case 't' -> mult = 1e12;
            }
        }
        return Math.round(v * mult);
    }
}
