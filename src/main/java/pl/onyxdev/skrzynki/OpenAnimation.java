package pl.onyxdev.skrzynki;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Lidded;
import org.bukkit.entity.Display;
import org.bukkit.entity.Firework;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class OpenAnimation extends BukkitRunnable implements Anim {
    private static final int[] GAPS = {2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 5, 5, 6, 7, 8, 10, 14};
    private static final int LID_OPEN = 14;
    private static final int RISE = 24;
    private static final int REVEAL;
    private static final int FLY;
    private static final int GIVE;
    private static final int CLOSE;
    private static final int END;

    static {
        int t = RISE + 2;
        for (int g : GAPS) t += g;
        REVEAL = t;
        FLY = REVEAL + 48;
        GIVE = FLY + 8;
        CLOSE = GIVE + 3;
        END = CLOSE + 8;
    }

    private final SunSkrzynki plugin;
    private final Crate crate;
    private final BlockPos pos;
    private final UUID playerId;
    private final String playerName;
    private final Reward result;
    private final boolean test;

    private final Location base;
    private final Location center;
    private final List<ItemDisplay> orbit = new ArrayList<>();
    private final int[] switchAt = new int[GAPS.length];
    private ItemDisplay main;
    private TextDisplay label;
    private int t;
    private int switchIndex;
    private float angle;
    private float spinSpeed = 0.55f;
    private float scale = 0.2f;
    private boolean delivered;
    private boolean finished;

    private final boolean teaseNearMiss;

    public OpenAnimation(SunSkrzynki plugin, Crate crate, BlockPos pos, Player player, Reward result, boolean test) {
        this.plugin = plugin;
        this.crate = crate;
        this.pos = pos;
        this.playerId = player == null ? null : player.getUniqueId();
        this.playerName = player == null ? "konsola" : player.getName();
        this.result = result;
        this.test = test;
        this.base = pos.top();
        this.center = base.clone().add(0, 1.15, 0);
        int at = RISE + 2;
        for (int i = 0; i < GAPS.length; i++) {
            at += GAPS[i];
            switchAt[i] = at;
        }
        this.teaseNearMiss = result.rarity != Rarity.LEGENDARNY
                && crate.rewards.stream().anyMatch(r -> r.rarity == Rarity.LEGENDARNY)
                && ThreadLocalRandom.current().nextInt(100) < 45;
    }

    @Override
    public BlockPos pos() {
        return pos;
    }

    @Override
    public String playerName() {
        return playerName;
    }

    @Override
    public void run() {
        try {
            tick();
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Błąd animacji skrzynki", e);
            finishNow();
            return;
        }
        t++;
        if (t > END) finishNow();
    }

    private void tick() {
        if (base.getWorld() == null || !pos.isLoaded()) {
            finishNow();
            return;
        }
        if (t < RISE) charge();
        if (t == LID_OPEN) lid(true);
        if (t == RISE) spawnDisplays();
        if (t > RISE && t < REVEAL) spin();
        if (t == REVEAL) reveal();
        if (t > REVEAL && t < FLY) hold();
        if (t == FLY) fly();
        if (t == GIVE) deliver();
        if (t == CLOSE) lid(false);
        if (main != null && t < FLY && t % 2 == 0) {
            angle += spinSpeed * 2;
            float bob = t > REVEAL ? (float) Math.sin((t - REVEAL) * 0.12) * 0.08f : 0f;
            float s = scale;
            if (t < RISE + 10) s = 0.2f + 0.8f * (t - RISE) / 10f;
            else if (t >= REVEAL && t < REVEAL + 4) s = 1.85f;
            transform(main, s, angle, bob, t >= REVEAL + 4 && t < REVEAL + 10 ? 6 : 2);
        }
    }

    private void charge() {
        Color light = crate.theme.lightColor();
        Color dark = crate.theme.darkColor();
        double r = 1.15 - t * 0.032;
        for (int k = 0; k < 3; k++) {
            double a = t * 0.55 + k * (Math.PI * 2 / 3);
            Location l = base.clone().add(Math.cos(a) * r, -0.9 + t * 0.075, Math.sin(a) * r);
            Fx.dustFade(l, light, dark, 1.2f, 1, 0);
        }
        if (t % 2 == 0) Fx.plain(base.clone().add(0, 0.3, 0), Particle.ENCHANT, 12, 0.9, 1.2);
        if (t == 0) {
            Fx.sound(base, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.9f, 1.3f);
            Fx.sound(base, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 0.8f);
        }
        if (t % 3 == 0) {
            Fx.sound(base, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, Fx.note(4 + t / 2));
        }
    }

    private void lid(boolean open) {
        Block b = pos.block();
        if (b == null) return;
        BlockState st = b.getState();
        if (st instanceof Lidded lidded) {
            if (open) lidded.open();
            else lidded.close();
        }
        Material m = b.getType();
        Sound s;
        if (m == Material.ENDER_CHEST) s = open ? Sound.BLOCK_ENDER_CHEST_OPEN : Sound.BLOCK_ENDER_CHEST_CLOSE;
        else if (m == Material.BARREL) s = open ? Sound.BLOCK_BARREL_OPEN : Sound.BLOCK_BARREL_CLOSE;
        else if (m.name().endsWith("SHULKER_BOX")) s = open ? Sound.BLOCK_SHULKER_BOX_OPEN : Sound.BLOCK_SHULKER_BOX_CLOSE;
        else s = open ? Sound.BLOCK_CHEST_OPEN : Sound.BLOCK_CHEST_CLOSE;
        Fx.sound(base, s, 1f, open ? 0.85f : 1f);
        if (open) {
            Fx.sound(base, Sound.BLOCK_VAULT_OPEN_SHUTTER, 1f, 1.1f);
            Fx.plain(base.clone().add(0, 0.1, 0), Particle.END_ROD, 30, 0.2, 0.18);
            Fx.plain(base.clone().add(0, 0.1, 0), Particle.CLOUD, 12, 0.25, 0.05);
        }
    }

    private void spawnDisplays() {
        Reward first = crate.randomForShow();
        main = base.getWorld().spawn(base.clone().add(0, 0.1, 0), ItemDisplay.class, d -> {
            setup(d);
            d.setItemStack(showItem(first));
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            d.setTransformation(tr(0.2f, 0, 0));
            d.setGlowing(true);
            d.setGlowColorOverride(first.rarity.color());
        });
        scale = 1.0f;

        label = base.getWorld().spawn(center.clone().add(0, 0.95, 0), TextDisplay.class, d -> {
            setup(d);
            d.setBillboard(Display.Billboard.CENTER);
            d.setBackgroundColor(Color.fromARGB(110, 10, 8, 20));
            d.setShadowed(true);
            d.setAlignment(TextDisplay.TextAlignment.CENTER);
            d.text(nameLine(first));
        });

        for (int i = 0; i < 6; i++) {
            Reward r = crate.randomForShow();
            ItemDisplay o = base.getWorld().spawn(base.clone().add(0, 0.2, 0), ItemDisplay.class, d -> {
                setup(d);
                d.setItemStack(showItem(r));
                d.setTransformation(tr(0.42f, 0, 0));
            });
            o.setTeleportDuration(2);
            orbit.add(o);
        }
        Fx.sound(base, Sound.BLOCK_VAULT_EJECT_ITEM, 1f, 0.9f);
        Fx.sound(base, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.5f, 1.4f);
    }

    private void spin() {
        double progress = (double) (t - RISE) / (REVEAL - RISE);
        spinSpeed = (float) (0.55 - 0.45 * progress);

        if (switchIndex < switchAt.length - 1 && t == switchAt[switchIndex]) {
            boolean penultimate = switchIndex == switchAt.length - 2;
            Reward r = penultimate && teaseNearMiss ? legendary() : crate.randomForShow();
            if (r != null) {
                main.setItemStack(showItem(r));
                main.setGlowColorOverride(r.rarity.color());
                label.text(nameLine(r));
            }
            float pitch = (float) (0.9 + progress * 0.9);
            Fx.sound(center, Sound.BLOCK_NOTE_BLOCK_HAT, 0.8f, pitch);
            Fx.sound(center, Sound.BLOCK_NOTE_BLOCK_BIT, 0.35f, pitch);
            if (switchIndex >= switchAt.length - 3) {
                Fx.sound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.6f, 1.6f);
            }
            if (switchIndex % 3 == 0) {
                ItemDisplay o = orbit.get(switchIndex / 3 % orbit.size());
                Reward rr = crate.randomForShow();
                if (rr != null) o.setItemStack(showItem(rr));
            }
            switchIndex++;
        }

        if (t == RISE + 2) {
            main.setTeleportDuration(10);
            main.teleport(center);
        }
        double radius = 1.3 - 0.45 * progress;
        boolean launch = t == RISE + 2;
        if (launch || (t > RISE + 10 && t % 2 == 0)) {
            int tt = launch ? RISE + 12 : t;
            for (int i = 0; i < orbit.size(); i++) {
                double a = -tt * (0.22 - 0.1 * progress) + i * Math.PI * 2 / orbit.size();
                double y = Math.sin(tt * 0.15 + i) * 0.15;
                ItemDisplay o = orbit.get(i);
                o.setTeleportDuration(launch ? 9 : 2);
                o.teleport(center.clone().add(Math.cos(a) * radius, y, Math.sin(a) * radius));
            }
        }

        Color c = crate.theme.lightColor();
        if (t % 2 == 0) {
            for (int k = 0; k < 4; k++) {
                double a = t * 0.3 + k * Math.PI / 2;
                Fx.dust(center.clone().add(Math.cos(a) * 0.75, -0.55, Math.sin(a) * 0.75), c, 0.9f, 1, 0);
            }
        }
        if (t % 5 == 0) Fx.plain(center, Particle.ELECTRIC_SPARK, 4, 0.35, 0.05);
    }

    private Reward legendary() {
        List<Reward> legends = crate.rewards.stream().filter(r -> r.rarity == Rarity.LEGENDARNY).toList();
        return legends.isEmpty() ? crate.randomForShow() : legends.get(ThreadLocalRandom.current().nextInt(legends.size()));
    }

    private void reveal() {
        Rarity rar = result.rarity;
        main.setItemStack(showItem(result));
        main.setGlowColorOverride(rar.color());
        scale = rar == Rarity.LEGENDARNY ? 1.5f : 1.3f;
        transform(main, 1.85f, angle, 0, 3);
        spinSpeed = 0.06f;
        label.text(revealText());
        label.setTransformation(new Transformation(new Vector3f(0, 0.1f, 0), new AxisAngle4f(),
                new Vector3f(rar.ordinal() >= 2 ? 1.25f : 1.1f), new AxisAngle4f()));

        for (ItemDisplay o : orbit) {
            o.setTeleportDuration(4);
            o.teleport(center);
            transform(o, 0f, 0, 0, 4);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            orbit.forEach(e -> e.remove());
            orbit.clear();
        }, 5);

        for (int k = 0; k < 10; k++) {
            int step = k;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (finished) return;
                Fx.ring(center.clone().add(0, -0.3, 0), 0.4 + step * 0.36, 18 + step * 5, rar.color(), 1.7f - step * 0.1f);
            }, k);
        }

        Celebrate.burst(plugin, center, rar, true);
        Celebrate.sound(plugin, center, rar);
        if (!test) Celebrate.announce(plugin, crate, playerId, playerName, result);
    }

    private void hold() {
        int since = t - REVEAL;
        Rarity rar = result.rarity;
        if (t % 3 == 0) {
            double a = since * 0.35;
            for (int k = 0; k < 2; k++) {
                double aa = a + k * Math.PI;
                Fx.dust(center.clone().add(Math.cos(aa) * 0.8, Math.sin(since * 0.1) * 0.4, Math.sin(aa) * 0.8),
                        rar.color(), 1.1f, 1, 0);
            }
        }
        if (rar == Rarity.LEGENDARNY && t % 2 == 0) {
            double a = since * 0.4;
            Fx.plain(center.clone().add(Math.cos(a) * 1.1, -0.8 + (since % 20) * 0.1, Math.sin(a) * 1.1),
                    Particle.TOTEM_OF_UNDYING, 2, 0.02, 0.01);
        }
        if (rar == Rarity.EPICKI && t % 4 == 0) Fx.plain(center, Particle.WITCH, 6, 0.5, 0.02);
        if (t % 6 == 0) Fx.plain(center, Particle.END_ROD, 1, 0.5, 0.01);
    }

    private void fly() {
        if (label != null) {
            label.remove();
            label = null;
        }
        Player p = playerId == null ? null : Bukkit.getPlayer(playerId);
        if (p != null && p.getWorld().equals(center.getWorld()) && p.getLocation().distanceSquared(center) < 24 * 24) {
            main.setTeleportDuration(8);
            main.teleport(p.getEyeLocation().subtract(0, 0.45, 0));
        }
        transform(main, 0.15f, angle + 3f, 0, 8);
        Fx.sound(center, Sound.ENTITY_BREEZE_WIND_BURST, 0.4f, 1.6f);
    }

    private void deliver() {
        if (delivered) return;
        delivered = true;
        if (main != null) Fx.plain(main.getLocation(), Particle.POOF, 5, 0.05, 0.02);
        if (!test) plugin.service.deliver(playerId, crate, result);
    }

    @Override
    public void finishNow() {
        if (finished) return;
        finished = true;
        try {
            cancel();
        } catch (IllegalStateException ignored) {
        }
        if (t < REVEAL && !test) {
            Celebrate.record(plugin, crate, playerName, result);
        }
        deliver();
        if (main != null) main.remove();
        if (label != null) label.remove();
        orbit.forEach(e -> e.remove());
        orbit.clear();
        if (t < CLOSE && pos.isLoaded()) {
            Block b = pos.block();
            if (b != null && b.getState() instanceof Lidded l) l.close();
        }
        plugin.service.finished(this);
    }

    private void setup(Display d) {
        d.setPersistent(false);
        d.getPersistentDataContainer().set(plugin.keys.fx, PersistentDataType.BYTE, (byte) 1);
        d.setBrightness(new Display.Brightness(15, 15));
        d.setViewRange(1.2f);
    }

    private static Transformation tr(float s, float angle, float y) {
        return new Transformation(new Vector3f(0, y, 0), new Quaternionf().rotateY(angle),
                new Vector3f(s, s, s), new Quaternionf());
    }

    private static void transform(Display d, float s, float angle, float y, int ticks) {
        d.setInterpolationDelay(0);
        d.setInterpolationDuration(ticks);
        d.setTransformation(tr(s, angle, y));
    }

    private static ItemStack showItem(Reward r) {
        if (r == null) return new ItemStack(Material.BARRIER);
        ItemStack it = r.item.clone();
        it.setAmount(1);
        return it;
    }

    static Component rewardName(Reward r) {
        return Celebrate.rewardName(r);
    }

    private Component nameLine(Reward r) {
        return Text.mm("<" + Text.hex(r.rarity.rgb) + ">").append(rewardName(r));
    }

    private Component revealText() {
        String hex = Text.hex(result.rarity.rgb);
        String deco = switch (result.rarity) {
            case LEGENDARNY -> "✦ ✦ ";
            case EPICKI -> "✦ ";
            default -> "";
        };
        String deco2 = new StringBuilder(deco).reverse().toString();
        Component top = Text.mm("<" + hex + "><b>" + deco + result.rarity.label.toUpperCase() + deco2 + "</b>");
        Component bottom = rewardName(result);
        Component out = top.append(Component.newline()).append(bottom);
        if (test) out = out.append(Component.newline()).append(Text.mm("<dark_gray>(podgląd admina)"));
        return out;
    }
}
