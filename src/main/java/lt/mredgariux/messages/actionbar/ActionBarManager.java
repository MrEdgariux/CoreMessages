package lt.mredgariux.messages.actionbar;

import lt.mredgariux.messages.language.LanguageKey;
import lt.mredgariux.messages.language.TranslationProvider;
import lt.mredgariux.messages.text.LegacyText;
import org.bukkit.entity.Player;

import java.util.Objects;

public final class ActionBarManager {
    private final TranslationProvider translations;

    public ActionBarManager(TranslationProvider translations) {
        this.translations = Objects.requireNonNull(translations, "translations");
    }

    public void sendActionBar(Player player, LanguageKey key, Object... args) {
        player.sendActionBar(LegacyText.parse(translations.get(player, key, args)));
    }
}
