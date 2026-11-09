package com.encorecraft.encorecarts.registry;

import com.encorecraft.encorecarts.EncoreCarts;
import com.encorecraft.encorecarts.entity.CartEntity;
import com.encorecraft.encorecarts.entity.CoachmanEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, EncoreCarts.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<CartEntity>> CART = ENTITY_TYPES.register("cart",
            () -> EntityType.Builder.<CartEntity>of(CartEntity::new, MobCategory.MISC)
                    .sized(2.0F, 2.0F)
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .fireImmune()
                    .build(EncoreCarts.MOD_ID + ":cart"));

    public static final DeferredHolder<EntityType<?>, EntityType<CoachmanEntity>> COACHMAN = ENTITY_TYPES.register("coachman",
            () -> EntityType.Builder.<CoachmanEntity>of(CoachmanEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .eyeHeight(1.62F)
                    .clientTrackingRange(10)
                    .fireImmune()
                    .build(EncoreCarts.MOD_ID + ":coachman"));

    private ModEntities() {
    }

    public static void onCreateAttributes(EntityAttributeCreationEvent event) {
        event.put(COACHMAN.get(), CoachmanEntity.createAttributes().build());
    }
}
