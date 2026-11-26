package dev.whizzle.tweaks;

import org.bukkit.block.Block;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * Sends a block update to the six neighbours of a block, as Minecraft's {@code Level.updateNeighborsAt} does (a
 * lever calls it on itself and on the block it hangs on when it flips). Bukkit has no API for it, so this calls
 * Minecraft by reflection (Paper runs with Mojang's names). If those names ever change it logs once and does nothing:
 * levers still turn, but redstone next to a powered one only notices at its next update.
 */
final class Neighbours {
    private static final Logger LOG = Logger.getLogger("Tweaks");
    private static volatile boolean broken;
    private static Method getHandle, getBlockState, getBlock, updateNeighborsAt;
    private static Constructor<?> blockPos;

    private Neighbours() {}

    static void update(Block b) {
        if (broken) return;
        try {
            init(b);
            Object level = getHandle.invoke(b.getWorld());
            Object pos = blockPos.newInstance(b.getX(), b.getY(), b.getZ());
            Object nmsBlock = getBlock.invoke(getBlockState.invoke(level, pos));
            updateNeighborsAt.invoke(level, pos, nmsBlock, null);   // null orientation: what vanilla passes without the redstone experiment
        } catch (ReflectiveOperationException | RuntimeException e) {
            broken = true;
            LOG.warning("Tweaks: can't send neighbour updates on this server (" + e + "). Levers still turn; redstone "
                + "next to a powered lever updates later.");
        }
    }

    private static synchronized void init(Block b) throws ReflectiveOperationException {
        if (updateNeighborsAt != null) return;
        Method handle = b.getWorld().getClass().getMethod("getHandle");
        Class<?> level = Class.forName("net.minecraft.world.level.Level", false, handle.getReturnType().getClassLoader());
        ClassLoader cl = level.getClassLoader();
        Class<?> pos = Class.forName("net.minecraft.core.BlockPos", false, cl);
        Class<?> nmsBlock = Class.forName("net.minecraft.world.level.block.Block", false, cl);
        Class<?> orientation = Class.forName("net.minecraft.world.level.redstone.Orientation", false, cl);
        Method state = level.getMethod("getBlockState", pos);
        Method block = state.getReturnType().getMethod("getBlock");
        Method update = level.getMethod("updateNeighborsAt", pos, nmsBlock, orientation);
        blockPos = pos.getConstructor(int.class, int.class, int.class);
        getHandle = handle;
        getBlockState = state;
        getBlock = block;
        updateNeighborsAt = update;
    }
}
