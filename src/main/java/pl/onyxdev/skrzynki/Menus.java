package pl.onyxdev.skrzynki;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class Menus {
    private Menus() {
    }

    public static class Main extends Menu {
        public Main(SunSkrzynki plugin, Player viewer) {
            super(plugin, viewer, 6, Text.mm("<dark_gray>Skrzynki · panel admina"));
        }

        @Override
        protected void render() {
            int slot = 0;
            for (Crate c : plugin.store.all()) {
                if (slot >= 45) break;
                set(slot++, Text.item(c.isWheel() ? Material.TARGET : Material.CHEST)
                        .name(c.displayName())
                        .lore("<dark_gray>id: " + c.id + (c.isWheel() ? " · koło fortuny" : ""),
                                "",
                                "<gray>Nagrody: <white>" + c.rewards.size(),
                                "<gray>Postawione: <white>" + c.locations.size(),
                                "<gray>Otwarcia: <white>" + c.opens,
                                "<gray>Motyw: <white>" + c.theme.label,
                                "",
                                "<yellow>LPM</yellow> <gray>edytuj",
                                "<yellow>PPM</yellow> <gray>weź 1 " + (c.isWheel() ? "żeton" : "klucz")
                                        + " · <yellow>Shift+PPM</yellow> <gray>16 sztuk")
                        .glint(!c.locations.isEmpty())
                        .build(), e -> {
                    if (e.isRightClick()) {
                        viewer.getInventory().addItem(plugin.keys.keyItem(c, e.isShiftClick() ? 16 : 1));
                        ok();
                    } else {
                        new CrateEdit(plugin, viewer, c).open();
                    }
                });
            }
            fill(45, 53);
            set(45, Text.item(Material.KNOWLEDGE_BOOK).name("<gold><b>Jak to działa</b>")
                    .lore("<gray>1. Utwórz skrzynkę i dodaj nagrody",
                            "<gray>   (przeciągnij przedmiot z ekwipunku).",
                            "<gray>2. Weź różdżkę i kliknij skrzynię/beczkę",
                            "<gray>   w świecie albo blok, na którym ma stanąć.",
                            "<gray>3. Rozdaj klucze: <white>/skrzynki give <gracz> <id> [ilość]",
                            "",
                            "<gray>Nazwy obsługują MiniMessage, np.",
                            "<white><gradient:#ff0:#f80>Złota</gradient>").build(), null);
            set(49, Text.item(Material.NETHER_STAR).name("<gold><b>+ Nowa skrzynka</b>")
                    .lore("<gray>Kliknij i wpisz nazwę na czacie.").glint(true).build(), e ->
                    plugin.prompts.ask(viewer, "<gray>Wpisz nazwę nowej skrzynki <dark_gray>(MiniMessage działa)</dark_gray>:",
                            name -> {
                                Crate c = new Crate(plugin.store.freeId(name), name);
                                Theme[] t = Theme.values();
                                c.theme = t[plugin.store.crates.size() % t.length];
                                plugin.store.crates.put(c.id, c);
                                plugin.store.save();
                                Text.send(viewer, "<gray>Utworzono skrzynkę <white>" + c.id + "</white>. Dodaj nagrody.");
                                new CrateEdit(plugin, viewer, c).open();
                            }, () -> new Main(plugin, viewer).open()));
            set(51, Text.item(Material.TARGET).name("<gold><b>+ Nowe koło fortuny</b>")
                    .lore("<gray>Koło z polami-nagrodami (2–16 pól),", "<gray>kręci się żetonem albo raz dziennie za darmo.")
                    .glint(true).build(), e ->
                    plugin.prompts.ask(viewer, "<gray>Wpisz nazwę koła <dark_gray>(MiniMessage działa)</dark_gray>:",
                            name -> {
                                Crate c = new Crate(plugin.store.freeId(name), name);
                                c.kind = Crate.Kind.KOLO;
                                c.keyMaterial = Material.SUNFLOWER;
                                plugin.store.crates.put(c.id, c);
                                plugin.store.save();
                                Text.send(viewer, "<gray>Utworzono koło <white>" + c.id + "</white>. Dodaj pola (nagrody).");
                                new CrateEdit(plugin, viewer, c).open();
                            }, () -> new Main(plugin, viewer).open()));
            set(53, Text.item(Material.BARRIER).name("<gray>Zamknij").build(), e -> viewer.closeInventory());
        }
    }

    public static class CrateEdit extends Menu {
        private static final Material[] KEY_MATERIALS = {Material.TRIPWIRE_HOOK, Material.NAME_TAG,
                Material.AMETHYST_SHARD, Material.NETHER_STAR, Material.GOLD_NUGGET, Material.ECHO_SHARD,
                Material.HEART_OF_THE_SEA, Material.PRISMARINE_SHARD, Material.FIRE_CHARGE, Material.SUNFLOWER,
                Material.TRIAL_KEY, Material.OMINOUS_TRIAL_KEY};
        private final Crate c;

        public CrateEdit(SunSkrzynki plugin, Player viewer, Crate c) {
            super(plugin, viewer, 5, Text.mm("<dark_gray>Skrzynka: ").append(c.displayName()));
            this.c = c;
        }

        @Override
        protected boolean allowBottom() {
            return true;
        }

        @Override
        protected void render() {
            set(4, Text.item(c.isWheel() ? Material.TARGET : Material.CHEST).name(c.displayName())
                    .lore("<dark_gray>id: " + c.id, "<gray>Otwarcia: <white>" + c.opens).glint(true).build(), null);

            set(19, Text.item(Material.CHEST_MINECART).name("<gold><b>Nagrody</b>")
                    .lore("<gray>" + (c.isWheel() ? "Pola na kole" : "W skrzynce") + ": <white>" + c.rewards.size() + "</white>/" + c.maxRewards(),
                            "<gray>Suma wag: <white>" + c.totalWeight(),
                            "", "<yellow>Kliknij</yellow> <gray>edytuj nagrody i szanse").build(),
                    e -> new Rewards(plugin, viewer, c).open());

            set(20, Text.item(Material.NAME_TAG).name("<gold><b>Nazwa</b>")
                    .lore("<gray>Teraz: ", "", "<yellow>Kliknij</yellow> <gray>i wpisz nową (MiniMessage)").build(), e ->
                    plugin.prompts.ask(viewer, "<gray>Nowa nazwa skrzynki:", name -> {
                        c.name = name;
                        plugin.store.save();
                        plugin.service.refreshAll();
                        new CrateEdit(plugin, viewer, c).open();
                    }, () -> new CrateEdit(plugin, viewer, c).open()));

            ItemStack nameItem = getInventory().getItem(20);
            if (nameItem != null) {
                var meta = nameItem.getItemMeta();
                List<Component> lore = new ArrayList<>(meta.lore());
                lore.set(0, Text.line("<gray>Teraz: ").append(c.displayName()));
                meta.lore(lore);
                nameItem.setItemMeta(meta);
            }

            set(21, Text.item(c.theme.icon).name("<gold><b>Motyw: </b><" + Text.hex(c.theme.light) + ">" + c.theme.label)
                    .lore("<gray>Kolor cząsteczek przy ładowaniu", "<gray>i kręceniu (rzadkość ma własne kolory).",
                            "", "<yellow>Kliknij</yellow> <gray>następny motyw").build(), e -> {
                c.theme = c.theme.next();
                plugin.store.save();
                redraw();
            });

            set(22, Text.item(c.keyMaterial).name("<gold><b>Wygląd klucza</b>")
                    .lore("<gray>Przedmiot: <white>" + c.keyMaterial.name().toLowerCase(),
                            "", "<yellow>Kliknij</yellow> <gray>następny wzór",
                            "<yellow>Kliknij przedmiotem na kursorze</yellow> <gray>ustaw ten przedmiot",
                            "<dark_gray>Stare klucze nadal działają (liczy się znacznik, nie wygląd).").build(), e -> {
                ItemStack cursor = e.getCursor();
                if (cursor != null && !cursor.getType().isAir()) {
                    c.keyMaterial = cursor.getType();
                } else {
                    int i = 0;
                    for (int k = 0; k < KEY_MATERIALS.length; k++) if (KEY_MATERIALS[k] == c.keyMaterial) i = k + 1;
                    c.keyMaterial = KEY_MATERIALS[i % KEY_MATERIALS.length];
                }
                plugin.store.save();
                redraw();
            });

            set(23, Text.item(plugin.keys.keyItem(c, 1)).name("<gold><b>Weź " + (c.isWheel() ? "żetony" : "klucze") + "</b>")
                    .lore("<yellow>LPM</yellow> <gray>1 · <yellow>PPM</yellow> <gray>16 · <yellow>Shift</yellow> <gray>64",
                            "", "<gray>Graczom: <white>/skrzynki give <gracz> " + c.id + " [ilość]").build(), e -> {
                int n = e.isShiftClick() ? 64 : e.isRightClick() ? 16 : 1;
                viewer.getInventory().addItem(plugin.keys.keyItem(c, n));
                ok();
            });

            set(24, Text.item(Material.BLAZE_ROD).name("<gold><b>Postaw " + (c.isWheel() ? "koło" : "skrzynię") + "</b>")
                    .lore(c.isWheel()
                            ? List.of("<gray>Dostajesz różdżkę. PPM w świecie:",
                            "<gray>• na ścianę → koło wisi, środek w klikniętym bloku,",
                            "<gray>• na podłogę → koło stoi, zwrócone do ciebie.",
                            "<gray>Usuwanie: <white>Shift + LPM</white> w koło z różdżką",
                            "<gray>albo w menu <white>Lokacje</white>.")
                            : List.of("<gray>Dostajesz różdżkę. PPM w świecie:",
                            "<gray>• na skrzynię/beczkę/ender chest → podpina ją,",
                            "<gray>• na inny blok → stawia na nim skrzynię.",
                            "<gray>Usuwanie: <white>Shift + zniszcz</white> skrzynię",
                            "<gray>albo w menu <white>Lokacje</white>.")).glint(true).build(), e -> {
                viewer.getInventory().addItem(plugin.keys.wandItem(c));
                viewer.closeInventory();
                Text.send(viewer, "<gray>Masz różdżkę. Kliknij PPM miejsce na " + (c.isWheel() ? "koło." : "skrzynię."));
                ok();
            });

            set(25, Text.item(Material.ENDER_EYE).name("<gold><b>Podgląd animacji</b>")
                    .lore("<gray>Odpala animację na najbliższym", "<gray>postawieniu (bez nagrody i logu).").build(), e -> {
                viewer.closeInventory();
                plugin.testOpen(viewer, c);
            });

            set(28, Text.item(Material.COMPASS).name("<gold><b>Lokacje</b>")
                    .lore("<gray>Postawione: <white>" + c.locations.size(), "", "<yellow>Kliknij</yellow> <gray>lista, teleport, usuwanie").build(),
                    e -> new Locations(plugin, viewer, c).open());

            set(29, Text.item(Material.WRITABLE_BOOK).name("<gold><b>Logi otwarć</b>")
                    .lore("<gray>Kto, kiedy i co wylosował.").build(), e -> new Logs(plugin, viewer, c).open());

            set(30, Text.item(Material.SPYGLASS).name("<gold><b>Podgląd jak gracz</b>")
                    .lore("<gray>To samo okno, które widzi gracz", "<gray>po LPM na skrzynię.").build(),
                    e -> new Preview(plugin, viewer, c, true).open());

            if (c.isWheel()) {
                set(31, Text.item(Material.SPECTRAL_ARROW).name("<gold><b>Rozmiar koła</b>")
                        .lore("<gray>Promień: <white>" + c.radius + "</white> bloku",
                                "", "<yellow>LPM</yellow> <gray>+0.5 · <yellow>PPM</yellow> <gray>−0.5 (1.5–5)").build(), e -> {
                    c.radius = Math.max(1.5, Math.min(5.0, c.radius + (e.isRightClick() ? -0.5 : 0.5)));
                    plugin.store.save();
                    redraw();
                });
                set(32, Text.item(c.freeDaily ? Material.CLOCK : Material.GRAY_DYE).name("<gold><b>Darmowy obrót dziennie</b>")
                        .lore("<gray>Teraz: <white>" + (c.freeDaily ? "włączony" : "wyłączony"),
                                "<gray>Każdy gracz raz dziennie kręci bez żetonu",
                                "<gray>(reset o północy czasu polskiego).",
                                "", "<yellow>Kliknij</yellow> <gray>przełącz").build(), e -> {
                    c.freeDaily = !c.freeDaily;
                    plugin.store.save();
                    plugin.service.refreshAll();
                    redraw();
                });
            }

            set(34, Text.item(Material.LAVA_BUCKET).name("<#ff5a5a><b>Usuń " + (c.isWheel() ? "koło" : "skrzynkę") + "</b>")
                    .lore("<gray>Usuwa definicję, nagrody i podpięte lokacje.", "<gray>Wydane klucze przestaną działać.").build(),
                    e -> new Confirm(plugin, viewer, "Usunąć " + c.id + "?", () -> {
                        plugin.service.clearCrate(c);
                        plugin.store.crates.remove(c.id);
                        plugin.store.save();
                        Text.send(viewer, "<gray>Usunięto skrzynkę <white>" + c.id);
                        new Main(plugin, viewer).open();
                    }, () -> new CrateEdit(plugin, viewer, c).open()).open());

            set(36, back(), e -> new Main(plugin, viewer).open());
            fill(0, 44);
        }
    }

    public static class Rewards extends Menu {
        private final Crate c;

        public Rewards(SunSkrzynki plugin, Player viewer, Crate c) {
            super(plugin, viewer, 6, Text.mm("<dark_gray>Nagrody: ").append(c.displayName()));
            this.c = c;
        }

        @Override
        protected boolean allowBottom() {
            return true;
        }

        @Override
        protected void render() {
            for (int i = 0; i < c.rewards.size() && i < c.maxRewards(); i++) {
                Reward r = c.rewards.get(i);
                set(i, icon(r), e -> edit(r, e));
            }
            fill(45, 53);
            set(45, back(), e -> new CrateEdit(plugin, viewer, c).open());
            set(46, Text.item(Material.BOOK).name("<gold><b>Sterowanie</b>")
                    .lore("<white>Dodawanie:</white> <gray>weź przedmiot na kursor i kliknij",
                            "<gray>pusty slot, albo Shift+klik w swoim ekwipunku.",
                            "<gray>Ilość w stacku = ilość w nagrodzie.",
                            "",
                            "<yellow>LPM / PPM</yellow> <gray>waga +1 / −1",
                            "<yellow>Shift+LPM / Shift+PPM</yellow> <gray>waga +10 / −10",
                            "<yellow>F</yellow> <gray>zmień rzadkość",
                            "<yellow>Q</yellow> <gray>usuń nagrodę",
                            "<yellow>1</yellow> <gray>wpisz wagę · <yellow>2</yellow> <gray>dodaj komendę",
                            "<yellow>3</yellow> <gray>wyczyść komendy · <yellow>4</yellow> <gray>daje przedmiot tak/nie",
                            "<gray>Klik z przedmiotem na kursorze w nagrodę = podmień przedmiot.",
                            "",
                            "<gray>Szansa = waga / suma wag. Waga 0 = wyłączona.").build(), null);
            if (c.isWheel()) {
                set(47, Text.item(Material.WITHER_ROSE).name("<gray><b>+ Pole „Pech”</b>")
                        .lore("<gray>Czarne pole, które nic nie daje.",
                                "<gray>Przy trafieniu gra smutny puzon.",
                                "<dark_gray>Kolejność pól = kolejność na kole.").build(), e -> {
                    if (c.rewards.size() >= c.maxRewards()) {
                        deny();
                        return;
                    }
                    ItemStack icon = Text.item(Material.WITHER_ROSE).name("<gray>Pech!").build();
                    Reward r = Reward.of(icon, 10, Rarity.POSPOLITY);
                    r.giveItem = false;
                    c.rewards.add(r);
                    plugin.store.save();
                    ok();
                    redraw();
                });
            }
            set(48, Text.item(Material.COMMAND_BLOCK).name("<gold><b>+ Nagroda-komenda</b>")
                    .lore("<gray>Np. pieniądze, ranga, uprawnienie.",
                            "<gray>Ikoną będzie przedmiot z twojej ręki",
                            "<gray>(albo papier). <white>{player}</white> = nick gracza.").build(), e -> addCommandReward());
            set(50, Text.item(Material.EXPERIENCE_BOTTLE).name("<gold><b>Auto-rzadkość</b>")
                    .lore("<gray>Ustawia rzadkości według szans:",
                            "<#ffb21a>Legendarny</#ffb21a> <gray>< 1.5% · <#b45cff>Epicki</#b45cff> <gray>< 6%",
                            "<#4da6ff>Rzadki</#4da6ff> <gray>< 20% · <#b8c4ce>Pospolity</#b8c4ce> <gray>reszta").build(), e -> {
                for (Reward r : c.rewards) r.rarity = Rarity.forChance(c.chance(r));
                plugin.store.save();
                ok();
                redraw();
            });
            set(52, Text.item(Material.HOPPER).name("<gold><b>Sortuj</b>")
                    .lore("<gray>Od najrzadszych.").build(), e -> {
                List<Reward> sorted = c.sortedByRarity();
                c.rewards.clear();
                c.rewards.addAll(sorted);
                plugin.store.save();
                redraw();
            });
            int total = c.totalWeight();
            set(53, Text.item(Material.CLOCK).name("<gold><b>Rozkład</b>")
                    .lore(summary(total)).build(), null);
        }

        private List<String> summary(int total) {
            List<String> out = new ArrayList<>();
            for (Rarity r : Rarity.values()) {
                int w = c.rewards.stream().filter(x -> x.rarity == r).mapToInt(x -> Math.max(0, x.weight)).sum();
                out.add("<" + Text.hex(r.rgb) + ">" + r.label + "</" + Text.hex(r.rgb) + "> <gray>"
                        + Text.pct(total == 0 ? 0 : 100.0 * w / total));
            }
            return out;
        }

        private ItemStack icon(Reward r) {
            List<String> lore = new ArrayList<>();
            lore.add("<dark_gray>──────────────");
            lore.add("<" + Text.hex(r.rarity.rgb) + ">● " + r.rarity.label);
            lore.add("<gray>Waga: <white>" + r.weight + "</white>  Szansa: <white>" + Text.pct(c.chance(r)));
            lore.add("<gray>Daje przedmiot: <white>" + (r.giveItem ? "tak ×" + r.item.getAmount() : "nie (tylko ikona)"));
            for (int i = 0; i < r.commands.size() && i < 4; i++) lore.add("<dark_gray>/" + Text.esc(strip(r.commands.get(i))));
            if (r.commands.size() > 4) lore.add("<dark_gray>… +" + (r.commands.size() - 4));
            lore.add("<dark_gray>LPM/PPM ±1 · Shift ±10 · F rzadkość · Q usuń");
            return Text.item(r.item).appendLore().lore(lore).build();
        }

        private static String strip(String cmd) {
            return cmd.startsWith("/") ? cmd.substring(1) : cmd;
        }

        private void edit(Reward r, InventoryClickEvent e) {
            ItemStack cursor = e.getCursor();
            ClickType ct = e.getClick();
            if (cursor != null && !cursor.getType().isAir() && (ct == ClickType.LEFT || ct == ClickType.RIGHT)) {
                r.item = cursor.clone();
                ok();
            } else switch (ct) {
                case LEFT -> r.weight += 1;
                case RIGHT -> r.weight = Math.max(0, r.weight - 1);
                case SHIFT_LEFT -> r.weight += 10;
                case SHIFT_RIGHT -> r.weight = Math.max(0, r.weight - 10);
                case SWAP_OFFHAND -> r.rarity = r.rarity.next();
                case DROP, CONTROL_DROP -> c.rewards.remove(r);
                case NUMBER_KEY -> {
                    switch (e.getHotbarButton()) {
                        case 0 -> {
                            plugin.prompts.ask(viewer, "<gray>Wpisz wagę (liczba ≥ 0):", s -> {
                                try {
                                    r.weight = Math.max(0, Integer.parseInt(s.trim()));
                                    plugin.store.save();
                                } catch (NumberFormatException ex) {
                                    Text.send(viewer, "<gray>To nie liczba.");
                                }
                                new Rewards(plugin, viewer, c).open();
                            }, () -> new Rewards(plugin, viewer, c).open());
                            return;
                        }
                        case 1 -> {
                            plugin.prompts.ask(viewer, "<gray>Wpisz komendę (bez / na początku też można). <white>{player}</white> = nick:", s -> {
                                r.commands.add(strip(s));
                                plugin.store.save();
                                new Rewards(plugin, viewer, c).open();
                            }, () -> new Rewards(plugin, viewer, c).open());
                            return;
                        }
                        case 2 -> r.commands.clear();
                        case 3 -> r.giveItem = !r.giveItem;
                        default -> {
                            return;
                        }
                    }
                }
                default -> {
                    return;
                }
            }
            plugin.store.save();
            redraw();
        }

        @Override
        protected void clickTop(InventoryClickEvent e) {
            ItemStack cursor = e.getCursor();
            if (e.getSlot() < c.maxRewards() && cursor != null && !cursor.getType().isAir()) add(cursor);
        }

        @Override
        protected void shiftFromBottom(InventoryClickEvent e) {
            ItemStack it = e.getCurrentItem();
            if (it != null && !it.getType().isAir()) add(it);
        }

        private void add(ItemStack it) {
            if (c.rewards.size() >= c.maxRewards()) {
                Text.send(viewer, "<gray>Limit " + c.maxRewards() + (c.isWheel() ? " pól na kole." : " nagród w jednej skrzynce."));
                deny();
                return;
            }
            c.rewards.add(Reward.of(it, 10, Rarity.POSPOLITY));
            plugin.store.save();
            ok();
            redraw();
        }

        private void addCommandReward() {
            if (c.rewards.size() >= c.maxRewards()) {
                deny();
                return;
            }
            ItemStack hand = viewer.getInventory().getItemInMainHand();
            ItemStack icon = hand.getType().isAir() ? new ItemStack(Material.PAPER) : hand.clone();
            icon.setAmount(1);
            plugin.prompts.ask(viewer, "<gray>Wpisz komendę nagrody, np. <white>eco give {player} 500</white>:", cmd ->
                    plugin.prompts.ask(viewer, "<gray>Wpisz nazwę wyświetlaną nagrody (MiniMessage), albo <white>-</white> żeby zostawić:", name -> {
                        if (!name.equals("-")) {
                            var meta = icon.getItemMeta();
                            meta.displayName(Text.line(name));
                            icon.setItemMeta(meta);
                        }
                        Reward r = Reward.of(icon, 10, Rarity.RZADKI);
                        r.giveItem = false;
                        r.commands.add(strip(cmd));
                        c.rewards.add(r);
                        plugin.store.save();
                        new Rewards(plugin, viewer, c).open();
                    }, () -> new Rewards(plugin, viewer, c).open()),
                    () -> new Rewards(plugin, viewer, c).open());
        }
    }

    public static class Locations extends Menu {
        private final Crate c;

        public Locations(SunSkrzynki plugin, Player viewer, Crate c) {
            super(plugin, viewer, 6, Text.mm("<dark_gray>Lokacje: ").append(c.displayName()));
            this.c = c;
        }

        @Override
        protected void render() {
            for (int i = 0; i < c.locations.size() && i < 45; i++) {
                BlockPos pos = c.locations.get(i);
                set(i, Text.item(Material.LODESTONE).name("<white>" + pos.pretty())
                        .lore("<yellow>LPM</yellow> <gray>teleport", "<yellow>Q</yellow> <gray>odepnij (blok zostaje)").build(), e -> {
                    if (e.getClick() == ClickType.DROP || e.getClick() == ClickType.CONTROL_DROP) {
                        plugin.service.removeVisuals(pos);
                        c.locations.remove(pos);
                        c.mounts.remove(pos);
                        plugin.store.save();
                        redraw();
                    } else if (pos.bukkitWorld() != null) {
                        viewer.teleport(pos.top().add(0, 0, 2).setDirection(new org.bukkit.util.Vector(0, -0.3, -1)));
                        viewer.closeInventory();
                    }
                });
            }
            fill(45, 53);
            set(45, back(), e -> new CrateEdit(plugin, viewer, c).open());
        }
    }

    public static class Logs extends Menu {
        private final Crate c;

        public Logs(SunSkrzynki plugin, Player viewer, Crate c) {
            super(plugin, viewer, 6, Text.mm("<dark_gray>Logi: ").append(c.displayName()));
            this.c = c;
        }

        @Override
        protected void render() {
            List<Store.LogEntry> logs = plugin.store.lastLogs(c, 45);
            for (int i = 0; i < logs.size(); i++) {
                Store.LogEntry l = logs.get(i);
                Reward r = null;
                for (Reward x : c.rewards) if (x.id.equals(l.rewardId())) r = x;
                ItemStack base = r != null ? r.item.clone() : new ItemStack(Material.PAPER);
                base.setAmount(1);
                String hex = Text.hex(l.rarity().rgb);
                set(i, Text.item(base).name("<" + hex + ">" + Text.esc(l.player()))
                        .lore("<gray>" + Text.esc(l.label()),
                                "<" + hex + ">" + l.rarity().label,
                                "<dark_gray>" + l.time()).build(), null);
            }
            fill(45, 53);
            set(45, back(), e -> new CrateEdit(plugin, viewer, c).open());
            List<String> stats = new ArrayList<>();
            stats.add("<gray>Otwarcia łącznie: <white>" + c.opens);
            for (Rarity r : Rarity.values()) {
                stats.add("<" + Text.hex(r.rgb) + ">" + r.label + "</" + Text.hex(r.rgb) + "> <gray>× <white>"
                        + plugin.store.countLogs(c, r));
            }
            stats.add("");
            stats.add("<dark_gray>Pełny log: plugins/SunSkrzynki/logs/" + c.id + ".log");
            set(49, Text.item(Material.WRITABLE_BOOK).name("<gold><b>Statystyki</b>").lore(stats).build(), null);
        }
    }

    public static class Preview extends Menu {
        private final Crate c;
        private final boolean fromAdmin;

        public Preview(SunSkrzynki plugin, Player viewer, Crate c, boolean fromAdmin) {
            super(plugin, viewer, 6, Text.mm("<dark_gray>Nagrody · ").append(c.displayName()));
            this.c = c;
            this.fromAdmin = fromAdmin;
        }

        @Override
        protected void render() {
            List<Reward> list = c.sortedByRarity().stream().filter(r -> r.weight > 0).toList();
            for (int i = 0; i < list.size() && i < 45; i++) {
                Reward r = list.get(i);
                String hex = Text.hex(r.rarity.rgb);
                Text.ItemBuilder b = Text.item(r.item).appendLore()
                        .lore("<" + hex + ">● " + r.rarity.label, "<gray>Szansa: <white>" + Text.pct(c.chance(r)));
                if (r.rarity == Rarity.LEGENDARNY) b.glint(true);
                set(i, b.build(), null);
            }
            fill(45, 53);
            set(49, Text.item(plugin.keys.keyItem(c, 1)).name(c.displayName())
                    .lore("<gray>Otwórz <white>kluczem</white> tej skrzynki (PPM na skrzynię).",
                            "<gray>Otwarto już: <white>" + c.opens + "</white> razy").build(), null);
            if (fromAdmin) set(45, back(), e -> new CrateEdit(plugin, viewer, c).open());
        }
    }

    public static class Confirm extends Menu {
        private final Runnable yes;
        private final Runnable no;

        public Confirm(SunSkrzynki plugin, Player viewer, String question, Runnable yes, Runnable no) {
            super(plugin, viewer, 3, Text.mm("<dark_gray>" + Text.esc(question)));
            this.yes = yes;
            this.no = no;
        }

        @Override
        protected void render() {
            set(11, Text.item(Material.TNT).name("<#ff5a5a><b>Tak, usuń</b>").build(), e -> yes.run());
            set(15, Text.item(Material.ARROW).name("<gray>Nie, wróć").build(), e -> no.run());
            fill(0, 26);
        }
    }
}
