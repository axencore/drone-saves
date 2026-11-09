package com.encorecraft.encorecarts.client.cutscene.route;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;

/**
 * Blocks of a route scene. They live only in this object (never in a level). The grid repeats along Z with
 * period {@link #LENGTH}, so the road can be drawn as endless copies of one tile and neighbours across the
 * tile border match. Read-only after generation, hence safe to mesh on a worker thread.
 */
public final class SceneBlocks implements BlockAndTintGetter {
    public static final int MIN_X = -48;
    public static final int WIDTH = 96;
    public static final int HEIGHT = 64;
    public static final int LENGTH = 160;

    private final short[] cells = new short[WIDTH * HEIGHT * LENGTH];
    private final List<BlockState> palette = new ArrayList<>();
    private final Object2IntOpenHashMap<BlockState> ids = new Object2IntOpenHashMap<>();
    private final Biome biome;
    private final boolean nether;

    SceneBlocks(Biome biome, boolean nether) {
        this.biome = biome;
        this.nether = nether;
        palette.add(Blocks.AIR.defaultBlockState());
        ids.put(Blocks.AIR.defaultBlockState(), 0);
    }

    private static int index(int x, int y, int z) {
        return (Math.floorMod(z, LENGTH) * HEIGHT + y) * WIDTH + (x - MIN_X);
    }

    static boolean inside(int x, int y) {
        return x >= MIN_X && x < MIN_X + WIDTH && y >= 0 && y < HEIGHT;
    }

    void set(int x, int y, int z, BlockState state) {
        if (!inside(x, y)) {
            return;
        }
        int id = ids.computeIfAbsent(state, s -> {
            palette.add(state);
            return palette.size() - 1;
        });
        cells[index(x, y, z)] = (short) id;
    }

    /** Places only into air, so decorations never overwrite terrain or each other. */
    boolean setIfAir(int x, int y, int z, BlockState state) {
        if (!inside(x, y) || cells[index(x, y, z)] != 0) {
            return false;
        }
        set(x, y, z, state);
        return true;
    }

    public BlockState get(int x, int y, int z) {
        return inside(x, y) ? palette.get(cells[index(x, y, z)]) : Blocks.AIR.defaultBlockState();
    }

    public boolean isAir(int x, int y, int z) {
        return !inside(x, y) || cells[index(x, y, z)] == 0;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return get(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return null;
    }

    @Override
    public float getShade(Direction direction, boolean shade) {
        if (!shade) {
            return nether ? 0.9F : 1.0F;
        }
        return switch (direction) {
            case DOWN -> nether ? 0.9F : 0.5F;
            case UP -> nether ? 0.9F : 1.0F;
            case NORTH, SOUTH -> 0.8F;
            case WEST, EAST -> 0.6F;
        };
    }

    @Override
    public LevelLightEngine getLightEngine() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            throw new IllegalStateException("No client level");
        }
        return minecraft.level.getLightEngine();
    }

    /** The scene is lit by full sky and block light, so it reads well at any time of day. */
    @Override
    public int getBrightness(LightLayer lightType, BlockPos pos) {
        return 15;
    }

    @Override
    public int getRawBrightness(BlockPos pos, int amount) {
        return 15;
    }

    @Override
    public boolean canSeeSky(BlockPos pos) {
        return true;
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver) {
        return resolver.getColor(biome, pos.getX(), pos.getZ());
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public int getMinBuildHeight() {
        return 0;
    }

    /** First solid-ish block hit between two scene points, used to keep cameras out of trees and hills. */
    public boolean blocked(double x0, double y0, double z0, double x1, double y1, double z1) {
        double dx = x1 - x0;
        double dy = y1 - y0;
        double dz = z1 - z0;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int steps = (int) Math.ceil(length / 0.25);
        for (int i = 1; i < steps; i++) {
            double t = (double) i / steps;
            int bx = (int) Math.floor(x0 + dx * t);
            int by = (int) Math.floor(y0 + dy * t);
            int bz = (int) Math.floor(z0 + dz * t);
            BlockState state = get(bx, by, bz);
            if (!state.isAir() && state.canOcclude()) {
                return true;
            }
        }
        return false;
    }
}
