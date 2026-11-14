package pl.onyxdev.skrzynki;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class Crate {
    public enum Kind { SKRZYNIA, KOLO }

    public static final int MAX_WHEEL_SEGMENTS = 16;

    public final String id;

    public String name;
    public Kind kind = Kind.SKRZYNIA;
    public Theme theme = Theme.SLONCE;
    public Material keyMaterial = Material.TRIPWIRE_HOOK;
    public int opens;
    public final List<Reward> rewards = new ArrayList<>();
    public final List<BlockPos> locations = new ArrayList<>();

    public double radius = 2.5;

    public boolean freeDaily = true;

    public final Map<BlockPos, String> mounts = new HashMap<>();

    public Crate(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public boolean isWheel() {
        return kind == Kind.KOLO;
    }

    public int maxRewards() {
        return isWheel() ? MAX_WHEEL_SEGMENTS : 45;
    }

    public static boolean isBlank(Reward r) {
        return !r.giveItem && r.commands.isEmpty();
    }

    public Component displayName() {
        return MiniMessage.miniMessage().deserialize(name);
    }

    public int totalWeight() {
        int sum = 0;
        for (Reward r : rewards) sum += Math.max(0, r.weight);
        return sum;
    }

    public double chance(Reward r) {
        int total = totalWeight();
        return total <= 0 ? 0 : 100.0 * Math.max(0, r.weight) / total;
    }

    public Reward roll() {
        int total = totalWeight();
        if (total <= 0) return null;
        int pick = ThreadLocalRandom.current().nextInt(total);
        for (Reward r : rewards) {
            if (r.weight <= 0) continue;
            pick -= r.weight;
            if (pick < 0) return r;
        }
        return rewards.getLast();
    }

    public Reward randomForShow() {
        if (rewards.isEmpty()) return null;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        if (rnd.nextInt(3) == 0) return rewards.get(rnd.nextInt(rewards.size()));
        Reward r = roll();
        return r != null ? r : rewards.getFirst();
    }

    public List<Reward> sortedByRarity() {
        List<Reward> list = new ArrayList<>(rewards);
        list.sort(Comparator.comparing((Reward r) -> r.rarity).reversed().thenComparingInt(r -> r.weight));
        return list;
    }
}
