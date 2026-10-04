package ckpb1.modules.utility;

import ckpb1.client.core.Category;
import ckpb1.client.core.Module;

/**
 * No Slow: removes the eating/shielding movement slowdown via a mixin on the
 * client player tick (simplified Wurst "NoSlowdown"). The mixin asks this
 * module whether the slowdown should be skipped.
 */
public final class NoSlow extends Module {

    private static volatile boolean active;

    public NoSlow() {
        super("No Slow", "No movement slowdown while eating or using items", Category.UTILITY);
    }

    @Override
    protected void onEnable() {
        active = true;
    }

    @Override
    protected void onDisable() {
        active = false;
    }

    /** Called by ClientPlayerEntityMixin. */
    public static boolean shouldSkipSlowdown() {
        return active;
    }
}
