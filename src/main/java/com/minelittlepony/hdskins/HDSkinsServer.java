package com.minelittlepony.hdskins;

import com.minelittlepony.hdskins.server.SkinServerList;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;

public class HDSkinsServer implements ModInitializer {
    private static HDSkinsServer instance;

    public static HDSkinsServer getInstance() {
        if (instance == null) {
            instance = new HDSkinsServer();
        }
        return instance;
    }

    private final SkinServerList servers = new SkinServerList();

    public HDSkinsServer() {
        instance = this;
    }

    public SkinServerList getServers() {
        return servers;
    }

    @Override
    public void onInitialize() {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(servers);
    }
}
