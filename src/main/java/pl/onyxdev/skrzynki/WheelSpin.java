package pl.onyxdev.skrzynki;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class WheelSpin extends BukkitRunnable implements Anim {
    private static final int WINDUP = 10;
    private static final int SPIN = 130;
    private static final int STOP = WINDUP + SPIN;
    private static final int REVEAL = STOP + 6;
    private static final int FLY = REVEAL + 40;
    private static final int GIVE = FLY + 8;
    private static final int END = GIVE + 14;

    private final SunSkrzynki plugin;
    private final Crate crate;
    private final BlockPos pos;
    private final WheelVisual wheel;
    private final UUID playerId;
    private final String playerName;
    private final Reward result;
    private final int target;
    private final boolean test;

    private final float phi0;
    private final float phiStart;
    private final float distance;
    private int t;
    private int lastSeg;
    private int flapTicks;
    private ItemDisplay pop;
    private TextDisplay label;
    private boolean delivered;
    private boolean finished;

    public WheelSpin(SunSkrzynki plugin, Crate crate, BlockPos pos, WheelVisual wheel, Player player, Reward result, boolean test) {
        this.plugin = plugin;
        this.crate = crate;
        this.pos = pos;
        this.wheel = wheel;
        this.playerId = player == null ? null : player.getUniqueId();
        this.playerName = player == null ? "konsola" : player.getName();
        this.result = result;
        this.test = test;
        this.target = crate.rewards.indexOf(result);

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        float f = landingFraction(rnd);
        double localTarget = (target + f) * wheel.seg;
        this.phi0 = wheel.phi();
        this.phiStart = phi0 - 0.35f;
        double twoPi = Math.PI * 2;
        double delta = ((-localTarget - phiStart) % twoPi + twoPi) % twoPi;
        this.distance = (float) ((4 + rnd.nextInt(2)) * twoPi + delta);
        this.lastSeg = wheel.segmentAt(phi0);
    }

    private float landingFraction(ThreadLocalRandom rnd) {
        int n = crate.rewards.size();
        if (n >= 3 && result.rarity != Rarity.LEGENDARNY && rnd.nextInt(100) < 45) {
            Reward after = crate.rewards.get((target + 1) % n);
            Reward before = crate.rewards.get((target - 1 + n) % n);
            if (after.rarity == Rarity.LEGENDARNY) return 0.88f + rnd.nextFloat() * 0.08f;
            if (before.rarity == Rarity.LEGENDARNY) return 0.04f + rnd.nextFloat() * 0.07f;
        }
        return 0.2f + rnd.nextFloat() * 0.6f;
    }

    @Override
    public BlockPos pos() {
        return pos;
    }

    @Override
    public String playerName() {
        return playerName;
    }

    private float phiAt(int tick) {
        if (tick <= 0) return phi0;
        if (tick < WINDUP) {
            double x = (double) tick / WINDUP;
            return (float) (phi0 - 0.35 * Math.sin(x * Math.PI / 2));
        }
        if (tick >= STOP) return phiStart + distance;
        double x = (double) (tick - WINDUP) / SPIN;
        double ease = 1 - Math.pow(1 - x, 4);
        return (float) (phiStart + distance * ease);
    }

    @Override
    public void run() {
        try {
            tick();
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Błąd animacji koła", e);
            finishNow();
            return;
        }
        t++;
        if (t > END) finishNow();
    }

    private void tick() {
        if (!wheel.isValid() || !pos.isLoaded()) {
            finishNow();
            return;
        }
        Location c = wheel.center;

        if (t == 0) {
            Fx.sound(c, Sound.BLOCK_LEVER_CLICK, 1f, 0.7f);
            Fx.sound(c, Sound.ITEM_CROSSBOW_LOADING_START, 1f, 0.6f);
        }
        if (t == WINDUP) {
            Fx.sound(c, Sound.ENTITY_BREEZE_WIND_BURST, 0.9f, 0.9f);
            Fx.sound(c, Sound.ITEM_TRIDENT_RIPTIDE_1, 0.7f, 1.3f);
            Fx.plain(c, Particle.CLOUD, 14, wheel.radius * 0.4, 0.04);
        }

        if (t <= STOP) {
            float phi = phiAt(t);
            if (t % 2 == 0 || t == STOP) wheel.apply(phi, t == STOP ? 1 : 2);
            int now = wheel.segmentAt(phi);
            if (now != lastSeg) {
                lastSeg = now;
                double speed = Math.abs(phiAt(t + 1) - phi);
                float pitch = (float) Math.max(0.8, Math.min(1.8, 0.9 + speed * 1.2));
                Fx.sound(c, Sound.BLOCK_NOTE_BLOCK_HAT, 0.9f, pitch);
                Fx.sound(c, Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 0.45f, 1.7f);
                wheel.flap(-0.45f, 1);
                flapTicks = 3;
            } else if (flapTicks > 0 && --flapTicks == 0) {
                wheel.flap(0, 3);
            }
            if (t % 2 == 0) wheel.bulbs(t / 2, 3);
            if (t > WINDUP && t < WINDUP + 50 && t % 3 == 0) {
                Fx.dust(wheel.point(ThreadLocalRandom.current().nextFloat() * 6.28f, 0, wheel.radius + 0.1f, 0.1f),
                        crate.theme.lightColor(), 1f, 1, 0.02);
            }
        }
        if (t == STOP) {
            int under = wheel.segmentAt(phiAt(STOP));
            if (under != target) {
                plugin.getLogger().warning("Koło " + crate.id + ": wskaźnik na polu " + under + ", a wylosowano " + target);
            }
            wheel.flap(0, 2);
            Fx.sound(c, Sound.BLOCK_WOODEN_TRAPDOOR_CLOSE, 1f, 0.8f);
            wheel.bulbs(0, 1);
        }

        if (t == REVEAL) reveal();
        if (t > REVEAL && t < FLY) hold();
        if (t == FLY) fly();
        if (t == GIVE) deliver();
        if (t == END - 4) {
            wheel.highlight(target, false, null);
            wheel.bulbs(0, 2);
        }
    }

    private void reveal() {
        boolean blank = Crate.isBlank(result);
        Color col = blank ? Color.fromRGB(0x555555) : result.rarity.color();
        wheel.highlight(target, true, col);

        Location from = wheel.iconLocation(target);
        Location front = wheel.center.clone().add(wheel.normal.clone().multiply(1.4));
        ItemStack show = blank ? new ItemStack(org.bukkit.Material.WITHER_ROSE) : result.item.clone();
        show.setAmount(1);
        pop = from.getWorld().spawn(from, ItemDisplay.class, d -> {
            tag(d);
            d.setItemStack(show);
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            d.setBillboard(Display.Billboard.CENTER);
            d.setBrightness(new Display.Brightness(15, 15));
            d.setGlowing(!blank);
            if (!blank) d.setGlowColorOverride(col);
            d.setTransformation(tr(0.5f));
        });
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (finished || pop == null) return;
            pop.setTeleportDuration(6);
            pop.teleport(front);
            scale(pop, blank ? 0.9f : 1.5f, 6);
        }, 2);

        label = front.getWorld().spawn(front.clone().add(0, 1.0, 0), TextDisplay.class, d -> {
            tag(d);
            d.setBillboard(Display.Billboard.CENTER);
            d.setBackgroundColor(Color.fromARGB(120, 10, 8, 20));
            d.setShadowed(true);
            d.setAlignment(TextDisplay.TextAlignment.CENTER);
            d.text(revealText(blank));
            d.setBrightness(new Display.Brightness(15, 15));
        });

        if (blank) {
            Celebrate.sadTrombone(plugin, wheel.center);
            Fx.plain(front, Particle.LARGE_SMOKE, 25, 0.3, 0.02);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!finished) Celebrate.burst(plugin, front, result.rarity, false);
            }, 6);
            Celebrate.sound(plugin, wheel.center, result.rarity);
        }
        if (!test) Celebrate.announce(plugin, crate, playerId, playerName, result);
    }

    private void hold() {
        int since = t - REVEAL;
        if (since % 4 == 0) wheel.bulbs(since / 4, 2);
        if (since % 6 == 0 && since < 30 && !Crate.isBlank(result)) wheel.highlight(target, since % 12 == 0, result.rarity.color());
        if (pop != null && !Crate.isBlank(result) && t % 3 == 0) {
            Fx.dust(pop.getLocation().add(0, 0.1, 0), result.rarity.color(), 1f, 2, 0.35);
            if (result.rarity == Rarity.LEGENDARNY) Fx.plain(pop.getLocation(), Particle.TOTEM_OF_UNDYING, 3, 0.3, 0.05);
        }
    }

    private void fly() {
        if (label != null) {
            label.remove();
            label = null;
        }
        if (pop == null) return;
        Player p = playerId == null ? null : Bukkit.getPlayer(playerId);
        if (p != null && p.getWorld().equals(pop.getWorld()) && p.getLocation().distanceSquared(pop.getLocation()) < 24 * 24) {
            pop.setTeleportDuration(8);
            pop.teleport(p.getEyeLocation().subtract(0, 0.45, 0));
        }
        scale(pop, 0.15f, 8);
        if (!Crate.isBlank(result)) Fx.sound(pop.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 0.4f, 1.6f);
    }

    private void deliver() {
        if (delivered) return;
        delivered = true;
        if (pop != null) {
            Fx.plain(pop.getLocation(), Particle.POOF, 5, 0.05, 0.02);
            pop.remove();
            pop = null;
        }
        if (!test && !Crate.isBlank(result)) plugin.service.deliver(playerId, crate, result);
    }

    @Override
    public void finishNow() {
        if (finished) return;
        finished = true;
        try {
            cancel();
        } catch (IllegalStateException ignored) {
        }
        if (t < REVEAL && !test) Celebrate.record(plugin, crate, playerName, result);
        deliver();
        if (label != null) label.remove();
        if (wheel.isValid()) {
            if (t < STOP) wheel.apply(phiStart + distance, 0);
            wheel.highlight(target, false, null);
            wheel.flap(0, 0);
            wheel.bulbs(0, 2);
        }
        plugin.service.finished(this);
    }

    private Component revealText(boolean blank) {
        if (blank) return Text.mm("<gray><b>PECH!</b>");
        String hex = Text.hex(result.rarity.rgb);
        String deco = switch (result.rarity) {
            case LEGENDARNY -> "✦ ✦ ";
            case EPICKI -> "✦ ";
            default -> "";
        };
        String deco2 = new StringBuilder(deco).reverse().toString();
        Component out = Text.mm("<" + hex + "><b>" + deco + result.rarity.label.toUpperCase() + deco2 + "</b>")
                .append(Component.newline()).append(Celebrate.rewardName(result));
        if (test) out = out.append(Component.newline()).append(Text.mm("<dark_gray>(podgląd admina)"));
        return out;
    }

    private void tag(Display d) {
        d.setPersistent(false);
        d.getPersistentDataContainer().set(plugin.keys.fx, PersistentDataType.BYTE, (byte) 1);
    }

    private static Transformation tr(float s) {
        return new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(s, s, s), new Quaternionf());
    }

    private static void scale(Display d, float s, int ticks) {
        d.setInterpolationDelay(0);
        d.setInterpolationDuration(ticks);
        d.setTransformation(tr(s));
    }
}
