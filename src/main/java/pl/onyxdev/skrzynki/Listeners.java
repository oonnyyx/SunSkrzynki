package pl.onyxdev.skrzynki;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Lidded;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Listeners implements Listener {
    private final SunSkrzynki plugin;

    private final Map<UUID, Long> lastUse = new HashMap<>();

    public Listeners(SunSkrzynki plugin) {
        this.plugin = plugin;
    }

    private boolean debounce(Player p) {
        long now = System.currentTimeMillis();
        Long prev = lastUse.put(p.getUniqueId(), now);
        return prev != null && now - prev < 200;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityUse(PlayerInteractEntityEvent e) {
        BlockPos pos = plugin.service.hitboxAt(e.getRightClicked());
        if (pos == null) return;
        e.setCancelled(true);
        if (e.getHand() != EquipmentSlot.HAND || debounce(e.getPlayer())) return;
        Crate c = plugin.store.crateAt(pos);
        if (c != null) use(e.getPlayer(), c, pos);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityHit(PrePlayerAttackEntityEvent e) {
        BlockPos pos = plugin.service.hitboxAt(e.getAttacked());
        if (pos == null) return;
        e.setCancelled(true);
        Crate c = plugin.store.crateAt(pos);
        if (c != null) leftClick(e.getPlayer(), c, pos);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || e.getClickedBlock() == null) return;
        Player p = e.getPlayer();
        Block b = e.getClickedBlock();
        BlockPos pos = BlockPos.of(b);
        Crate c = plugin.store.crateAt(pos);

        String wandCrate = plugin.keys.wandOf(e.getItem());
        if (wandCrate != null && e.getAction() == Action.RIGHT_CLICK_BLOCK && p.hasPermission("skrzynki.admin")) {
            e.setCancelled(true);
            if (!debounce(p)) placeWithWand(p, b, e.getBlockFace(), wandCrate);
            return;
        }
        if (c == null) return;
        e.setCancelled(true);
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (!debounce(p)) use(p, c, pos);
        } else if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
            leftClick(p, c, pos);
        }
    }

    private void leftClick(Player p, Crate c, BlockPos pos) {
        if (p.hasPermission("skrzynki.admin") && p.isSneaking() && plugin.keys.wandOf(p.getInventory().getItemInMainHand()) != null) {
            unregister(p, c, pos);
            return;
        }
        new Menus.Preview(plugin, p, c, false).open();
    }

    private void use(Player p, Crate c, BlockPos pos) {
        ItemStack main = p.getInventory().getItemInMainHand();
        ItemStack off = p.getInventory().getItemInOffHand();
        ItemStack key = c.id.equals(plugin.keys.keyOf(main)) ? main : c.id.equals(plugin.keys.keyOf(off)) ? off : null;

        if (c.isWheel()) {
            useWheel(p, c, pos, key);
            return;
        }
        if (key == null) {
            String other = plugin.keys.keyOf(main);
            Crate otherCrate = other == null ? null : plugin.store.crates.get(other);
            if (otherCrate != null) {
                Msg.chat(p, Msg.NO_KEY, Text.mm(Text.PREFIX + "<gray>To klucz do innej skrzyni: ").append(otherCrate.displayName()));
            } else {
                Msg.chat(p, Msg.NO_KEY, Text.mm(Text.PREFIX + "<gray>Potrzebujesz klucza: ").append(c.displayName())
                        .append(Text.mm("<gray>. <white>LPM</white> pokazuje nagrody.")));
            }
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
            Vector push = p.getLocation().toVector().subtract(pos.top().toVector()).setY(0);
            if (push.lengthSquared() > 0.001) p.setVelocity(push.normalize().multiply(0.45).setY(0.25));
            return;
        }
        if (plugin.service.isBusy(pos)) {
            Msg.chat(p, Msg.BUSY, "<gray>Ktoś właśnie otwiera tę skrzynię. Chwila!");
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.8f);
            return;
        }
        Reward r = c.roll();
        if (r == null) {
            Text.send(p, "<gray>Ta skrzynia nie ma jeszcze nagród.");
            return;
        }
        key.setAmount(key.getAmount() - 1);
        plugin.service.open(p, c, pos, r, false);
    }

    private void useWheel(Player p, Crate c, BlockPos pos, ItemStack token) {
        if (plugin.service.isBusy(pos)) {
            Msg.chat(p, Msg.BUSY, "<gray>Koło właśnie się kręci. Chwila!");
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.8f);
            return;
        }
        if (!plugin.service.ready(c, pos)) {
            Text.send(p, "<gray>To koło nie jest jeszcze gotowe (potrzebne min. 2 pola).");
            return;
        }
        boolean free = false;
        if (token == null) {
            if (c.freeDaily && !plugin.store.usedFreeToday(c, p.getUniqueId())) {
                free = true;
            } else {
                Msg.chat(p, Msg.NO_KEY, Text.mm(Text.PREFIX + "<gray>Potrzebujesz żetonu: ").append(c.displayName())
                        .append(Text.mm(c.freeDaily
                                ? "<gray>. Darmowy obrót wróci za <white>" + Store.untilMidnight() + "</white>."
                                : "<gray>.")));
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
                return;
            }
        }
        Reward r = c.roll();
        if (r == null) {
            Text.send(p, "<gray>To koło nie ma jeszcze pól.");
            return;
        }
        if (free) {
            plugin.store.markFree(c, p.getUniqueId());
            Msg.chat(p, Msg.FREE_SPIN, "<#ffe066>Darmowy obrót na dziś! <gray>Następny jutro.");
        } else {
            token.setAmount(token.getAmount() - 1);
        }
        plugin.service.open(p, c, pos, r, false);
    }

    private void placeWithWand(Player p, Block clicked, org.bukkit.block.BlockFace face, String crateId) {
        Crate c = plugin.store.crates.get(crateId);
        if (c == null) {
            Text.send(p, "<gray>Ta skrzynka już nie istnieje.");
            return;
        }
        if (c.isWheel()) {
            placeWheel(p, c, clicked, face);
            return;
        }
        Block target = clicked;
        BlockState st = clicked.getState();
        boolean container = st instanceof Lidded || clicked.getType() == Material.BARREL;
        if (!container) {
            target = clicked.getRelative(face);
            if (!target.getType().isAir() && !target.isReplaceable()) {
                Text.send(p, "<gray>Tu nie ma miejsca na skrzynię.");
                return;
            }
            target.setType(Material.CHEST, false);
            if (target.getBlockData() instanceof Directional d) {
                d.setFacing(p.getFacing().getOppositeFace());
                target.setBlockData(d, false);
            }
        }
        BlockPos pos = BlockPos.of(target);
        Crate existing = plugin.store.crateAt(pos);
        if (existing != null) {
            p.sendMessage(Text.mm(Text.PREFIX + "<gray>Tu już stoi: ").append(existing.displayName()));
            return;
        }
        c.locations.add(pos);
        plugin.store.save();
        p.sendMessage(Text.mm(Text.PREFIX + "<gray>Postawiono ").append(c.displayName())
                .append(Text.mm("<gray> na <white>" + pos.pretty())));
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.4f);
        Fx.plain(pos.top(), Particle.END_ROD, 30, 0.3, 0.1);
    }

    private void placeWheel(Player p, Crate c, Block clicked, org.bukkit.block.BlockFace face) {
        if (face == org.bukkit.block.BlockFace.DOWN) {
            Text.send(p, "<gray>Kliknij ścianę (koło zawiśnie) albo podłogę (koło stanie).");
            return;
        }
        BlockPos pos = BlockPos.of(clicked);
        Crate existing = plugin.store.crateAt(pos);
        if (existing != null) {
            p.sendMessage(Text.mm(Text.PREFIX + "<gray>Tu już stoi: ").append(existing.displayName()));
            return;
        }
        String mount = face == org.bukkit.block.BlockFace.UP
                ? "FLOOR:" + p.getFacing().getOppositeFace().name()
                : "WALL:" + face.name();
        c.locations.add(pos);
        c.mounts.put(pos, mount);
        plugin.store.save();
        p.sendMessage(Text.mm(Text.PREFIX + "<gray>Postawiono ").append(c.displayName())
                .append(Text.mm("<gray> (" + (mount.startsWith("WALL") ? "na ścianie" : "na podłodze")
                        + ", promień " + c.radius + "). Pojawi się za chwilę.")));
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.4f);
    }

    private void unregister(Player p, Crate c, BlockPos pos) {
        plugin.service.removeVisuals(pos);
        c.locations.remove(pos);
        c.mounts.remove(pos);
        plugin.store.save();
        p.sendMessage(Text.mm(Text.PREFIX + "<gray>Odpięto skrzynię ").append(c.displayName())
                .append(Text.mm("<gray> z <white>" + pos.pretty() + "</white> (blok został).")));
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.2f);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent e) {
        BlockPos pos = BlockPos.of(e.getBlock());
        Crate c = plugin.store.crateAt(pos);
        if (c == null) return;
        Player p = e.getPlayer();
        if (p.hasPermission("skrzynki.admin") && p.isSneaking()) {
            unregister(p, c, pos);
            return;
        }
        e.setCancelled(true);
        if (p.hasPermission("skrzynki.admin")) Text.send(p, "<gray>To skrzynka. Usuń ją przez <white>Shift + zniszcz</white>.");
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(b -> plugin.store.crateAt(BlockPos.of(b)) != null);
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent e) {
        e.blockList().removeIf(b -> plugin.store.crateAt(BlockPos.of(b)) != null);
    }

    @EventHandler
    public void onFireworkDamage(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Firework fw
                && fw.getPersistentDataContainer().has(plugin.keys.fx, PersistentDataType.BYTE)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder(false) instanceof Menu m && e.getWhoClicked() instanceof Player) m.handle(e);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getInventory().getHolder(false) instanceof Menu)) return;
        int top = e.getView().getTopInventory().getSize();
        for (int slot : e.getRawSlots()) {
            if (slot < top) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) plugin.service.givePending(p);
        }, 40);
    }
}
