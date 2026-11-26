package dev.whizzle.tweaks;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.FaceAttachable.AttachedFace;
import org.bukkit.block.data.type.Switch;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LeverRotateTest {
    ServerMock server;
    TweaksPlugin plugin;
    WorldMock world;
    PlayerMock player;
    Block lever;
    final LeverRotate listener = new LeverRotate();

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(TweaksPlugin.class);
        world = server.addSimpleWorld("world");
        player = server.addPlayer();
        player.setGameMode(GameMode.SURVIVAL);
        player.setSneaking(true);
        lever = world.getBlockAt(0, 70, 0);
        placeLever(AttachedFace.WALL, BlockFace.NORTH, false);
    }

    @AfterEach
    void tearDown() { MockBukkit.unmock(); }

    void placeLever(AttachedFace face, BlockFace facing, boolean powered) {
        lever.setType(Material.LEVER);
        Switch s = (Switch) lever.getBlockData();
        s.setAttachedFace(face);
        s.setFacing(facing);
        s.setPowered(powered);
        lever.setBlockData(s);
    }

    Switch data() { return (Switch) lever.getBlockData(); }

    PlayerInteractEvent click(Action action, EquipmentSlot hand) {
        return new PlayerInteractEvent(player, action, null, lever, BlockFace.UP, hand);
    }

    PlayerInteractEvent click() { return click(Action.RIGHT_CLICK_BLOCK, EquipmentSlot.HAND); }

    @Test
    void enabledByDefault() {
        assertTrue(plugin.leverRotateEnabled());
        assertTrue(plugin.getConfig().getBoolean("lever-rotate.enabled"));
    }

    @Test
    void permissionDefaultsToEveryone() {
        assertTrue(player.hasPermission(LeverRotate.PERMISSION));
    }

    @Test
    void nextFollowsTheDebugStick() {
        assertEquals(BlockFace.EAST, LeverRotate.next(BlockFace.NORTH));
        assertEquals(BlockFace.SOUTH, LeverRotate.next(BlockFace.EAST));
        assertEquals(BlockFace.WEST, LeverRotate.next(BlockFace.SOUTH));
        assertEquals(BlockFace.NORTH, LeverRotate.next(BlockFace.WEST));
    }

    @ParameterizedTest
    @EnumSource(AttachedFace.class)
    void fourTurnsComeBackKeepingFaceAndPowered(AttachedFace face) {
        for (boolean powered : new boolean[] {false, true}) {
            placeLever(face, BlockFace.NORTH, powered);
            List<BlockFace> seen = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                assertTrue(LeverRotate.rotate(lever));
                assertEquals(Material.LEVER, lever.getType());
                assertEquals(face, data().getAttachedFace(), "face");
                assertEquals(powered, data().isPowered(), "powered");
                seen.add(data().getFacing());
            }
            assertEquals(List.of(BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.NORTH), seen);
        }
    }

    @Test
    void rotateIgnoresOtherBlocks() {
        lever.setType(Material.STONE_BUTTON);
        assertFalse(LeverRotate.rotate(lever));
        assertEquals(Material.STONE_BUTTON, lever.getType());
    }

    @Test
    void supportIsBelowAboveOrBehind() {
        assertEquals(lever.getRelative(BlockFace.SOUTH), LeverRotate.support(lever, data()));   // facing north: hangs on the south block
        placeLever(AttachedFace.FLOOR, BlockFace.EAST, false);
        assertEquals(lever.getRelative(BlockFace.DOWN), LeverRotate.support(lever, data()));
        placeLever(AttachedFace.CEILING, BlockFace.EAST, false);
        assertEquals(lever.getRelative(BlockFace.UP), LeverRotate.support(lever, data()));
    }

    @Test
    void sneakClickWithEmptyHandsTurnsAndCancels() {
        PlayerInteractEvent e = click();
        listener.onInteract(e);
        assertEquals(BlockFace.EAST, data().getFacing());
        assertFalse(data().isPowered(), "the click must not flip it");
        assertEquals(Event.Result.DENY, e.useInteractedBlock());
    }

    void assertNothingHappens(PlayerInteractEvent e) {
        assertFalse(LeverRotate.shouldRotate(e));
        listener.onInteract(e);
        assertEquals(BlockFace.NORTH, data().getFacing());
        assertNotEquals(Event.Result.DENY, e.useInteractedBlock(), "vanilla behaviour must be left alone");
    }

    @Test
    void notSneaking() {
        player.setSneaking(false);
        assertNothingHappens(click());
    }

    @Test
    void itemInMainHand() {
        player.getInventory().setItemInMainHand(new ItemStack(Material.STONE));
        assertNothingHappens(click());
    }

    @Test
    void itemInOffHand() {
        player.getInventory().setItemInOffHand(new ItemStack(Material.TORCH));
        assertNothingHappens(click());
    }

    @Test
    void offHandEvent() {
        assertNothingHappens(click(Action.RIGHT_CLICK_BLOCK, EquipmentSlot.OFF_HAND));
    }

    @Test
    void leftClick() {
        assertNothingHappens(click(Action.LEFT_CLICK_BLOCK, EquipmentSlot.HAND));
    }

    @Test
    void adventureAndSpectator() {
        player.setGameMode(GameMode.ADVENTURE);
        assertNothingHappens(click());
        player.setGameMode(GameMode.SPECTATOR);
        assertNothingHappens(click());
    }

    @Test
    void creativeWorks() {
        player.setGameMode(GameMode.CREATIVE);
        assertTrue(LeverRotate.shouldRotate(click()));
    }

    @Test
    void noPermission() {
        player.addAttachment(plugin, LeverRotate.PERMISSION, false);
        assertNothingHappens(click());
    }

    @Test
    void deniedByAnotherPlugin() {
        PlayerInteractEvent e = click();
        e.setUseInteractedBlock(Event.Result.DENY);
        assertFalse(LeverRotate.shouldRotate(e));
        listener.onInteract(e);
        assertEquals(BlockFace.NORTH, data().getFacing());
    }

    @Test
    void notALever() {
        lever.setType(Material.STONE_BUTTON);
        assertFalse(LeverRotate.shouldRotate(click()));
    }

    @Test
    void throughTheEventBus() {
        // The listener is registered by the plugin: a fired event turns the lever once.
        PlayerInteractEvent e = click();
        server.getPluginManager().callEvent(e);
        assertEquals(BlockFace.EAST, data().getFacing());
    }
}
