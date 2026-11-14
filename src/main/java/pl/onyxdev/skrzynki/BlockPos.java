package pl.onyxdev.skrzynki;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public record BlockPos(String world, int x, int y, int z) {
    public static BlockPos of(Block b) {
        return new BlockPos(b.getWorld().getName(), b.getX(), b.getY(), b.getZ());
    }

    public static BlockPos parse(String s) {
        String[] p = s.split(";");
        return new BlockPos(p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
    }

    public String serialize() {
        return world + ";" + x + ";" + y + ";" + z;
    }

    public World bukkitWorld() {
        return Bukkit.getWorld(world);
    }

    public boolean isLoaded() {
        World w = bukkitWorld();
        return w != null && w.isChunkLoaded(x >> 4, z >> 4);
    }

    public Block block() {
        World w = bukkitWorld();
        return w == null ? null : w.getBlockAt(x, y, z);
    }

    public Location top() {
        return new Location(bukkitWorld(), x + 0.5, y + 1.0, z + 0.5);
    }

    public String pretty() {
        return world + " " + x + " " + y + " " + z;
    }
}
