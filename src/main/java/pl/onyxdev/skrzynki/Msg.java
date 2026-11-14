package pl.onyxdev.skrzynki;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import pl.onyxdev.suncore.Messages;
import pl.onyxdev.suncore.SunCore;

public final class Msg {
    private Msg() {
    }

    public static final String WIN_CHAT = "skrzynki.wygrana.czat";
    public static final String WIN_TITLE = "skrzynki.wygrana.tytul";
    public static final String LEGEND_BROADCAST = "skrzynki.legenda.ogloszenie";
    public static final String EPIC_BROADCAST = "skrzynki.epik.ogloszenie";
    public static final String BLANK_CHAT = "skrzynki.pech.czat";
    public static final String BLANK_TITLE = "skrzynki.pech.tytul";
    public static final String KEY_RECEIVED = "skrzynki.klucz.otrzymany";
    public static final String NO_KEY = "skrzynki.brak.klucza";
    public static final String BUSY = "skrzynki.zajeta";
    public static final String FREE_SPIN = "skrzynki.kolo.darmowy";
    public static final String PENDING = "skrzynki.zalegla.nagroda";

    public static void register(Plugin p) {
        Messages m = SunCore.messages();
        m.register(p, WIN_CHAT, "Wiadomość na czacie z tym, co wylosowałeś", true, true);
        m.register(p, WIN_TITLE, "Duży napis na ekranie z wygraną", true, true);
        m.register(p, LEGEND_BROADCAST, "Ogłoszenie na cały serwer, gdy ktoś wylosuje legendę", true, true);
        m.register(p, EPIC_BROADCAST, "Ogłoszenie na cały serwer, gdy ktoś wylosuje przedmiot epicki", false, true);
        m.register(p, BLANK_CHAT, "Wiadomość na czacie po trafieniu pola Pech na kole", true, true);
        m.register(p, BLANK_TITLE, "Napis PECH! na ekranie po trafieniu pola Pech", true, true);
        m.register(p, KEY_RECEIVED, "Informacja, że dostałeś klucz lub żeton", true, true);
        m.register(p, NO_KEY, "Komunikat, że do otwarcia potrzebny jest klucz lub żeton", true, false);
        m.register(p, BUSY, "Komunikat, że ktoś właśnie otwiera skrzynię lub kręci kołem", true, false);
        m.register(p, FREE_SPIN, "Informacja o darmowym obrocie koła na dziś", true, true);
        m.register(p, PENDING, "Odebranie nagrody wylosowanej, gdy cię nie było", true, false);
    }

    public static void chat(Player p, String key, Component c) {
        SunCore.messages().send(p, key, c);
    }

    public static void chat(Player p, String key, String mini) {
        SunCore.messages().send(p, key, Text.mm(Text.PREFIX + mini));
    }

    public static void title(Player p, String key, Title t) {
        SunCore.messages().title(p, key, t);
    }

    public static void broadcast(String key, Component c) {
        SunCore.messages().broadcast(key, c);
    }
}
