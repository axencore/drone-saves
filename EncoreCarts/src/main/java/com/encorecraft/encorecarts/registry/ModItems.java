package com.encorecraft.encorecarts.registry;

import com.encorecraft.encorecarts.EncoreCarts;
import com.encorecraft.encorecarts.item.CartItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EncoreCarts.MOD_ID);

    /** No recipe or loot: the cart is obtainable from the creative inventory only and placing it is checked server-side. */
    public static final DeferredItem<CartItem> CART = ITEMS.register("cart",
            () -> new CartItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    private ModItems() {
    }

    public static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES
                || (event.getTabKey() == CreativeModeTabs.OP_BLOCKS && event.hasPermissions())) {
            event.accept(CART.get());
        }
    }
}
