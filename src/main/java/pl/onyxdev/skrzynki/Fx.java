package pl.onyxdev.skrzynki;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;

public final class Fx {
    private Fx() {
    }

    public static void sound(Location at, Sound s, float volume, float pitch) {
        at.getWorld().playSound(at, s, SoundCategory.MASTER, volume, pitch);
    }

    public static float note(int semitones) {
        return (float) Math.pow(2.0, (semitones - 12) / 12.0);
    }

    public static void dust(Location at, Color c, float size, int count, double spread) {
        at.getWorld().spawnParticle(Particle.DUST, at, count, spread, spread, spread, 0,
                new Particle.DustOptions(c, size));
    }

    public static void dustFade(Location at, Color from, Color to, float size, int count, double spread) {
        at.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, at, count, spread, spread, spread, 0,
                new Particle.DustTransition(from, to, size));
    }

    public static void plain(Location at, Particle p, int count, double spread, double speed) {
        if (p.getDataType() != Void.class) return;
        World w = at.getWorld();
        w.spawnParticle(p, at, count, spread, spread, spread, speed);
    }

    public static void ring(Location center, double radius, int points, Color c, float size) {
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2 * i / points;
            Location l = center.clone().add(Math.cos(a) * radius, 0, Math.sin(a) * radius);
            center.getWorld().spawnParticle(Particle.DUST, l, 1, 0, 0, 0, 0, new Particle.DustOptions(c, size));
        }
    }

    public static Color mix(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return Color.fromRGB(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }
}
