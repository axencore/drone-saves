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
 * Recognises "c: 12", "c=12", "медные 12" as well as "12c", "12 медных" for every configured alias.
 */
public final class WalletInfoParser {
    private static final String NUMBER = "(\\d{1,3}(?:[\\u00A0\\u202F' ]\\d{3})+|\\d+)";
    private static final Pattern FORMATTING_CODES = Pattern.compile("§.");

    private final Map<Coin, List<Pattern>> patterns = new EnumMap<>(Coin.class);

    public WalletInfoParser(Map<Coin, List<String>> aliases) {
        for (Coin coin : Coin.values()) {
            List<Pattern> list = new ArrayList<>();
            for (String alias : aliases.getOrDefault(coin, List.of())) {
                String a = alias.trim().toLowerCase(Locale.ROOT);
                if (a.isEmpty()) {
                    continue;
                }
                String word = Pattern.quote(a) + (a.length() == 1 ? "" : "\\p{L}*") + "(?![\\p{L}\\p{N}])";
                list.add(Pattern.compile("(?<![\\p{L}\\p{N}])" + word + "\\s*[:=]?\\s*" + NUMBER, Pattern.UNICODE_CASE));
                list.add(Pattern.compile("(?<![\\p{L}\\p{N}])" + NUMBER + "\\s*" + word, Pattern.UNICODE_CASE));
            }
            patterns.put(coin, list);
        }
    }

    /** @return the parsed purse, or empty if no coin amount could be found in {@code text} */
    public Optional<CoinPurse> parse(String text) {
        String normalized = FORMATTING_CODES.matcher(text).replaceAll("").toLowerCase(Locale.ROOT);
        long[] amounts = new long[Coin.values().length];
        boolean found = false;
        for (Coin coin : Coin.values()) {
            int bestStart = Integer.MAX_VALUE;
            long bestValue = 0;
            for (Pattern pattern : patterns.get(coin)) {
                Matcher matcher = pattern.matcher(normalized);
                if (matcher.find() && matcher.start() < bestStart) {
                    bestStart = matcher.start();
                    bestValue = parseNumber(matcher.group(1));
                }
            }
            if (bestStart != Integer.MAX_VALUE) {
                amounts[coin.ordinal()] = bestValue;
                found = true;
            }
        }
        if (!found) {
            return Optional.empty();
        }
        return Optional.of(new CoinPurse(amounts[0], amounts[1], amounts[2], amounts[3]));
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
