package com.minelittlepony.hdskins.client.gui.player.skins;

import java.util.function.Supplier;

import com.minelittlepony.hdskins.profile.SkinType;

import net.minecraft.util.Identifier;

public class PreviousServerPlayerSkins extends PlayerSkins<PlayerSkins.PlayerSkin> {

    private final PlayerSkins<?> fallback;
    private final ServerPlayerSkins.Skin skin;

    public PreviousServerPlayerSkins(PlayerSkins<?> fallback, ServerPlayerSkins.Skin skin) {
        super(Posture.NULL);
        this.fallback = fallback;
        this.skin = skin;
    }

    @Override
    protected PlayerSkin createTexture(SkinType type, Supplier<Identifier> blank) {
        if (!isProvided(type)) {
            return fallback.createTexture(type, blank);
        }
        return skin;
    }

    @Override
    protected boolean isProvided(SkinType type) {
        return getPosture().getActiveSkinType() == type;
    }

    @Override
    public String getSkinVariant() {
        return fallback.getSkinVariant();
    }
}
