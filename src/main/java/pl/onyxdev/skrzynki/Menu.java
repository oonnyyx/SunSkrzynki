package pl.onyxdev.skrzynki;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class Menu implements InventoryHolder {
    protected final SunSkrzynki plugin;
    protected final Player viewer;
    private final Inventory inv;
    private final Map<Integer, Consumer<InventoryClickEvent>> buttons = new HashMap<>();

    protected Menu(SunSkrzynki plugin, Player viewer, int rows, Component title) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.inv = Bukkit.createInventory(this, rows * 9, title);
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    protected abstract void render();

    public void open() {
        redraw();
        viewer.openInventory(inv);
    }

    public void redraw() {
        inv.clear();
        buttons.clear();
        render();
    }

    protected void set(int slot, ItemStack item, Consumer<InventoryClickEvent> onClick) {
        inv.setItem(slot, item);
        if (onClick != null) buttons.put(slot, onClick);
        else buttons.remove(slot);
    }

    protected void fill(int from, int to) {
        ItemStack pane = Text.item(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = from; i <= to; i++) if (inv.getItem(i) == null) inv.setItem(i, pane);
    }

    protected boolean allowBottom() {
        return false;
    }

    protected void clickTop(InventoryClickEvent e) {
    }

    protected void shiftFromBottom(InventoryClickEvent e) {
    }

    public void handle(InventoryClickEvent e) {
        boolean top = e.getClickedInventory() == inv;
        if (!top) {
            if (e.isShiftClick()) {
                e.setCancelled(true);
                shiftFromBottom(e);
            } else if (e.getClick() == org.bukkit.event.inventory.ClickType.DOUBLE_CLICK) {
                e.setCancelled(true);
            } else {
                e.setCancelled(!allowBottom());
            }
            return;
        }
        e.setCancelled(true);
        Consumer<InventoryClickEvent> b = buttons.get(e.getSlot());
        if (b != null) {
            click();
            b.accept(e);
        } else {
            clickTop(e);
        }
    }

    protected void click() {
        viewer.playSound(viewer.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.4f);
    }

    protected void ok() {
        viewer.playSound(viewer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 1.6f);
    }

    protected void deny() {
        viewer.playSound(viewer.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
    }

    protected ItemStack back() {
        return Text.item(Material.ARROW).name("<gray>← Wróć").build();
    }
}
