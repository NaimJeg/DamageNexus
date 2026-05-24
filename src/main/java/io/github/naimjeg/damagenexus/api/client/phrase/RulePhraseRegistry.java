package io.github.naimjeg.damagenexus.api.client.phrase;

import io.github.naimjeg.damagenexus.api.rule.DamageRuleCondition;
import io.github.naimjeg.damagenexus.api.rule.DamageRuleOperation;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Client-only registry for semantic rule descriptions. */
public final class RulePhraseRegistry implements RulePhraseFactory {
    private final Map<RulePhraseType, RulePhraseSchema> schemas = new LinkedHashMap<>();
    private final Map<Identifier, Binding<?>> conditions = new LinkedHashMap<>();
    private final Map<Identifier, Binding<?>> operations = new LinkedHashMap<>();
    private boolean frozen;

    public synchronized void registerSchema(RulePhraseSchema schema) {
        requireMutable();
        Objects.requireNonNull(schema, "schema");
        if (schemas.putIfAbsent(schema.type(), schema) != null) {
            throw new IllegalStateException("Duplicate phrase schema: " + schema.type().id());
        }
    }

    public synchronized <T extends DamageRuleCondition> void registerCondition(
            Identifier conditionType,
            Class<T> valueClass,
            RulePhraseProvider<? super T> provider
    ) {
        registerBinding(conditions, conditionType, valueClass, provider, "condition");
    }

    public synchronized <T extends DamageRuleOperation> void registerOperation(
            Identifier operationType,
            Class<T> valueClass,
            RulePhraseProvider<? super T> provider
    ) {
        registerBinding(operations, operationType, valueClass, provider, "operation");
    }

    private <T> void registerBinding(
            Map<Identifier, Binding<?>> bindings,
            Identifier type,
            Class<T> valueClass,
            RulePhraseProvider<? super T> provider,
            String kind
    ) {
        requireMutable();
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(valueClass, "valueClass");
        Objects.requireNonNull(provider, "provider");
        if (bindings.putIfAbsent(type, new Binding<>(valueClass, provider)) != null) {
            throw new IllegalStateException("Duplicate " + kind + " phrase provider: " + type);
        }
    }

    @Override
    public synchronized RulePhrase create(
            RulePhraseType type,
            PhraseVariant variant,
            PhraseArguments arguments
    ) {
        RulePhraseSchema schema = schemas.get(type);
        if (schema == null) {
            throw new IllegalArgumentException("Unknown phrase schema: " + type.id());
        }
        return schema.create(variant, arguments);
    }

    public synchronized Optional<RulePhrase> describeCondition(
            DamageRuleCondition condition
    ) {
        if (condition == null) {
            return Optional.empty();
        }
        return describe(conditions.get(condition.type()), condition, "condition");
    }

    public synchronized Optional<RulePhrase> describeOperation(
            DamageRuleOperation operation
    ) {
        if (operation == null) {
            return Optional.empty();
        }
        return describe(operations.get(operation.type()), operation, "operation");
    }

    private <T> Optional<RulePhrase> describe(
            Binding<?> untyped,
            T value,
            String kind
    ) {
        if (untyped == null) {
            return Optional.empty();
        }
        if (!untyped.valueClass().isInstance(value)) {
            throw new IllegalStateException(
                    "Registered " + kind + " provider expected "
                            + untyped.valueClass().getName()
                            + " but received " + value.getClass().getName()
            );
        }
        @SuppressWarnings("unchecked")
        Binding<T> binding = (Binding<T>) untyped;
        RulePhrase phrase = Objects.requireNonNull(
                binding.provider().provide(value, this),
                kind + " phrase provider result"
        );
        RulePhraseSchema schema = schemas.get(phrase.type());
        if (schema == null) {
            throw new IllegalStateException(
                    "Provider returned an unregistered phrase type: " + phrase.type().id()
            );
        }
        schema.validate(phrase.variant(), phrase.arguments());
        return Optional.of(phrase);
    }

    public synchronized void freeze() {
        frozen = true;
    }

    public synchronized boolean isFrozen() {
        return frozen;
    }

    public synchronized Set<Identifier> conditionTypes() {
        return Set.copyOf(conditions.keySet());
    }

    public synchronized Set<Identifier> operationTypes() {
        return Set.copyOf(operations.keySet());
    }

    public synchronized Collection<RulePhraseSchema> schemas() {
        return ListCopy.copyOf(schemas.values());
    }

    private void requireMutable() {
        if (frozen) {
            throw new IllegalStateException("Rule phrase registry is frozen");
        }
    }

    private record Binding<T>(
            Class<T> valueClass,
            RulePhraseProvider<? super T> provider
    ) {
    }

    private static final class ListCopy {
        private ListCopy() {
        }

        static <T> java.util.List<T> copyOf(Collection<T> values) {
            return java.util.List.copyOf(values);
        }
    }
}
