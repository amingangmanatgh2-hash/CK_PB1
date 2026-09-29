package ckpb1.modules.bedwars;

import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared bed scanning + defense analysis for the CK_PB1 BedWars modules
 * (test environments).
 */
public final class BedScanner {

    /** A detected bed with analysis data. */
    public static final class BedInfo {
        public BlockPos head;
        public BlockPos foot;
        public double distance;
        public int defense;

        @Override
        public String toString() {
            return String.format("%s (%dm, def %d)", head.toShortString(), (int) distance, defense);
        }
    }

    private BedScanner() {
    }

    /** Resolves the head block of a bed from either of its two blocks. */
    public static BlockPos headOf(net.minecraft.block.BlockState state, BlockPos pos) {
        if (state.getBlock() instanceof BedBlock && state.get(BedBlock.PART) == BedPart.FOOT) {
            return pos.offset(state.get(BedBlock.FACING));
        }
        return pos;
    }

    /** Scans loaded chunks around a center for beds. */
    public static List<BedInfo> scanBeds(ClientWorld world, BlockPos center, int hRadius, int vRadius,
                                         long budgetMs) {
        List<BlockPos> found = ckpb1.client.core.BlockScanner.scan(world, center, hRadius, vRadius,
                (pos, state) -> state.getBlock() instanceof BedBlock, budgetMs);
        List<BedInfo> beds = new ArrayList<>();
        for (BlockPos pos : found) {
            BedInfo info = new BedInfo();
            info.head = headOf(world.getBlockState(pos), pos);
            // skip the foot duplicates: only register from the head position
            var headState = world.getBlockState(info.head);
            if (!(headState.getBlock() instanceof BedBlock)) {
                continue;
            }
            if (headState.get(BedBlock.PART) == BedPart.FOOT) {
                info.foot = info.head;
                info.head = headOf(headState, info.head);
            } else {
                info.foot = info.head.offset(headState.get(BedBlock.FACING).getOpposite());
            }
            if (beds.stream().anyMatch(b -> b.head.equals(info.head))) {
                continue;
            }
            info.distance = Math.sqrt(center.getSquaredDistance(info.head));
            info.defense = defenseLevel(world, info.head);
            beds.add(info);
        }
        return beds;
    }

    /**
     * Defense level around a bed: 0 = open, 1 = light, 2 = medium, 3+ = heavy.
     * Counts typical BedWars defense blocks in a 5x3x5 shell around the bed.
     */
    public static int defenseLevel(ClientWorld world, BlockPos bedHead) {
        int count = 0;
        for (BlockPos pos : BlockPos.iterate(bedHead.add(-2, -1, -2), bedHead.add(2, 2, 2))) {
            if (world.isChunkLoaded(pos) && isDefenseBlock(world.getBlockState(pos).getBlock())) {
                count++;
            }
        }
        if (count == 0) return 0;
        if (count <= 3) return 1;
        if (count <= 8) return 2;
        return 3;
    }

    public static boolean isDefenseBlock(Block block) {
        return block == Blocks.OBSIDIAN
                || block == Blocks.CRYING_OBSIDIAN
                || block == Blocks.END_STONE
                || block == Blocks.RESPAWN_ANCHOR
                || block == Blocks.BLAST_FURNACE
                || block == Blocks.ANCIENT_DEBRIS;
    }

    /** Finds a walkable standing position adjacent to the bed. */
    public static BlockPos standPosition(ClientWorld world, BlockPos bedHead) {
        for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            BlockPos candidate = bedHead.offset(dir);
            if (ckpb1.client.core.PathFinder.isWalkable(world, candidate)) {
                return candidate;
            }
        }
        // try one block further out
        for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            BlockPos candidate = bedHead.offset(dir, 2);
            if (ckpb1.client.core.PathFinder.isWalkable(world, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /** Direction of the face of the bed block that faces the given position. */
    public static Direction faceTowards(BlockPos bedHead, BlockPos from) {
        int dx = from.getX() - bedHead.getX();
        int dz = from.getZ() - bedHead.getZ();
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx >= 0 ? Direction.EAST : Direction.WEST;
        }
        return dz >= 0 ? Direction.SOUTH : Direction.NORTH;
    }
}
