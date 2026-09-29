# CoreMessages

A small Java library for Paper plugins: YAML translations, chat messages, broadcasts, titles, and action bars. Each plugin defines its own language keys and translation files.

**Requirements:** Java 21 and a Paper API compatible with 1.21.8. CoreMessages is a library, not a standalone server plugin.

## Add the dependency

Use [JitPack](https://jitpack.io/) to consume a tagged release from `MrEdgariux/CoreMessages`.

### Gradle (Groovy DSL)

In your plugin's `build.gradle`:

```groovy
repositories {
    mavenCentral()
    maven { url = uri('https://repo.papermc.io/repository/maven-public/') }
    maven { url = uri('https://jitpack.io') }
}

dependencies {
    compileOnly 'io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT'
    implementation 'com.github.MrEdgariux:CoreMessages:1.0.0'
}
```

If your project declares repositories in `settings.gradle`, add JitPack and the Paper repository there instead.

### Maven

In your plugin's `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
    <repository>
        <id>papermc</id>
        <url>https://repo.papermc.io/repository/maven-public/</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.MrEdgariux</groupId>
        <artifactId>CoreMessages</artifactId>
        <version>1.0.0</version>
    </dependency>
    <dependency>
        <groupId>io.papermc.paper</groupId>
        <artifactId>paper-api</artifactId>
        <version>1.21.8-R0.1-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

**Runtime packaging:** Paper does not install Maven artifacts from your build file. Include CoreMessages in your plugin JAR using Gradle Shadow or Maven Shade. Do not bundle `paper-api`. If several plugins may embed different versions, relocate the CoreMessages package to a private package in each plugin. For example, with Gradle Shadow:

```groovy
plugins {
    id 'com.gradleup.shadow' version '8.3.9'
}

tasks.named('shadowJar') {
    archiveClassifier.set('')
    relocate 'lt.mredgariux.messages', 'com.example.myplugin.libs.messages'
}
tasks.named('build') { dependsOn tasks.named('shadowJar') }
```

Use the shaded JAR from `build/libs/` on the server, and replace `com.example.myplugin` with your plugin's package.

## Define your plugin's language keys

Create an enum in **your plugin**, not in this library. Each enum value maps to one YAML path:

```java
package com.example.myplugin;

import lt.mredgariux.messages.language.LanguageKey;

public enum MessageKey implements LanguageKey {
    PREFIX("prefix"),
    GREETING("greeting"),
    COUNT("count"),
    TITLE("title"),
    SUBTITLE("subtitle"),
    STATUS("status");

    private final String path;

    MessageKey(String path) { this.path = path; }

    @Override
    public String key() { return path; }
}
```

Add `src/main/resources/langs/lt.yml` to your plugin JAR. The bundled default file must define a string for **every** key in the enum:

```yaml
prefix: '&bExample &8»&r'
greeting: '&aHello, %0%!'
count: '&e%0% <0:žaidėjas|žaidėjai|žaidėjų>'
title: '&aWelcome'
subtitle: '&7Hello, %0%!'
status: '&#55AAFFReady'
```

## Create and use the managers

Initialize them in your plugin's `onEnable()` and store them as fields so commands and listeners can use them:

```java
import lt.mredgariux.messages.actionbar.ActionBarManager;
import lt.mredgariux.messages.chat.ChatManager;
import lt.mredgariux.messages.language.LanguageManager;
import lt.mredgariux.messages.title.TitleManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class MyPlugin extends JavaPlugin {
    private LanguageManager language;
    private ChatManager chat;
    private TitleManager titles;
    private ActionBarManager actionBars;

    @Override
    public void onEnable() {
        language = new LanguageManager(this, MessageKey.class, "lt");
        language.loadLanguages();

        chat = new ChatManager(language, getServer(), MessageKey.PREFIX);
        titles = new TitleManager(language);
        actionBars = new ActionBarManager(language);
    }
}
```

For example, from a command or listener that has access to these managers and a `Player player`:

```java
chat.sendMessage(player, MessageKey.GREETING, player.getName());
chat.sendMessageNoPrefix(player, MessageKey.COUNT, 3);
chat.sendBroadcast(MessageKey.GREETING, "everyone");

titles.sendTitle(player, MessageKey.TITLE);
titles.sendTitleWithSubtitle(player, MessageKey.TITLE, MessageKey.SUBTITLE, player.getName());
titles.sendTitle(player, MessageKey.TITLE, 10, 70, 20); // timings in ticks

actionBars.sendActionBar(player, MessageKey.STATUS);
```

Pass `null` instead of `MessageKey.PREFIX` to the `ChatManager` constructor if you do not want a prefix. Broadcasts use the default language and have no player context.

## Translation behavior

- `%0%`, `%1%`, and so on insert method arguments.
- `<0:one|few|many>` selects Lithuanian plural forms using numeric argument `0`. A nonnumeric argument leaves the tag unchanged.
- Legacy `&` color codes and hex colors (`&#RRGGBB`, `#RRGGBB`, `#RGB`) are converted to Adventure components when sent.
- `loadLanguages()` copies or updates the bundled default file in your plugin's `langs/` data folder, preserving existing values. Calling it again reloads files from disk.
- Additional `.yml` files in that folder can be selected with `language.setDefaultLanguage("en")` after loading. Missing keys in those files fall back to the bundled language. `language.getInLanguage("en", player, MessageKey.GREETING, player.getName())` retrieves a specific language explicitly. Player overloads otherwise use the default language; per-player language selection is up to your plugin.

### Optional PlaceholderAPI integration

CoreMessages has no direct PlaceholderAPI dependency. If your plugin uses PlaceholderAPI, add its API as a compile-only dependency, declare `softdepend: [PlaceholderAPI]` in your `plugin.yml`, and register a resolver when PlaceholderAPI is enabled:

```java
if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
    language.setPlaceholderResolver(me.clip.placeholderapi.PlaceholderAPI::setPlaceholders);
}
```

Placeholder expansion runs for player-specific messages. Console messages and broadcasts have no player context.

## Publish a release from source

1. Put this project's files at the root of a public GitHub repository named `CoreMessages` under `MrEdgariux`. If the account or repository name differs, change the dependency coordinates shown above.
2. Build with JDK 21 using `gradle build publishToMavenLocal`. The included `jitpack.yml` selects JDK 21 on JitPack. To pin Gradle too, generate and commit a Gradle wrapper, then change `jitpack.yml` to invoke `./gradlew build publishToMavenLocal`.
3. Commit and push the source, create a tag such as `1.0.0`, and push the tag: `git tag 1.0.0 && git push origin 1.0.0`.
4. Look up the repository on [jitpack.io](https://jitpack.io/) and confirm the tagged build succeeds. JitPack will serve the resulting Maven artifact.

For later releases, update `version` in `build.gradle`, tag a new version, and update that version in consuming plugins. Rebuild and redeploy those plugins to use the new code.
