package dev.ogrevillages.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ogrevillages.OgreVillagesMod;
import dev.ogrevillages.entity.OgreEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Rendert den Ogre mit dem Spieler-Modell (64x64-Skins inkl. zweiter Ebene) plus Horn-Layer.
 * Skins: textures/entity/ogre/<stage>_<variant>.png, z. B. ogre_0.png, enlightened_ogre_1.png.
 */
public class OgreRenderer extends HumanoidMobRenderer<OgreEntity, PlayerModel<OgreEntity>> {
    private static final ResourceLocation[][] TEXTURES =
            new ResourceLocation[OgreEntity.Stage.values().length][OgreEntity.VARIANT_COUNT];

    static {
        for (OgreEntity.Stage stage : OgreEntity.Stage.values()) {
            for (int v = 0; v < OgreEntity.VARIANT_COUNT; v++) {
                TEXTURES[stage.ordinal()][v] = ResourceLocation.fromNamespaceAndPath(
                        OgreVillagesMod.MODID, "textures/entity/ogre/" + stage.texture + "_" + v + ".png");
            }
        }
    }

    public OgreRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.6F);
        this.addLayer(new OgreHornLayer(this, context.getModelSet()));
    }

    @Override
    public ResourceLocation getTextureLocation(OgreEntity entity) {
        return TEXTURES[entity.getStage().ordinal()][Math.floorMod(entity.getVariant(), OgreEntity.VARIANT_COUNT)];
    }

    /** Spielermodell ist 1,8 m hoch; Ogre ist 2 m groß. */
    @Override
    protected void scale(OgreEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(1.11F, 1.11F, 1.11F);
    }
}
