package pl.onyxdev.skrzynki;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class SunSkrzynki extends JavaPlugin implements TabExecutor {
    public Keys keys;
    public Store store;
    public CrateService service;
    public Prompts prompts;

    @Override
    public void onEnable() {
        Msg.register(this);
        keys = new Keys(this);
        store = new Store(this);
        store.load();
        service = new CrateService(this);
        prompts = new Prompts(this);
        Bukkit.getPluginManager().registerEvents(new Listeners(this), this);
        Bukkit.getPluginManager().registerEvents(prompts, this);
        PluginCommand cmd = getCommand("skrzynki");
        if (cmd != null) {
            cmd.setExecutor(this);
            cmd.setTabCompleter(this);
        }
        service.start();
        getLogger().info("Wczytano skrzynek: " + store.crates.size());
    }

    @Override
    public void onDisable() {
        if (service != null) service.shutdown();
        if (store != null) store.save();
    }

    public void testOpen(Player p, Crate c) {
        BlockPos pos = service.nearest(c, p.getLocation());
        if (pos == null || !pos.isLoaded()) {
            Text.send(p, "<gray>Ta skrzynka nie stoi nigdzie w tym świecie. Postaw ją różdżką.");
            return;
        }
        if (service.isBusy(pos)) {
            Text.send(p, "<gray>Ta skrzynia właśnie się otwiera.");
            return;
        }
        if (!service.ready(c, pos)) {
            Text.send(p, "<gray>Koło jeszcze się buduje albo ma mniej niż 2 pola.");
            return;
        }
        Reward r = c.roll();
        if (r == null) {
            Text.send(p, "<gray>Skrzynka nie ma nagród.");
            return;
        }
        service.open(p, c, pos, r, true);
    }

    @Override
    public boolean onCommand(CommandSender s, Command command, String label, String[] a) {
        boolean admin = s.hasPermission("skrzynki.admin");
        if (!admin) {
            Text.send(s, "<gray>Skrzynie stoją na spawnie. Otwierasz je <white>kluczem</white> (PPM), LPM pokazuje nagrody.");
            return true;
        }
        if (a.length == 0) {
            if (s instanceof Player p) new Menus.Main(this, p).open();
            else help(s);
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "give", "daj" -> give(s, a);
            case "lista", "list" -> {
                Text.send(s, "<gray>Skrzynki:");
                for (Crate c : store.all()) {
                    s.sendMessage(Text.mm(" <dark_gray>•</dark_gray> <white>" + c.id + "</white> <dark_gray>—</dark_gray> ")
                            .append(c.displayName()).append(Text.mm(" <gray>(" + c.rewards.size() + " nagród, "
                                    + c.locations.size() + " postawionych, " + c.opens + " otwarć)")));
                }
            }
            case "reload" -> {
                service.shutdown();
                store.load();
                Text.send(s, "<gray>Przeładowano. Skrzynek: <white>" + store.crates.size());
            }
            case "test" -> {
                Crate c = a.length > 1 ? store.crates.get(a[1]) : null;
                if (c == null) {
                    Text.send(s, "<gray>Użycie: /skrzynki test <id>");
                    return true;
                }
                if (s instanceof Player p) {
                    testOpen(p, c);
                } else {
                    BlockPos pos = c.locations.stream().filter(BlockPos::isLoaded).findFirst().orElse(null);
                    Reward r = c.roll();
                    if (pos == null || r == null || service.isBusy(pos) || !service.ready(c, pos)) Text.send(s, "<gray>Brak załadowanej wolnej lokacji albo nagród.");
                    else service.open(null, c, pos, r, true);
                }
            }
            case "przyklady" -> {
                int added = 0;
                for (Crate c : Presets.all()) {
                    if (store.crates.containsKey(c.id)) continue;
                    store.crates.put(c.id, c);
                    added++;
                }

                Crate wheel = Presets.wheel(this);
                if (!store.crates.containsKey(wheel.id)) {
                    store.crates.put(wheel.id, wheel);
                    added++;
                }
                store.save();
                Text.send(s, "<gray>Dodano gotowych skrzynek: <white>" + added + "</white>. Postaw je różdżką z /skrzynki.");
            }
            case "postaw" -> {
                Crate c = a.length > 5 ? store.crates.get(a[1]) : null;
                if (c == null || Bukkit.getWorld(a[2]) == null) {
                    Text.send(s, "<gray>Użycie: /skrzynki postaw <id> <świat> <x> <y> <z>");
                    return true;
                }
                try {
                    BlockPos pos = new BlockPos(a[2], Integer.parseInt(a[3]), Integer.parseInt(a[4]), Integer.parseInt(a[5]));
                    if (store.crateAt(pos) != null) {
                        Text.send(s, "<gray>Tu już jest skrzynka.");
                        return true;
                    }
                    c.locations.add(pos);

                    if (c.isWheel()) c.mounts.put(pos, a.length > 6 ? a[6].toUpperCase() : "FLOOR:SOUTH");
                    store.save();
                    Text.send(s, "<gray>Podpięto <white>" + c.id + "</white> na <white>" + pos.pretty());
                } catch (NumberFormatException e) {
                    Text.send(s, "<gray>Koordynaty muszą być liczbami.");
                }
            }
            default -> help(s);
        }
        return true;
    }

    private void help(CommandSender s) {
        Text.send(s, "<white>/skrzynki</white> <gray>panel admina");
        Text.send(s, "<white>/skrzynki give <gracz|@a> <id> [ilość]</white> <gray>daj klucze");
        Text.send(s, "<white>/skrzynki lista</white> <gray>· <white>/skrzynki test <id></white> <gray>· <white>/skrzynki reload</white>");
    }

    private void give(CommandSender s, String[] a) {
        if (a.length < 3) {
            Text.send(s, "<gray>Użycie: /skrzynki give <gracz|@a> <id> [ilość]");
            return;
        }
        Crate c = store.crates.get(a[2]);
        if (c == null) {
            Text.send(s, "<gray>Nie ma skrzynki <white>" + Text.esc(a[2]) + "</white>. /skrzynki lista");
            return;
        }
        int amount = 1;
        if (a.length > 3) {
            try {
                amount = Math.max(1, Math.min(2304, Integer.parseInt(a[3])));
            } catch (NumberFormatException e) {
                Text.send(s, "<gray>Ilość musi być liczbą.");
                return;
            }
        }
        List<Player> targets = new ArrayList<>();
        if (a[1].equals("@a") || a[1].equals("*")) targets.addAll(Bukkit.getOnlinePlayers());
        else {
            Player p = Bukkit.getPlayerExact(a[1]);
            if (p == null) {
                Text.send(s, "<gray>Gracz <white>" + Text.esc(a[1]) + "</white> nie jest online.");
                return;
            }
            targets.add(p);
        }
        for (Player p : targets) {
            int left = amount;
            while (left > 0) {
                int n = Math.min(64, left);
                left -= n;
                ItemStack key = keys.keyItem(c, n);
                for (ItemStack rest : p.getInventory().addItem(key).values()) p.getWorld().dropItem(p.getLocation(), rest);
            }
            Msg.chat(p, Msg.KEY_RECEIVED, Text.mm(Text.PREFIX + "<gray>Otrzymujesz <white>" + amount + "×</white> "
                    + (c.isWheel() ? "żeton" : "klucz") + ": ").append(c.displayName()));
            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.3f);
        }
        Text.send(s, "<gray>Dano <white>" + amount + "×</white> klucz <white>" + c.id + "</white> → <white>"
                + (targets.size() == 1 ? Text.esc(targets.getFirst().getName()) : targets.size() + " graczy"));
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command command, String label, String[] a) {
        if (!s.hasPermission("skrzynki.admin")) return List.of();
        if (a.length == 1) return filter(Stream.of("give", "lista", "test", "reload"), a[0]);
        if (a.length == 2 && a[0].equalsIgnoreCase("give")) {
            return filter(Stream.concat(Stream.of("@a"), Bukkit.getOnlinePlayers().stream().map(Player::getName)), a[1]);
        }
        if (a.length == 2 && a[0].equalsIgnoreCase("test")) return filter(store.crates.keySet().stream(), a[1]);
        if (a.length == 3 && a[0].equalsIgnoreCase("give")) return filter(store.crates.keySet().stream(), a[2]);
        if (a.length == 4 && a[0].equalsIgnoreCase("give")) return List.of("1", "5", "10", "64");
        return List.of();
    }

    private static List<String> filter(Stream<String> in, String prefix) {
        String p = prefix.toLowerCase();
        return in.filter(x -> x.toLowerCase().startsWith(p)).toList();
    }
}
