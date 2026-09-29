package lt.mredgariux.messages.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class LegacyText {
    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.builder()
            .character('&').hexColors().build();

    private LegacyText() { }

    public static Component parse(String message) {
        return SERIALIZER.deserialize(message);
    }
}
