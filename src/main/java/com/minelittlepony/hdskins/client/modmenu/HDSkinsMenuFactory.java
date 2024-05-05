package com.minelittlepony.hdskins.client.modmenu;

import com.minelittlepony.hdskins.client.gui.GuiSkins;
import com.minelittlepony.hdskins.HDSkinsServer;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class HDSkinsMenuFactory implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return s -> GuiSkins.create(s, HDSkinsServer.getInstance().getServers());
    }
}
