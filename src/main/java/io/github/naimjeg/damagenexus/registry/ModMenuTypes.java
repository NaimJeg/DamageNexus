package io.github.naimjeg.damagenexus.registry;

import io.github.naimjeg.damagenexus.DamageNexus;
import io.github.naimjeg.damagenexus.menu.DamageDummyMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * DamageNexus menu type registry.
 *
 * <p>{@code DamageDummyMenu} uses the 26.1.2 NeoForge opening-data support:
 * {@link IMenuTypeExtension#create} builds a {@link MenuType} whose client
 * factory receives a {@code RegistryFriendlyByteBuf} containing the anchor
 * {@code BlockPos} written by the block entity when the menu is opened.</p>
 */
public final class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, DamageNexus.MODID);

    public static final DeferredHolder<
            MenuType<?>,
            MenuType<DamageDummyMenu>
            > DAMAGE_DUMMY =
            MENU_TYPES.register(
                    "damage_dummy",
                    () -> IMenuTypeExtension.create(DamageDummyMenu::new)
            );

    private ModMenuTypes() {
    }

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}
