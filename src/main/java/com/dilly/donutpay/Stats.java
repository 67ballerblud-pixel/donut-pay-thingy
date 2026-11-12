package com.dilly.donutpay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Lifetime stats are persisted locally; session stats live in memory only. */
public class Stats {
    public static class Payment {
        public String from; public long amount; public long time;
        public Payment() {}
        public Payment(String f, long a, long t) { from = f; amount = a; time = t; }
    }

    // persisted
    public long lifetimeTotal;
    public long lifetimeCount;
    public long largest;
    public String largestFrom = "";
    public long largestAt;
    public Map<String, Long> byPlayer = new HashMap<>();
    public Map<String, Integer> countByPlayer = new HashMap<>();
    public List<Payment> recent = new ArrayList<>(); // newest first, capped

    // session (not persisted)
    public transient long sessionTotal, sessionCount, sessionLargest;
    public transient String sessionLargestFrom = "";
    public transient long sessionStart = System.currentTimeMillis();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Stats INSTANCE;

    public static Stats get() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }

    public void record(String from, long amount) {
        long now = System.currentTimeMillis();
        lifetimeTotal += amount; lifetimeCount++;
        sessionTotal += amount; sessionCount++;
        byPlayer.merge(from, amount, Long::sum);
        countByPlayer.merge(from, 1, Integer::sum);
        if (amount > largest) { largest = amount; largestFrom = from; largestAt = now; }
        if (amount > sessionLargest) { sessionLargest = amount; sessionLargestFrom = from; }
        recent.add(0, new Payment(from, amount, now));
        while (recent.size() > 300) recent.remove(recent.size() - 1);
        save();
    }

    public List<Map.Entry<String, Long>> topPayers(int n) {
        List<Map.Entry<String, Long>> l = new ArrayList<>(byPlayer.entrySet());
        l.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        return l.subList(0, Math.min(n, l.size()));
    }

    public void resetSession() {
        sessionTotal = 0; sessionCount = 0; sessionLargest = 0; sessionLargestFrom = "";
        sessionStart = System.currentTimeMillis();
    }

    public static void resetAll() {
        INSTANCE = new Stats();
        INSTANCE.save();
    }

    public static void load() {
        Path f = Config.dir().resolve("stats.json");
        try (Reader r = Files.newBufferedReader(f)) {
            INSTANCE = GSON.fromJson(r, Stats.class);
        } catch (Exception e) {
            INSTANCE = null;
        }
        if (INSTANCE == null) INSTANCE = new Stats();
        INSTANCE.sessionStart = System.currentTimeMillis();
    }

    public synchronized void save() {
        try (Writer w = Files.newBufferedWriter(Config.dir().resolve("stats.json"))) {
            GSON.toJson(this, w);
        } catch (Exception e) {
            DonutPayClient.LOG.error("Could not save stats", e);
        }
    }

    // ---- number helpers ----
    public static String fmt(long v) {
        if (v >= 1_000_000_000_000L) return "$" + d(v / 1e12) + "T";
        if (v >= 1_000_000_000L) return "$" + d(v / 1e9) + "B";
        if (v >= 1_000_000L) return "$" + d(v / 1e6) + "M";
        if (v >= 1_000L) return "$" + d(v / 1e3) + "K";
        return "$" + v;
    }
    public static String exact(long v) { return String.format(Locale.ROOT, "$%,d", v); }
    private static String d(double x) {
        String s = String.format(Locale.ROOT, "%.2f", x);
        if (s.contains(".")) s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }
}
