package com.minelittlepony.hdskins.client;

import com.minelittlepony.common.client.gui.element.Button;
import com.minelittlepony.common.event.ScreenInitCallback;
import com.minelittlepony.common.util.GamePaths;
import com.minelittlepony.hdskins.HDSkinsServer;
import com.minelittlepony.hdskins.client.gui.GuiSkins;
import com.minelittlepony.hdskins.client.profile.SkinLoader;
import com.minelittlepony.hdskins.client.resources.EquipmentList;
import com.minelittlepony.hdskins.client.resources.SkinResourceManager;
import com.minelittlepony.hdskins.server.SkinServerList;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class HDSkins implements ClientModInitializer {
    public static final Logger LOGGER = LogManager.getLogger();

    private static HDSkins instance;

    public static HDSkins getInstance() {
        return instance;
    }

    public static Identifier id(String name) {
        return HDSkinsServer.id(name);
    }

    private final HDConfig config = new HDConfig(GamePaths.getConfigDirectory().resolve("hdskins.json"));
    private final EquipmentList equipmentList = new EquipmentList();
    private final SkinResourceManager resources = new SkinResourceManager();
    private final SkinLoader repository = new SkinLoader();

    private final PrioritySorter skinPrioritySorter = new PrioritySorter();

    public HDSkins() {
        instance = this;
    }

    public HDConfig getConfig() {
        return config;
    }

    @Override
    public void onInitializeClient() {
        config.load();
        HDSkinsServer.getInstance().setSessionService(MinecraftClient.getInstance()::getSessionService);
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(resources);
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(HDSkinsServer.getInstance().getServers());
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(equipmentList);
        ScreenInitCallback.EVENT.register(this::onScreenInit);

        FabricLoader.getInstance().getEntrypoints("hdskins", ClientModInitializer.class).forEach(ClientModInitializer::onInitializeClient);
    }

    private void onScreenInit(Screen screen, ScreenInitCallback.ButtonList buttons) {
        if (config.disablePantsButton.get()) {
            return;
        }
        if (screen instanceof TitleScreen) {
            Button button = buttons.addButton(new Button(screen.width - 50, screen.height - 50, 20, 20))
                .onClick(sender -> MinecraftClient.getInstance().setScreen(GuiSkins.create(screen, HDSkinsServer.getInstance().getServers())));
            button.getStyle()
                    .setIcon(new ItemStack(Items.LEATHER_LEGGINGS), 0x3c5dcb)
                    .setTooltip("hdskins.manager", 0, 10);
            button.setY(screen.height - 50); // ModMenu;
        }
    }

    public SkinResourceManager getResourceManager() {
        return resources;
    }

    public SkinLoader getProfileRepository() {
        return repository;
    }

    @Deprecated
    public SkinServerList getSkinServerList() {
        return HDSkinsServer.getInstance().getServers();
    }

    public EquipmentList getDummyPlayerEquipmentList() {
        return equipmentList;
    }

    public PrioritySorter getSkinPrioritySorter() {
        return skinPrioritySorter;
    }
}
