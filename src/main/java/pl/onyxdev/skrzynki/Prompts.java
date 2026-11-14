package pl.onyxdev.skrzynki;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class Prompts implements Listener {
    private record Pending(Consumer<String> then, Runnable cancelled, long expires) {
    }

    private final SunSkrzynki plugin;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    public Prompts(SunSkrzynki plugin) {
        this.plugin = plugin;
    }

    public void ask(Player p, String question, Consumer<String> then, Runnable cancelled) {
        p.closeInventory();
        pending.put(p.getUniqueId(), new Pending(then, cancelled, System.currentTimeMillis() + 120_000));
        Text.send(p, question);
        Text.send(p, "<dark_gray>Napisz na czacie. <gray>anuluj</gray> = przerwij.");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent e) {
        Pending pe = pending.remove(e.getPlayer().getUniqueId());
        if (pe == null || System.currentTimeMillis() > pe.expires()) return;
        e.setCancelled(true);
        String msg = Text.plain(e.message()).trim();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (msg.equalsIgnoreCase("anuluj")) {
                Text.send(e.getPlayer(), "<gray>Anulowano.");
                if (pe.cancelled() != null) pe.cancelled().run();
            } else {
                pe.then().accept(msg);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        pending.remove(e.getPlayer().getUniqueId());
    }
}
