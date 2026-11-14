package pl.onyxdev.skrzynki;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class CrateService {
    private final SunSkrzynki plugin;
    private final Map<BlockPos, TextDisplay> holos = new HashMap<>();
    private final Map<BlockPos, Interaction> hitboxes = new HashMap<>();
    private final Map<UUID, BlockPos> hitboxPos = new HashMap<>();
    private final Map<BlockPos, Anim> running = new HashMap<>();
    private final Map<BlockPos, WheelVisual> wheels = new HashMap<>();
    private int tick;

    public CrateService(SunSkrzynki plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20, 10);
    }

    private void tick() {
        tick++;
        for (Crate c : plugin.store.all()) {
            for (BlockPos pos : c.locations) {
                if (!pos.isLoaded()) continue;
                if (c.isWheel()) {
                    ensureWheel(c, pos);
                    WheelVisual v = wheels.get(pos);
                    if (v != null && !running.containsKey(pos) && tick % 2 == 0 && playerNear(pos, 28)) v.bulbs(tick / 2, 2);
                    continue;
                }
                ensure(c, pos);
                if (tick % 2 == 0 && !running.containsKey(pos) && playerNear(pos, 20)) idle(c, pos);
            }
        }
    }

    private boolean playerNear(BlockPos pos, double r) {
        Location at = pos.top();
        for (Player p : at.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(at) < r * r) return true;
        }
        return false;
    }

    private void idle(Crate c, BlockPos pos) {
        Location top = pos.top();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < 3; i++) {
            double a = rnd.nextDouble(Math.PI * 2);
            Fx.dustFade(top.clone().add(Math.cos(a) * 0.62, -0.5 + rnd.nextDouble(0.8), Math.sin(a) * 0.62),
                    c.theme.lightColor(), c.theme.darkColor(), 0.8f, 1, 0);
        }
        top.getWorld().spawnParticle(Particle.END_ROD, top.clone().add(rnd.nextDouble(-0.3, 0.3), 0.05,
                rnd.nextDouble(-0.3, 0.3)), 0, 0, 0.6, 0, 0.03);
    }

    private void ensure(Crate c, BlockPos pos) {
        TextDisplay holo = holos.get(pos);
        if (holo == null || !holo.isValid()) {
            Location at = pos.top().add(0, 0.75, 0);
            holo = at.getWorld().spawn(at, TextDisplay.class, d -> {
                tag(d);
                d.setBillboard(Display.Billboard.CENTER);
                d.setBackgroundColor(Color.fromARGB(90, 8, 6, 16));
                d.setShadowed(true);
                d.setAlignment(TextDisplay.TextAlignment.CENTER);
                d.setViewRange(0.6f);
                d.setBrightness(new Display.Brightness(15, 15));
            });
            holos.put(pos, holo);
            refreshHolo(c, pos);
        }
        Interaction hit = hitboxes.get(pos);
        if (hit == null || !hit.isValid()) {
            Location at = new Location(pos.bukkitWorld(), pos.x() + 0.5, pos.y() - 0.02, pos.z() + 0.5);
            hit = at.getWorld().spawn(at, Interaction.class, e -> {
                tag(e);
                e.setInteractionWidth(1.06f);
                e.setInteractionHeight(1.06f);
                e.setResponsive(true);
            });
            if (hitboxes.containsKey(pos)) hitboxPos.values().remove(pos);
            hitboxes.put(pos, hit);
            hitboxPos.put(hit.getUniqueId(), pos);
        }
    }

    private void ensureWheel(Crate c, BlockPos pos) {
        WheelVisual v = wheels.get(pos);
        boolean spinning = running.containsKey(pos);
        if (v != null && v.isValid() && (spinning || v.signature.equals(WheelVisual.signature(c)))) {
            ensureHolo(c, pos, v.holoLocation());
            return;
        }
        if (spinning) return;
        if (v != null) dropWheel(pos, v);
        v = new WheelVisual(plugin, c, pos).spawn();
        wheels.put(pos, v);
        for (Interaction i : v.hitboxes()) hitboxPos.put(i.getUniqueId(), pos);
        TextDisplay old = holos.remove(pos);
        if (old != null) old.remove();
        ensureHolo(c, pos, v.holoLocation());
    }

    private void dropWheel(BlockPos pos, WheelVisual v) {
        for (Interaction i : v.hitboxes()) hitboxPos.remove(i.getUniqueId());
        v.remove();
        wheels.remove(pos);
    }

    public WheelVisual wheel(BlockPos pos) {
        return wheels.get(pos);
    }

    private void ensureHolo(Crate c, BlockPos pos, Location at) {
        TextDisplay holo = holos.get(pos);
        if (holo != null && holo.isValid()) return;
        holo = at.getWorld().spawn(at, TextDisplay.class, d -> {
            tag(d);
            d.setBillboard(Display.Billboard.CENTER);
            d.setBackgroundColor(Color.fromARGB(90, 8, 6, 16));
            d.setShadowed(true);
            d.setAlignment(TextDisplay.TextAlignment.CENTER);
            d.setViewRange(0.8f);
            d.setBrightness(new Display.Brightness(15, 15));
        });
        holos.put(pos, holo);
        refreshHolo(c, pos);
    }

    public void refreshHolo(Crate c, BlockPos pos) {
        TextDisplay holo = holos.get(pos);
        if (holo == null || !holo.isValid()) return;
        Anim anim = running.get(pos);
        Component second;
        if (anim != null) {
            second = Text.mm("<gray>" + (c.isWheel() ? "Kręci" : "Otwiera") + ": <white>" + Text.esc(anim.playerName()));
        } else if (c.isWheel()) {
            second = Text.mm("<gray>PPM <white>żetonem</white> · LPM <white>pola i szanse</white>"
                    + (c.freeDaily ? "<newline><#ffe066>1 darmowy obrót dziennie!" : ""));
        } else {
            second = Text.mm("<gray>PPM <white>kluczem</white> · LPM <white>nagrody</white>");
        }
        holo.text(c.displayName().append(Component.newline()).append(second));
    }

    public void refreshAll() {
        for (Crate c : plugin.store.all()) for (BlockPos pos : c.locations) refreshHolo(c, pos);
    }

    private void tag(Entity e) {
        e.setPersistent(false);
        e.getPersistentDataContainer().set(plugin.keys.fx, PersistentDataType.BYTE, (byte) 1);
    }

    public BlockPos hitboxAt(Entity e) {
        return hitboxPos.get(e.getUniqueId());
    }

    public boolean isOurs(Entity e) {
        return e.getPersistentDataContainer().has(plugin.keys.fx, PersistentDataType.BYTE);
    }

    public void removeVisuals(BlockPos pos) {
        Anim a = running.get(pos);
        if (a != null) a.finishNow();
        WheelVisual v = wheels.get(pos);
        if (v != null) dropWheel(pos, v);
        TextDisplay h = holos.remove(pos);
        if (h != null) h.remove();
        Interaction i = hitboxes.remove(pos);
        if (i != null) {
            hitboxPos.remove(i.getUniqueId());
            i.remove();
        }
    }

    public boolean isBusy(BlockPos pos) {
        return running.containsKey(pos);
    }

    public void open(Player player, Crate crate, BlockPos pos, Reward result, boolean test) {
        BukkitRunnable anim;
        if (crate.isWheel()) {
            WheelVisual v = wheels.get(pos);
            if (v == null || !v.isValid()) return;
            anim = new WheelSpin(plugin, crate, pos, v, player, result, test);
        } else {
            anim = new OpenAnimation(plugin, crate, pos, player, result, test);
        }
        running.put(pos, (Anim) anim);
        refreshHolo(crate, pos);
        anim.runTaskTimer(plugin, 0, 1);
    }

    public boolean ready(Crate crate, BlockPos pos) {
        if (!crate.isWheel()) return true;
        WheelVisual v = wheels.get(pos);
        return v != null && v.isValid() && crate.rewards.size() >= 2;
    }

    void finished(Anim anim) {
        if (running.get(anim.pos()) == anim) running.remove(anim.pos());
        Crate c = plugin.store.crateAt(anim.pos());
        if (c != null) refreshHolo(c, anim.pos());
    }

    public void deliver(UUID playerId, Crate crate, Reward r) {
        Player p = playerId == null ? null : Bukkit.getPlayer(playerId);
        ItemStack item = r.giveItem ? r.item.clone() : null;
        List<String> cmds = new ArrayList<>(r.commands);
        String label = Text.plain(OpenAnimation.rewardName(r));
        if (p == null) {
            if (playerId != null) plugin.store.addPending(playerId, new Store.Pending(item, cmds, label));
            return;
        }
        give(p, item, cmds);
        p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 0.9f);
    }

    public void give(Player p, ItemStack item, List<String> cmds) {
        if (item != null) {
            for (ItemStack left : p.getInventory().addItem(item).values()) {
                World w = p.getWorld();
                w.dropItem(p.getLocation(), left, d -> d.setVelocity(new Vector()));
            }
        }
        for (String cmd : cmds) {
            String c = cmd.startsWith("/") ? cmd.substring(1) : cmd;
            c = c.replace("{player}", p.getName()).replace("{uuid}", p.getUniqueId().toString());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c);
        }
    }

    public void givePending(Player p) {
        List<Store.Pending> list = plugin.store.takePending(p.getUniqueId());
        if (list.isEmpty()) return;
        for (Store.Pending pe : list) {
            give(p, pe.item(), pe.commands());
            Msg.chat(p, Msg.PENDING, "<gray>Odbierasz nagrodę ze skrzynki z czasu, gdy cię nie było: <white>" + Text.esc(pe.label()));
        }
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
    }

    public void shutdown() {
        for (Anim a : new ArrayList<>(running.values())) a.finishNow();
        running.clear();
        for (WheelVisual v : wheels.values()) v.remove();
        wheels.clear();
        holos.values().forEach(Entity::remove);
        hitboxes.values().forEach(Entity::remove);
        holos.clear();
        hitboxes.clear();
        hitboxPos.clear();
    }

    public void clearCrate(Crate c) {
        for (BlockPos pos : new ArrayList<>(c.locations)) removeVisuals(pos);
    }

    public BlockPos nearest(Crate c, Location from) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos pos : c.locations) {
            if (!pos.world().equals(from.getWorld().getName())) continue;
            double d = pos.top().distanceSquared(from);
            if (d < bestD) {
                bestD = d;
                best = pos;
            }
        }
        return best;
    }
}
