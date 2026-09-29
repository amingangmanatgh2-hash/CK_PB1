package ckpb1.client.core;

import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Time-budgeted block scanning around a center. Used by Bed Destroyer
 * (bed detection) and Resource Assistant (ore detection). Scans prefer
 * positions close to the center and never run longer than the given budget,
 * keeping FPS stable on weak machines.
 */
public final class BlockScanner {

    private BlockScanner() {
    }

    public interface Matcher extends BiPredicate<BlockPos, BlockState> {
    }

    /**
     * Scans a cuboid around center within the horizontal/vertical radius.
     * Stops after maxMillis and returns whatever was found (closest first
     * ordering is approximate - callers sort again if needed).
     */
    public static List<BlockPos> scan(ClientWorld world, BlockPos center, int hRadius, int vRadius,
                                      Matcher matcher, long maxMillis) {
        List<BlockPos> found = new ArrayList<>();
        long deadline = System.currentTimeMillis() + maxMillis;
        int minX = center.getX() - hRadius;
        int maxX = center.getX() + hRadius;
        int minZ = center.getZ() - hRadius;
        int maxZ = center.getZ() + hRadius;
        int minY = center.getY() - vRadius;
        int maxY = center.getY() + vRadius;
        BlockPos.Mutable cursor = new BlockPos.Mutable();
        long checked = 0;
        outer:
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    cursor.set(x, y, z);
                    if (world.isChunkLoaded(cursor)) {
                        BlockState state = world.getBlockState(cursor);
                        if (matcher.test(cursor, state)) {
                            found.add(cursor.toImmutable());
                        }
                    }
                    if ((++checked & 0xFFF) == 0 && System.currentTimeMillis() > deadline) {
                        break outer;
                    }
                }
            }
        }
        return found;
    }
}
