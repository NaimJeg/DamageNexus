package io.github.naimjeg.damagenexus.registry;

import io.github.naimjeg.damagenexus.DamageNexus;
import io.github.naimjeg.damagenexus.block.DamageDummyBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * DamageNexus block registry.
 *
 * <p>Only the damage dummy pedestal is registered: a low-profile 14x1x14
 * pressure-plate-like base that physically anchors the
 * {@code DamageDummyEntity} standing directly on top of it. The block is the
 * lifecycle owner and the management-menu interaction surface; the entity
 * remains the combat target.</p>
 */
public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(DamageNexus.MODID);

    /**
     * Wooden pedestal (low-profile plate shape). Values mirror vanilla
     * oak plank properties (map color, 2/3 strength, wood sounds),
     * with an explicit {@link PushReaction#BLOCK} so pistons can never move
     * the anchor away from its entity.
     */
    public static final DeferredBlock<DamageDummyBlock> DAMAGE_DUMMY =
            BLOCKS.registerBlock(
                    "damage_dummy",
                    DamageDummyBlock::new,
                    () -> BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(2.0F, 3.0F)
                            .sound(SoundType.WOOD)
                            .pushReaction(PushReaction.BLOCK)
            );

    private ModBlocks() {
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
