package com.minelittlepony.hdskins.client;

import java.util.Optional;

import com.minelittlepony.hdskins.client.ducks.ClientPlayerInfo;
import net.minecraft.client.network.AbstractClientPlayerEntity;

public record PlayerSkins(PlayerSkinLayers layers, PlayerSkinLayers.Layer sorted) {
    public static Optional<PlayerSkins> of(AbstractClientPlayerEntity player) {
        return ClientPlayerInfo.of(player).map(ClientPlayerInfo::getSkins);
    }
}
