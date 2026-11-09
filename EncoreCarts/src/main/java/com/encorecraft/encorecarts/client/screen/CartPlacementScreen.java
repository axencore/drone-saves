package com.encorecraft.encorecarts.client.screen;

import com.encorecraft.encorecarts.network.payload.ConfirmPlacementPayload;
import com.encorecraft.encorecarts.network.payload.PlacementResultPayload;
import com.encorecraft.encorecarts.station.StationNames;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Asked when an admin places a cart: station name (required) and whether the station is open to everyone. */
public class CartPlacementScreen extends Screen {
    private static final int PANEL_WIDTH = 260;

    private final int requestId;
    private EditBox nameField;
    private Checkbox globalBox;
    private Button confirmButton;
    private String nameValue = "";
    private boolean globalValue;
    private boolean waiting;
    @Nullable
    private Component error;
    private MultiLineLabel help = MultiLineLabel.EMPTY;

    public CartPlacementScreen(int requestId) {
        super(Component.translatable("screen.encorecarts.placement.title"));
        this.requestId = requestId;
    }

    public int requestId() {
        return requestId;
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = this.height / 2 - 60;
        nameField = new EditBox(this.font, left, top + 20, PANEL_WIDTH, 20, Component.translatable("screen.encorecarts.placement.name"));
        nameField.setMaxLength(StationNames.MAX_LENGTH);
        nameField.setValue(nameValue);
        nameField.setHint(Component.translatable("screen.encorecarts.placement.name_hint").withStyle(ChatFormatting.DARK_GRAY));
        nameField.setResponder(value -> {
            nameValue = value;
            error = null;
            updateButtons();
        });
        addRenderableWidget(nameField);
        setInitialFocus(nameField);

        globalBox = Checkbox.builder(Component.translatable("screen.encorecarts.placement.global"), this.font)
                .pos(left, top + 48)
                .selected(globalValue)
                .tooltip(Tooltip.create(Component.translatable("screen.encorecarts.placement.global.tooltip")))
                .onValueChange((box, value) -> globalValue = value)
                .build();
        addRenderableWidget(globalBox);

        help = MultiLineLabel.create(this.font, Component.translatable("screen.encorecarts.placement.help")
                .withStyle(ChatFormatting.GRAY), PANEL_WIDTH);

        confirmButton = addRenderableWidget(Button.builder(Component.translatable("screen.encorecarts.placement.confirm"), b -> submit())
                .bounds(left, top + 112, PANEL_WIDTH / 2 - 4, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
                .bounds(left + PANEL_WIDTH / 2 + 4, top + 112, PANEL_WIDTH / 2 - 4, 20).build());
        updateButtons();
    }

    private void updateButtons() {
        if (confirmButton != null) {
            confirmButton.active = !waiting && !StationNames.normalize(nameValue).isEmpty();
        }
    }

    private void submit() {
        if (waiting || StationNames.normalize(nameValue).isEmpty()) {
            return;
        }
        waiting = true;
        error = null;
        updateButtons();
        PacketDistributor.sendToServer(new ConfirmPlacementPayload(requestId, nameValue, globalBox.selected()));
    }

    public void onResult(PlacementResultPayload result) {
        waiting = false;
        if (result.success()) {
            onClose();
        } else {
            error = result.message();
            updateButtons();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && nameField.isFocused()) {
            submit();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = this.height / 2 - 60;
        graphics.drawCenteredString(this.font, this.title, this.width / 2, top - 22, 0xFFE8C77A);
        graphics.drawString(this.font, Component.translatable("screen.encorecarts.placement.name"), left, top + 8, 0xFFE0E0E0);
        help.renderLeftAligned(graphics, left, top + 72, 10, 0xFFA0A0A0);
        if (error != null) {
            graphics.drawCenteredString(this.font, error.copy().withStyle(ChatFormatting.RED), this.width / 2, top + 138, 0xFFFF5555);
        } else if (waiting) {
            graphics.drawCenteredString(this.font, Component.translatable("screen.encorecarts.waiting"), this.width / 2, top + 138, 0xFFA0A0A0);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
