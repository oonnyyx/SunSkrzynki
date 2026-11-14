package pl.onyxdev.skrzynki;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Reward {
    public final String id;
    public ItemStack item;
    public int weight;
    public Rarity rarity;

    public boolean giveItem;
    public final List<String> commands = new ArrayList<>();

    public Reward(String id, ItemStack item, int weight, Rarity rarity, boolean giveItem) {
        this.id = id;
        this.item = item;
        this.weight = weight;
        this.rarity = rarity;
        this.giveItem = giveItem;
    }

    public static Reward of(ItemStack item, int weight, Rarity rarity) {
        return new Reward(UUID.randomUUID().toString().substring(0, 8), item.clone(), weight, rarity, true);
    }
}
