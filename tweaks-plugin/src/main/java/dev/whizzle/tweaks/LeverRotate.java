package dev.whizzle.tweaks;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.FaceAttachable.AttachedFace;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.PlayerInventory;

/**
 * Sneak + right-click a lever with both hands empty: its {@code facing} goes north -> east -> south -> west, like the
 * debug stick. {@code face} (floor, wall, ceiling) and {@code powered} stay; support isn't checked, also like the
 * debug stick, so a wall lever can end up hanging on a lightning rod, glass or air.
 */
public final class LeverRotate implements Listener {
    public static final String PERMISSION = "tweaks.leverrotate";

    // HIGH: after protection plugins (LOW/NORMAL) have had their say. A denied click is checked below.
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (!shouldRotate(event)) return;
        Block lever = event.getClickedBlock();
        if (!rotate(lever)) return;
        event.setCancelled(true);   // no flip; Paper sends the lever's real state back to the clicking player
        lever.getWorld().playSound(lever.getLocation().add(0.5, 0.5, 0.5), Sound.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.4f, 1.3f);
    }

    /** Whether this click should turn the lever instead of flipping it. */
    public static boolean shouldRotate(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return false;
        if (event.getHand() != EquipmentSlot.HAND) return false;   // fires once per hand: act once
        if (event.useInteractedBlock() == Event.Result.DENY) return false;   // another plugin said no
        Block b = event.getClickedBlock();
        if (b == null || b.getType() != Material.LEVER) return false;
        Player p = event.getPlayer();
        if (!p.isSneaking()) return false;
        GameMode mode = p.getGameMode();
        if (mode != GameMode.SURVIVAL && mode != GameMode.CREATIVE) return false;
        PlayerInventory inv = p.getInventory();
        if (!inv.getItemInMainHand().isEmpty() || !inv.getItemInOffHand().isEmpty()) return false;
        return p.hasPermission(PERMISSION);
    }

    /** The next direction, in the debug stick's order. */
    public static BlockFace next(BlockFace facing) {
        return switch (facing) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            default -> BlockFace.NORTH;
        };
    }

    /** A copy of the lever's data with the next direction; face and powered unchanged. */
    public static Switch rotated(Switch data) {
        Switch copy = (Switch) data.clone();
        copy.setFacing(next(data.getFacing()));
        return copy;
    }

    /** The block a lever hangs on: below on the floor, above on the ceiling, behind it on a wall. */
    public static Block support(Block lever, Switch data) {
        AttachedFace face = data.getAttachedFace();
        if (face == AttachedFace.FLOOR) return lever.getRelative(BlockFace.DOWN);
        if (face == AttachedFace.CEILING) return lever.getRelative(BlockFace.UP);
        return lever.getRelative(data.getFacing().getOppositeFace());
    }

    /**
     * Turns the lever one step. Written like the debug stick (no physics), so the lever never checks its support and
     * can't pop. A powered lever then updates its neighbours and those of the old and new block it hangs on, as a
     * lever does when flipped, so power moves to the new block. Returns false if the block isn't a lever.
     */
    public static boolean rotate(Block lever) {
        if (lever.getType() != Material.LEVER || !(lever.getBlockData() instanceof Switch before)) return false;
        Switch after = rotated(before);
        Block oldSupport = support(lever, before);
        lever.setBlockData(after, false);
        if (after.isPowered()) {
            Neighbours.update(lever);
            Neighbours.update(oldSupport);
            Neighbours.update(support(lever, after));
        }
        return true;
    }
}
