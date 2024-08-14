package com.minelittlepony.hdskins.mixin.client;

import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.minelittlepony.hdskins.client.resources.ImageFilter;

import net.minecraft.resource.ResourceManager;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.PlayerSkinTexture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mixin(PlayerSkinTexture.class)
abstract class MixinPlayerSkinTexture {
    private static final Logger LOGGER = LogManager.getLogger();
    
    @Shadow
    private String url;
    
    @Shadow
    public abstract NativeImage loadTexture(InputStream stream);
    
    @Shadow
    public abstract void onTextureLoaded(NativeImage image);
    
    /*
     * Hook the skin texture loading to allow loading from a file URI
     * (kinda).
     * 
     * This is used for loading skins from the offline server.
     */
    @Inject(method ="load",
            at = @At("INVOKE"),
            cancellable = true)
    private void onLoad1(ResourceManager manager, CallbackInfo info) throws IOException
    {
        // Java won't let me make this truly const ;-;
        String PROTOCOL = "file:///";
        
        if (this.url.substring(0, PROTOCOL.length()).equals(PROTOCOL))
        {
            LOGGER.info("Loading skin file from disk.");
            
            // We're working with a file URI, so override the onload handler to
            // load straight from disk.
            File imageFile = new File(this.url.substring(PROTOCOL.length()));
            
            NativeImage nativeImage;
            FileInputStream stream = new FileInputStream(imageFile);
            
            nativeImage = this.loadTexture(stream);
            
            if (nativeImage != null)
            {
                this.onTextureLoaded(nativeImage);
            }
            
            info.cancel();
        }
    }
    
    @Inject(method ="loadTexture(Ljava/io/InputStream;)Lnet/minecraft/client/texture/NativeImage;",
            at = @At("RETURN"),
            cancellable = true)
    private void onLoad(InputStream stream, CallbackInfoReturnable<NativeImage> info) {
        if (this instanceof ImageFilter) {
            info.setReturnValue(((ImageFilter)this).filterImage(info.getReturnValue()));
        }
    }
}
