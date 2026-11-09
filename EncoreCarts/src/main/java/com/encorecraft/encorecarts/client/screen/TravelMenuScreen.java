package com.encorecraft.encorecarts.client.screen;

import com.encorecraft.encorecarts.network.payload.RequestTravelPayload;
import com.encorecraft.encorecarts.network.payload.TravelMenuPayload;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Coachman menu: destinations open to the player in this dimension with distance and price.
 * Unaffordable destinations cannot be chosen; the server re-validates the choice anyway.
 */
public class TravelMenuScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final long DOUBLE_CLICK_MS = 300;

    private TravelMenuPayload data;
    private DestinationList list;
    private Button travelButton;
    @Nullable
    private Component notice;
    private boolean waiting;
    @Nullable
    private UUID selectedId;

    public TravelMenuScreen(TravelMenuPayload data) {
        super(Component.translatable("screen.encorecarts.travel.title", data.originName()));
        this.data = data;
        this.notice = data.notice().orElse(null);
    }

    public UUID originId() {
        return data.originId();
    }

    public void update(TravelMenuPayload newData) {
        this.data = newData;
        this.notice = newData.notice().orElse(null);
        this.waiting = false;
        rebuildEntries();
        updateButton();
    }

    private int panelWidth() {
        return Math.min(380, this.width - 20);
    }

    @Override
    protected void init() {
        int panel = panelWidth();
        int left = (this.width - panel) / 2;
        int listTop = 56;
        int listBottom = this.height - 40;
        list = new DestinationList(this.minecraft, panel, Math.max(ROW_HEIGHT, listBottom - listTop), listTop);
        list.setX(left);
        addRenderableWidget(list);
        rebuildEntries();

        travelButton = addRenderableWidget(Button.builder(Component.translatable("screen.encorecarts.travel.go"), b -> travel())
                .bounds(left, this.height - 30, panel / 2 - 4, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(left + panel / 2 + 4, this.height - 30, panel / 2 - 4, 20).build());
        updateButton();
    }

    private void rebuildEntries() {
        if (list == null) {
            return;
        }
        list.replaceAll(data);
        DestinationEntry keep = selectedId == null ? null : list.children().stream()
                .filter(e -> e.destination.id().equals(selectedId) && e.destination.affordable()).findFirst().orElse(null);
        if (keep == null) {
            selectedId = null;
        }
        list.setSelected(keep);
    }

    private void updateButton() {
        if (travelButton == null) {
            return;
        }
        DestinationEntry selected = list == null ? null : list.getSelected();
        travelButton.active = !waiting && selected != null && selected.destination.affordable();
        if (selected != null && !selected.destination.affordable()) {
            travelButton.setTooltip(Tooltip.create(Component.translatable("screen.encorecarts.travel.not_enough")));
        } else {
            travelButton.setTooltip(null);
        }
    }

    private void select(DestinationEntry entry) {
        selectedId = entry.destination.id();
        updateButton();
    }

    private void travel() {
        DestinationEntry selected = list.getSelected();
        if (waiting || selected == null || !selected.destination.affordable()) {
            return;
        }
        waiting = true;
        notice = Component.translatable("screen.encorecarts.waiting").withStyle(ChatFormatting.GRAY);
        updateButton();
        PacketDistributor.sendToServer(new RequestTravelPayload(data.originId(), selected.destination.id()));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int panel = panelWidth();
        int left = (this.width - panel) / 2;
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFE8C77A);

        Component walletLabel = Component.translatable("screen.encorecarts.travel.wallet");
        graphics.drawString(this.font, walletLabel, left, 26, 0xFFD0D0D0);
        int x = left + this.font.width(walletLabel) + 6;
        if (data.balanceKnown()) {
            CoinDisplay.draw(graphics, this.font, x, 26, data.balance(), 0xFFFFFFFF);
        } else {
            graphics.drawString(this.font, Component.translatable("screen.encorecarts.travel.wallet_unknown")
                    .withStyle(ChatFormatting.RED), x, 26, 0xFFFF5555);
        }
        Component rate = Component.translatable("screen.encorecarts.travel.rate");
        int rateWidth = this.font.width(rate) + 4 + CoinDisplay.width(this.font, data.copperPer100Blocks());
        int rateX = left + panel - rateWidth;
        graphics.drawString(this.font, rate, rateX, 26, 0xFF909090);
        CoinDisplay.draw(graphics, this.font, rateX + this.font.width(rate) + 4, 26, data.copperPer100Blocks(), 0xFFB0B0B0);

        if (notice != null) {
            graphics.drawCenteredString(this.font, notice, this.width / 2, 40, 0xFFFFFFFF);
        }
        if (data.destinations().isEmpty()) {
            graphics.drawCenteredString(this.font, Component.translatable("screen.encorecarts.travel.empty")
                    .withStyle(ChatFormatting.GRAY), this.width / 2, 56 + 20, 0xFFA0A0A0);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private final class DestinationList extends ObjectSelectionList<DestinationEntry> {
        DestinationList(Minecraft minecraft, int width, int height, int y) {
            super(minecraft, width, height, y, ROW_HEIGHT);
        }

        void replaceAll(TravelMenuPayload payload) {
            clearEntries();
            payload.destinations().forEach(d -> addEntry(new DestinationEntry(d)));
            setScrollAmount(getScrollAmount());
        }

        @Override
        public int getRowWidth() {
            return this.width - 16;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getX() + this.width - 6;
        }

        @Override
        public void setSelected(@Nullable DestinationEntry entry) {
            if (entry != null && !entry.destination.affordable()) {
                return;
            }
            super.setSelected(entry);
            if (entry != null) {
                select(entry);
            }
        }

        @Override
        @Nullable
        protected DestinationEntry nextEntry(ScreenDirection direction) {
            return nextEntry(direction, entry -> entry.destination.affordable());
        }
    }

    private final class DestinationEntry extends ObjectSelectionList.Entry<DestinationEntry> {
        final TravelMenuPayload.Destination destination;
        private long lastClick;

        DestinationEntry(TravelMenuPayload.Destination destination) {
            this.destination = destination;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovering, float partialTick) {
            boolean affordable = destination.affordable();
            int nameColor = affordable ? 0xFFFFFFFF : 0xFF808080;
            int textY = top + (height - 8) / 2;
            graphics.drawString(font, destination.name(), left + 4, textY, nameColor, true);
            Component distance = Component.translatable("screen.encorecarts.travel.distance", destination.distance());
            int priceWidth = CoinDisplay.width(font, destination.price());
            int priceX = left + width - priceWidth - 6;
            graphics.drawString(font, distance, priceX - font.width(distance) - 12, textY, 0xFFA0A0A0, true);
            CoinDisplay.draw(graphics, font, priceX, textY, destination.price(), affordable ? 0xFFFFE08A : 0xFFFF5555);
            if (hovering && !affordable) {
                TravelMenuScreen.this.setTooltipForNextRenderPass(Component.translatable("screen.encorecarts.travel.not_enough"));
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!destination.affordable()) {
                return true;
            }
            long now = Util.getMillis();
            boolean doubleClick = list.getSelected() == this && now - lastClick < DOUBLE_CLICK_MS;
            lastClick = now;
            list.setSelected(this);
            if (doubleClick) {
                travel();
            }
            return true;
        }

        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", destination.name());
        }
    }
}
