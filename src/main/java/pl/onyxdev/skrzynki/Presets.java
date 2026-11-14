package pl.onyxdev.skrzynki;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;

public final class Presets {
    private Presets() {
    }

    public static List<Crate> all() {
        List<Crate> out = new ArrayList<>();
        out.add(wyspiarska());
        out.add(gornicza());
        out.add(wojenna());
        out.add(mityczna());
        return out;
    }

    private static Crate wyspiarska() {
        Crate c = new Crate("wyspiarska", "<gradient:#b6ff6a:#2fbf71:#1e8c5a><b>Skrzynia Wyspiarza</b></gradient>");
        c.theme = Theme.NATURA;
        c.keyMaterial = Material.TRIAL_KEY;
        add(c, stack(Material.BONE_MEAL, 32), 30, Rarity.POSPOLITY);
        add(c, stack(Material.WHEAT_SEEDS, 32), 25, Rarity.POSPOLITY);
        add(c, stack(Material.OAK_SAPLING, 8), 25, Rarity.POSPOLITY);
        add(c, stack(Material.SUGAR_CANE, 16), 20, Rarity.POSPOLITY);
        add(c, stack(Material.DIRT, 64), 20, Rarity.POSPOLITY);
        add(c, stack(Material.CHERRY_SAPLING, 4), 12, Rarity.RZADKI);
        add(c, stack(Material.WATER_BUCKET, 1), 12, Rarity.RZADKI);
        add(c, stack(Material.LAVA_BUCKET, 1), 10, Rarity.RZADKI);
        add(c, stack(Material.COW_SPAWN_EGG, 2), 8, Rarity.RZADKI);
        add(c, stack(Material.SHEEP_SPAWN_EGG, 2), 8, Rarity.RZADKI);
        add(c, stack(Material.MYCELIUM, 8), 4, Rarity.EPICKI);
        add(c, named(Material.GOLDEN_HOE, "<#b6ff6a>Motyka Urodzaju", "<gray>Plony same się proszą.",
                Enchantment.EFFICIENCY, 5, Enchantment.UNBREAKING, 3, Enchantment.FORTUNE, 3), 3, Rarity.EPICKI);
        add(c, stack(Material.SNIFFER_EGG, 1), 1, Rarity.LEGENDARNY);
        return c;
    }

    private static Crate gornicza() {
        Crate c = new Crate("gornicza", "<gradient:#e0b3ff:#9b59ff:#5a2bb8><b>Skrzynia Górnika</b></gradient>");
        c.theme = Theme.AMETYST;
        c.keyMaterial = Material.AMETHYST_SHARD;
        add(c, stack(Material.COAL, 32), 30, Rarity.POSPOLITY);
        add(c, stack(Material.RAW_IRON, 24), 25, Rarity.POSPOLITY);
        add(c, stack(Material.RAW_COPPER, 32), 20, Rarity.POSPOLITY);
        add(c, stack(Material.TORCH, 64), 20, Rarity.POSPOLITY);
        add(c, stack(Material.RAW_GOLD, 16), 14, Rarity.RZADKI);
        add(c, stack(Material.LAPIS_LAZULI, 32), 12, Rarity.RZADKI);
        add(c, stack(Material.TNT, 8), 10, Rarity.RZADKI);
        add(c, named(Material.IRON_PICKAXE, "<#e0b3ff>Kilof Sztygara", "<gray>Solidny, na pierwsze sztolnie.",
                Enchantment.EFFICIENCY, 4, Enchantment.UNBREAKING, 3), 10, Rarity.RZADKI);
        add(c, stack(Material.DIAMOND, 6), 6, Rarity.EPICKI);
        add(c, stack(Material.EMERALD, 16), 5, Rarity.EPICKI);
        add(c, named(Material.DIAMOND_PICKAXE, "<#c77dff>Kilof Głębin", "<gray>Kopie szybciej, niż myślisz.",
                Enchantment.EFFICIENCY, 5, Enchantment.UNBREAKING, 3, Enchantment.FORTUNE, 3), 3, Rarity.EPICKI);
        add(c, stack(Material.ANCIENT_DEBRIS, 4), 2, Rarity.LEGENDARNY);
        add(c, named(Material.NETHERITE_PICKAXE, "<gradient:#ffd36e:#ff8a00><b>Kilof Pradawnych</b>",
                "<gray>Wykuty w sercu wyspy.", Enchantment.EFFICIENCY, 5, Enchantment.UNBREAKING, 3,
                Enchantment.FORTUNE, 3, Enchantment.MENDING, 1), 1, Rarity.LEGENDARNY);
        return c;
    }

    private static Crate wojenna() {
        Crate c = new Crate("wojenna", "<gradient:#ffb36b:#ff5a1f:#b3001b><b>Skrzynia Wojenna</b></gradient>");
        c.theme = Theme.OGIEN;
        c.keyMaterial = Material.FIRE_CHARGE;
        add(c, stack(Material.ARROW, 48), 28, Rarity.POSPOLITY);
        add(c, stack(Material.COOKED_BEEF, 24), 25, Rarity.POSPOLITY);
        add(c, stack(Material.SHIELD, 1), 18, Rarity.POSPOLITY);
        add(c, potion(PotionType.STRONG_HEALING, 3), 18, Rarity.POSPOLITY);
        add(c, named(Material.IRON_SWORD, "<#ffb36b>Miecz Rekruta", "<gray>Każdy od czegoś zaczynał.",
                Enchantment.SHARPNESS, 3, Enchantment.UNBREAKING, 2), 14, Rarity.RZADKI);
        add(c, named(Material.BOW, "<#ffb36b>Łuk Zwiadowcy", "<gray>Cicho i z daleka.",
                Enchantment.POWER, 4, Enchantment.PUNCH, 1), 12, Rarity.RZADKI);
        add(c, potion(PotionType.STRONG_STRENGTH, 2), 10, Rarity.RZADKI);
        add(c, stack(Material.GOLDEN_APPLE, 4), 10, Rarity.RZADKI);
        add(c, named(Material.DIAMOND_CHESTPLATE, "<#ff7a3d>Napierśnik Obrońcy", "<gray>Przyjmie niejeden cios.",
                Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3), 4, Rarity.EPICKI);
        add(c, named(Material.DIAMOND_SWORD, "<#ff5a1f>Ostrze Żaru", "<gray>Wciąż ciepłe po kuźni.",
                Enchantment.SHARPNESS, 5, Enchantment.FIRE_ASPECT, 2, Enchantment.UNBREAKING, 3), 3, Rarity.EPICKI);
        add(c, stack(Material.TOTEM_OF_UNDYING, 1), 3, Rarity.EPICKI);
        add(c, named(Material.NETHERITE_SWORD, "<gradient:#ffe066:#ff4d00><b>Gniew Słońca</b>",
                "<gray>Legenda mówi, że nigdy nie chybia.", Enchantment.SHARPNESS, 5, Enchantment.FIRE_ASPECT, 2,
                Enchantment.LOOTING, 3, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1), 1, Rarity.LEGENDARNY);
        return c;
    }

    private static Crate mityczna() {
        Crate c = new Crate("mityczna", "<gradient:#9fb4ff:#6f7bff:#b45cff><b>Mityczna Skrzynia</b></gradient>");
        c.theme = Theme.NOC;
        c.keyMaterial = Material.OMINOUS_TRIAL_KEY;
        add(c, stack(Material.DIAMOND, 12), 22, Rarity.RZADKI);
        add(c, stack(Material.EXPERIENCE_BOTTLE, 64), 20, Rarity.RZADKI);
        add(c, book(Enchantment.MENDING, 1), 16, Rarity.RZADKI);
        add(c, stack(Material.ENDER_PEARL, 16), 14, Rarity.RZADKI);
        add(c, stack(Material.SHULKER_BOX, 1), 8, Rarity.EPICKI);
        add(c, stack(Material.ENCHANTED_GOLDEN_APPLE, 2), 7, Rarity.EPICKI);
        add(c, stack(Material.NETHERITE_INGOT, 2), 6, Rarity.EPICKI);
        add(c, stack(Material.TOTEM_OF_UNDYING, 2), 5, Rarity.EPICKI);
        add(c, stack(Material.BEACON, 1), 2, Rarity.LEGENDARNY);
        add(c, named(Material.ELYTRA, "<gradient:#9fb4ff:#b45cff><b>Skrzydła Nocy</b>",
                "<gray>Dla tych, co chcą zobaczyć archipelag z góry.", Enchantment.UNBREAKING, 3,
                Enchantment.MENDING, 1), 2, Rarity.LEGENDARNY);
        add(c, stack(Material.DRAGON_HEAD, 1), 1, Rarity.LEGENDARNY);
        return c;
    }

    public static Crate wheel(SunSkrzynki plugin) {
        Crate c = new Crate("kolo", "<gradient:#ffe066:#ff4d8d:#8a5cff><b>Koło Fortuny</b></gradient>");
        c.kind = Crate.Kind.KOLO;
        c.theme = Theme.SLONCE;
        c.keyMaterial = Material.SUNFLOWER;
        c.radius = 2.5;
        c.freeDaily = true;

        add(c, stack(Material.IRON_INGOT, 16), 18, Rarity.POSPOLITY);
        key(plugin, c, "wyspiarska", 1, 16, Rarity.POSPOLITY);
        add(c, stack(Material.GOLDEN_APPLE, 2), 10, Rarity.RZADKI);
        blank(c, 14);
        key(plugin, c, "gornicza", 1, 11, Rarity.RZADKI);
        add(c, stack(Material.EXPERIENCE_BOTTLE, 32), 16, Rarity.POSPOLITY);
        blank(c, 14);
        key(plugin, c, "mityczna", 3, 1, Rarity.LEGENDARNY);
        add(c, plugin.keys.keyItem(c, 2), 8, Rarity.RZADKI);
        add(c, stack(Material.DIAMOND, 3), 9, Rarity.RZADKI);
        key(plugin, c, "wojenna", 1, 10, Rarity.RZADKI);
        key(plugin, c, "mityczna", 1, 3, Rarity.EPICKI);
        return c;
    }

    private static void key(SunSkrzynki plugin, Crate wheel, String crateId, int amount, int weight, Rarity rarity) {
        Crate target = plugin.store.crates.get(crateId);
        if (target != null) add(wheel, plugin.keys.keyItem(target, amount), weight, rarity);
    }

    private static void blank(Crate c, int weight) {
        Reward r = Reward.of(Text.item(Material.WITHER_ROSE).name("<gray>Pech!").build(), weight, Rarity.POSPOLITY);
        r.giveItem = false;
        c.rewards.add(r);
    }

    private static void add(Crate c, ItemStack item, int weight, Rarity rarity) {
        c.rewards.add(Reward.of(item, weight, rarity));
    }

    private static ItemStack stack(Material m, int amount) {
        return new ItemStack(m, amount);
    }

    private static ItemStack named(Material m, String name, String lore, Object... enchants) {
        ItemStack it = Text.item(m).name(name).lore(lore).build();
        ItemMeta meta = it.getItemMeta();
        for (int i = 0; i + 1 < enchants.length; i += 2) {
            meta.addEnchant((Enchantment) enchants[i], (Integer) enchants[i + 1], true);
        }
        it.setItemMeta(meta);
        return it;
    }

    private static ItemStack book(Enchantment e, int level) {
        ItemStack it = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) it.getItemMeta();
        meta.addStoredEnchant(e, level, true);
        it.setItemMeta(meta);
        return it;
    }

    private static ItemStack potion(PotionType type, int amount) {
        ItemStack it = new ItemStack(Material.SPLASH_POTION, amount);
        PotionMeta meta = (PotionMeta) it.getItemMeta();
        meta.setBasePotionType(type);
        meta.setMaxStackSize(Math.max(1, amount));
        it.setItemMeta(meta);
        return it;
    }
}
