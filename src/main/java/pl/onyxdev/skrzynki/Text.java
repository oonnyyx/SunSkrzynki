package pl.onyxdev.skrzynki;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Text {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    public static final String PREFIX = "<gradient:#ffc02e:#ff6a00><b>Skrzynki</b></gradient> <dark_gray>»</dark_gray> ";

    private Text() {
    }

    public static Component mm(String s) {
        return MM.deserialize(s);
    }

    public static Component line(String s) {
        return MM.deserialize(s).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public static void send(CommandSender to, String s) {
        to.sendMessage(mm(PREFIX + s));
    }

    public static String pct(double p) {
        if (p >= 10) return String.format(Locale.ROOT, "%.1f%%", p);
        if (p >= 1) return String.format(Locale.ROOT, "%.2f%%", p);
        return String.format(Locale.ROOT, "%.3f%%", p);
    }

    public static String hex(int rgb) {
        return String.format("#%06x", rgb);
    }

    public static String plain(Component c) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(c);
    }

    public static String esc(String s) {
        return MM.escapeTags(s);
    }

    public static ItemBuilder item(Material m) {
        return new ItemBuilder(new ItemStack(m));
    }

    public static ItemBuilder item(ItemStack base) {
        return new ItemBuilder(base.clone());
    }

    public static final class ItemBuilder {
        private final ItemStack stack;
        private Component name;
        private final List<Component> lore = new ArrayList<>();
        private boolean keepLore;
        private Boolean glint;

        private ItemBuilder(ItemStack stack) {
            this.stack = stack;
        }

        public ItemBuilder name(String mini) {
            this.name = line(mini);
            return this;
        }

        public ItemBuilder name(Component c) {
            this.name = c.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
            return this;
        }

        public ItemBuilder lore(String... lines) {
            for (String l : lines) lore.add(line(l));
            return this;
        }

        public ItemBuilder lore(List<String> lines) {
            for (String l : lines) lore.add(line(l));
            return this;
        }

        public ItemBuilder appendLore() {
            this.keepLore = true;
            return this;
        }

        public ItemBuilder glint(boolean g) {
            this.glint = g;
            return this;
        }

        public ItemBuilder amount(int a) {
            stack.setAmount(Math.max(1, Math.min(a, stack.getMaxStackSize())));
            return this;
        }

        public ItemStack build() {
            ItemMeta meta = stack.getItemMeta();
            if (meta == null) return stack;
            if (name != null) meta.displayName(name);
            List<Component> all = new ArrayList<>();
            if (keepLore && meta.lore() != null) {
                all.addAll(meta.lore());
                if (!lore.isEmpty()) all.add(Component.empty());
            }
            all.addAll(lore);
            if (!lore.isEmpty() || keepLore) meta.lore(all);
            if (glint != null) meta.setEnchantmentGlintOverride(glint);
            stack.setItemMeta(meta);
            return stack;
        }
    }
}
