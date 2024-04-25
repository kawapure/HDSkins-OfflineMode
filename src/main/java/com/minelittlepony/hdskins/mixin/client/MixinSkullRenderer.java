package com.minelittlepony.hdskins.mixin.client;

import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.client.profile.SkinLoader.ProvidedSkins;
import com.minelittlepony.hdskins.profile.SkinType;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.util.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import org.jetbrains.annotations.Nullable;

@Mixin(SkullBlockEntityRenderer.class)
abstract class MixinSkullRenderer implements BlockEntityRenderer<SkullBlockEntity> {
    @Inject(method = "getRenderLayer(Lnet/minecraft/block/SkullBlock$SkullType;Lnet/minecraft/component/type/ProfileComponent;)Lnet/minecraft/client/render/RenderLayer;",
            cancellable = true,
            at = @At(value = "HEAD"))
    private static void onGetSkullTexture(SkullBlock.SkullType type, @Nullable ProfileComponent profile, CallbackInfoReturnable<RenderLayer> info) {
        if (type == SkullBlock.Type.PLAYER && profile != null) {
            @Nullable
            Identifier skin = HDSkins.getInstance().getProfileRepository().load(profile.gameProfile()).getNow(ProvidedSkins.EMPTY).skins().get(SkinType.SKIN);

            if (skin != null) {
                info.setReturnValue(RenderLayer.getEntityTranslucent(skin));
            }
        }
    }
}
