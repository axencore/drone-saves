package com.encorecraft.encorecarts.station;

import java.util.Locale;
import java.util.Optional;
import net.minecraft.network.chat.Component;

/** Station name rules: trimmed, single spaces, 1..32 printable characters, unique case-insensitively. */
public final class StationNames {
    public static final int MAX_LENGTH = 32;
    /** Upper bound accepted on the wire; real validation happens in {@link #validate(String)}. */
    public static final int MAX_WIRE_LENGTH = 128;

    private StationNames() {
    }

    public static String normalize(String raw) {
        StringBuilder sb = new StringBuilder(raw.length());
        boolean lastSpace = true;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '§' || Character.isISOControl(c)) {
                continue;
            }
            if (Character.isWhitespace(c)) {
                if (!lastSpace) {
                    sb.append(' ');
                    lastSpace = true;
                }
            } else {
                sb.append(c);
                lastSpace = false;
            }
        }
        return sb.toString().trim();
    }

    public static Optional<Component> validate(String normalized) {
        if (normalized.isEmpty()) {
            return Optional.of(Component.translatable("message.encorecarts.name.empty"));
        }
        if (normalized.length() > MAX_LENGTH) {
            return Optional.of(Component.translatable("message.encorecarts.name.too_long", MAX_LENGTH));
        }
        return Optional.empty();
    }

    public static String key(String name) {
        return normalize(name).toLowerCase(Locale.ROOT);
    }
}
