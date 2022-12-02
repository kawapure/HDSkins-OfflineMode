package com.minelittlepony.hdskins.client.resources;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import com.minelittlepony.hdskins.client.HDSkins;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

public class TextureLoader {
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();

    /**
     * Schedule texture loading on the main thread.
     * @param textureLocation
     * @param texture
     */
    public static void loadTexture(final Identifier textureLocation, final AbstractTexture texture) {
        CLIENT.execute(() -> {
            RenderSystem.recordRenderCall(() -> {
                CLIENT.getTextureManager().registerTexture(textureLocation, texture);
            });
        });
    }

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private final Function<NativeImage, NativeImage> filter;

    private final String id;

    public TextureLoader(String id, Function<NativeImage, NativeImage> filter) {
        this.id = id;
        this.filter = filter;
    }

    public void stop() {
        executor.shutdownNow();
        executor = Executors.newSingleThreadExecutor();
    }

    public CompletableFuture<Identifier> loadAsync(Identifier imageId) {
        return CompletableFuture.supplyAsync(() -> getImage(imageId), executor)
        .thenApplyAsync(loaded -> loaded
                .flatMap(image -> Optional.ofNullable(filter.apply(image))
                .filter(i -> i != null && i != image)), CLIENT)
        .thenApplyAsync(updated -> {
            return updated.map(image -> {
                Identifier convertedId = new Identifier(imageId.getNamespace(), "dynamic/" + id + "/" + imageId.getPath());
                CLIENT.getTextureManager().registerTexture(convertedId, new NativeImageBackedTexture(image));
                return convertedId;
            }).orElse(imageId);
        }, CLIENT).exceptionally(t -> {
            HDSkins.LOGGER.warn("Errored while processing {}. Using original.", imageId, t);
            return imageId;
        });
    }

    @Nullable
    private Optional<NativeImage> getImage(Identifier res) {
        return CLIENT.getResourceManager().getResource(res).map(resource -> {
            try (InputStream in = resource.getInputStream()) {
                return NativeImage.read(in);
            } catch (IOException e) {
                HDSkins.LOGGER.warn("Errored while reading image file ({}): {}.", res, e);
            }
            return null;
        });
    }
}
