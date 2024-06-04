package com.minelittlepony.hdskins.profile;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.TypeAdapter;
import com.minelittlepony.common.util.registry.RegistryTypeAdapter;
import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.common.util.registry.Registries;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registry;

public class SkinType implements Comparable<SkinType> {
    public static final SkinType UNKNOWN = new SkinType(HDSkins.id("unknown"), ItemStack.EMPTY, false);
    public static final Registry<SkinType> REGISTRY = Registries.createDefaulted(HDSkins.id("skin_type"), SkinType::getId, UNKNOWN);

    private static final TypeAdapter<SkinType> ADAPTER = RegistryTypeAdapter.of(REGISTRY, (ls, registry) -> {
        return registry.stream().filter(type -> type.getParameterizedName().equalsIgnoreCase(ls)).findFirst().orElseGet(() -> createUnsupported(ls));
    });
    private static final Map<MinecraftProfileTexture.Type, SkinType> VANILLA = new EnumMap<>(MinecraftProfileTexture.Type.class);

    public static final SkinType SKIN = forVanilla(MinecraftProfileTexture.Type.SKIN, new ItemStack(Items.LEATHER_CHESTPLATE));
    public static final SkinType CAPE = forVanilla(MinecraftProfileTexture.Type.CAPE, new ItemStack(Items.BARRIER));
    public static final SkinType ELYTRA = forVanilla(MinecraftProfileTexture.Type.ELYTRA, new ItemStack(Items.ELYTRA));

    private final Identifier id;
    private final ItemStack iconStack;
    private final Identifier icon;

    private final boolean unsupported;

    protected SkinType(Identifier id, ItemStack iconStack, boolean unsupported) {
        this.id = id;
        this.icon = getId().withPath(p -> "textures/gui/skin_type/" + p + ".png");
        this.iconStack = iconStack;
        this.unsupported = unsupported;
    }

    public Identifier icon() {
        return icon;
    }

    public ItemStack iconStack() {
        return iconStack;
    }

    public String name() {
        return getId().toString();
    }

    @Deprecated
    public String getParameterizedName() {
        return name().replace(':', '_').replace('/', '_').toLowerCase(Locale.US);
    }

    public String getPathName() {
        return getId().getNamespace() + "/" + getId().getPath();
    }

    public final Identifier getId() {
        return id;
    }

    public final int ordinal() {
        return REGISTRY.getRawId(this);
    }

    public boolean isKnown() {
        return this != UNKNOWN;
    }

    public boolean isVanilla() {
        return getEnum().isPresent();
    }

    public boolean isUnsupported() {
        return unsupported;
    }

    public Optional<MinecraftProfileTexture.Type> getEnum() {
        return Optional.empty();
    }

    @Override
    public final boolean equals(Object other) {
        return other instanceof SkinType && compareTo((SkinType)other) == 0;
    }

    @Override
    public final int compareTo(SkinType o) {
        return getId().compareTo(o.getId());
    }

    @Override
    public String toString() {
        return getId().toString();
    }

    @Override
    public final int hashCode() {
        return getId().hashCode();
    }

    public static TypeAdapter<SkinType> adapter() {
        return ADAPTER;
    }

    public static Stream<SkinType> values() {
        return REGISTRY.stream();
    }

    private static SkinType createUnsupported(String parameterizedName) {
        Identifier id = deParameterize(parameterizedName);
        return Registry.register(REGISTRY, id, new SkinType(id, Items.BARRIER.getDefaultStack(), true));
    }

    private static Identifier deParameterize(String parameterizedName) {
        String[] parts = parameterizedName.split("_", 2);
        parts[1] = parts[1].replace('_', '/');
        return Identifier.of(parts[0], parts[1]);
    }

    public static SkinType register(Identifier id, ItemStack iconStack) {
        return Registry.register(REGISTRY, id, new SkinType(id, iconStack, false));
    }

    public static SkinType forVanilla(MinecraftProfileTexture.Type vanilla) {
        return VANILLA.getOrDefault(vanilla, UNKNOWN);
    }

    public static SkinType forVanilla(MinecraftProfileTexture.Type vanilla, ItemStack iconStack) {
        return VANILLA.computeIfAbsent(vanilla, v -> new VanillaType(vanilla, iconStack));
    }

    public static <T> Map<SkinType, T> convertMap(Map<MinecraftProfileTexture.Type, T> textures) {
        return textures.entrySet()
                .stream()
                .collect(Collectors.toMap(e -> forVanilla(e.getKey()), Map.Entry::getValue));
    }

    private static final class VanillaType extends SkinType {
        private final Optional<MinecraftProfileTexture.Type> vanilla;

        VanillaType(MinecraftProfileTexture.Type vanilla, ItemStack iconStack) {
            super(Identifier.ofVanilla(vanilla.name().toLowerCase(Locale.US)), iconStack, false);
            this.vanilla = Optional.of(vanilla);
            Registry.register(REGISTRY, getId(), this);
        }

        @Override
        public String name() {
            return vanilla.get().name();
        }

        @Override
        public Optional<MinecraftProfileTexture.Type> getEnum() {
            return vanilla;
        }
    }
}
