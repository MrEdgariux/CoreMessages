package lt.mredgariux.messages.title;

import lt.mredgariux.messages.language.LanguageKey;
import lt.mredgariux.messages.language.TranslationProvider;
import lt.mredgariux.messages.text.LegacyText;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Objects;

public final class TitleManager {
    private final TranslationProvider translations;

    public TitleManager(TranslationProvider translations) {
        this.translations = Objects.requireNonNull(translations, "translations");
    }

    public void sendTitle(Player player, LanguageKey key, Object... args) {
        player.showTitle(Title.title(LegacyText.parse(translations.get(player, key, args)), Component.empty()));
    }

    public void sendTitleWithSubtitle(Player player, LanguageKey titleKey, LanguageKey subtitleKey, Object... args) {
        player.showTitle(Title.title(LegacyText.parse(translations.get(player, titleKey, args)),
                LegacyText.parse(translations.get(player, subtitleKey, args))));
    }

    /** Times are in Minecraft ticks (50 ms per tick). */
    public void sendTitle(Player player, LanguageKey key, int fadeIn, int stay, int fadeOut, Object... args) {
        player.showTitle(Title.title(LegacyText.parse(translations.get(player, key, args)), Component.empty(),
                times(fadeIn, stay, fadeOut)));
    }

    /** Times are in Minecraft ticks (50 ms per tick). */
    public void sendTitleWithSubtitle(Player player, LanguageKey titleKey, LanguageKey subtitleKey,
                                      int fadeIn, int stay, int fadeOut, Object... args) {
        player.showTitle(Title.title(LegacyText.parse(translations.get(player, titleKey, args)),
                LegacyText.parse(translations.get(player, subtitleKey, args)), times(fadeIn, stay, fadeOut)));
    }

    private static Title.Times times(int fadeIn, int stay, int fadeOut) {
        if (fadeIn < 0 || stay < 0 || fadeOut < 0) throw new IllegalArgumentException("Title ticks must be non-negative");
        return Title.Times.times(Duration.ofMillis(fadeIn * 50L),
                Duration.ofMillis(stay * 50L), Duration.ofMillis(fadeOut * 50L));
    }
}
