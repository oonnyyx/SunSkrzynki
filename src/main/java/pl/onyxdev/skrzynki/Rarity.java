package pl.onyxdev.skrzynki;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Material;

public enum Rarity {
    POSPOLITY("Pospolity", 0xB8C4CE, Material.LIGHT_GRAY_DYE),
    RZADKI("Rzadki", 0x4DA6FF, Material.LIGHT_BLUE_DYE),
    EPICKI("Epicki", 0xB45CFF, Material.PURPLE_DYE),
    LEGENDARNY("Legendarny", 0xFFB21A, Material.ORANGE_DYE);

    public final String label;
    public final int rgb;
    public final Material icon;

    Rarity(String label, int rgb, Material icon) {
        this.label = label;
        this.rgb = rgb;
        this.icon = icon;
    }

    public TextColor text() {
        return TextColor.color(rgb);
    }

    public Color color() {
        return Color.fromRGB(rgb);
    }

    public Rarity next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static Rarity forChance(double percent) {
        if (percent < 1.5) return LEGENDARNY;
        if (percent < 6) return EPICKI;
        if (percent < 20) return RZADKI;
        return POSPOLITY;
    }

    public static Rarity parse(String s) {
        try {
            return valueOf(s.toUpperCase());
        } catch (Exception e) {
            return POSPOLITY;
        }
    }
}
