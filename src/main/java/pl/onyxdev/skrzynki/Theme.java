package pl.onyxdev.skrzynki;

import org.bukkit.Color;
import org.bukkit.Material;

public enum Theme {
    SLONCE("Słońce", 0xFFC02E, 0xFF6A00, Material.SUNFLOWER),
    OGIEN("Ogień", 0xFF5A1F, 0xB3001B, Material.BLAZE_POWDER),
    LOD("Lód", 0x9EF0FF, 0x2F7BFF, Material.PACKED_ICE),
    AMETYST("Ametyst", 0xD08CFF, 0x6A1FD6, Material.AMETHYST_SHARD),
    NATURA("Natura", 0x9BFF6A, 0x1E9E4A, Material.OAK_SAPLING),
    NOC("Noc", 0x6F7BFF, 0x14123A, Material.ECHO_SHARD);

    public final String label;
    public final int light;
    public final int dark;
    public final Material icon;

    Theme(String label, int light, int dark, Material icon) {
        this.label = label;
        this.light = light;
        this.dark = dark;
        this.icon = icon;
    }

    public Color lightColor() {
        return Color.fromRGB(light);
    }

    public Color darkColor() {
        return Color.fromRGB(dark);
    }

    public Theme next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static Theme parse(String s) {
        try {
            return valueOf(s.toUpperCase());
        } catch (Exception e) {
            return SLONCE;
        }
    }
}
