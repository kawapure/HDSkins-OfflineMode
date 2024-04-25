package com.minelittlepony.hdskins.client;

import java.util.*;
import java.util.function.Supplier;

import com.minelittlepony.hdskins.client.profile.DynamicSkinTextures;
import com.minelittlepony.hdskins.profile.SkinType;
import com.mojang.authlib.GameProfile;

import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;

public record PlayerSkinLayers (
        Layer vanilla,
        Layer hd,
        Layer combined
) {
    public boolean hasChanged() {
        return vanilla.dynamic().hasChanged() || hd.dynamic().hasChanged() || combined.dynamic().hasChanged();
    }

    public static PlayerSkinLayers of(GameProfile profile, Supplier<SkinTextures> vanillaSkins) {
        var vanilla = new Layer(DynamicSkinTextures.of(vanillaSkins), Memoize.nonExpiring(vanillaSkins));
        var hd = new Layer(
                HDSkins.getInstance().getResourceManager().getSkinTextures(profile)
                .union(HDSkins.getInstance().getProfileRepository().get(profile))
        );
        var combined = new Layer(hd.dynamic().union(vanilla.dynamic()));
        return new PlayerSkinLayers(vanilla, hd, combined);
    }

    public record Layer (DynamicSkinTextures dynamic, Memoize<SkinTextures> resolved) {
        public Layer(DynamicSkinTextures dynamic) {
            this(dynamic, Memoize.withExpiration(() -> DynamicSkinTextures.toSkinTextures(dynamic)));
        }

        public Set<Identifier> getProvidedSkinTypes() {
            return dynamic().getProvidedSkinTypes();
        }

        public Optional<Identifier> getSkin(SkinType type) {
            return dynamic().getSkin(type);
        }

        public String getModel() {
            return Objects.requireNonNullElse(dynamic().getModel(VanillaModels.DEFAULT), VanillaModels.DEFAULT);
        }

        public SkinTextures getSkinTextures() {
            if (dynamic().hasChanged()) {
                resolved.expireNow();
            }
            return resolved.get();
        }
    }
}
