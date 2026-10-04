package ckpb1.modules.combat;

import java.util.concurrent.ThreadLocalRandom;

/** Randomized CPS attack timing shared by combat modules. */
final class AttackTiming {

    private long nextAttackAt;

    boolean ready() {
        return System.currentTimeMillis() >= nextAttackAt;
    }

    /** Consumes an attack slot with a randomized delay between two CPS bounds. */
    void spend(int minCps, int maxCps) {
        int lo = Math.max(1, Math.min(minCps, maxCps));
        int hi = Math.max(lo, maxCps);
        double cps = ThreadLocalRandom.current().nextDouble(lo, hi + 0.01);
        long delayMs = (long) (1000.0 / cps);
        nextAttackAt = System.currentTimeMillis() + delayMs;
    }
}
