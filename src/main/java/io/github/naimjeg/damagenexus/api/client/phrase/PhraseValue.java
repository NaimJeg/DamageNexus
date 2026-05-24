package io.github.naimjeg.damagenexus.api.client.phrase;

/** Semantic values accepted by rule phrase schemas. */
public sealed interface PhraseValue permits
        NumberValue,
        PercentValue,
        ChannelValue,
        EntityRoleValue,
        EffectValue,
        EntityTypeValue,
        TagValue,
        RequestKindValue,
        IdentifierValue,
        MobCategoryValue,
        NestedPhraseValue {
}
