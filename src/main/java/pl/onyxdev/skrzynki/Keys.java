package pl.onyxdev.skrzynki;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class Keys {
    public final NamespacedKey key;
    public final NamespacedKey wand;
    public final NamespacedKey fx;

    public Keys(Plugin plugin) {
        key = new NamespacedKey(plugin, "klucz");
        wand = new NamespacedKey(plugin, "rozdzka");
        fx = new NamespacedKey(plugin, "efekt");
    }

    public ItemStack keyItem(Crate crate, int amount) {
        ItemStack it = crate.isWheel()
                ? Text.item(crate.keyMaterial)
                .name(crate.displayName().append(Text.mm("<gray> · <white>Żeton")))
                .lore("<gray>Kliknij <white>PPM</white> na koło, żeby zakręcić.",
                        "<gray>LPM na koło pokazuje pola i szanse.",
                        "",
                        "<dark_gray>SunCraftMC")
                .glint(true)
                .build()
                : Text.item(crate.keyMaterial)
                .name(crate.displayName().append(Text.mm("<gray> · <white>Klucz")))
                .lore("<gray>Kliknij <white>PPM</white> na skrzynię, żeby ją otworzyć.",
                        "<gray>LPM na skrzynię pokazuje możliwe nagrody.",
                        "",
                        "<dark_gray>SunCraftMC")
                .glint(true)
                .build();
        ItemMeta meta = it.getItemMeta();
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, crate.id);
        meta.setMaxStackSize(64);
        it.setItemMeta(meta);
        it.setAmount(Math.max(1, Math.min(64, amount)));
        return it;
    }

    public String keyOf(ItemStack it) {
        if (it == null || it.getType().isAir() || !it.hasItemMeta()) return null;
        return it.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    public ItemStack wandItem(Crate crate) {
        ItemStack it = crate.isWheel()
                ? Text.item(Material.BLAZE_ROD)
                .name(Text.mm("<gold><b>Różdżka koła</b></gold> <gray>· ").append(crate.displayName()))
                .lore("<gray>PPM na <white>ścianę</white> → koło wisi na ścianie,",
                        "<gray>środek w klikniętym bloku.",
                        "<gray>PPM na <white>podłogę</white> → koło stoi, zwrócone do ciebie.",
                        "<gray>Usuwanie: <white>Shift + LPM</white> w koło z różdżką.")
                .glint(true)
                .build()
                : Text.item(Material.BLAZE_ROD)
                .name(Text.mm("<gold><b>Różdżka skrzyń</b></gold> <gray>· ").append(crate.displayName()))
                .lore("<gray>PPM na skrzynię/beczkę → staje się skrzynką.",
                        "<gray>PPM na inny blok → stawia na nim skrzynię.",
                        "<gray>Usuwanie: <white>Shift + zniszcz</white> skrzynię.")
                .glint(true)
                .build();
        ItemMeta meta = it.getItemMeta();
        meta.getPersistentDataContainer().set(wand, PersistentDataType.STRING, crate.id);
        it.setItemMeta(meta);
        return it;
    }

    public String wandOf(ItemStack it) {
        if (it == null || it.getType().isAir() || !it.hasItemMeta()) return null;
        return it.getItemMeta().getPersistentDataContainer().get(wand, PersistentDataType.STRING);
    }
}
