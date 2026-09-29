package lt.mredgariux.messages.language;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Loads each plugin's own bundled languages and enum keys. Call loadLanguages on enable/reload. */
public final class LanguageManager implements TranslationProvider {
    private static final Pattern HEX = Pattern.compile("(?<![&\\w])#([0-9a-fA-F]{6}|[0-9a-fA-F]{3})(?![0-9a-fA-F])");
    private static final Pattern PLURAL = Pattern.compile("<(\\d+):([^|]+)\\|([^|]+)\\|([^>]+)>");

    private final JavaPlugin plugin;
    private final Set<String> keys = new HashSet<>();
    private final Map<String, YamlConfiguration> languages = new HashMap<>();
    private final String bundledLanguage;
    private String defaultLanguage;
    private PlaceholderResolver placeholderResolver = (player, message) -> message;

    /** @param keyType your plugin's enum class implementing LanguageKey */
    public <E extends Enum<E> & LanguageKey> LanguageManager(JavaPlugin plugin, Class<E> keyType,
                                                             String bundledLanguage) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(keyType, "keyType");
        this.bundledLanguage = normalize(bundledLanguage);
        this.defaultLanguage = this.bundledLanguage;
        for (E key : keyType.getEnumConstants()) {
            String path = Objects.requireNonNull(key.key(), "key path");
            if (path.isBlank() || !keys.add(path)) throw new IllegalArgumentException("Duplicate/empty key: " + path);
        }
    }

    public void setPlaceholderResolver(PlaceholderResolver resolver) {
        this.placeholderResolver = Objects.requireNonNull(resolver, "resolver");
    }

    /** Copies/merges bundled langs/{bundledLanguage}.yml; reloads disk files and validates default keys. */
    public void loadLanguages() {
        File folder = new File(plugin.getDataFolder(), "langs");
        if (!folder.isDirectory() && !folder.mkdirs()) throw new IllegalStateException("Cannot create " + folder);

        String resource = "langs/" + bundledLanguage + ".yml";
        try (InputStream stream = plugin.getResource(resource)) {
            if (stream == null) throw new IllegalStateException("Missing bundled resource: " + resource);
            YamlConfiguration bundled = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            File file = new File(folder, bundledLanguage + ".yml");
            YamlConfiguration existing = file.isFile() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
            boolean changed = !file.isFile();
            for (String key : bundled.getKeys(true)) {
                if (bundled.isConfigurationSection(key)) continue;
                if (!existing.contains(key)) {
                    existing.set(key, bundled.get(key));
                    changed = true;
                }
            }
            if (changed) existing.save(file);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot update " + resource, e);
        }

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) throw new IllegalStateException("Cannot list " + folder);
        Map<String, YamlConfiguration> loaded = new HashMap<>();
        Arrays.sort(files);
        for (File file : files) {
            String code = normalize(file.getName().substring(0, file.getName().length() - 4));
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            for (String key : config.getKeys(true)) {
                if (!config.isConfigurationSection(key) && !keys.contains(key)) {
                    plugin.getLogger().warning("[" + code + "] Unknown translation key: " + key);
                }
            }
            loaded.put(code, config);
        }
        YamlConfiguration bundled = loaded.get(bundledLanguage);
        for (String key : keys) {
            if (bundled == null || !bundled.isString(key)) {
                throw new IllegalStateException("Missing translation '" + key + "' in " + resource);
            }
        }
        if (!loaded.containsKey(defaultLanguage)) defaultLanguage = bundledLanguage;
        languages.clear();
        languages.putAll(loaded);
    }

    public String getDefaultLanguage() { return defaultLanguage; }

    public void setDefaultLanguage(String code) {
        String normalized = normalize(code);
        if (!languages.containsKey(normalized)) throw new IllegalArgumentException("Language not loaded: " + normalized);
        defaultLanguage = normalized;
    }

    @Override
    public String get(LanguageKey key, Object... args) {
        return format(raw(defaultLanguage, key), null, args);
    }

    @Override
    public String get(Player player, LanguageKey key, Object... args) {
        return format(raw(defaultLanguage, key), player, args);
    }

    /** Explicit language selection; a missing key falls back to the bundled language. */
    public String getInLanguage(String code, Player player, LanguageKey key, Object... args) {
        return format(raw(normalize(code), key), player, args);
    }

    private String raw(String code, LanguageKey key) {
        String path = Objects.requireNonNull(key, "key").key();
        if (!keys.contains(path)) throw new IllegalArgumentException("Unregistered language key: " + path);
        YamlConfiguration fallback = languages.get(bundledLanguage);
        if (fallback == null) throw new IllegalStateException("Call loadLanguages() first");
        YamlConfiguration selected = languages.getOrDefault(code, languages.get(defaultLanguage));
        String value = selected.getString(path);
        return value != null ? value : Objects.requireNonNull(fallback.getString(path));
    }

    private String format(String input, Player player, Object... args) {
        Objects.requireNonNull(args, "args");
        Matcher plural = PLURAL.matcher(input);
        StringBuffer buffer = new StringBuffer();
        while (plural.find()) {
            String replacement = plural.group();
            int index = Integer.parseInt(plural.group(1));
            if (index < args.length) {
                try {
                    long number = Long.parseLong(String.valueOf(args[index]));
                    long lastTwo = Math.abs(number % 100);
                    long last = Math.abs(number % 10);
                    replacement = last == 1 && lastTwo != 11 ? plural.group(2)
                            : last == 0 || lastTwo >= 11 && lastTwo <= 19 ? plural.group(4) : plural.group(3);
                } catch (NumberFormatException ignored) { /* Preserve tag if argument is not an integer. */ }
            }
            plural.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        plural.appendTail(buffer);
        String message = buffer.toString();
        for (int i = 0; i < args.length; i++) message = message.replace("%" + i + "%", String.valueOf(args[i]));
        Matcher hex = HEX.matcher(message);
        StringBuffer colors = new StringBuffer();
        while (hex.find()) {
            String code = hex.group(1);
            if (code.length() == 3) code = "" + code.charAt(0) + code.charAt(0)
                    + code.charAt(1) + code.charAt(1) + code.charAt(2) + code.charAt(2);
            hex.appendReplacement(colors, "&#" + code);
        }
        hex.appendTail(colors);
        return player == null ? colors.toString() : placeholderResolver.resolve(player, colors.toString());
    }

    private static String normalize(String code) {
        Objects.requireNonNull(code, "language code");
        if (!code.matches("[A-Za-z0-9_-]+")) throw new IllegalArgumentException("Invalid language code: " + code);
        return code.toLowerCase(Locale.ROOT);
    }
}
