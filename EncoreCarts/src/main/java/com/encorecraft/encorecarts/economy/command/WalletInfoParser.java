package com.encorecraft.encorecarts.economy.command;

import com.encorecraft.encorecarts.economy.Coin;
import com.encorecraft.encorecarts.economy.CoinPurse;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts coin amounts from the free-form text printed by the wallet info command.
 * Labels with an explicit separator win ({@code c=12}, {@code Медные: 12}, {@code Медные монеты: 12});
 * otherwise {@code 12 медных} and {@code Медные 12} are tried and the reading that finds more coins is used.
 * Only horizontal whitespace may separate a label from its number, so entries on different lines never mix.
 */
public final class WalletInfoParser {
    private static final String NUMBER = "(\\d{1,3}(?:[\\u00A0\\u202F' ]\\d{3})+(?!\\d)|\\d+)";
    private static final Pattern FORMATTING_CODES = Pattern.compile("§.");

    private final List<Map<Coin, List<Pattern>>> layouts = new ArrayList<>();

    public WalletInfoParser(Map<Coin, List<String>> aliases) {
        for (int layout = 0; layout < 3; layout++) {
            Map<Coin, List<Pattern>> patterns = new EnumMap<>(Coin.class);
            for (Coin coin : Coin.values()) {
                List<Pattern> list = new ArrayList<>();
                for (String alias : aliases.getOrDefault(coin, List.of())) {
                    String a = alias.trim().toLowerCase(Locale.ROOT);
                    if (a.isEmpty()) {
                        continue;
                    }
                    String word = "(?<![\\p{L}\\p{N}])" + Pattern.quote(a) + (a.length() == 1 ? "" : "\\p{L}*") + "(?![\\p{L}\\p{N}])";
                    String regex = switch (layout) {
                        case 0 -> word + "(?:\\h+\\p{L}+)?\\h*[:=]\\h*" + NUMBER;
                        case 1 -> "(?<![\\p{L}\\p{N}])" + NUMBER + "\\h*" + word;
                        default -> word + "\\h+" + NUMBER;
                    };
                    list.add(Pattern.compile(regex, Pattern.UNICODE_CASE));
                }
                patterns.put(coin, list);
            }
            layouts.add(patterns);
        }
    }

    /** @return the parsed purse, or empty if no coin amount could be found in {@code text} */
    public Optional<CoinPurse> parse(String text) {
        String normalized = FORMATTING_CODES.matcher(text).replaceAll("").toLowerCase(Locale.ROOT);
        long[] explicit = new long[Coin.values().length];
        if (match(layouts.get(0), normalized, explicit) > 0) {
            return Optional.of(purse(explicit));
        }
        // Without separators "a 5 b 6" is ambiguous: keep the reading that explains more coins (number-first on ties).
        long[] numberFirst = new long[Coin.values().length];
        long[] labelFirst = new long[Coin.values().length];
        int numberFirstHits = match(layouts.get(1), normalized, numberFirst);
        int labelFirstHits = match(layouts.get(2), normalized, labelFirst);
        if (numberFirstHits == 0 && labelFirstHits == 0) {
            return Optional.empty();
        }
        return Optional.of(purse(labelFirstHits > numberFirstHits ? labelFirst : numberFirst));
    }

    private static int match(Map<Coin, List<Pattern>> layout, String text, long[] amounts) {
        int hits = 0;
        for (Coin coin : Coin.values()) {
            int bestStart = Integer.MAX_VALUE;
            long bestValue = 0;
            for (Pattern pattern : layout.get(coin)) {
                Matcher matcher = pattern.matcher(text);
                if (matcher.find() && matcher.start() < bestStart) {
                    bestStart = matcher.start();
                    bestValue = parseNumber(matcher.group(1));
                }
            }
            if (bestStart != Integer.MAX_VALUE) {
                amounts[coin.ordinal()] = bestValue;
                hits++;
            }
        }
        return hits;
    }

    private static CoinPurse purse(long[] amounts) {
        return new CoinPurse(amounts[0], amounts[1], amounts[2], amounts[3]);
    }

    private static long parseNumber(String digits) {
        String clean = digits.replaceAll("[^0-9]", "");
        try {
            return Long.parseLong(clean);
        } catch (NumberFormatException e) {
            return Long.MAX_VALUE;
        }
    }
}
