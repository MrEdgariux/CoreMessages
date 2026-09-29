package lt.mredgariux.messages.chat;

import lt.mredgariux.messages.language.LanguageKey;
import lt.mredgariux.messages.language.TranslationProvider;
import lt.mredgariux.messages.text.LegacyText;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

/** Chat messages and server broadcasts only. */
public final class ChatManager {
    private final TranslationProvider translations;
    private final Server server;
    private final LanguageKey prefixKey;

    /** Pass null for prefixKey if the plugin does not use a prefix. */
    public ChatManager(TranslationProvider translations, Server server, LanguageKey prefixKey) {
        this.translations = Objects.requireNonNull(translations, "translations");
        this.server = Objects.requireNonNull(server, "server");
        this.prefixKey = prefixKey;
    }

    public void sendMessage(CommandSender sender, LanguageKey key, Object... args) {
        Objects.requireNonNull(sender, "sender");
        String message = sender instanceof Player player
                ? translations.get(player, key, args) : translations.get(key, args);
        sender.sendMessage(withPrefix(message));
    }

    public void sendMessageNoPrefix(CommandSender sender, LanguageKey key, Object... args) {
        Objects.requireNonNull(sender, "sender");
        String message = sender instanceof Player player
                ? translations.get(player, key, args) : translations.get(key, args);
        sender.sendMessage(LegacyText.parse(message));
    }

    public void sendBroadcast(LanguageKey key, Object... args) {
        server.sendMessage(withPrefix(translations.get(key, args)));
    }

    public void sendBroadcastNoPrefix(LanguageKey key, Object... args) {
        server.sendMessage(LegacyText.parse(translations.get(key, args)));
    }

    private Component withPrefix(String message) {
        if (prefixKey == null) return LegacyText.parse(message);
        String prefix = translations.get(prefixKey);
        if (prefix.isBlank()) return LegacyText.parse(message);
        // Separate components so formatting in the prefix cannot bleed into the message.
        return LegacyText.parse(prefix + " ").append(LegacyText.parse(message));
    }
}
