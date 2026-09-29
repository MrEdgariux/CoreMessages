package lt.mredgariux.messages.language;

import org.bukkit.entity.Player;

/** Optional bridge to PlaceholderAPI or another placeholder system. */
@FunctionalInterface
public interface PlaceholderResolver {
    String resolve(Player player, String message);
}
