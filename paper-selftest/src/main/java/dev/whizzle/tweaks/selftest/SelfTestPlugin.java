package dev.whizzle.tweaks.selftest;

import dev.whizzle.tweaks.LeverRotate;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.FaceAttachable.AttachedFace;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.Item;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Supplier;

/**
 * Development-only: builds levers on a real Paper server, turns them with {@link LeverRotate#rotate} and checks what
 * the world actually does (lever state, drops, which lamps are lit, a chunk unload and reload). Writes report.txt;
 * with -Dtweaks.selftest.exit=true it stops the server when done.
 */
public final class SelfTestPlugin extends JavaPlugin {
    record Step(String name, Runnable action, int ticks, Supplier<String> check) {}

    private static final int Y = -60;
    private static final List<BlockFace> TURNS = List.of(BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.NORTH);
    private static final List<BlockFace> SIDES = List.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST);
    private final List<Step> steps = new ArrayList<>();
    private final List<String> report = new ArrayList<>();
    private World w;
    private int failures;

    @Override public void onEnable() { getServer().getScheduler().runTaskLater(this, this::start, 40); }

    Block at(int x, int y, int z) { return w.getBlockAt(x, y, z); }

    /** Places a lever without physics (it never checks its support while being placed this way). */
    Block lever(Block b, AttachedFace face, BlockFace facing, boolean powered, boolean physics) {
        b.setType(Material.LEVER, false);
        Switch s = (Switch) b.getBlockData();
        s.setAttachedFace(face);
        s.setFacing(facing);
        s.setPowered(powered);
        b.setBlockData(s, physics);
        return b;
    }

    static boolean lit(Block b) { return b.getBlockData() instanceof Lightable l && l.isLit(); }

    int leverDrops() {
        int n = 0;
        for (Item it : w.getEntitiesByClass(Item.class)) if (it.getItemStack().getType() == Material.LEVER) n += it.getItemStack().getAmount();
        return n;
    }

    /** Null if the lever is there with this face, facing and power; otherwise what's wrong. */
    static String lever(Block b, AttachedFace face, BlockFace facing, boolean powered) {
        if (b.getType() != Material.LEVER) return "lever gone (" + b.getType() + ")";
        Switch s = (Switch) b.getBlockData();
        List<String> bad = new ArrayList<>();
        if (s.getAttachedFace() != face) bad.add("face " + s.getAttachedFace() + ", expected " + face);
        if (s.getFacing() != facing) bad.add("facing " + s.getFacing() + ", expected " + facing);
        if (s.isPowered() != powered) bad.add("powered " + s.isPowered() + ", expected " + powered);
        return bad.isEmpty() ? null : String.join("; ", bad);
    }

    static String all(String... problems) {
        StringJoiner j = new StringJoiner("; ");
        for (String p : problems) if (p != null) j.add(p);
        return j.length() == 0 ? null : j.toString();
    }

    String noDrops() { int d = leverDrops(); return d == 0 ? null : d + " lever item(s) dropped"; }

    void start() {
        w = getServer().getWorlds().getFirst();
        for (int cx = -2; cx <= 4; cx++) for (int cz = -2; cz <= 2; cz++) w.setChunkForceLoaded(cx, cz, true);
        w.setChunkForceLoaded(404 >> 4, 0, true);
        for (int x = -10; x <= 70; x++) for (int z = -10; z <= 10; z++) clearColumn(x, z);
        for (int x = 398; x <= 410; x++) for (int z = -3; z <= 3; z++) clearColumn(x, z);

        floorAndCeiling(0, AttachedFace.FLOOR, false);
        floorAndCeiling(4, AttachedFace.FLOOR, true);
        floorAndCeiling(8, AttachedFace.CEILING, false);
        floorAndCeiling(12, AttachedFace.CEILING, true);
        wall(20, true);
        wall(30, false);
        plainPhysicsComparison(40);
        pipeCorner(404);

        String only = System.getProperty("tweaks.selftest.only", "");
        if (!only.isBlank()) {
            List<String> prefixes = List.of(only.split(","));
            steps.removeIf(st -> prefixes.stream().noneMatch(pr -> st.name().startsWith(pr.trim())));
            report.add("INFO only steps starting with " + prefixes + " (" + steps.size() + " steps)");
        }
        run(0);
    }

    void clearColumn(int x, int z) {
        at(x, Y - 1, z).setType(Material.STONE, false);
        for (int y = Y; y < Y + 5; y++) at(x, y, z).setType(Material.AIR, false);
    }

    /** A floor or ceiling lever with a lamp beside it: four turns, the lamp follows the lever's power throughout. */
    void floorAndCeiling(int x, AttachedFace face, boolean powered) {
        String name = face.name().toLowerCase(Locale.ROOT) + (powered ? " on" : " off");
        int y = face == AttachedFace.FLOOR ? Y : Y + 1;
        Block l = at(x, y, 0), lamp = at(x + 1, y, 0);
        steps.add(new Step(name + ": placed, lamp " + (powered ? "lit" : "dark"), () -> {
            if (face == AttachedFace.CEILING) at(x, y + 1, 0).setType(Material.STONE);
            lamp.setType(Material.REDSTONE_LAMP);
            lever(l, face, BlockFace.NORTH, powered, true);   // physics: the lamp beside it hears about it
        }, 6, () -> all(lever(l, face, BlockFace.NORTH, powered), lit(lamp) == powered ? null : "lamp lit " + lit(lamp))));
        for (BlockFace want : TURNS) steps.add(new Step(name + ": turn to " + want, () -> LeverRotate.rotate(l), 6,
            () -> all(lever(l, face, want, powered), lit(lamp) == powered ? null : "lamp lit " + lit(lamp), noDrops())));
    }

    /**
     * A wall lever in the middle of four quartz blocks with a lamp behind each. The block it hangs on is strongly
     * powered, so exactly that one lamp is lit while the lever is on; turning it moves the power round.
     */
    void wall(int x, boolean powered) {
        String name = "wall " + (powered ? "on" : "off");
        int y = Y + 1;
        Block l = at(x, y, 0);
        Map<BlockFace, Block> lamps = new LinkedHashMap<>();
        for (BlockFace f : SIDES) lamps.put(f, l.getRelative(f, 2));
        steps.add(new Step(name + ": placed between four quartz blocks, lamps behind them", () -> {
            lever(l, AttachedFace.WALL, BlockFace.NORTH, powered, false);
            for (Block lamp : lamps.values()) lamp.setType(Material.REDSTONE_LAMP);
            for (BlockFace f : SIDES) l.getRelative(f).setType(Material.QUARTZ_BLOCK);   // last, so the lamps re-check
        }, 6, () -> all(lever(l, AttachedFace.WALL, BlockFace.NORTH, powered), lamps(lamps, powered ? BlockFace.SOUTH : null))));
        for (BlockFace want : TURNS) steps.add(new Step(name + ": turn to " + want, () -> LeverRotate.rotate(l), 6,
            () -> all(lever(l, AttachedFace.WALL, want, powered), lamps(lamps, powered ? want.getOppositeFace() : null), noDrops())));
    }

    /** Null if exactly the lamp on side {@code litSide} (or none) is lit. */
    static String lamps(Map<BlockFace, Block> lamps, BlockFace litSide) {
        List<String> bad = new ArrayList<>();
        lamps.forEach((f, b) -> { if (lit(b) != (f == litSide)) bad.add(f + " lamp " + (lit(b) ? "lit" : "dark")); });
        return bad.isEmpty() ? null : "expected only " + (litSide == null ? "no lamp" : "the " + litSide + " lamp") + " lit: " + bad;
    }

    /** For the record (INFO, not a check): what plain setBlockData(data, true) does with a powered wall lever. */
    void plainPhysicsComparison(int x) {
        int y = Y + 1;
        Block l = at(x, y, 0);
        Map<BlockFace, Block> lamps = new LinkedHashMap<>();
        for (BlockFace f : SIDES) lamps.put(f, l.getRelative(f, 2));
        steps.add(new Step("compare: powered wall lever for setBlockData(data, true)", () -> {
            lever(l, AttachedFace.WALL, BlockFace.NORTH, true, false);
            for (Block lamp : lamps.values()) lamp.setType(Material.REDSTONE_LAMP);
            for (BlockFace f : SIDES) l.getRelative(f).setType(Material.QUARTZ_BLOCK);
        }, 6, () -> lamps(lamps, BlockFace.SOUTH)));
        steps.add(new Step("compare: setBlockData(data, true) once, to east", () -> {
            Switch s = (Switch) l.getBlockData();
            l.setBlockData(LeverRotate.rotated(s), true);
        }, 6, () -> {
            List<BlockFace> on = new ArrayList<>();
            lamps.forEach((f, b) -> { if (lit(b)) on.add(f); });
            report.add("INFO plain setBlockData(data, true): lever " + (l.getType() == Material.LEVER ? "kept" : "GONE")
                + ", lamps lit " + on + " (correct would be only [WEST]; LeverRotate.rotate gets it right, see 'wall on')");
            return null;
        }));
    }

    /**
     * The owner's build: lightning rods lying along a quartz wall as a pipe, a wall lever between two rods as a
     * valve. Turned sideways it hangs on a rod (no sturdy face) and must stay, through other block changes and a real
     * chunk unload and reload; breaking the rod it hangs on drops it, as with the debug stick.
     */
    void pipeCorner(int x) {
        int y = Y + 1;
        Block l = at(x, y, 0), westRod = at(x - 1, y, 0), eastRod = at(x + 1, y, 0);
        int cx = x >> 4;
        steps.add(new Step("pipe: lever placed on the wall between two rods", () -> {
            for (int dx = -2; dx <= 2; dx++) for (int dy = -1; dy <= 1; dy++) at(x + dx, y + dy, 1).setType(Material.QUARTZ_BLOCK);
            for (Block rod : List.of(westRod, eastRod)) {
                rod.setType(Material.LIGHTNING_ROD);
                Directional d = (Directional) rod.getBlockData();
                d.setFacing(BlockFace.EAST);   // lying along the wall
                rod.setBlockData(d);
            }
            lever(l, AttachedFace.WALL, BlockFace.NORTH, false, true);   // hangs on the quartz behind it, as placed by hand
        }, 6, () -> lever(l, AttachedFace.WALL, BlockFace.NORTH, false)));
        String[] on = {"the west rod", "air", "the east rod", "the quartz"};
        for (int i = 0; i < 4; i++) {
            BlockFace want = TURNS.get(i);
            steps.add(new Step("pipe: turn to " + want + " (hangs on " + on[i] + ")", () -> LeverRotate.rotate(l), 10,
                () -> all(lever(l, AttachedFace.WALL, want, false), noDrops())));
        }
        for (BlockFace want : List.of(BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST))
            steps.add(new Step("pipe: turn again to " + want, () -> LeverRotate.rotate(l), 4, () -> lever(l, AttachedFace.WALL, want, false)));
        steps.add(new Step("pipe: a block placed above the lever leaves it", () -> at(x, y + 1, 0).setType(Material.STONE), 6,
            () -> all(lever(l, AttachedFace.WALL, BlockFace.WEST, false), noDrops())));
        steps.add(new Step("pipe: the other rod broken leaves it", () -> westRod.setType(Material.AIR), 6,
            () -> all(lever(l, AttachedFace.WALL, BlockFace.WEST, false), noDrops())));
        steps.add(new Step("pipe: the other rod put back leaves it", () -> {
            westRod.setType(Material.LIGHTNING_ROD);
            Directional d = (Directional) westRod.getBlockData();
            d.setFacing(BlockFace.EAST);
            westRod.setBlockData(d);
        }, 6, () -> all(lever(l, AttachedFace.WALL, BlockFace.WEST, false), noDrops())));
        steps.add(new Step("pipe: release chunk", () -> w.setChunkForceLoaded(cx, 0, false), 2, () -> null));
        steps.add(new Step("pipe: chunk unloaded", () -> {
            if (!w.unloadChunk(cx, 0, true)) report.add("INFO chunk refused to unload");
        }, 4, () -> w.isChunkLoaded(cx, 0) ? "chunk still loaded" : null));
        steps.add(new Step("pipe: chunk loaded again, lever still on the east rod", () -> w.setChunkForceLoaded(cx, 0, true), 10,
            () -> all(lever(l, AttachedFace.WALL, BlockFace.WEST, false), eastRod.getType() == Material.LIGHTNING_ROD ? null : "rod gone", noDrops())));
        steps.add(new Step("pipe: breaking the rod it hangs on drops it", () -> eastRod.setType(Material.AIR), 6, () -> {
            if (l.getType() == Material.LEVER) return "lever still there";
            return leverDrops() == 1 ? null : leverDrops() + " lever items dropped, expected 1";
        }));
    }

    void run(int i) {
        if (i >= steps.size()) { finish(); return; }
        Step s = steps.get(i);
        try { s.action().run(); }
        catch (RuntimeException ex) { record(s.name(), "action threw " + ex); run(i + 1); return; }
        getServer().getScheduler().runTaskLater(this, () -> {
            String problem;
            try { problem = s.check().get(); } catch (RuntimeException ex) { problem = "check threw " + ex; }
            record(s.name(), problem);
            run(i + 1);
        }, Math.max(1, s.ticks()));
    }

    void record(String name, String problem) {
        if (problem == null) report.add("PASS " + name);
        else { failures++; report.add("FAIL " + name + ": " + problem); }
    }

    void finish() {
        report.add(failures == 0 ? "SELFTEST PASSED on " + getServer().getVersion() : "SELFTEST FAILED (" + failures + ")");
        try { getDataFolder().mkdirs(); Files.write(getDataFolder().toPath().resolve("report.txt"), report); }
        catch (IOException e) { getLogger().severe("Could not write report: " + e); }
        getLogger().info(String.join("\n", report));
        if (Boolean.getBoolean("tweaks.selftest.exit")) getServer().shutdown();
    }
}
