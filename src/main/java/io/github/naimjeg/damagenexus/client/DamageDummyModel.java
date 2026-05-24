package io.github.naimjeg.damagenexus.client;

import io.github.naimjeg.damagenexus.DamageNexus;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Static Blockbench model for the damage dummy.
 *
 * <p>The runtime Java {@link LayerDefinition} is generated/adapted from the
 * Blockbench DamageDummy source model. It is authored once here, registered
 * through {@link #registerLayerDefinitions} on the client mod event bus, and
 * baked into a {@link ModelPart} by {@code EntityRendererProvider.Context}
 * before it is handed to {@link DamageDummyRenderer}. No .bbmodel file is
 * loaded at runtime.</p>
 *
 * <p>Hierarchy: {@code root -> bb_main -> cube_r1 .. cube_r7}. The authored
 * {@code bb_main} part is rooted at model Y = 24 (entity feet convention) and
 * the mesh extends upward through negative local Y. Every cube, UV offset,
 * deformation and part pose below is preserved exactly from the Blockbench
 * export; the rotated {@code cube_r*} children are not flattened.</p>
 *
 * <p>The model never animates: the dummy is a static display target, so
 * {@link #setupAnim} only restores the authored static pose.</p>
 */
public final class DamageDummyModel extends EntityModel<DamageDummyRenderState> {

    /**
     * Model layer for the damage dummy:
     * {@code damagenexus:damage_dummy / main}.
     */
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(
                    Identifier.fromNamespaceAndPath(
                            DamageNexus.MODID,
                            "damage_dummy"
                    ),
                    "main"
            );

    private final ModelPart bbMain;

    public DamageDummyModel(ModelPart root) {
        super(root);
        this.bbMain = root.getChild("bb_main");
    }

    /**
     * Blockbench-adapted body layer. Texture atlas is 128 x 128, matching the
     * Blockbench export's texture size.
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition bb_main = partdefinition.addOrReplaceChild(
                "bb_main",
                CubeListBuilder.create()
                        .texOffs(40, 0)
                        .addBox(-4.0F, -17.0F, -8.0F, 8.0F, 9.0F, 3.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(0, 19)
                        .addBox(-4.0F, -21.0F, -4.0F, 8.0F, 5.0F, 8.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(0, 0)
                        .addBox(-5.0F, -16.0F, -5.0F, 10.0F, 9.0F, 10.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(32, 57)
                        .addBox(4.0F, -24.0F, 5.0F, 1.0F, 1.0F, 1.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(52, 38)
                        .addBox(-4.0F, -18.0F, 4.0F, 8.0F, 2.0F, 1.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(48, 50)
                        .addBox(-5.0F, -18.0F, -5.0F, 10.0F, 2.0F, 1.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(28, 44)
                        .addBox(-6.0F, -24.0F, -6.0F, 12.0F, 1.0F, 1.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(14, 57)
                        .addBox(-5.0F, -24.0F, 4.0F, 1.0F, 1.0F, 2.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(54, 44)
                        .addBox(-1.0F, -24.0F, 5.0F, 2.0F, 1.0F, 1.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(40, 12)
                        .addBox(-3.0F, -27.0F, -3.0F, 6.0F, 1.0F, 6.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(20, 57)
                        .addBox(2.0F, -19.0F, -6.0F, 1.0F, 2.0F, 1.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(8, 57)
                        .addBox(1.0F, -20.0F, -7.0F, 1.0F, 3.0F, 2.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(24, 57)
                        .addBox(-3.0F, -19.0F, -6.0F, 1.0F, 2.0F, 1.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(28, 57)
                        .addBox(0.0F, -19.0F, -6.0F, 1.0F, 2.0F, 1.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(48, 56)
                        .addBox(-2.0F, -20.0F, -7.0F, 2.0F, 3.0F, 2.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(56, 56)
                        .addBox(-5.0F, -28.0F, 0.0F, 2.0F, 2.0F, 2.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(0, 57)
                        .addBox(3.0F, -28.0F, 0.0F, 2.0F, 2.0F, 2.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(0, 46)
                        .addBox(5.0F, -17.0F, -2.0F, 4.0F, 7.0F, 4.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(16, 46)
                        .addBox(-9.0F, -17.0F, -2.0F, 4.0F, 7.0F, 4.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(32, 46)
                        .addBox(-2.0F, -7.0F, -2.0F, 4.0F, 7.0F, 4.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(28, 32)
                        .addBox(-5.0F, -17.0F, 5.0F, 10.0F, 10.0F, 2.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 24.0F, 0.0F)
        );

        bb_main.addOrReplaceChild(
                "cube_r1",
                CubeListBuilder.create()
                        .texOffs(48, 48)
                        .addBox(-3.0F, -13.0F, 1.0F, 11.0F, 1.0F, 1.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(
                        -4.0F, -11.0F, -2.0F,
                        0.0F, -1.5708F, 0.0F
                )
        );

        bb_main.addOrReplaceChild(
                "cube_r2",
                CubeListBuilder.create()
                        .texOffs(48, 46)
                        .addBox(-3.0F, -13.0F, 1.0F, 11.0F, 1.0F, 1.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(
                        7.0F, -11.0F, -2.0F,
                        0.0F, -1.5708F, 0.0F
                )
        );

        bb_main.addOrReplaceChild(
                "cube_r3",
                CubeListBuilder.create()
                        .texOffs(52, 35)
                        .addBox(-2.0F, -13.0F, 1.0F, 9.0F, 2.0F, 1.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(
                        6.0F, -5.0F, -2.0F,
                        0.0F, -1.5708F, 0.0F
                )
        );

        bb_main.addOrReplaceChild(
                "cube_r4",
                CubeListBuilder.create()
                        .texOffs(52, 32)
                        .addBox(-2.0F, -13.0F, 1.0F, 9.0F, 2.0F, 1.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(
                        -3.0F, -5.0F, -2.0F,
                        0.0F, -1.5708F, 0.0F
                )
        );

        bb_main.addOrReplaceChild(
                "cube_r5",
                CubeListBuilder.create()
                        .texOffs(0, 32)
                        .addBox(-7.0F, -7.0F, 0.0F, 12.0F, 14.0F, 2.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(
                        1.0F, -23.0F, 0.0F,
                        -1.5708F, 0.0F, 0.0F
                )
        );

        bb_main.addOrReplaceChild(
                "cube_r6",
                CubeListBuilder.create()
                        .texOffs(32, 19)
                        .addBox(-6.0F, -4.0F, -2.0F, 10.0F, 10.0F, 3.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(
                        1.0F, -24.0F, 1.0F,
                        -1.5708F, 0.0F, 0.0F
                )
        );

        bb_main.addOrReplaceChild(
                "cube_r7",
                CubeListBuilder.create()
                        .texOffs(48, 53)
                        .addBox(-2.0F, -2.0F, 0.0F, 3.0F, 1.0F, 2.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(52, 41)
                        .addBox(-7.0F, -2.0F, 0.0F, 3.0F, 1.0F, 2.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(
                        3.0F, -25.0F, 4.0F,
                        -1.5708F, 0.0F, 0.0F
                )
        );

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    /**
     * Client-side registration of {@link #LAYER_LOCATION} -> {@link
     * #createBodyLayer}.
     */
    public static void registerLayerDefinitions(
            EntityRenderersEvent.RegisterLayerDefinitions event
    ) {
        event.registerLayerDefinition(
                LAYER_LOCATION,
                DamageDummyModel::createBodyLayer
        );
    }

    @Override
    public void setupAnim(DamageDummyRenderState state) {
        super.setupAnim(state);
        // Static display target: restores the authored Blockbench pose and
        // applies no animation.
    }
}
