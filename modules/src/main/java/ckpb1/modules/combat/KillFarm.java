package ckpb1.modules.combat;

import ckpb1.client.core.Category;
import ckpb1.client.core.ChatUtil;
import ckpb1.client.core.CombatState;
import ckpb1.client.core.Module;
import ckpb1.client.core.RotationController;
import ckpb1.client.core.StatusOverlay;
import ckpb1.client.core.setting.BoolSetting;
import ckpb1.client.core.setting.ListSetting;
import ckpb1.client.core.setting.ModeSetting;
import ckpb1.client.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;

/**
 * Kill Farm: attacks a configurable list of specific players (target list),
 * with target priority, switching delay, tracking and auto-stop. Designed
 * exclusively for singleplayer / private test servers.
 */
public final class KillFarm extends Module {

    public final ListSetting targetList = add(new ListSetting("Target List",
            "Players that may be attacked (add names in the Click GUI or via .targets add <name>)",
            List.of()));
    public final ModeSetting priority = add(new ModeSetting("Target Priority",
            "How the next target is chosen", "List Order", "Nearest", "Lowest Health"));
    public final NumberSetting range = add(new NumberSetting("Range",
            "Attack range in blocks", 4.5, 1.0, 6.0, 0.1, "m"));
    public final NumberSetting switchDelay = add(new NumberSetting("Switch Delay",
            "Seconds to wait before switching targets", 0.5, 0.0, 3.0, 0.1, "s"));
    public final ModeSetting attackMode = add(new ModeSetting("Attack Mode",
            "Single keeps one target until it dies; Switch rotates through the list",
            "Single", "Switch"));
    public final NumberSetting cpsMin = add(new NumberSetting("CPS Min",
            "Minimum clicks per second", 8, 1, 20, 1));
    public final NumberSetting cpsMax = add(new NumberSetting("CPS Max",
            "Maximum clicks per second", 12, 1, 20, 1));
    public final BoolSetting targetTracking = add(new BoolSetting("Target Tracking",
            "Keep aiming at the current target", true));
    public final BoolSetting stopWhenTargetsLeave = add(new BoolSetting("Stop When Targets Leave",
            "Automatically disable the module when no target is reachable", true));

    private final AttackTiming timing = new AttackTiming();
    private Entity currentTarget;
    private long targetSwitchAllowedAt;

    public KillFarm() {
        super("Kill Farm", "Attacks a specific player target list with switching and tracking (test environments only)",
                Category.COMBAT);
    }

    /** All online players that are on the target list. */
    private List<PlayerEntity> matchingTargets(MinecraftClient mc) {
        List<PlayerEntity> out = new ArrayList<>();
        if (mc.world == null || mc.player == null) {
            return out;
        }
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || !p.isAlive() || p.isSpectator()) {
                continue;
            }
            if (p.isInvisible()) {
                continue;
            }
            if (!targetList.containsIgnoreCase(p.getGameProfile().getName())) {
                continue;
            }
            out.add(p);
        }
        return out;
    }

    private PlayerEntity pickTarget(MinecraftClient mc, List<PlayerEntity> candidates) {
        ClientPlayerEntity self = mc.player;
        PlayerEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (PlayerEntity p : candidates) {
            if (p == currentTarget) {
                return p; // stick with the current target while it lives
            }
            double score;
            switch (priority.get()) {
                case "Nearest" -> score = self.distanceTo(p);
                case "Lowest Health" -> score = p.getHealth();
                default -> { // List Order
                    int idx = indexOfTarget(p.getGameProfile().getName());
                    score = idx < 0 ? 999 : idx;
                }
            }
            if (score < bestScore) {
                bestScore = score;
                best = p;
            }
        }
        return best;
    }

    private int indexOfTarget(String name) {
        List<String> list = targetList.get();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null || mc.world == null) {
            return;
        }
        List<PlayerEntity> candidates = matchingTargets(mc);
        List<String> status = new ArrayList<>();

        if (candidates.isEmpty()) {
            status.add("§cno targets online");
            StatusOverlay.set("Kill Farm", status);
            if (stopWhenTargetsLeave.isOn()) {
                ChatUtil.message("§cKill Farm: no targets left - stopping");
                setEnabled(false, true);
                currentTarget = null;
            }
            return;
        }

        // validate the current target (dead / out of range / gone)
        if (currentTarget != null) {
            boolean valid = currentTarget.isAlive() && mc.player.distanceTo(currentTarget) <= range.get() + 2;
            if (!valid) {
                currentTarget = null;
                targetSwitchAllowedAt = System.currentTimeMillis() + (long) (switchDelay.get() * 1000);
                status.add("§etarget lost - switching");
            }
        }

        if (currentTarget == null && System.currentTimeMillis() >= targetSwitchAllowedAt) {
            currentTarget = pickTarget(mc, candidates);
        }

        if (currentTarget == null) {
            status.add("§7waiting " + String.format("%.1f", Math.max(0,
                    (targetSwitchAllowedAt - System.currentTimeMillis()) / 1000.0)) + "s to switch");
            StatusOverlay.set("Kill Farm", status);
            return;
        }

        double dist = mc.player.distanceTo(currentTarget);
        CombatAssistant.publishTarget(currentTarget, dist);
        status.add("target: §f" + currentTarget.getName().getString());
        status.add(String.format("dist: §f%.1fm §7| combo: §f%d", dist, CombatState.combo()));
        status.add(String.format("targets online: §f%d/%d", candidates.size(), targetList.get().size()));
        StatusOverlay.set("Kill Farm", status);

        if (dist > range.get()) {
            status.add("§7out of range");
            return; // no auto-chase - keeps the module predictable
        }

        if (targetTracking.isOn()) {
            RotationController.lookAt(currentTarget, 35f);
        }

        if (timing.ready()) {
            timing.spend(cpsMin.getInt(), cpsMax.getInt());
            mc.interactionManager.attackEntity(mc.player, currentTarget);
            mc.player.swingHand(Hand.MAIN_HAND);
        }

        // Switch mode: rotate to next candidate after the delay even if alive
        if (attackMode.is("Switch") && currentTarget instanceof LivingEntity living
                && living.getHealth() <= 4.0f && System.currentTimeMillis() >= targetSwitchAllowedAt) {
            currentTarget = null;
            targetSwitchAllowedAt = System.currentTimeMillis() + (long) (switchDelay.get() * 1000);
        }
    }

    @Override
    protected void onDisable() {
        currentTarget = null;
        StatusOverlay.clear("Kill Farm");
        CombatState.targetLastSeen = 0;
    }
}
