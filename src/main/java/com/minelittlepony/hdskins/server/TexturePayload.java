package com.minelittlepony.hdskins.server;

import java.util.*;

import com.minelittlepony.hdskins.profile.SkinType;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

public record TexturePayload (
    long timestamp,
    UUID profileId,
    String profileName,
    boolean isPublic,
    Map<SkinType, MinecraftProfileTexture> textures
) {
    public TexturePayload(GameProfile profile, Map<SkinType, MinecraftProfileTexture> textures) {
        this(System.currentTimeMillis(), profile.getId(), profile.getName(), true, Map.copyOf(textures));
    }
}
