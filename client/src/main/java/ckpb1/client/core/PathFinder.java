package ckpb1.client.core;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Grid A* pathfinder used by the CK_PB1 BedWars/World modules.
 * Walk nodes are ground positions (feet) the player can stand on.
 */
public final class PathFinder {

    public static final class Path {
        public final List<BlockPos> waypoints;
        public final boolean reachedGoal;

        Path(List<BlockPos> waypoints, boolean reachedGoal) {
            this.waypoints = waypoints;
            this.reachedGoal = reachedGoal;
        }

        public boolean isEmpty() {
            return waypoints.isEmpty();
        }
    }

    private static final class Node {
        final BlockPos pos;
        final double g;
        final double h;
        final Node parent;

        Node(BlockPos pos, double g, double h, Node parent) {
            this.pos = pos;
            this.g = g;
            this.h = h;
            this.parent = parent;
        }

        double f() {
            return g + h;
        }
    }

    private PathFinder() {
    }

    public static Path findPath(ClientWorld world, BlockPos start, BlockPos goal, int maxNodes) {
        if (start.equals(goal) || maxNodes <= 0) {
            return new Path(new ArrayList<>(), start.equals(goal));
        }
        if (!world.isChunkLoaded(start) || !world.isChunkLoaded(goal)) {
            return new Path(new ArrayList<>(), false);
        }

        PriorityQueue<Node> open = new PriorityQueue<>(256, (a, b) -> Double.compare(a.f(), b.f()));
        Map<Long, Double> bestG = new HashMap<>();
        Node startNode = new Node(start, 0, heuristic(start, goal), null);
        open.add(startNode);
        bestG.put(start.asLong(), 0.0);
        int expanded = 0;
        Node best = startNode;

        while (!open.isEmpty() && expanded < maxNodes) {
            Node current = open.poll();
            if (current.pos.equals(goal)) {
                return new Path(simplify(reconstruct(current)), true);
            }
            expanded++;
            if (current.h < best.h) {
                best = current;
            }
            for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                // flat / step up / drop down candidates
                int[] dyOptions = {0, 1, -1, -2, -3};
                for (int dy : dyOptions) {
                    BlockPos next = current.pos.offset(dir).up(dy);
                    if (dy == 1 && !isPassable(world, current.pos.up(2))) {
                        continue; // no headroom to jump here
                    }
                    if (!isWalkable(world, next)) {
                        continue;
                    }
                    double cost = dy == 0 ? 1.0 : (dy > 0 ? 1.6 : 0.9);
                    double g = current.g + cost;
                    long key = next.asLong();
                    Double prev = bestG.get(key);
                    if (prev != null && prev <= g) {
                        continue;
                    }
                    bestG.put(key, g);
                    open.add(new Node(next, g, heuristic(next, goal), current));
                }
            }
        }
        return new Path(simplify(reconstruct(best)), false);
    }

    private static double heuristic(BlockPos a, BlockPos b) {
        int dx = Math.abs(a.getX() - b.getX());
        int dy = Math.abs(a.getY() - b.getY());
        int dz = Math.abs(a.getZ() - b.getZ());
        return dx + dz + dy * 1.2;
    }

    /** A node is walkable when the two body cells are passable and the floor is solid. */
    public static boolean isWalkable(ClientWorld world, BlockPos feet) {
        return isPassable(world, feet) && isPassable(world, feet.up()) && isSolidGround(world, feet.down());
    }

    public static boolean isPassable(ClientWorld world, BlockPos pos) {
        if (world.isOutOfHeightLimit(pos)) {
            return false;
        }
        return world.getBlockState(pos).getCollisionShape(world, pos).isEmpty();
    }

    public static boolean isSolidGround(ClientWorld world, BlockPos pos) {
        if (world.isOutOfHeightLimit(pos)) {
            return false;
        }
        return !world.getBlockState(pos).getCollisionShape(world, pos).isEmpty();
    }

    private static List<BlockPos> reconstruct(Node end) {
        List<BlockPos> out = new ArrayList<>();
        Node n = end;
        while (n != null) {
            out.add(n.pos);
            n = n.parent;
        }
        java.util.Collections.reverse(out);
        return out;
    }

    /** Merges collinear waypoints so walking looks smoother. */
    private static List<BlockPos> simplify(List<BlockPos> path) {
        List<BlockPos> out = new ArrayList<>();
        for (int i = 0; i < path.size(); i++) {
            BlockPos cur = path.get(i);
            if (i == 0 || i == path.size() - 1) {
                out.add(cur);
                continue;
            }
            BlockPos prev = out.get(out.size() - 1);
            BlockPos next = path.get(i + 1);
            boolean collinear = prev.getX() == cur.getX() && cur.getX() == next.getX()
                    || prev.getZ() == cur.getZ() && cur.getZ() == next.getZ();
            if (!collinear) {
                out.add(cur);
            }
        }
        return out;
    }
}
