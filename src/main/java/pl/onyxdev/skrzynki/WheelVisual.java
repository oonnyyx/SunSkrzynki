package pl.onyxdev.skrzynki;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class WheelVisual {
    private static final class Part {
        final Display d;
        final float angle;
        final Vector3f offset;
        final Vector3f scale;
        final boolean rotates;

        Part(Display d, float angle, Vector3f offset, Vector3f scale, boolean rotates) {
            this.d = d;
            this.angle = angle;
            this.offset = offset;
            this.scale = scale;
            this.rotates = rotates;
        }
    }

    private final SunSkrzynki plugin;
    private final Crate crate;
    public final Location center;
    public final Vector normal;
    private final Quaternionf face;
    public final float radius;
    public final int segments;
    public final float seg;
    public final String signature;

    private final List<Part> parts = new ArrayList<>();
    private final List<List<Part>> segmentParts = new ArrayList<>();
    private final List<Part> icons = new ArrayList<>();
    private final List<Part> bulbs = new ArrayList<>();
    private final List<Interaction> hits = new ArrayList<>();
    private Part hub;
    private Part pointer;
    private float phi;

    public WheelVisual(SunSkrzynki plugin, Crate crate, BlockPos pos) {
        this.plugin = plugin;
        this.crate = crate;
        this.radius = (float) crate.radius;
        this.segments = Math.max(1, crate.rewards.size());
        this.seg = (float) (Math.PI * 2 / segments);
        this.signature = signature(crate);

        String mount = crate.mounts.getOrDefault(pos, "FLOOR:SOUTH");
        boolean wall = mount.startsWith("WALL");
        BlockFace f;
        try {
            f = BlockFace.valueOf(mount.substring(mount.indexOf(':') + 1));
        } catch (Exception e) {
            f = BlockFace.SOUTH;
        }
        this.normal = new Vector(f.getModX(), 0, f.getModZ());
        World w = pos.bukkitWorld();
        Location c = wall
                ? new Location(w, pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5).add(normal.clone().multiply(0.56))
                : new Location(w, pos.x() + 0.5, pos.y() + 1 + radius + 0.3, pos.z() + 0.5);
        c.setYaw(0);
        c.setPitch(0);
        this.center = c;
        this.face = new Quaternionf().rotateY((float) Math.atan2(normal.getX(), normal.getZ()));
    }

    public static String signature(Crate c) {
        StringBuilder sb = new StringBuilder().append(c.radius).append('|').append(c.theme);
        for (Reward r : c.rewards) {
            sb.append('|').append(r.id).append(r.rarity.ordinal()).append(Crate.isBlank(r) ? 'b' : 'r')
                    .append(r.item.getType()).append(r.item.getAmount());
        }
        return sb.toString();
    }

    public WheelVisual spawn() {
        buildSegments();
        buildSeparators();
        buildIcons();
        buildRim();
        hub = block(Material.GOLD_BLOCK, 0, new Vector3f(-0.32f, -0.32f, 0.03f), new Vector3f(0.64f, 0.64f, 0.12f), true);
        Part cap = block(Material.RAW_GOLD_BLOCK, 0, new Vector3f(-0.14f, -0.14f, 0.1f), new Vector3f(0.28f, 0.28f, 0.1f), true);
        cap.d.setBrightness(new Display.Brightness(15, 15));
        buildPointer();
        buildHitboxes();
        apply(0, 0);
        return this;
    }

    private void buildSegments() {
        float inner = 0.3f;
        int bands = Math.max(2, (int) Math.ceil((radius - inner) / 0.6f));
        for (int i = 0; i < segments; i++) {
            List<Part> list = new ArrayList<>();
            Material mat = segmentMaterial(i);
            float z = (i % 2) * 0.008f + ((segments % 2 == 1 && i == segments - 1) ? 0.004f : 0f);
            for (int b = 0; b < bands; b++) {
                float ri = inner + (radius - inner) * b / bands;
                float ro = inner + (radius - inner) * (b + 1) / bands;
                int m = Math.max(1, (int) Math.ceil(seg * ro / 0.5f));
                float slice = seg / m;
                float width = (float) (2 * ro * Math.sin(slice / 2)) * 1.03f;
                for (int k = 0; k < m; k++) {
                    float a = i * seg + (k + 0.5f) * slice;
                    list.add(block(mat, a, new Vector3f(-width / 2, ri - 0.02f, z), new Vector3f(width, ro - ri + 0.04f, 0.02f), true));
                }
            }
            segmentParts.add(list);
        }
    }

    private void buildSeparators() {
        if (segments < 2) return;
        for (int i = 0; i < segments; i++) {
            block(Material.BLACK_CONCRETE, i * seg, new Vector3f(-0.07f, 0.28f, 0.03f),
                    new Vector3f(0.14f, radius - 0.26f, 0.025f), true);
        }
    }

    private void buildIcons() {
        float s = Math.max(0.4f, 0.52f * radius / 2.5f);
        for (int i = 0; i < segments && i < crate.rewards.size(); i++) {
            Reward r = crate.rewards.get(i);
            ItemStack show = Crate.isBlank(r) ? new ItemStack(Material.WITHER_ROSE) : r.item.clone();
            show.setAmount(1);
            float a = (i + 0.5f) * seg;
            ItemDisplay d = center.getWorld().spawn(center, ItemDisplay.class, e -> {
                setup(e);
                e.setItemStack(show);
                e.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            });
            Part p = new Part(d, a, new Vector3f(0, radius * 0.66f, 0.1f), new Vector3f(s, s, s), true);
            parts.add(p);
            icons.add(p);
        }
    }

    private void buildRim() {
        int n = 44;
        float len = (float) (2 * Math.PI * (radius + 0.06) / n) * 1.12f;
        for (int k = 0; k < n; k++) {
            block(Material.GOLD_BLOCK, (float) (k * 2 * Math.PI / n), new Vector3f(-len / 2, radius - 0.03f, -0.03f),
                    new Vector3f(len, 0.16f, 0.12f), false);
        }
        int nb = 24;
        for (int k = 0; k < nb; k++) {
            float a = (float) (k * 2 * Math.PI / nb);
            Part b = block(Material.PEARLESCENT_FROGLIGHT, a, new Vector3f(-0.07f, radius + 0.02f, 0.08f),
                    new Vector3f(0.14f, 0.14f, 0.08f), false);
            bulbs.add(b);
        }
    }

    private void buildPointer() {
        pointer = block(Material.GOLD_BLOCK, (float) (Math.PI / 4), new Vector3f(-0.19f, -0.19f, 0.12f),
                new Vector3f(0.38f, 0.38f, 0.1f), false);
        pointer.d.setGlowing(true);
        pointer.d.setGlowColorOverride(org.bukkit.Color.fromRGB(0xFFE066));
    }

    private void buildHitboxes() {
        float step = 0.8f;
        int n = (int) Math.ceil(radius / step);
        for (int ix = -n; ix <= n; ix++) {
            for (int iy = -n; iy <= n; iy++) {
                float x = ix * step, y = iy * step;
                if (x * x + y * y > (radius + 0.2f) * (radius + 0.2f)) continue;
                Vector3f v = face.transform(new Vector3f(x, y, 0.05f));
                Location at = center.clone().add(v.x, v.y - step / 2, v.z);
                hits.add(center.getWorld().spawn(at, Interaction.class, e -> {
                    e.setPersistent(false);
                    e.getPersistentDataContainer().set(plugin.keys.fx, PersistentDataType.BYTE, (byte) 1);
                    e.setInteractionWidth(step);
                    e.setInteractionHeight(step);
                    e.setResponsive(true);
                }));
            }
        }
    }

    private Part block(Material m, float angle, Vector3f offset, Vector3f scale, boolean rotates) {
        BlockDisplay d = center.getWorld().spawn(center, BlockDisplay.class, e -> {
            setup(e);
            e.setBlock(m.createBlockData());
        });
        Part p = new Part(d, angle, offset, scale, rotates);
        parts.add(p);
        return p;
    }

    private void setup(Display d) {
        d.setPersistent(false);
        d.getPersistentDataContainer().set(plugin.keys.fx, PersistentDataType.BYTE, (byte) 1);
        d.setViewRange(1.0f);
    }

    private Material segmentMaterial(int i) {
        if (i >= crate.rewards.size()) return Material.WHITE_CONCRETE;
        Reward r = crate.rewards.get(i);
        boolean alt = i % 2 == 1;
        if (Crate.isBlank(r)) return alt ? Material.GRAY_CONCRETE : Material.BLACK_CONCRETE;
        return switch (r.rarity) {
            case POSPOLITY -> alt ? Material.LIGHT_GRAY_CONCRETE : Material.WHITE_CONCRETE;
            case RZADKI -> alt ? Material.BLUE_CONCRETE : Material.LIGHT_BLUE_CONCRETE;
            case EPICKI -> alt ? Material.MAGENTA_CONCRETE : Material.PURPLE_CONCRETE;
            case LEGENDARNY -> alt ? Material.ORANGE_CONCRETE : Material.YELLOW_CONCRETE;
        };
    }

    public float phi() {
        return phi;
    }

    public void apply(float phi, int ticks) {
        this.phi = phi;
        for (Part p : parts) {
            if (!p.rotates && ticks > 0) continue;
            set(p, p.rotates ? p.angle + phi : p.angle, ticks);
        }
    }

    private void set(Part p, float angle, int ticks) {
        Quaternionf l = new Quaternionf(face).rotateZ(angle);
        Vector3f t = l.transform(new Vector3f(p.offset));
        p.d.setInterpolationDelay(0);
        p.d.setInterpolationDuration(ticks);
        p.d.setTransformation(new Transformation(t, l, new Vector3f(p.scale), new Quaternionf()));
    }

    public void flap(float amount, int ticks) {
        set(pointer, (float) (Math.PI / 4) + amount, ticks);
    }

    public int segmentAt(float phi) {
        double local = ((-phi) % (Math.PI * 2) + Math.PI * 2) % (Math.PI * 2);
        return Math.min(segments - 1, (int) (local / seg));
    }

    public void highlight(int i, boolean on, org.bukkit.Color color) {
        if (i < 0 || i >= segmentParts.size()) return;
        for (Part p : segmentParts.get(i)) {
            p.d.setGlowing(on);
            if (on) p.d.setGlowColorOverride(color);
            p.d.setBrightness(on ? new Display.Brightness(15, 15) : null);
        }
        if (i < icons.size()) icons.get(i).d.setBrightness(on ? new Display.Brightness(15, 15) : null);
    }

    public void bulbs(int step, int every) {
        for (int k = 0; k < bulbs.size(); k++) {
            boolean lit = every <= 1 || (k + step) % every == 0;
            bulbs.get(k).d.setBrightness(lit ? new Display.Brightness(15, 15) : new Display.Brightness(2, 2));
        }
    }

    public Location point(float angle, float x, float y, float z) {
        Vector3f v = new Quaternionf(face).rotateZ(angle).transform(new Vector3f(x, y, z));
        return center.clone().add(v.x, v.y, v.z);
    }

    public Location iconLocation(int i) {
        return point((i + 0.5f) * seg + phi, 0, radius * 0.66f, 0.15f);
    }

    public Location holoLocation() {
        return center.clone().add(0, radius + 0.85, 0);
    }

    public List<Interaction> hitboxes() {
        return hits;
    }

    public boolean isValid() {
        return hub != null && hub.d.isValid();
    }

    public void remove() {
        for (Part p : parts) p.d.remove();
        for (Entity e : hits) e.remove();
        parts.clear();
        hits.clear();
    }
}
