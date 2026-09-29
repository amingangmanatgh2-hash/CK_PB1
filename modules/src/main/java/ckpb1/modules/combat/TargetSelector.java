package ckpb1.modules.combat;

import ckpb1.client.core.Category;
import ckpb1.client.core.Module;
import ckpb1.client.core.setting.BoolSetting;
import ckpb1.client.core.setting.ListSetting;
import ckpb1.client.core.setting.ModeSetting;
import ckpb1.client.core.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Target Selector: shared target selection rules used by Combat Assistant and
 * Kill Farm. Supports selecting specific players by name (private/test
 * environments).
 */
public final class TargetSelector extends Module {

    public final BoolSetting playersOnly = add(new BoolSetting("Players Only",
            "Only target players (recommended for test PvP)", true));
    public final BoolSetting includeMobs = add(new BoolSetting("Include Mobs",
            "Also allow hostile mobs as targets", false));
    public final ListSetting specificPlayers = add(new ListSetting("Specific Players",
            "When not empty, only these players can be targeted", List.of()));
    public final ModeSetting priority = add(new ModeSetting("Priority",
            "How the best target is chosen", "Nearest", "Lowest Health", "Furthest"));
    public final NumberSetting range = add(new NumberSetting("Range",
            "Maximum selection range in blocks", 32, 4, 64, 1, "m"));
    public final BoolSetting requireLineOfSight = add(new BoolSetting("Require Line of Sight",
            "Only pick targets visible from the eye position", true));

    public TargetSelector() {
        super("Target Selector", "Chooses combat targets (players by name, priority modes, range)", Category.COMBAT);
    }

    /** Finds the best target respecting this module's settings (or null). */
    public Entity findTarget(MinecraftClient mc, double overrideRange) {
        ClientPlayerEntity self = mc.player;
        if (self == null || mc.world == null) {
            return null;
        }
        double maxRange = overrideRange > 0 ? overrideRange : range.get();
        List<Entity> candidates = new ArrayList<>();
        for (Entity entity : mc.world.getEntities()) {
            if (entity == self || !entity.isAlive()) {
                continue;
            }
            if (entity instanceof PlayerEntity player && player.isSpectator()) {
                continue;
            }
            boolean isPlayer = entity instanceof PlayerEntity;
            if (isPlayer && playersOnly.isOn()) {
                String playerName = ((PlayerEntity) entity).getGameProfile().getName();
                if (!specificPlayers.isEmpty() && !specificPlayers.containsIgnoreCase(playerName)) {
                    continue;
                }
            } else if (!isPlayer) {
                if (!includeMobs.isOn()) {
                    continue;
                }
                if (!(entity instanceof LivingEntity)) {
                    continue;
                }
            }
            if (self.distanceTo(entity) > maxRange) {
                continue;
            }
            if (requireLineOfSight.isOn() && !self.canSee(entity)) {
                continue;
            }
            candidates.add(entity);
        }
        return pickBest(self, candidates);
    }

    private Entity pickBest(ClientPlayerEntity self, List<Entity> candidates) {
        Entity best = null;
        double bestScore = Double.MAX_VALUE;
        for (Entity e : candidates) {
            double score;
            switch (priority.get()) {
                case "Lowest Health" -> {
                    if (!(e instanceof LivingEntity living)) {
                        continue;
                    }
                    score = living.getHealth();
                }
                case "Furthest" -> score = -self.distanceTo(e);
                default -> score = self.distanceTo(e);
            }
            if (score < bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }
}
