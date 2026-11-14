package pl.onyxdev.skrzynki;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class Store {
    private final JavaPlugin plugin;
    private final File cratesFile;
    private final File pendingFile;
    private final File logsDir;
    public final Map<String, Crate> crates = new LinkedHashMap<>();

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Store(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getDataFolder().mkdirs();
        cratesFile = new File(plugin.getDataFolder(), "crates.yml");
        pendingFile = new File(plugin.getDataFolder(), "pending.yml");
        logsDir = new File(plugin.getDataFolder(), "logs");
        logsDir.mkdirs();
    }

    public void load() {
        crates.clear();
        if (!cratesFile.exists()) {
            createDefault();
            save();
            return;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(cratesFile);
        ConfigurationSection root = yml.getConfigurationSection("crates");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            Crate c = new Crate(id, s.getString("name", id));
            c.theme = Theme.parse(s.getString("theme", "SLONCE"));
            Material km = Material.matchMaterial(s.getString("key-material", "TRIPWIRE_HOOK"));
            c.keyMaterial = km != null && km.isItem() ? km : Material.TRIPWIRE_HOOK;
            c.opens = s.getInt("opens");
            c.kind = "KOLO".equalsIgnoreCase(s.getString("kind")) ? Crate.Kind.KOLO : Crate.Kind.SKRZYNIA;
            c.radius = Math.max(1.5, Math.min(5.0, s.getDouble("radius", 2.5)));
            c.freeDaily = s.getBoolean("free-daily", true);
            for (String l : s.getStringList("locations")) {
                try {
                    String[] parts = l.split("\\|", 2);
                    BlockPos pos = BlockPos.parse(parts[0]);
                    c.locations.add(pos);
                    if (parts.length > 1) c.mounts.put(pos, parts[1]);
                } catch (Exception e) {
                    plugin.getLogger().warning("Zła lokacja skrzynki " + id + ": " + l);
                }
            }
            for (Map<?, ?> m : s.getMapList("rewards")) {
                try {
                    ItemStack item = decode(String.valueOf(m.get("item")));
                    Reward r = new Reward(String.valueOf(m.get("id")), item,
                            ((Number) m.get("weight")).intValue(),
                            Rarity.parse(String.valueOf(m.get("rarity"))),
                            !Boolean.FALSE.equals(m.get("give-item")));
                    Object cmds = m.get("commands");
                    if (cmds instanceof List<?> list) for (Object o : list) r.commands.add(String.valueOf(o));
                    c.rewards.add(r);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Nie wczytano nagrody w skrzynce " + id, e);
                }
            }
            crates.put(id, c);
        }
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        yml.options().setHeader(List.of(
                "SunSkrzynki: skrzynki edytuje się w grze przez /skrzynki.",
                "Przedmioty są zapisane binarnie (base64), więc ręczna edycja ma sens tylko dla nazw, wag i rzadkości."));
        for (Crate c : crates.values()) {
            String p = "crates." + c.id + ".";
            yml.set(p + "name", c.name);
            yml.set(p + "theme", c.theme.name());
            yml.set(p + "key-material", c.keyMaterial.name());
            yml.set(p + "opens", c.opens);
            yml.set(p + "kind", c.kind.name());
            if (c.isWheel()) {
                yml.set(p + "radius", c.radius);
                yml.set(p + "free-daily", c.freeDaily);
            }
            yml.set(p + "locations", c.locations.stream()
                    .map(pos -> c.mounts.containsKey(pos) ? pos.serialize() + "|" + c.mounts.get(pos) : pos.serialize())
                    .toList());
            List<Map<String, Object>> rewards = new ArrayList<>();
            for (Reward r : c.rewards) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", r.id);
                m.put("weight", r.weight);
                m.put("rarity", r.rarity.name());
                m.put("give-item", r.giveItem);
                m.put("commands", new ArrayList<>(r.commands));
                m.put("item", encode(r.item));
                rewards.add(m);
            }
            yml.set(p + "rewards", rewards);
        }
        try {
            yml.save(cratesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Nie zapisano crates.yml", e);
        }
    }

    private void createDefault() {
        Crate c = new Crate("sloneczna", "<gradient:#ffe066:#ff8a00:#ff4d00><b>Słoneczna skrzynia</b></gradient>");
        c.theme = Theme.SLONCE;
        add(c, new ItemStack(Material.BREAD, 16), 40, Rarity.POSPOLITY);
        add(c, new ItemStack(Material.IRON_INGOT, 12), 35, Rarity.POSPOLITY);
        add(c, new ItemStack(Material.GOLDEN_APPLE, 3), 15, Rarity.RZADKI);
        add(c, new ItemStack(Material.DIAMOND, 4), 12, Rarity.RZADKI);
        add(c, new ItemStack(Material.EXPERIENCE_BOTTLE, 32), 10, Rarity.RZADKI);
        add(c, new ItemStack(Material.ENDER_PEARL, 8), 8, Rarity.RZADKI);
        add(c, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1), 3, Rarity.EPICKI);
        add(c, new ItemStack(Material.TOTEM_OF_UNDYING, 1), 3, Rarity.EPICKI);
        add(c, new ItemStack(Material.NETHERITE_INGOT, 1), 2, Rarity.EPICKI);
        add(c, new ItemStack(Material.ELYTRA, 1), 1, Rarity.LEGENDARNY);
        crates.put(c.id, c);
    }

    private static void add(Crate c, ItemStack item, int weight, Rarity rarity) {
        c.rewards.add(Reward.of(item, weight, rarity));
    }

    public static String encode(ItemStack it) {
        return Base64.getEncoder().encodeToString(it.serializeAsBytes());
    }

    public static ItemStack decode(String s) {
        return ItemStack.deserializeBytes(Base64.getDecoder().decode(s));
    }

    public String freeId(String name) {
        String base = java.text.Normalizer.normalize(Text.plain(Text.mm(name)).toLowerCase().replace('ł', 'l'),
                        java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (base.isEmpty()) base = "skrzynka";
        if (base.length() > 24) base = base.substring(0, 24);
        String id = base;
        for (int i = 2; crates.containsKey(id); i++) id = base + "_" + i;
        return id;
    }

    public Crate crateAt(BlockPos pos) {
        for (Crate c : crates.values()) if (c.locations.contains(pos)) return c;
        return null;
    }

    public Collection<Crate> all() {
        return crates.values();
    }

    public record Pending(ItemStack item, List<String> commands, String label) {
    }

    public void addPending(UUID player, Pending p) {
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(pendingFile);
        List<Map<?, ?>> list = new ArrayList<>(yml.getMapList(player.toString()));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("item", p.item() == null ? "" : encode(p.item()));
        m.put("commands", p.commands());
        m.put("label", p.label());
        list.add(m);
        yml.set(player.toString(), list);
        try {
            yml.save(pendingFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Nie zapisano pending.yml", e);
        }
    }

    public List<Pending> takePending(UUID player) {
        if (!pendingFile.exists()) return List.of();
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(pendingFile);
        List<Map<?, ?>> raw = yml.getMapList(player.toString());
        if (raw.isEmpty()) return List.of();
        List<Pending> out = new ArrayList<>();
        for (Map<?, ?> m : raw) {
            String item = String.valueOf(m.get("item"));
            List<String> cmds = new ArrayList<>();
            if (m.get("commands") instanceof List<?> l) for (Object o : l) cmds.add(String.valueOf(o));
            out.add(new Pending(item.isEmpty() || item.equals("null") ? null : decode(item), cmds,
                    String.valueOf(m.get("label"))));
        }
        yml.set(player.toString(), null);
        try {
            yml.save(pendingFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Nie zapisano pending.yml", e);
        }
        return out;
    }

    private static final java.time.ZoneId PL = java.time.ZoneId.of("Europe/Warsaw");
    private YamlConfiguration spins;

    private File spinsFile() {
        return new File(plugin.getDataFolder(), "spins.yml");
    }

    private YamlConfiguration spins() {
        if (spins == null) spins = YamlConfiguration.loadConfiguration(spinsFile());
        return spins;
    }

    public static String today() {
        return java.time.LocalDate.now(PL).toString();
    }

    public boolean usedFreeToday(Crate wheel, UUID player) {
        return today().equals(spins().getString(wheel.id + "." + player));
    }

    public void markFree(Crate wheel, UUID player) {
        spins().set(wheel.id + "." + player, today());
        try {
            spins().save(spinsFile());
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Nie zapisano spins.yml", e);
        }
    }

    public static String untilMidnight() {
        var now = java.time.ZonedDateTime.now(PL);
        long min = java.time.Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay(PL)).toMinutes();
        return (min / 60) + " h " + (min % 60) + " min";
    }

    public record LogEntry(String time, String player, Rarity rarity, String rewardId, String label) {
    }

    public void log(Crate crate, String playerName, Reward r, String label) {
        String line = String.join("|", LocalDateTime.now().format(TIME), playerName, r.rarity.name(), r.id,
                label.replace('|', '/')) + "\n";
        File f = new File(logsDir, crate.id + ".log");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Files.writeString(f.toPath(), line, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Nie zapisano logu", e);
            }
        });
    }

    public List<LogEntry> lastLogs(Crate crate, int max) {
        File f = new File(logsDir, crate.id + ".log");
        if (!f.exists()) return List.of();
        try {
            List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
            List<LogEntry> out = new ArrayList<>();
            for (int i = lines.size() - 1; i >= 0 && out.size() < max; i--) {
                String[] p = lines.get(i).split("\\|", 5);
                if (p.length < 5) continue;
                out.add(new LogEntry(p[0], p[1], Rarity.parse(p[2]), p[3], p[4]));
            }
            return out;
        } catch (IOException e) {
            return List.of();
        }
    }

    public int countLogs(Crate crate, Rarity rarity) {
        File f = new File(logsDir, crate.id + ".log");
        if (!f.exists()) return 0;
        try (var lines = Files.lines(f.toPath(), StandardCharsets.UTF_8)) {
            String tag = "|" + rarity.name() + "|";
            return (int) lines.filter(l -> l.contains(tag)).count();
        } catch (IOException e) {
            return 0;
        }
    }
}
