package com.encorecraft.encorecarts.client.compat.xaero;

import com.encorecraft.encorecarts.EncoreCarts;
import com.encorecraft.encorecarts.client.CartsClientConfig;
import com.encorecraft.encorecarts.client.ClientStations;
import com.encorecraft.encorecarts.network.payload.SyncStationsPayload.MapStation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Iterator;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import xaero.lib.client.gui.widget.Tooltip;
import xaero.map.element.render.ElementReader;
import xaero.map.element.render.ElementRenderInfo;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.element.render.ElementRenderProvider;
import xaero.map.element.render.ElementRenderer;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;

/**
 * World map element for stations open to the player: an icon without any label;
 * the station name appears only in Xaero's hover tooltip.
 */
final class StationMapElementRenderer extends ElementRenderer<MapStation, StationMapElementRenderer.Context, StationMapElementRenderer> {
    private static final ResourceLocation ICON = EncoreCarts.id("textures/gui/map_station.png");
    private static final int ICON_SIZE = 16;
    private static final float ICON_SCALE = 1.5F;

    static final class Context {
        @Nullable
        ResourceKey<Level> mapDimension;
    }

    private StationMapElementRenderer(Context context, Provider provider, Reader reader) {
        super(context, provider, reader);
    }

    static StationMapElementRenderer create() {
        return new StationMapElementRenderer(new Context(), new Provider(), new Reader());
    }

    @Override
    public void preRender(ElementRenderInfo renderInfo, MultiBufferSource.BufferSource buffers,
                          MultiTextureRenderTypeRendererProvider rendererProvider, boolean shadow) {
        this.context.mapDimension = renderInfo.mapDimension;
    }

    @Override
    public void postRender(ElementRenderInfo renderInfo, MultiBufferSource.BufferSource buffers,
                           MultiTextureRenderTypeRendererProvider rendererProvider, boolean shadow) {
    }

    @Override
    public void renderElementShadow(MapStation element, boolean hovered, float optionalScale, double partialX, double partialY,
                                    ElementRenderInfo renderInfo, GuiGraphics graphics, MultiBufferSource.BufferSource buffers,
                                    MultiTextureRenderTypeRendererProvider rendererProvider) {
    }

    @Override
    public boolean renderElement(MapStation element, boolean hovered, double optionalDepth, float optionalScale,
                                 double partialX, double partialY, ElementRenderInfo renderInfo, GuiGraphics graphics,
                                 MultiBufferSource.BufferSource buffers, MultiTextureRenderTypeRendererProvider rendererProvider) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(partialX, partialY, 0.0);
        float scale = optionalScale * ICON_SCALE * (hovered ? 1.25F : 1.0F);
        pose.scale(scale, scale, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(ICON, -ICON_SIZE / 2, -ICON_SIZE / 2, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        pose.popPose();
        return false;
    }

    @Override
    public boolean shouldRender(ElementRenderLocation location, boolean shadow) {
        return !shadow && location == ElementRenderLocation.WORLD_MAP && CartsClientConfig.MAP_ICONS.get();
    }

    @Override
    public int getOrder() {
        return 150;
    }

    @Override
    public boolean shouldBeDimScaled() {
        return false;
    }

    static final class Provider extends ElementRenderProvider<MapStation, Context> {
        @Nullable
        private Iterator<MapStation> iterator;

        @Override
        public void begin(ElementRenderLocation location, Context context) {
            iterator = ClientStations.all().iterator();
        }

        @Override
        public boolean hasNext(ElementRenderLocation location, Context context) {
            return iterator != null && iterator.hasNext();
        }

        @Override
        public MapStation getNext(ElementRenderLocation location, Context context) {
            return iterator == null ? null : iterator.next();
        }

        @Override
        public void end(ElementRenderLocation location, Context context) {
            iterator = null;
        }
    }

    static final class Reader extends ElementReader<MapStation, Context, StationMapElementRenderer> {
        private static final int HALF_BOX = Math.round(ICON_SIZE * ICON_SCALE / 2);

        @Override
        public boolean isHidden(MapStation element, Context context) {
            return context.mapDimension == null || !context.mapDimension.location().equals(element.dimension());
        }

        @Override
        public double getRenderX(MapStation element, Context context, float partialTicks) {
            return element.x() + 0.5;
        }

        @Override
        public double getRenderZ(MapStation element, Context context, float partialTicks) {
            return element.z() + 0.5;
        }

        @Override
        public int getInteractionBoxLeft(MapStation element, Context context, float partialTicks) {
            return -HALF_BOX;
        }

        @Override
        public int getInteractionBoxRight(MapStation element, Context context, float partialTicks) {
            return HALF_BOX;
        }

        @Override
        public int getInteractionBoxTop(MapStation element, Context context, float partialTicks) {
            return -HALF_BOX;
        }

        @Override
        public int getInteractionBoxBottom(MapStation element, Context context, float partialTicks) {
            return HALF_BOX;
        }

        @Override
        public int getRenderBoxLeft(MapStation element, Context context, float partialTicks) {
            return -HALF_BOX - 4;
        }

        @Override
        public int getRenderBoxRight(MapStation element, Context context, float partialTicks) {
            return HALF_BOX + 4;
        }

        @Override
        public int getRenderBoxTop(MapStation element, Context context, float partialTicks) {
            return -HALF_BOX - 4;
        }

        @Override
        public int getRenderBoxBottom(MapStation element, Context context, float partialTicks) {
            return HALF_BOX + 4;
        }

        @Override
        public int getLeftSideLength(MapStation element, Minecraft minecraft) {
            return 9 + minecraft.font.width(element.name());
        }

        @Override
        public String getMenuName(MapStation element) {
            return element.name();
        }

        @Override
        public String getFilterName(MapStation element) {
            return element.name();
        }

        @Override
        public int getMenuTextFillLeftPadding(MapStation element) {
            return 0;
        }

        @Override
        public int getRightClickTitleBackgroundColor(MapStation element) {
            return 0xFF6B4A2B;
        }

        @Override
        public boolean shouldScaleBoxWithOptionalScale() {
            return true;
        }

        @Override
        public boolean isInteractable(ElementRenderLocation location, MapStation element) {
            return true;
        }

        @Override
        public Tooltip getTooltip(MapStation element, Context context, boolean overMenu) {
            return new Tooltip(element.name());
        }
    }
}
