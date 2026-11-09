package com.encorecraft.encorecarts.item;

import com.encorecraft.encorecarts.station.PlacementRequests;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Admin-only placement item. The client only plays the swing; the server validates permissions,
 * computes the station layout and asks the admin for the station name.
 */
public class CartItem extends Item {
    public CartItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getClickedFace() != Direction.UP) {
            if (!context.getLevel().isClientSide() && context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(
                        Component.translatable("message.encorecarts.place.top_face").withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }
        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (context.getPlayer() instanceof ServerPlayer player) {
            PlacementRequests.begin(player, context.getClickedPos());
            return InteractionResult.CONSUME;
        }
        return InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.encorecarts.cart.tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.encorecarts.cart.tooltip.remove").withStyle(ChatFormatting.DARK_GRAY));
    }
}
