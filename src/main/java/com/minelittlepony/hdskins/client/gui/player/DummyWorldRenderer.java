package com.minelittlepony.hdskins.client.gui.player;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.joml.Matrix4f;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.jukebox.JukeboxSong;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

final class DummyWorldRenderer extends WorldRenderer {

    public static final Supplier<CompletableFuture<DummyWorldRenderer>> FUTURE_INSTANCE = () -> {
        return CompletableFuture.supplyAsync(DummyWorldRenderer::new, MinecraftClient.getInstance());
    };

    public DummyWorldRenderer() {
        super(MinecraftClient.getInstance(),
                MinecraftClient.getInstance().getEntityRenderDispatcher(),
                MinecraftClient.getInstance().getBlockEntityRenderDispatcher(),
                MinecraftClient.getInstance().getBufferBuilders());
    }

    @Override
    public void reload() {
        // noop
    }
    @Override
    public void render(
            RenderTickCounter tickDelta,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightmapTextureManager lightmapTextureManager,
            Matrix4f matrix4f,
            Matrix4f matrix4f2
        ) {
        // noop
    }
    @Override
    public void addBuiltChunk(ChunkBuilder.BuiltChunk chunk) {
        // noop
    }
    @Override
    public void onResized(int width, int height) {
        // noop
    }
    @Override
    public void reloadTransparencyPostProcessor() {
        // noop
    }
    @Override
    protected boolean canDrawEntityOutlines() {
        return false;
    }
    @Override
    public void reload(ResourceManager manager) {
        // noop
    }
    @Override
    public void tickRainSplashing(Camera camera) {
        // noop
    }
    @Override
    public void tick() {
        // noop
    }
    @Override
    public void renderSky(Matrix4f matrix4f, Matrix4f projectionMatrix, float tickDelta, Camera camera, boolean thickFog, Runnable fogCallback) {
        // noop
    }
    @Override
    public void renderClouds(MatrixStack matrices, Matrix4f matrix4f, Matrix4f matrix4f2, float tickDelta, double cameraX, double cameraY, double cameraZ) {
        // noop
    }
    @Override
    public void updateBlock(BlockView world, BlockPos pos, BlockState oldState, BlockState newState, int flags) {
        // noop
    }
    @Override
    public void scheduleBlockRenders(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        // noop
    }
    @Override
    public void scheduleBlockRerenderIfNeeded(BlockPos pos, BlockState old, BlockState updated) {
        // noop
    }
    @Override
    public void scheduleBlockRenders(int x, int y, int z) {
        // noop
    }
    @Override
    public void scheduleBlockRender(int x, int y, int z) {
        // noop
    }
    @Override
    public void playJukeboxSong(RegistryEntry<JukeboxSong> song, BlockPos jukeboxPos) {
        // noop
    }
    @Override
    public void addParticle(ParticleEffect parameters, boolean shouldAlwaysSpawn, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
        // noop
    }
    @Override
    public void addParticle(ParticleEffect parameters, boolean shouldAlwaysSpawn, boolean important, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
        // noop
    }
    @Override
    public void processGlobalEvent(int eventId, BlockPos pos, int data) {
        // noop
    }
    @Override
    public void processWorldEvent(int eventId, BlockPos pos, int data) {
        // noop
    }
    @Override
    public void setBlockBreakingInfo(int entityId, BlockPos pos, int stage) {
        // noop
    }
    @Override
    public void updateNoCullingBlockEntities(Collection<BlockEntity> removed, Collection<BlockEntity> added) {
        // noop
    }
    @Override
    public boolean isRenderingReady(BlockPos pos) {
        return true;
    }
}
