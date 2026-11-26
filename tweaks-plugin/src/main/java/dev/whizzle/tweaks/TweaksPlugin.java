package dev.whizzle.tweaks;

import org.bukkit.plugin.java.JavaPlugin;

/** Small one-click tweaks. Each tweak is a listener, switched on or off in config.yml. Not final: MockBukkit subclasses it. */
public class TweaksPlugin extends JavaPlugin {
    private boolean leverRotate;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        leverRotate = getConfig().getBoolean("lever-rotate.enabled", true);
        if (leverRotate) getServer().getPluginManager().registerEvents(new LeverRotate(), this);
        getLogger().info("Lever rotate: " + (leverRotate ? "on" : "off (config.yml)"));
    }

    /** Whether the lever tweak was switched on when the plugin started. */
    public boolean leverRotateEnabled() { return leverRotate; }
}
