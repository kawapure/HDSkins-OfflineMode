package com.minelittlepony.hdskins;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.minelittlepony.hdskins.server.SkinServerList;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

public class HDSkinsServer implements ModInitializer {
    public static final String DEFAULT_NAMESPACE = "hdskins";

    public static final Logger LOGGER = LogManager.getLogger();

    private static HDSkinsServer instance;

    public static HDSkinsServer getInstance() {
        if (instance == null) {
            instance = new HDSkinsServer();
        }
        return instance;
    }

    public static Identifier id(String name) {
        return new Identifier(DEFAULT_NAMESPACE, name);
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
