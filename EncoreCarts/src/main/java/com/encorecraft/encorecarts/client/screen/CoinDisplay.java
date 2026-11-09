package com.encorecraft.encorecarts.client.screen;

import com.encorecraft.encorecarts.EncoreCarts;
import com.encorecraft.encorecarts.economy.Coin;
import com.encorecraft.encorecarts.economy.CoinMath;
import com.encorecraft.encorecarts.economy.CoinPurse;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Draws coin amounts as "12 (icon) 30 (icon)" using the platinum/gold/silver/copper icons. */
public final class CoinDisplay {
    private static final ResourceLocation COINS = EncoreCarts.id("textures/gui/coins.png");
    private static final Coin[] ORDER = {Coin.PLATINUM, Coin.GOLD, Coin.SILVER, Coin.COPPER};
    private static final int ICON = 8;
    private static final int GAP = 4;

    private CoinDisplay() {
    }

    public static int width(Font font, long copper) {
        int width = 0;
        CoinPurse coins = CoinMath.fromCopper(Math.max(0, copper));
        boolean any = false;
        for (Coin coin : ORDER) {
            long amount = coins.get(coin);
            if (amount > 0 || (!any && coin == Coin.COPPER)) {
                width += (any ? GAP : 0) + font.width(Long.toString(amount)) + 1 + ICON;
                any = true;
            }
        }
        return width;
    }

    /** @return the drawn width */
    public static int draw(GuiGraphics graphics, Font font, int x, int y, long copper, int textColor) {
        CoinPurse coins = CoinMath.fromCopper(Math.max(0, copper));
        int cursor = x;
        boolean any = false;
        for (Coin coin : ORDER) {
            long amount = coins.get(coin);
            if (amount > 0 || (!any && coin == Coin.COPPER)) {
                if (any) {
                    cursor += GAP;
                }
                String text = Long.toString(amount);
                graphics.drawString(font, text, cursor, y, textColor, true);
                cursor += font.width(text) + 1;
                graphics.blit(COINS, cursor, y, coin.ordinal() * ICON, 0, ICON, ICON, ICON * 4, ICON);
                cursor += ICON;
                any = true;
            }
        }
        return cursor - x;
    }
}
