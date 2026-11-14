package pl.onyxdev.skrzynki;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class Celebrate {
    private Celebrate() {
    }

    public static void sound(SunSkrzynki plugin, Location at, Rarity rar) {
        switch (rar) {
            case POSPOLITY -> {
                Fx.sound(at, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.4f);
                Fx.sound(at, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
            }
            case RZADKI -> {
                Fx.sound(at, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.1f);
                Fx.sound(at, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 1f);
                arpeggio(plugin, at, Sound.BLOCK_NOTE_BLOCK_BELL, 2, 12, 16, 19);
            }
            case EPICKI -> {
                Fx.sound(at, Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1.3f);
                Fx.sound(at, Sound.BLOCK_END_PORTAL_FRAME_FILL, 1f, 0.8f);
                Fx.sound(at, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.8f, 1.2f);
                arpeggio(plugin, at, Sound.BLOCK_NOTE_BLOCK_CHIME, 2, 7, 11, 14, 19);
                arpeggio(plugin, at, Sound.BLOCK_NOTE_BLOCK_BELL, 2, 7, 11, 14, 19);
            }
            case LEGENDARNY -> {
                Fx.sound(at, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                Fx.sound(at, Sound.ITEM_TOTEM_USE, 0.7f, 1.2f);
                Fx.sound(at, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1f, 0.9f);
                arpeggio(plugin, at, Sound.BLOCK_NOTE_BLOCK_BELL, 2, 0, 4, 7, 12, 16, 19, 24);
                arpeggio(plugin, at, Sound.BLOCK_NOTE_BLOCK_PLING, 2, 0, 4, 7, 12, 16, 19, 24);
                Bukkit.getScheduler().runTaskLater(plugin,
                        () -> Fx.sound(at, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1f, 1f), 14);
            }
        }
    }

    public static void sadTrombone(SunSkrzynki plugin, Location at) {
        int[] notes = {7, 6, 5, 4};
        for (int i = 0; i < notes.length; i++) {
            float pitch = Fx.note(notes[i]);
            boolean last = i == notes.length - 1;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Fx.sound(at, Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1f, pitch);
                Fx.sound(at, Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, pitch);
                if (last) {
                    for (int k = 1; k <= 3; k++) {
                        Bukkit.getScheduler().runTaskLater(plugin,
                                () -> Fx.sound(at, Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 0.6f, pitch * 0.97f), k * 3L);
                    }
                }
            }, i * 7L);
        }
        Fx.sound(at, Sound.ENTITY_VILLAGER_NO, 1f, 0.9f);
    }

    public static void arpeggio(SunSkrzynki plugin, Location at, Sound s, int gap, int... semitones) {
        for (int i = 0; i < semitones.length; i++) {
            float pitch = Fx.note(semitones[i]);
            Bukkit.getScheduler().runTaskLater(plugin, () -> Fx.sound(at, s, 1f, pitch), (long) i * gap);
        }
    }

    public static void burst(SunSkrzynki plugin, Location c, Rarity rar, boolean lightning) {
        int tier = rar.ordinal();
        Fx.plain(c, Particle.FIREWORK, 25 + tier * 25, 0.1, 0.22 + tier * 0.05);
        Fx.plain(c, Particle.END_ROD, 15 + tier * 10, 0.1, 0.15);
        if (tier >= 1) Fx.plain(c, Particle.WAX_ON, 30, 0.8, 1);
        if (tier >= 2) {
            Fx.dustFade(c, rar.color(), Color.WHITE, 1.8f, 60, 1.1);
            Fx.plain(c, Particle.SONIC_BOOM, 1, 0, 0);
        }
        if (tier >= 3) {
            Fx.plain(c, Particle.TOTEM_OF_UNDYING, 140, 0.3, 0.75);
            firework(plugin, c.clone().add(0, 1.2, 0));
            Bukkit.getScheduler().runTaskLater(plugin, () -> firework(plugin, c.clone().add(1.4, 2.2, -1.1)), 6);
            Bukkit.getScheduler().runTaskLater(plugin, () -> firework(plugin, c.clone().add(-1.3, 2.6, 1.2)), 11);
            if (lightning) c.getWorld().strikeLightningEffect(c.clone().add(0, -2, 0));
        }
    }

    public static void firework(SunSkrzynki plugin, Location at) {
        Firework fw = at.getWorld().spawn(at, Firework.class, f -> {
            f.getPersistentDataContainer().set(plugin.keys.fx, PersistentDataType.BYTE, (byte) 1);
            FireworkMeta meta = f.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder()
                    .with(ThreadLocalRandom.current().nextBoolean() ? FireworkEffect.Type.BALL_LARGE : FireworkEffect.Type.STAR)
                    .withColor(Color.fromRGB(0xFFB21A), Color.fromRGB(0xFFE680), Color.WHITE)
                    .withFade(Color.fromRGB(0xFF5A00))
                    .trail(true).flicker(true).build());
            f.setFireworkMeta(meta);
        });
        Bukkit.getScheduler().runTaskLater(plugin, fw::detonate, 1);
    }

    public static Component rewardName(Reward r) {
        if (Crate.isBlank(r) && !r.item.getItemMeta().hasDisplayName()) return Text.mm("<gray>Pech!");
        Component name = r.item.effectiveName().decoration(TextDecoration.ITALIC, false)
                .colorIfAbsent(r.rarity.text());
        int amount = r.item.getAmount();
        if (r.giveItem && amount > 1) name = name.append(Text.mm("<gray> ×" + amount));
        return name;
    }

    public static void announce(SunSkrzynki plugin, Crate crate, UUID playerId, String playerName, Reward r) {
        Player p = playerId == null ? null : Bukkit.getPlayer(playerId);
        Component itemName = rewardName(r).hoverEvent(r.item.asHoverEvent());
        boolean blank = Crate.isBlank(r);
        if (p != null) {
            if (blank) {
                Msg.title(p, Msg.BLANK_TITLE, Title.title(Text.mm("<gray><b>PECH!</b>"), Text.mm("<dark_gray>Może następnym razem…"),
                        Title.Times.times(Duration.ofMillis(150), Duration.ofMillis(1800), Duration.ofMillis(600))));
                Msg.chat(p, Msg.BLANK_CHAT, "<gray>Tym razem nic. Koło bywa okrutne.");
            } else {
                String hex = Text.hex(r.rarity.rgb);
                Msg.title(p, Msg.WIN_TITLE, Title.title(
                        Text.mm("<" + hex + "><b>" + r.rarity.label.toUpperCase() + "</b>"),
                        itemName,
                        Title.Times.times(Duration.ofMillis(150), Duration.ofMillis(2200), Duration.ofMillis(700))));
                Msg.chat(p, Msg.WIN_CHAT, Text.mm(Text.PREFIX + "Wylosowano <" + hex + ">" + r.rarity.label + "</" + hex + ">: ")
                        .append(itemName));
            }
        }
        if (!blank && (r.rarity == Rarity.LEGENDARNY || r.rarity == Rarity.EPICKI)) {
            boolean legend = r.rarity == Rarity.LEGENDARNY;
            String what = legend ? "<#ffb21a><b>LEGENDĘ</b></#ffb21a>" : "<#b45cff><b>EPIKA</b></#b45cff>";
            Msg.broadcast(legend ? Msg.LEGEND_BROADCAST : Msg.EPIC_BROADCAST,
                    Text.mm(Text.PREFIX + "<white>" + Text.esc(playerName) + "</white> <gray>wylosował(a) "
                                    + what + " <gray>" + (crate.isWheel() ? "na " : "ze skrzyni "))
                            .append(crate.displayName()).append(Text.mm("<gray>: ")).append(itemName));
        }
        record(plugin, crate, playerName, r);
    }

    public static void record(SunSkrzynki plugin, Crate crate, String playerName, Reward r) {
        crate.opens++;
        plugin.store.log(crate, playerName, r, Text.plain(rewardName(r)));
        plugin.store.save();
    }
}
