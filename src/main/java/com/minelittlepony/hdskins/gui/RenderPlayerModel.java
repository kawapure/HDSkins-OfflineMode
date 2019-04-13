package com.minelittlepony.hdskins.gui;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.model.ModelElytra;
import net.minecraft.client.renderer.entity.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.ModelBiped;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityBed;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import java.util.Set;

import static net.minecraft.client.renderer.GlStateManager.*;

public class RenderPlayerModel<M extends EntityPlayerModel> extends RenderLivingBase<M> {

    /**
     * The basic Elytra texture.
     */
    protected final ResourceLocation TEXTURE_ELYTRA = new ResourceLocation("textures/entity/elytra.png");

    private static final ModelPlayer FAT = new ModelPlayer(0, false);
    private static final ModelPlayer THIN = new ModelPlayer(0, true);

    public RenderPlayerModel(RenderManager renderer) {
        super(renderer, FAT, 0);
        this.addLayer(this.getElytraLayer());
    }

    protected LayerRenderer<? extends EntityLivingBase> getElytraLayer() {
        final ModelElytra modelElytra = new ModelElytra();
        return new LayerRenderer<EntityLivingBase>() {
            @Override
            public void render(EntityLivingBase entityBase, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
                EntityPlayerModel entity = (EntityPlayerModel) entityBase;
                ItemStack itemstack = entity.getItemStackFromSlot(EntityEquipmentSlot.CHEST);

                if (itemstack.getItem() == Items.ELYTRA) {
                    GlStateManager.color4f(1, 1, 1, 1);
                    GlStateManager.enableBlend();
                    GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

                    bindTexture(entity.getLocal(Type.ELYTRA).getTexture());

                    GlStateManager.pushMatrix();
                    GlStateManager.translatef(0, 0, 0.125F);

                    modelElytra.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
                    modelElytra.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);

                    GlStateManager.disableBlend();
                    GlStateManager.popMatrix();
                }
            }

            @Override
            public boolean shouldCombineTextures() {
                return false;
            }
        };
    }

    @Override
    protected ResourceLocation getEntityTexture(M entity) {
        return entity.getLocal(Type.SKIN).getTexture();
    }

    @Override
    protected boolean canRenderName(M entity) {
        return Minecraft.getInstance().player != null && super.canRenderName(entity);
    }

    @Override
    protected boolean setBrightness(M entity, float partialTicks, boolean combineTextures) {
        return Minecraft.getInstance().world != null && super.setBrightness(entity, partialTicks, combineTextures);
    }

    public ModelPlayer getEntityModel(M entity) {
        return entity.usesThinSkin() ? THIN : FAT;
    }

    @Override
    public void doRender(M entity, double x, double y, double z, float entityYaw, float partialTicks) {

        if (entity.isPlayerSleeping()) {
            BedHead.instance.render(entity);
        }

        if (entity.isPassenger()) {
            MrBoaty.instance.render();
        }

        ModelPlayer player = getEntityModel(entity);
        mainModel = player;

        Set<EnumPlayerModelParts> parts = Minecraft.getInstance().gameSettings.getModelParts();
        player.bipedHeadwear.isHidden = !parts.contains(EnumPlayerModelParts.HAT);
        player.bipedBodyWear.isHidden = !parts.contains(EnumPlayerModelParts.JACKET);
        player.bipedLeftLegwear.isHidden = !parts.contains(EnumPlayerModelParts.LEFT_PANTS_LEG);
        player.bipedRightLegwear.isHidden = !parts.contains(EnumPlayerModelParts.RIGHT_PANTS_LEG);
        player.bipedLeftArmwear.isHidden = !parts.contains(EnumPlayerModelParts.LEFT_SLEEVE);
        player.bipedRightArmwear.isHidden = !parts.contains(EnumPlayerModelParts.RIGHT_SLEEVE);
        player.isSneak = entity.isSneaking();

        player.leftArmPose = ModelBiped.ArmPose.EMPTY;
        player.rightArmPose = ModelBiped.ArmPose.EMPTY;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        double offset = entity.getYOffset() + entity.posY;

        if (entity.isPassenger()) {
            offset = entity.getMountedYOffset() - entity.height;
        }

        if (entity.isPlayerSleeping()) {
            y--;
            z += 0.75F;
        } else if (player.isSneak) {
            y -= 0.125D;
        }

        pushMatrix();
        enableBlend();
        color4f(1, 1, 1, 0.3F);
        translated(0, offset, 0);

        if (entity.isPlayerSleeping()) {
            rotatef(-90, 1, 0, 0);
        }

        super.doRender(entity, x, y, z, entityYaw, partialTicks);

        color4f(1, 1, 1, 1);
        disableBlend();
        popMatrix();
        GL11.glPopAttrib();

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        pushMatrix();
        scalef(1, -1, 1);
        translated(0.001, offset, 0.001);

        if (entity.isPlayerSleeping()) {
            rotatef(-90, 1, 0, 0);
        }

        super.doRender(entity, x, y, z, entityYaw, partialTicks);
        popMatrix();
        GL11.glPopAttrib();
    }

    static class BedHead extends TileEntityBed {
        public static BedHead instance = new BedHead(Blocks.RED_BED.getDefaultState());

        public IBlockState state;

        public BedHead(IBlockState state) {
            this.state = state;
        }

        @Override
        public IBlockState getBlockState() {
            return state;
        }

        public void render(Entity entity) {
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            pushMatrix();

            scalef(-1, -1, -1);

            TileEntityRendererDispatcher dispatcher = TileEntityRendererDispatcher.instance;

            dispatcher.prepare(entity.getEntityWorld(), Minecraft.getInstance().getTextureManager(), Minecraft.getInstance().getRenderManager().getFontRenderer(), entity, null, 0);
            dispatcher.getRenderer(this).render(BedHead.instance, -0.5F, 0, 0, 0, -1);

            popMatrix();
            GL11.glPopAttrib();
        }
    }

    static class MrBoaty extends EntityBoat {
        public static MrBoaty instance = new MrBoaty();

        public MrBoaty() {
            super(DummyWorld.INSTANCE);
        }

        public void render() {
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            pushMatrix();

            scalef(-1, -1, -1);

            Render<EntityBoat> render = Minecraft.getInstance().getRenderManager().getEntityRenderObject(this);

            render.doRender(this, 0, 0, 0, 0, 0);

            popMatrix();
            GL11.glPopAttrib();
        }
    }
}
