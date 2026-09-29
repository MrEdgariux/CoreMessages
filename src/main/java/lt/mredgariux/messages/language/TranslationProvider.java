package lt.mredgariux.messages.language;

import org.bukkit.entity.Player;

public interface TranslationProvider {
    String get(LanguageKey key, Object... args);
    String get(Player player, LanguageKey key, Object... args);
}
