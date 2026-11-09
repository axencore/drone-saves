package com.encorecraft.encorecarts.economy.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.encorecraft.encorecarts.economy.Coin;
import com.encorecraft.encorecarts.economy.CoinPurse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class WalletInfoParserTest {
    /** Same defaults as CartsConfig [economy.infoAliases]. */
    private static final WalletInfoParser PARSER = new WalletInfoParser(Map.of(
            Coin.COPPER, List.of("c", "медн", "медь", "бронз", "copper", "bronze"),
            Coin.SILVER, List.of("i", "серебр", "silver"),
            Coin.GOLD, List.of("g", "золот", "gold"),
            Coin.PLATINUM, List.of("p", "платин", "platinum")));

    @Test
    void parsesTestWalletOutput() {
        assertEquals(Optional.of(new CoinPurse(5, 3, 0, 1)),
                PARSER.parse("[EncoreCarts test wallet] Dev: c=5, i=3, g=0, p=1"));
    }

    @Test
    void parsesRussianLabels() {
        assertEquals(Optional.of(new CoinPurse(12, 3, 7, 1)),
                PARSER.parse("Кошелёк игрока Steve:\nМедные: 12\nСеребряные: 3\nЗолотые: 7\nПлатиновые: 1"));
        assertEquals(Optional.of(new CoinPurse(12, 3, 7, 1)),
                PARSER.parse("Медь: 12 | Серебро: 3 | Золото: 7 | Платина: 1"));
        assertEquals(Optional.of(new CoinPurse(40, 2, 0, 0)),
                PARSER.parse("У игрока 40 медных и 2 серебряных монеты"));
        assertEquals(Optional.of(new CoinPurse(9, 0, 0, 0)), PARSER.parse("Бронзовые монеты: 9"));
        assertEquals(Optional.of(new CoinPurse(5, 6, 0, 0)), PARSER.parse("Медные 5 Серебряные 6"));
    }

    @Test
    void parsesEnglishLabelsFormattingCodesAndGrouping() {
        assertEquals(Optional.of(new CoinPurse(1200, 0, 5, 0)),
                PARSER.parse("§6Gold§r: 5 | §cCopper§r: 1 200"));
    }

    @Test
    void playerNameDoesNotLookLikeACoin() {
        assertEquals(Optional.of(new CoinPurse(4, 0, 0, 0)), PARSER.parse("Gold_Digger: c=4"));
    }

    @Test
    void unparsableOutputIsEmpty() {
        assertEquals(Optional.empty(), PARSER.parse("Unknown or incomplete command"));
    }
}
