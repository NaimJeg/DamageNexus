package io.github.naimjeg.damagenexus.registry;

import io.github.naimjeg.damagenexus.DamageNexus;
import io.github.naimjeg.damagenexus.core.trace.DamageTransactionQueue;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(
                    NeoForgeRegistries.ATTACHMENT_TYPES,
                    DamageNexus.MODID
            );

    public static final DeferredHolder<
            AttachmentType<?>,
            AttachmentType<Boolean>
            > PENDING_JUMP_CRIT =
            ATTACHMENTS.register(
                    "pending_jump_crit",
                    () -> AttachmentType
                            .builder(() -> false)
                            .build()
            );

    public static final DeferredHolder<
            AttachmentType<?>,
            AttachmentType<DamageTransactionQueue>
            > DAMAGE_TRANSACTIONS =
            ATTACHMENTS.register(
                    "damage_transactions",
                    () -> AttachmentType
                            .builder(DamageTransactionQueue::new)
                            .build()
            );
}
