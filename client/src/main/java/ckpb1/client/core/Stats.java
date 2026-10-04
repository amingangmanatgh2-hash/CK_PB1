package ckpb1.client.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.Locale;

/** Session information for the Session HUD element. */
public final class Stats {

    private static long worldJoinTime;
    private static String serverLabel = "";
    private static int kills;
    private static boolean wasInWorld;

    private Stats() {
    }

    public static void tick(MinecraftClient mc) {
        ClientPlayerEntity player = mc.player;
        if (player == null) {
            wasInWorld = false;
            return;
        }
        if (!wasInWorld) {
            wasInWorld = true;
            worldJoinTime = System.currentTimeMillis();
            kills = 0;
            serverLabel = mc.getCurrentServerEntry() != null
                    ? mc.getCurrentServerEntry().address
                    : "Singleplayer";
        }
    }

    public static void onKill() {
        kills++;
    }

    public static String username(MinecraftClient mc) {
        return mc.getSession().getUsername();
    }

    public static String server() {
        return serverLabel;
    }

    public static int kills() {
        return kills;
    }

    public static String playtime() {
        if (worldJoinTime == 0) {
            return "0m";
        }
        long seconds = (System.currentTimeMillis() - worldJoinTime) / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        return hours > 0
                ? String.format(Locale.ROOT, "%dh %02dm", hours, minutes)
                : String.format(Locale.ROOT, "%dm", minutes);
    }
}
