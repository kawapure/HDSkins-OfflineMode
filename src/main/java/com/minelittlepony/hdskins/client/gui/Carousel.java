package com.minelittlepony.hdskins.client.gui;

import java.io.Closeable;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.minelittlepony.common.client.gui.ITextContext;
import com.minelittlepony.common.client.gui.dimension.Bounds;
import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.client.gui.player.DummyPlayer;
import com.minelittlepony.hdskins.client.gui.player.DummyPlayerRenderer;
import com.minelittlepony.hdskins.client.gui.player.DummyWorld;
import com.minelittlepony.hdskins.client.gui.player.DummyPlayerRenderer.BedHead;
import com.minelittlepony.hdskins.client.gui.player.skins.PlayerSkins;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;

public class Carousel<T extends PlayerSkins<? extends PlayerSkins.PlayerSkin>> implements Closeable, ITextContext {
    public static final int HOR_MARGIN = 30;
    private static final int TOP = 50;

    private final MinecraftClient minecraft = MinecraftClient.getInstance();

    private final Text title;

    private Optional<DummyPlayer> entity = Optional.empty();
    private final T skins;

    public final Bounds bounds = new Bounds(TOP, HOR_MARGIN, 0, 0);

    private int horizon;
    private float scale;

    public Carousel(Text title, T skins, BiFunction<ClientWorld, PlayerSkins<?>, DummyPlayer> playerFactory) {
        this.title = title;
        this.skins = skins;
        DummyWorld.getOrDummyFuture().thenAcceptAsync(w -> {
            try {
                entity = Optional.of(playerFactory.apply(w, skins));
            } catch (Throwable t) {
                HDSkins.LOGGER.error("Error creating players", t);
            }
        }, MinecraftClient.getInstance());
    }

    public Optional<DummyPlayer> getEntity() {
        return entity;
    }

    public T getSkins() {
        return skins;
    }

    public boolean mouseClicked(int width, int height, double mouseX, double mouseY, int button) {
        if (bounds.contains(mouseX, mouseY)) {
            entity.ifPresent(p -> p.swingHand(button == 0 ? Hand.MAIN_HAND : Hand.OFF_HAND));
            return true;
        }
        return false;
    }

    public void init(GuiSkins screen) {
        horizon = screen.height / 2 + screen.height / 5;
        scale = screen.height / 4F;
    }

    public void render(int mouseX, int mouseY, int ticks, float partialTick, DrawContext context, @Nullable Consumer<DummyPlayer> postAction) {
        context.enableScissor(bounds.left, bounds.top, bounds.right(), bounds.bottom());
        drawBackground(context, bounds.left, bounds.right(), bounds.bottom(), bounds.top, horizon);

        entity.ifPresent(player -> {
            try {
                DummyPlayerRenderer.wrap(() -> {
                    renderPlayerModel(player, bounds.left + bounds.width / 2, bounds.top + bounds.height * 0.9F, scale, horizon - mouseY, mouseX, ticks, partialTick, context, context.getVertexConsumers());

                    if (postAction != null) {
                        postAction.accept(player);
                    }
                });
            } catch (Exception e) {
                HDSkins.LOGGER.error("Exception whilst rendering player preview.", e);
            }
        });
        context.draw();
        context.disableScissor();

        context.getMatrices().push();
        bounds.translate(context.getMatrices());
        drawLabel(context, title, 5, 5, 0xffffff, 0);
        context.getMatrices().pop();
    }

    protected void drawBackground(DrawContext context, int frameLeft, int frameRight, int frameBottom, int frameTop, int horizon) {
        context.fill(        frameLeft, frameTop, frameRight, frameBottom,                        0xA0000000);
        context.fillGradient(frameLeft, horizon,  frameRight, frameBottom, 0x05FFFFFF, 0x40FFFFFF);
    }

    /*
     *       /   |
     *     1/    |o      Q = t + q
     *     /q    |       x = xPosition - mouseX
     *     *-----*       sin(q) = o             cos(q) = x        tan(q) = o/x
     *   --|--x------------------------------------
     *     |
     *      mouseX
     */
    protected void renderPlayerModel(DummyPlayer thePlayer, float xPosition, float yPosition, float scale, float mouseY, float mouseX, int ticks, float partialTick, DrawContext context, VertexConsumerProvider renderContext) {

        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();

        if (dispatcher.getRenderer(thePlayer) == null) {
            HDSkins.LOGGER.warn("Entity " + thePlayer.toString() + " does not have a valid renderer. Did resource loading fail?");
            return;
        }

        float rot = ((ticks + partialTick) * 2.5F) % 360;
        float lookFactor = (float)Math.sin((rot * (Math.PI / 180)) + 45);
        float lookX = (float)Math.atan((xPosition - mouseX) / 20) * -30;

        thePlayer.setHeadYaw(lookX * lookFactor);
        thePlayer.setPitch(thePlayer.isSleeping() ? 10 : (float)Math.atan(mouseY / 40) * -20);

        MatrixStack modelStack = RenderSystem.getModelViewStack();
        modelStack.push();
        modelStack.translate(xPosition, yPosition, 1050);
        modelStack.scale(1, 1, -1);
        RenderSystem.applyModelViewMatrix();

        MatrixStack matrixStack = context.getMatrices();
        matrixStack.push();
        matrixStack.translate(0, 0, 1000);
        matrixStack.scale(scale, scale, scale);

        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15));
        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rot));

        DiffuseLighting.method_34742();

        VertexConsumerProvider.Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

        renderPlayerEntity(matrixStack, thePlayer, immediate, dispatcher);

        immediate.draw();

        matrixStack.pop();
        modelStack.pop();
        RenderSystem.applyModelViewMatrix();
        DiffuseLighting.enableGuiDepthLighting();
    }

    protected void renderPlayerEntity(MatrixStack matrixStack, DummyPlayer thePlayer, VertexConsumerProvider renderContext, EntityRenderDispatcher dispatcher) {
        if (thePlayer.isSleeping()) {
            BedHead.instance.render(thePlayer, matrixStack, renderContext);
        }

        if (thePlayer.hasVehicle()) {
            thePlayer.boat.render(matrixStack, renderContext);
        }

        double offset = thePlayer.getY();

        if (thePlayer.hasVehicle()) {
            offset = thePlayer.getMountedHeightOffset() - thePlayer.getHeight();
        }

        float x = 0;
        float y = 0;
        float z = 0;

        if (thePlayer.isSneaking()) {
            y += 0.125D;
        }

        matrixStack.push();
        matrixStack.translate(0.001, offset, 0.001);

        if (thePlayer.isSleeping()) {
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));

            y += 0.7F;
            x += 1;
        }
        if (thePlayer.isSwimming()) {
            DummyWorld.fillWith(Blocks.WATER.getDefaultState());
            matrixStack.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(45));

            if (thePlayer.getVelocity().x < 100) {
                thePlayer.addVelocity(100, 0, 0);
            }

            y += 0.5F;
            x += 0;
            z += 1;
        } else {
            DummyWorld.fillWith(Blocks.AIR.getDefaultState());

            if (thePlayer.getVelocity().x >= 100) {
                thePlayer.addVelocity(-100, 0, 0);
            }
        }

        Entity camera = minecraft.getCameraEntity();
        minecraft.setCameraEntity(thePlayer);

        dispatcher.render(thePlayer, x, y, z, 0, 1, matrixStack, renderContext, 0xF000F0);

        minecraft.setCameraEntity(camera);

        matrixStack.pop();
    }

    @Override
    public void close() {
        skins.close();
    }
}