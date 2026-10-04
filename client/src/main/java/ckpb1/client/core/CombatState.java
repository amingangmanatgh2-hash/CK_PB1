package ckpb1.client.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared live combat data: current target HUD values, reach of the last hit,
 * combo counter and kill tracking. Fed by the attack mixin and combat modules,
 * read by HUD elements (Target HUD, Reach Display, Combo, Session).
 */
public final class CombatState {

    // --- Target HUD values (updated by Kill Farm / Combat Assistant) ---
    public static volatile String targetName = "";
    public static volatile float targetHealth;
    public static volatile float targetMaxHealth;
    public static volatile double targetDistance;
    public static volatile long targetLastSeen;

    // --- Last attack info ---
    public static volatile double lastReach;
    public static volatile long lastAttackTime;
    public static volatile String lastHitTarget = "";
    public static volatile float lastHitTargetHealth;
    public static volatile LivingEntity lastHitEntity;

    // --- Combo tracking ---
    private static int combo;
    private static long lastComboHit;
    private static long lastHurtTime;

    // --- Kill tracking (entities we damaged recently) ---
    private static final Map<UUID, Long> RECENT_HITS = new ConcurrentHashMap<>();

    private CombatState() {
    }

    public static void onAttackEntity(PlayerEntity attacker, Entity target) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || attacker != mc.player) {
            return;
        }
        double dist = attacker.distanceTo(target);
        lastReach = dist;
        lastAttackTime = System.currentTimeMillis();
        combo = System.currentTimeMillis() - lastComboHit < 2500 ? combo + 1 : 1;
        lastComboHit = System.currentTimeMillis();
        if (target instanceof LivingEntity living) {
            lastHitTarget = target.getName().getString();
            lastHitTargetHealth = living.getHealth();
            lastHitEntity = living;
            RECENT_HITS.put(target.getUuid(), System.currentTimeMillis());
        }
    }

    /** Detects damage taken (resets combo) and kills of recently hit entities. */
    public static void tick(MinecraftClient mc) {
        if (mc.player == null) {
            return;
        }
        if (mc.player.hurtTime > 0 && System.currentTimeMillis() - lastHurtTime > 200) {
            lastHurtTime = System.currentTimeMillis();
            combo = 0;
        }
        // count kills for entities we hit in the last 3 seconds
        Iterator<Map.Entry<UUID, Long>> it = RECENT_HITS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> e = it.next();
            long t = e.getValue();
            if (System.currentTimeMillis() - t > 3000) {
                it.remove();
                continue;
            }
            if (mc.world != null) {
                Entity entity = null;
                for (Entity candidate : mc.world.getEntities()) {
                    if (candidate.getUuid().equals(e.getKey())) {
                        entity = candidate;
                        break;
                    }
                }
                if (entity == null || !entity.isAlive()) {
                    it.remove();
                    Stats.onKill();
                }
            }
        }
    }

    public static int combo() {
        return System.currentTimeMillis() - lastComboHit < 2500 ? combo : 0;
    }

    public static void resetCombo() {
        combo = 0;
    }

    /** True when a combat module published a target within the last second. */
    public static boolean hasTarget() {
        return !targetName.isEmpty() && System.currentTimeMillis() - targetLastSeen < 1000;
    }
}
