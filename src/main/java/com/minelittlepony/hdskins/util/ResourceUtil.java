package com.minelittlepony.hdskins.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

public interface ResourceUtil {
    static Stream<Resource> streamAllResources(ResourceManager manager, ResourceType type, Identifier path) {
        return manager.streamResourcePacks().flatMap(pack -> {
            List<Resource> resources = new ArrayList<>();
            pack.findResources(type, path.getNamespace(), path.getPath(), (id, stream) -> {
                resources.add(new Resource(pack, stream));
            });
            return resources.stream();
        });
    }
}
