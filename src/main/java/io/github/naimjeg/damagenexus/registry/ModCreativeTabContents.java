package io.github.naimjeg.damagenexus.registry;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/**
 * Creative inventory integration for DamageNexus content.
 *
 * <p>The existing pedestal {@code BlockItem} ({@link ModItems#DAMAGE_DUMMY})
 * is appended to the end of the vanilla Functional Blocks tab through the
 * normal {@code BuildCreativeModeTabContentsEvent} append operation. The
 * default {@code PARENT_AND_SEARCH_TABS} visibility keeps the item
 * discoverable in the creative search tab without injecting a second copy
 * into {@link CreativeModeTabs#SEARCH}. No custom DamageNexus creative tab
 * is created.</p>
 */
public final class ModCreativeTabContents {

    private ModCreativeTabContents() {
    }

    public static void buildContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModItems.DAMAGE_DUMMY.get());
        }
    }
}
