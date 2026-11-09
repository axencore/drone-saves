package com.encorecraft.encorecarts.client.cutscene;

import com.encorecraft.encorecarts.EncoreCarts;
import com.encorecraft.encorecarts.client.CartsClientConfig;
import com.encorecraft.encorecarts.network.payload.CutsceneReadyPayload;
import com.encorecraft.encorecarts.network.payload.StartCutscenePayload;
import javax.annotation.Nullable;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client side of a trip, seen only by the traveller:
 * fade to black → cart shot (one of ten camera compositions) → fade to black → wait for the server's teleport and
 * for the destination terrain → black fades away with the player standing at the destination.
 */
public final class CutsceneController {
    private enum Phase { IDLE, FADE_OUT, SCENE_IN, SCENE, SCENE_OUT, WAIT_ARRIVAL, FADE_IN }

    private static final int FADE_OUT_TICKS = 16;
    private static final int SCENE_IN_TICKS = 8;
    private static final int SCENE_TICKS = 50;
    private static final int SCENE_OUT_TICKS = 12;
    private static final int FADE_IN_TICKS = 18;
    private static final int MIN_WAIT_TICKS = 3;
    private static final int MAX_WAIT_TICKS = 140;
    private static final int SCENE_TOTAL_TICKS = SCENE_IN_TICKS + SCENE_TICKS + SCENE_OUT_TICKS;
    private static final ResourceLocation OVERLAY_LAYER = EncoreCarts.id("travel_cutscene");

    private static Phase phase = Phase.IDLE;
    private static int phaseTicks;
    private static int sceneTicks;
    @Nullable
    private static StartCutscenePayload data;
    @Nullable
    private static CutsceneScene scene;
    @Nullable
    private static CameraPose lastPose;
    @Nullable
    private static CameraType savedCameraType;
    private static boolean arrived;

    private CutsceneController() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(CutsceneController::onClientTick);
        bus.addListener(CutsceneController::onRenderLevel);
        bus.addListener(CutsceneController::onCameraAngles);
        bus.addListener(CutsceneController::onFov);
        bus.addListener(CutsceneController::onGuiLayer);
        bus.addListener(CutsceneController::onRenderHand);
        bus.addListener(CutsceneController::onHighlight);
        bus.addListener(CutsceneController::onNameTag);
        bus.addListener(CutsceneController::onMovementInput);
        bus.addListener(CutsceneController::onInteractionKey);
    }

    public static void start(StartCutscenePayload payload) {
        abort();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        data = payload;
        arrived = false;
        minecraft.setScreen(null);
        savedCameraType = minecraft.options.getCameraType();
        minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        enter(Phase.FADE_OUT);
    }

    public static void onArrived(int sessionId) {
        if (data != null && data.sessionId() == sessionId) {
            arrived = true;
        }
    }

    public static void abort() {
        if (phase != Phase.IDLE) {
            finish();
        }
    }

    public static boolean isActive() {
        return phase != Phase.IDLE;
    }

    private static boolean sceneVisible() {
        return phase == Phase.SCENE_IN || phase == Phase.SCENE || phase == Phase.SCENE_OUT;
    }

    /** The real station cart/coachman are hidden while their cutscene doubles drive away. */
    public static boolean isHidden(Entity entity) {
        return sceneVisible() && data != null && entity.getId() >= 0
                && (entity.getId() == data.cartEntityId() || entity.getId() == data.coachmanEntityId());
    }

    /** Called from {@code CameraMixin} at the end of {@code Camera#setup}. */
    @Nullable
    public static CameraPose cameraPose(float partialTick) {
        if (!sceneVisible() || scene == null) {
            lastPose = null;
            return null;
        }
        double progress = Mth.clamp((sceneTicks + partialTick) / SCENE_TOTAL_TICKS, 0, 1);
        lastPose = scene.camera(partialTick, progress);
        return lastPose;
    }

    private static void enter(Phase next) {
        phase = next;
        phaseTicks = 0;
    }

    private static void finish() {
        Minecraft minecraft = Minecraft.getInstance();
        if (savedCameraType != null) {
            minecraft.options.setCameraType(savedCameraType);
        }
        savedCameraType = null;
        scene = null;
        lastPose = null;
        data = null;
        phase = Phase.IDLE;
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        if (phase == Phase.IDLE) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || data == null) {
            finish();
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        phaseTicks++;
        switch (phase) {
            case FADE_OUT -> {
                if (phaseTicks >= FADE_OUT_TICKS) {
                    int forced = CartsClientConfig.FORCED_SHOT.get();
                    CameraShots.Shot shot = CameraShots.get(forced >= 0 ? forced : data.shot());
                    scene = new CutsceneScene(minecraft.level, data, shot);
                    sceneTicks = 0;
                    enter(Phase.SCENE_IN);
                }
            }
            case SCENE_IN, SCENE, SCENE_OUT -> {
                sceneTicks++;
                if (scene != null) {
                    scene.tick();
                }
                if (phase == Phase.SCENE_IN && phaseTicks >= SCENE_IN_TICKS) {
                    enter(Phase.SCENE);
                } else if (phase == Phase.SCENE && phaseTicks >= SCENE_TICKS) {
                    enter(Phase.SCENE_OUT);
                } else if (phase == Phase.SCENE_OUT && phaseTicks >= SCENE_OUT_TICKS) {
                    scene = null;
                    PacketDistributor.sendToServer(new CutsceneReadyPayload(data.sessionId()));
                    enter(Phase.WAIT_ARRIVAL);
                }
            }
            case WAIT_ARRIVAL -> {
                if ((arrived && phaseTicks >= MIN_WAIT_TICKS && terrainReady(minecraft)) || phaseTicks >= MAX_WAIT_TICKS) {
                    enter(Phase.FADE_IN);
                }
            }
            case FADE_IN -> {
                if (phaseTicks >= FADE_IN_TICKS) {
                    finish();
                }
            }
            default -> {
            }
        }
    }

    private static boolean terrainReady(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return false;
        }
        int cx = player.blockPosition().getX() >> 4;
        int cz = player.blockPosition().getZ() >> 4;
        return minecraft.level.getChunkSource().hasChunk(cx, cz) && minecraft.levelRenderer.isSectionCompiled(player.blockPosition());
    }

    private static float progress(float partialTick, int length) {
        return Mth.clamp((phaseTicks + partialTick) / length, 0.0F, 1.0F);
    }

    private static float smooth(float x) {
        return x * x * (3 - 2 * x);
    }

    private static float blackAlpha(float partialTick) {
        return switch (phase) {
            case FADE_OUT -> smooth(progress(partialTick, FADE_OUT_TICKS));
            case SCENE_IN -> 1 - smooth(progress(partialTick, SCENE_IN_TICKS));
            case SCENE_OUT -> smooth(progress(partialTick, SCENE_OUT_TICKS));
            case WAIT_ARRIVAL -> 1;
            case FADE_IN -> 1 - smooth(progress(partialTick, FADE_IN_TICKS));
            default -> 0;
        };
    }

    private static float letterbox(float partialTick) {
        if (!CartsClientConfig.LETTERBOX.get()) {
            return 0;
        }
        return switch (phase) {
            case SCENE_IN -> smooth(progress(partialTick, SCENE_IN_TICKS));
            case SCENE, SCENE_OUT -> 1;
            default -> 0;
        };
    }

    /** GUI layer drawn above everything else (also with F1 pressed). */
    public static void renderOverlay(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (phase == Phase.IDLE || data == null) {
            return;
        }
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        float bars = letterbox(partialTick);
        if (bars > 0) {
            int barHeight = Math.round(height * 0.11F * bars);
            graphics.fill(0, 0, width, barHeight, 0xFF000000);
            graphics.fill(0, height - barHeight, width, height, 0xFF000000);
        }
        float alpha = blackAlpha(partialTick);
        if (alpha > 0.002F) {
            graphics.fill(0, 0, width, height, ((int) (alpha * 255) << 24));
        }
        float titleAlpha = switch (phase) {
            case WAIT_ARRIVAL -> Math.min(1, (phaseTicks + partialTick) / 6.0F);
            case FADE_IN -> 1 - smooth(progress(partialTick, FADE_IN_TICKS));
            default -> 0;
        };
        if (titleAlpha > 0.02F) {
            Minecraft minecraft = Minecraft.getInstance();
            int a = Mth.clamp((int) (titleAlpha * 255), 5, 255) << 24;
            Component title = Component.literal(data.destinationName());
            graphics.pose().pushPose();
            graphics.pose().translate(width / 2.0F, height / 2.0F - 14, 0);
            graphics.pose().scale(2.0F, 2.0F, 1.0F);
            graphics.drawCenteredString(minecraft.font, title, 0, 0, 0xE8C77A | a);
            graphics.pose().popPose();
            graphics.drawCenteredString(minecraft.font, Component.translatable("screen.encorecarts.cutscene.arrival"),
                    width / 2, height / 2 + 10, 0xC0C0C0 | a);
        }
    }

    private static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES && sceneVisible() && scene != null) {
            scene.render(event.getPoseStack(), Minecraft.getInstance().renderBuffers().bufferSource(),
                    event.getCamera().getPosition(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
        }
    }

    private static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (sceneVisible() && lastPose != null) {
            event.setRoll(lastPose.roll());
        }
    }

    private static void onFov(ViewportEvent.ComputeFov event) {
        if (sceneVisible() && lastPose != null && event.usedConfiguredFov()) {
            event.setFOV(lastPose.fov());
        }
    }

    private static void onGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (sceneVisible() && !OVERLAY_LAYER.equals(event.getName())) {
            event.setCanceled(true);
        }
    }

    private static void onRenderHand(RenderHandEvent event) {
        if (phase != Phase.IDLE && phase != Phase.FADE_IN) {
            event.setCanceled(true);
        }
    }

    private static void onHighlight(RenderHighlightEvent.Block event) {
        if (sceneVisible()) {
            event.setCanceled(true);
        }
    }

    private static void onNameTag(RenderNameTagEvent event) {
        if (sceneVisible()) {
            event.setCanRender(TriState.FALSE);
        }
    }

    private static void onMovementInput(MovementInputUpdateEvent event) {
        if (phase != Phase.IDLE && phase != Phase.FADE_IN) {
            Input input = event.getInput();
            input.forwardImpulse = 0;
            input.leftImpulse = 0;
            input.up = false;
            input.down = false;
            input.left = false;
            input.right = false;
            input.jumping = false;
            input.shiftKeyDown = false;
        }
    }

    private static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (phase != Phase.IDLE && phase != Phase.FADE_IN) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }
}
