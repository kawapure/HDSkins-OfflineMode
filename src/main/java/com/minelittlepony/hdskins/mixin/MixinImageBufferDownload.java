package com.minelittlepony.hdskins.mixin;

import net.minecraft.client.texture.ImageFilter;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.SkinRemappingImageFilter;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.minelittlepony.hdskins.HDSkins;
import com.minelittlepony.hdskins.resources.texture.SimpleDrawer;

@Mixin(SkinRemappingImageFilter.class)
public abstract class MixinImageBufferDownload implements ImageFilter {

    @Inject(method = "filterImage(Lnet/minecraft/client/texture/NativeImage;)Lnet/minecraft/client/texture/NativeImage;",
            at = @At("RETURN"),
            cancellable = true)
    private void update(NativeImage image, CallbackInfoReturnable<NativeImage> ci) {
        // convert skins from mojang server
        NativeImage image2 = ci.getReturnValue();
        boolean isLegacy = image.getHeight() == 32;
        if (isLegacy) {
            HDSkins.getInstance().convertSkin((SimpleDrawer)(() -> image2));
        }
    }
}
