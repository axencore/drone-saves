package com.encorecraft.encorecarts.client.model.bb;

import com.mojang.blaze3d.platform.NativeImage;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

/** A model texture. The image is decoded off-thread and uploaded to the GPU on reload. */
public final class BbTexture {
    public final String name;
    public final float uvWidth;
    public final float uvHeight;
    @Nullable
    NativeImage image;
    boolean translucent;
    private ResourceLocation location = MissingTextureAtlasSprite.getLocation();

    BbTexture(String name, float uvWidth, float uvHeight, @Nullable NativeImage image) {
        this.name = name;
        this.uvWidth = uvWidth;
        this.uvHeight = uvHeight;
        setImage(image);
    }

    /** Replaces the decoded image (e.g. a skin override), closing the previous one. */
    public void replaceImage(NativeImage newImage) {
        if (this.image != null && this.image != newImage) {
            this.image.close();
        }
        setImage(newImage);
    }

    void setImage(@Nullable NativeImage newImage) {
        this.image = newImage;
        this.translucent = false;
        if (newImage == null) {
            return;
        }
        for (int y = 0; y < newImage.getHeight() && !translucent; y++) {
            for (int x = 0; x < newImage.getWidth(); x++) {
                int alpha = newImage.getPixelRGBA(x, y) >>> 24;
                if (alpha > 0 && alpha < 255) {
                    translucent = true;
                    break;
                }
            }
        }
    }

    @Nullable
    public NativeImage image() {
        return image;
    }

    public boolean translucent() {
        return translucent;
    }

    public ResourceLocation location() {
        return location;
    }

    public void setLocation(ResourceLocation location) {
        this.location = location;
    }
}
