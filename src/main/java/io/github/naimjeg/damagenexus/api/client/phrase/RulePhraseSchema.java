package io.github.naimjeg.damagenexus.api.client.phrase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class RulePhraseSchema {
    public static final int MAX_ARGUMENTS = 16;
    public static final int MAX_NESTING_DEPTH = 8;
    public static final int MAX_IDENTIFIER_LENGTH = 256;

    private final RulePhraseType type;
    private final Set<PhraseVariant> variants;
    private final List<PhraseSlot<?>> slots;

    public RulePhraseSchema(
            RulePhraseType type,
            Set<PhraseVariant> variants,
            List<PhraseSlot<?>> slots
    ) {
        this.type = Objects.requireNonNull(type, "type");
        if (variants == null || variants.isEmpty()) {
            throw new IllegalArgumentException("A phrase schema needs a variant");
        }
        this.variants = Collections.unmodifiableSet(EnumSet.copyOf(variants));
        this.slots = slots == null ? List.of() : List.copyOf(slots);
        if (this.slots.size() > MAX_ARGUMENTS) {
            throw new IllegalArgumentException("Too many phrase slots");
        }
        Set<PhraseSlot<?>> identities = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<String> names = new java.util.HashSet<>();
        for (PhraseSlot<?> slot : this.slots) {
            if (slot == null || !identities.add(slot) || !names.add(slot.name())) {
                throw new IllegalArgumentException("Duplicate or null phrase slot");
            }
        }
    }

    public RulePhraseType type() {
        return type;
    }

    public Set<PhraseVariant> variants() {
        return variants;
    }

    public List<PhraseSlot<?>> slots() {
        return slots;
    }

    public RulePhrase create(PhraseVariant variant, PhraseArguments arguments) {
        validate(variant, arguments);
        return new RulePhrase(type, variant, arguments);
    }

    public void validate(PhraseVariant variant, PhraseArguments arguments) {
        Objects.requireNonNull(variant, "variant");
        PhraseArguments safe = arguments == null ? PhraseArguments.EMPTY : arguments;
        if (!variants.contains(variant)) {
            throw new IllegalArgumentException(
                    "Illegal variant " + variant + " for " + type.id()
            );
        }
        if (safe.entries().size() > MAX_ARGUMENTS) {
            throw new IllegalArgumentException("Too many phrase arguments");
        }

        Set<PhraseSlot<?>> known = Collections.newSetFromMap(new IdentityHashMap<>());
        known.addAll(slots);
        Set<PhraseSlot<?>> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<String> seenNames = new java.util.HashSet<>();
        for (PhraseArgument argument : safe.entries()) {
            if (!known.contains(argument.slot())) {
                throw new IllegalArgumentException(
                        "Unknown slot " + argument.slot().name() + " for " + type.id()
                );
            }
            if (!seen.add(argument.slot()) || !seenNames.add(argument.slot().name())) {
                throw new IllegalArgumentException(
                        "Duplicate slot " + argument.slot().name()
                );
            }
            if (!argument.slot().valueType().isInstance(argument.value())) {
                throw new IllegalArgumentException(
                        "Wrong value type for slot " + argument.slot().name()
                );
            }
            validateValue(argument.value(), 1);
        }
        List<String> missing = new ArrayList<>();
        for (PhraseSlot<?> slot : slots) {
            if (slot.required() && !seen.contains(slot)) {
                missing.add(slot.name());
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Missing required slots: " + missing);
        }
    }

    private static void validateValue(PhraseValue value, int depth) {
        if (depth > MAX_NESTING_DEPTH) {
            throw new IllegalArgumentException("Phrase nesting is too deep");
        }
        if (value instanceof NestedPhraseValue nested) {
            for (PhraseArgument argument : nested.phrase().arguments().entries()) {
                validateValue(argument.value(), depth + 1);
            }
        }
        String identifier = switch (value) {
            case ChannelValue v -> v.channelId().toString();
            case EffectValue v -> v.effectId().toString();
            case EntityTypeValue v -> v.entityTypeId().toString();
            case TagValue v -> v.tagId().toString();
            case IdentifierValue v -> v.identifier().toString();
            default -> "";
        };
        if (identifier.codePointCount(0, identifier.length()) > MAX_IDENTIFIER_LENGTH) {
            throw new IllegalArgumentException("Phrase identifier is too long");
        }
    }
}
