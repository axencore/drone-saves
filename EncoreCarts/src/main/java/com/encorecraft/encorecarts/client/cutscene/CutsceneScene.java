package com.encorecraft.encorecarts.client.cutscene;

import com.encorecraft.encorecarts.client.CartsClientConfig;
import com.encorecraft.encorecarts.client.model.CartsModels;
import com.encorecraft.encorecarts.client.model.ModelDefinition;
import com.encorecraft.encorecarts.client.render.CartRenderer;
import com.encorecraft.encorecarts.client.render.CoachmanRenderer;
import com.encorecraft.encorecarts.entity.CartEntity;
import com.encorecraft.encorecarts.entity.CoachmanEntity;
import com.encorecraft.encorecarts.network.payload.StartCutscenePayload;
import com.encorecraft.encorecarts.registry.ModEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.phys.Vec3;

/**
 * Client-only actors of the cart shot (cart, horse, seated coachman and the traveller's look-alike), moved along a
 * {@link CutscenePath}. None of them is added to the level, so nobody else can see or interact with them.
 */
final class CutsceneScene {
    private static final double SEATED_PLAYER_DROP = 0.62;
    private static final Vec3 DEFAULT_DRIVER_SEAT = new Vec3(0, 1.25, 1.2);
    private static final Vec3 DEFAULT_PASSENGER_SEAT = new Vec3(0.25, 1.0, -0.31);

    private final ClientLevel level;
    private final CutscenePath path;
    private final CameraShots.Shot shot;
    private final CartEntity cart;
    @Nullable
    private final Horse horse;
    private final CoachmanEntity driver;
    @Nullable
    private final CutscenePlayer passenger;
    @Nullable
    private final ModelDefinition cartDef;
    private final double driverDrop;
    private int tick;
    private double distance;
    private double distanceO;

    CutsceneScene(ClientLevel level, StartCutscenePayload data, CameraShots.Shot shot) {
        this.level = level;
        this.shot = shot;
        this.path = CutscenePath.build(level, new Vec3(data.x(), data.y(), data.z()), data.yaw());
        this.cartDef = CartsModels.get(CartsModels.CART);

        cart = new CartEntity(ModEntities.CART.get(), level);
        cart.setId(-1_000_001);
        cart.cutsceneActor = true;
        if (level.getEntity(data.cartEntityId()) instanceof CartEntity real) {
            // Same UUID → same horse coat as the station's own cart.
            cart.setUUID(real.getUUID());
        }
        horse = cartDef != null && cartDef.horse ? CartRenderer.horseFor(cart) : null;
        if (horse != null) {
            horse.setId(-1_000_002);
        }

        driver = new CoachmanEntity(ModEntities.COACHMAN.get(), level);
        driver.setId(-1_000_003);
        driver.cutsceneSeated = true;
        if (level.getEntity(data.coachmanEntityId()) instanceof CoachmanEntity real) {
            driver.setSlim(real.isSlim());
        }
        ModelDefinition driverDef = CoachmanRenderer.definition(driver);
        driverDrop = driverDef == null ? 0.3 : driverDef.seatHeight;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            passenger = new CutscenePlayer(level, minecraft.player);
            passenger.setId(-1_000_004);
            passenger.setSeat(cart);
        } else {
            passenger = null;
        }
        distance = distanceO = shot.distance(0);
        place(distance);
        place(distance);
    }

    void tick() {
        tick++;
        distanceO = distance;
        distance = shot.distance(tick);
        place(distance);
        playSounds();
    }

    private void place(double s) {
        Vec3 pos = path.position(s);
        float yaw = path.yaw();
        float pitch = path.pitch(s);
        move(cart, pos, yaw);
        cart.tickCount = tick;
        cart.renderPitchO = cart.renderPitch;
        cart.renderPitch = pitch;
        cart.wheelSpinO = cart.wheelSpin;
        float radius = cartDef == null ? 0.5F : Math.max(0.1F, cartDef.wheelRadius);
        cart.wheelSpin = (float) (s / radius);

        double speed = Math.abs(distance - distanceO);
        if (horse != null && cartDef != null) {
            Vec3 offset = cartDef.horseOffset;
            Vec3 hp = path.position(s + offset.z);
            Vec3 side = CartRenderer.localOffset(yaw, new Vec3(offset.x, 0, 0));
            moveLiving(horse, new Vec3(hp.x + side.x, hp.y + offset.y, hp.z + side.z), yaw);
            horse.walkAnimation.update((float) Math.min(1.0, speed * 4.5), 0.4F);
            horse.tickCount = tick;
        }

        Vec3 driverSeat = seatWorld(pos, yaw, pitch, cartDef == null ? DEFAULT_DRIVER_SEAT : cartDef.seat("driver", DEFAULT_DRIVER_SEAT));
        moveLiving(driver, driverSeat.subtract(0, driverDrop, 0), yaw);
        driver.yHeadRot = yaw + (float) (6 * Math.sin(tick * 0.05));
        driver.tickCount = tick;

        if (passenger != null) {
            Vec3 seat = seatWorld(pos, yaw, pitch, cartDef == null ? DEFAULT_PASSENGER_SEAT : cartDef.seat("passenger", DEFAULT_PASSENGER_SEAT));
            moveLiving(passenger, seat.subtract(0, SEATED_PLAYER_DROP, 0), yaw);
            passenger.yHeadRot = yaw + (float) (25 * Math.sin(tick * 0.035));
            passenger.setXRot((float) (6 + 4 * Math.sin(tick * 0.05)));
            passenger.xCloakO = passenger.xCloak;
            passenger.yCloakO = passenger.yCloak;
            passenger.zCloakO = passenger.zCloak;
            passenger.xCloak = passenger.getX() - passenger.getLookAngle().x * 0.1;
            passenger.yCloak = passenger.getY();
            passenger.zCloak = passenger.getZ() - passenger.getLookAngle().z * 0.1;
            passenger.tickCount = tick;
        }
    }

    private static Vec3 seatWorld(Vec3 cartPos, float yaw, float pitch, Vec3 seat) {
        double p = Math.toRadians(pitch);
        double up = seat.y * Math.cos(p) + seat.z * Math.sin(p);
        double forward = seat.z * Math.cos(p) - seat.y * Math.sin(p);
        return cartPos.add(CartRenderer.localOffset(yaw, new Vec3(seat.x, up, forward)));
    }

    private static void move(Entity entity, Vec3 pos, float yaw) {
        entity.xo = entity.xOld = entity.getX();
        entity.yo = entity.yOld = entity.getY();
        entity.zo = entity.zOld = entity.getZ();
        entity.setPos(pos.x, pos.y, pos.z);
        entity.yRotO = entity.getYRot();
        entity.setYRot(yaw);
        entity.xRotO = entity.getXRot();
    }

    private static void moveLiving(LivingEntity entity, Vec3 pos, float yaw) {
        move(entity, pos, yaw);
        entity.yBodyRotO = entity.yBodyRot;
        entity.yBodyRot = yaw;
        entity.yHeadRotO = entity.yHeadRot;
        entity.yHeadRot = yaw;
    }

    private void playSounds() {
        if (!CartsClientConfig.CUTSCENE_SOUNDS.get()) {
            return;
        }
        Entity source = horse != null ? horse : cart;
        if (tick % 6 == 0) {
            level.playLocalSound(source.getX(), source.getY(), source.getZ(), SoundEvents.HORSE_STEP, SoundSource.NEUTRAL,
                    0.45F, 0.85F + level.random.nextFloat() * 0.2F, false);
        }
        if (tick % 23 == 5) {
            level.playLocalSound(cart.getX(), cart.getY(), cart.getZ(), SoundEvents.WOOD_STEP, SoundSource.NEUTRAL,
                    0.35F, 0.6F, false);
        }
    }

    CameraPose camera(float partialTick, double progress) {
        double s = Mth.lerp(partialTick, distanceO, distance);
        Vec3 cartPos = path.position(s);
        CameraShots.Context context = new CameraShots.Context(level, path, cartPos, path.forward(), path.right(), s,
                tick + partialTick, progress);
        return shot.pose(context);
    }

    void render(PoseStack poseStack, MultiBufferSource.BufferSource buffers, Vec3 camera, float partialTick) {
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        renderActor(dispatcher, cart, poseStack, buffers, camera, partialTick);
        if (horse != null) {
            renderActor(dispatcher, horse, poseStack, buffers, camera, partialTick);
        }
        renderActor(dispatcher, driver, poseStack, buffers, camera, partialTick);
        if (passenger != null) {
            renderActor(dispatcher, passenger, poseStack, buffers, camera, partialTick);
        }
        buffers.endBatch();
    }

    private static void renderActor(EntityRenderDispatcher dispatcher, Entity entity, PoseStack poseStack,
                                    MultiBufferSource buffers, Vec3 camera, float partialTick) {
        Vec3 pos = lerp(entity, partialTick);
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        int light = dispatcher.getPackedLightCoords(entity, partialTick);
        dispatcher.render(entity, pos.x - camera.x, pos.y - camera.y, pos.z - camera.z, yaw, partialTick, poseStack, buffers, light);
    }

    private static Vec3 lerp(Entity entity, float partialTick) {
        return new Vec3(Mth.lerp(partialTick, entity.xo, entity.getX()), Mth.lerp(partialTick, entity.yo, entity.getY()),
                Mth.lerp(partialTick, entity.zo, entity.getZ()));
    }
}
