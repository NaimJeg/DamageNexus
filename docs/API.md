# DamageNexus public API

DamageNexus is a server-authoritative damage framework, not a content pack. It
ships zero production entries, affixes, static templates, equipment rules,
gameplay items, entities, effects, enchantments, or damage types. Integrations
use their own namespace and import `io.github.naimjeg.damagenexus.api...`;
`core`, `internal`, `registry`, diagnostics, debug, and test packages are not
public dependencies.

## Development damage dummy

The only shipped entity, `damagenexus:damage_dummy` (summonable with
`/summon damagenexus:damage_dummy`), is a development/testing LivingEntity
target, not a content feature. It is a stationary `PathfinderMob` that behaves
as a normal damage target: ordinary attacks flow through the vanilla/NeoForge/
DamageNexus damage path and reduce its health normally.

At startup it attaches every registered entity Attribute (vanilla, NeoForge,
DamageNexus, and third-party) through the supported NeoForge attribute
lifecycle, using each attribute's declared default base value. There is no
hardcoded attribute list, so a newly registered Attribute becomes available to
the dummy without source changes.

The dummy's attribute handling (including the internal catalog used by future
developer tooling) is an implementation detail of the mod, not a stable public
API, so external code must not depend on it. The entity's `AttributeMap` is
always the authoritative state; any future editor GUI is internal to
DamageNexus and will mutate the real `AttributeInstance`s server-side. Presence
of an attribute does not imply the dummy's code consumes it semantically.

## Authoritative damage lifecycle

```text
Primary request
→ validation/admission
→ transaction
→ rule collection
→ seven-phase pipeline
→ vanilla application
→ immutable settlement snapshot
→ transaction cleanup
→ public observation event
→ registered settlement callback
→ child request
→ new transaction
```

`DamageNexusApi.submitDamage` is synchronous and must run on the owning server
thread. Native and public damage share the managed pipeline. A public request
is validated, attributed by server resolvers, admitted once, and then submitted
to `hurtServer`. Native damage starts from a normalized vanilla attribution and
may be claimed by the same trusted resolver registry.

Attribution roles are deliberately separate:

- `directEntity`: collision/direct source, such as a projectile or proxy.
- `logicalAttacker`: combat-rule attacker and source of offensive attributes.
- `effectOwner`: creator/owner of a projectile, area, or proxy effect.
- `equipmentOwner`: entity whose equipment contributes offensive item rules.

Without a registered resolver, non-null `equipmentOwner` must be the exact
`logicalAttacker`. A resolver runs on the server and must prove proxy ownership
from authoritative state; copied request fields are not proof.

`DamageSettledEvent` is a synchronous, read-only, non-cancelable NeoForge
observation event. It is published exactly once after submission, hurt, and
transaction-activity scopes have exited, but it never carries child authority.
Re-posting the same event object remains observational. NeoForge aborts its
remaining listener loop when an observer throws; DamageNexus catches the post
failure after all already-entered callbacks exit, preserves the committed
damage, and continues its own callback/FIFO processing.

Public and native completions share one bounded FIFO. Authority-bearing
listeners are registered through `registerSettlementListener` and dispatched
individually by DamageNexus after the NeoForge observation post. Each callback
failure is isolated. A child submitted by one of these callbacks returns a
structured result immediately, but its observation/callback delivery remains
queued until the current parent delivery completes. The outer synchronous
drain finishes before the root `submitDamage` call returns.

Settlement values have distinct meanings:

- `resolvedDamage`: final DamageNexus formula result.
- `appliedDamage`: authoritative amount after `LivingDamageEvent.Pre` supplied
  to vanilla application.
- `healthDamage`: `max(0, healthBefore - healthAfter)`.
- `absorptionDamage`: `max(0, absorptionBefore - absorptionAfter)`.

Neither state delta replaces `appliedDamage`, and `resolvedDamage` remains an
independent formula result.

`DamageParentRef` child authority can only come from
`DamageSettlementCallback.childAuthority()` during an exact registered APPLIED
callback invocation. `DamageSettledEvent`, `DamageSettlementSnapshot`, and
`DamageResult` are observations; NOT_APPLIED callbacks, direct event re-posts,
saved/expired authorities, and authorities from another callback or server
cannot authorize a child. Parentless root requests are rejected while a
settlement observation or callback is being dispatched.
Direct managed `LivingEntity.hurtServer` calls are also rejected at that
dispatch boundary before a hurt scope, native admission, or rejection
settlement is created. A registered callback must use its current
`DamageParentRef` through `DamageNexusApi.submitDamage`; nested native damage
during an ordinary mutable hurt lifecycle remains supported.
Every registered callback invocation receives a unique `DamageParentRef`
identity, including callbacks for the same parent settlement. Those distinct
references still wrap the same immutable parent origin and therefore share the
same lineage and root-derivation budget. Listener priority is restricted to the
inclusive range `-10000..10000`.
Root depth is 0;
each child receives a fresh damage ID, the same root ID, the parent damage ID,
and depth + 1. Trigger authorization answers whether a parent may create an
immediate PROC/REFLECTED/THORNS child. The child's downstream policy is then
frozen independently as:

```text
defaultsFor(finalKind) ∩ callerRestrictions ∩ parentPolicy
```

Depth, per-root derivation count, and per-server-tick admission are separate
budgets. Rejected requests do not enter the pipeline or publish a settlement.
PROC/REFLECTED/THORNS default downstream suppression prevents same-kind loops;
DOT and custom chains are bounded by the generic budgets.

## Completed-damage child example

The compilable form is maintained in
`src/test/java/io/github/naimjeg/damagenexus/externalapi/ExternalSettlementProcExample.java`.
The content mod—not DamageNexus—owns equipment checks, probability, target
search, and child damage calculation:

```java
@SubscribeEvent
public static void register(DamageNexusRegisterEvent event) {
    event.registerSettlementListener(
            Identifier.fromNamespaceAndPath(
                    "examplemod", "example_proc_listener"),
            0,
            ExampleHandler::onDamageSettled);
}

public static void onDamageSettled(DamageSettlementCallback callback) {
    DamageSettlementSnapshot snapshot = callback.snapshot();
    Optional<DamageParentRef> authority = callback.childAuthority();
    if (authority.isEmpty()
            || snapshot.requestKind() != DamageRequestKind.PRIMARY
            || !snapshot.triggerPolicy().procAllowed()
            || snapshot.logicalAttacker() == null
            || !ExampleContentEquipment.hasEffect(snapshot.logicalAttacker())
            || !ExampleContentProbability.shouldTrigger(snapshot)) {
        return;
    }

    for (LivingEntity target : ExampleContentTargets.findTargets(snapshot)) {
        DamageRequest child = DamageRequest.builder(
                        snapshot.level(), target, snapshot.source())
                .kind(DamageRequestKind.PROC)
                .parent(authority.orElseThrow())
                .logicalAttacker(snapshot.logicalAttacker())
                .actionId(Identifier.fromNamespaceAndPath(
                        "examplemod", "example_proc"))
                .sourceTag(Identifier.fromNamespaceAndPath(
                        "examplemod", "area_damage"))
                .baseDamage(ExampleContentDamage.calculate(snapshot))
                .build();
        DamageResult result = DamageNexusApi.submitDamage(child);
        ExampleContentDiagnostics.observe(result.status(), result.failure());
    }
}
```

The `PRIMARY` filter ensures this listener ignores its own PROC requests.
The authority is valid only while this exact registered callback invocation is
active; the NeoForge event, snapshots, and results cannot authorize children.

## Rules, providers, and source views

`DamageContextView` exposes immutable origin, attribution, lineage, kind,
action ID, source tags, metadata, and `logicalAttacker()`.

`EquippedItemRuleSource` contributes copied `ItemStack` sources bound by the
framework to OFFENSIVE/equipment-owner or DEFENSIVE/target direction. Providers
are ordered by priority then full Identifier; transaction-local source keys,
slot semantics, owner identity, direction, and stack identity give deterministic
deduplication. Providers cannot return runtime rules or bypass item security.

Phase 6 conditions include attacker/target MobEffect tags, source action ID,
source metadata tag, request kind, primary/proc, parent presence, and downstream
PROC permission. `damage_type_tag` checks a Minecraft DamageType tag;
`source_tag` checks `DamageOrigin.sourceTags`. Native damage has no action ID or
source tags by default. Tag contents are looked up live after reload.

Critical decision providers contribute FORCE or SUPPRESS before ordinary
`CRITICAL_HIT` rules. Contributions freeze once; highest priority wins and
SUPPRESS wins a same-priority conflict. Vanilla melee/projectile captures and
attribute probability share the same one-shot decision engine. Children start
with a fresh decision state.

## Entry and affix display summaries

`DamageEntryDisplay.authoredSummary()` and
`DamageAffixDisplay.authoredSummary()` are optional authored summaries. They
are compact-mode replacement text only, never part of the generated rule
detail view.

When `authoredSummary` is present:

- Compact uses the authored summary.
- Expanded replaces the authored summary with modular rule details.

When `authoredSummary` is absent:

- DamageNexus generates a modular Compact summary.
- Expanded keeps that generated summary and appends modular details.

The canonical serialized field is `authored_summary`. Legacy `tooltip` data
continues to decode as the authored summary, and conflicting `authored_summary`
and `tooltip` fields fail explicitly. New serialization writes only
`authored_summary`. The deprecated `tooltip()` accessor returns the same
immutable `authoredSummary()` list.

Entry example:

```java
DamageEntryDisplay entryDisplay = new DamageEntryDisplay(
        Optional.of(DisplayText.translatableWithFallback(
                "example.entry.name", "Example Entry")),
        List.of(DisplayText.translatableWithFallback(
                "example.entry.summary", "Authored compact summary")),
        Optional.of(DisplayText.translatableWithFallback(
                "example.entry.flavor", "Flavor text")),
        true
);
```

Affix example:

```java
DamageAffixDisplay affixDisplay = new DamageAffixDisplay(
        Optional.of(DisplayText.translatableWithFallback(
                "example.affix.name", "Example Affix")),
        List.of(DisplayText.translatableWithFallback(
                "example.affix.summary", "Affix authored compact summary")),
        Optional.of(DisplayText.translatableWithFallback(
                "example.affix.flavor", "Affix flavor text")),
        true
);
```

## Attributes and formulas

After all `TYPE_SCALING` rules, a private handoff applies final-channel damage
attributes. `0.25` means `+25%`: values in one pre-multiplier bucket add, while
different buckets multiply. Projectile classification wins a melee conflict
and may use the final resolver-authoritative `directEntity`; request kind and a
non-null logical attacker do not imply either category.

Channel, temporary, and melee/projectile category resistance ratings are added
first, then evaluated once:

```text
total = channelRating + temporaryRating + categoryRating
R >= 0: reduction = R / (R + K)
R < 0:  reduction = R / K
reduction = clamp(reduction, -1.0, 0.95)
```

Finite overflow saturates instead of becoming zero. True-damage buckets and
the existing BYPASSES tags retain their bypass behavior. The registered
`vulnerable_damage_additive`, `dodge_chance`, and `healing_received` attributes
are reserved and currently unconsumed.

## Stable IDs and registration

Use `DamageNexusConditionIds`, `DamageNexusOperationIds`,
`DamageNexusPreMultiplierBuckets`, `DamageNexusAttributes`, `DamageChannel`,
and `DamageApplicationBucket`. Runtime integer channel/bucket indexes are not
persistent IDs. `DamageNexusIds.id(path)` is only for DamageNexus-owned IDs.

All registrar methods are valid only during `DamageNexusRegisterEvent`.
Retaining the registrar after the callback fails. Resolver, external item
source, critical provider, condition, operation, pre-multiplier bucket, and
template IDs preserve the caller's namespace.

## Complete static templates

```text
Java/datapack definition
→ validated registry snapshot
→ payload-free ItemStack reference
→ server resolution
→ combined item graph validation
→ stacking
→ existing rule pipeline
```

Java templates register complete `DamageEntryDefinition` or
`DamageAffixDefinition` values during `DamageNexusRegisterEvent`. The registry
ID must equal `definition.id()`. Datapacks use:

```java
Identifier templateId = Identifier.fromNamespaceAndPath(
        "examplemod", "example_entry");
DamageEntryDefinition completeDefinition = new DamageEntryDefinition(
        templateId,
        DamageEntryDisplay.EMPTY,
        DamageEntrySlot.ITEM,
        List.of(completeRuleDefinition),
        DamageEntryStacking.STACK,
        Optional.empty());
event.registerEntryTemplate(templateId, completeDefinition);

// The ItemStack stores only the ID; server execution resolves the payload.
DamageNexusItemApi.addEntryTemplateReference(
        stack, new DamageEntryTemplateReference(templateId));
```

The compilation fixture in `ExternalModApiFixture` exercises the same public
imports and registration lifecycle.

```text
data/<namespace>/damagenexus_entry_templates/<path>.json
data/<namespace>/damagenexus_affix_templates/<path>.json
```

Minimal complete static entry template:

```json
{
  "id": "examplemod:example_entry",
  "display": { "name": "Example entry" },
  "slot": "item",
  "rules": [{
    "id": "examplemod:example_entry_rule",
    "role": "offensive",
    "phase": "base_modification",
    "conditions": [],
    "operations": [{
      "type": "damagenexus:add_base_damage",
      "channel": "damagenexus:untyped",
      "value": 1.0
    }],
    "stacking": "stack"
  }],
  "stacking": "stack"
}
```

The file-derived ID must equal the definition ID. Java and datapack candidates
share one aggregate template/rule/condition/operation budget; datapacks cannot
override Java templates. Publication is atomic and revision increments only on
success.

Java freeze provides lookup but is not server-execution-ready. A successful
channel-aware server reload validates templates and binds the template snapshot
to the channel content revision. The reload graph explicitly contains
`CHANNELS → GLOBAL_RULES` and `CHANNELS → TEMPLATES`. If the channel revision is
unchanged, a failed reload can continue using the prior valid snapshot. If the
channel revision changes, stale templates and global rules fail closed rather
than resolving unknown channels as untyped. Active damage transactions retain
their already pinned immutable snapshot; new transactions see a complete old
or new compatible snapshot.

Items store only ordered IDs in `damage_template_references`. Existing
materialized definitions run first, followed by references in declaration
order. Unresolved or dependency-incompatible references remain stored and
networkable but execute nothing. A later successful reload can activate the
same ItemStack. `DamageNexusItemApi.get()` and the explicitly named
materialized accessors operate only on materialized definitions.
`getResolvedMaterialized*()` applies stacking resolution, not template lookup.
`set()` preserves references, while `clear()` removes both materialized
definitions and references.

Materialized entries and nested affix entries receive the same strict authored
reference validation immediately before execution. Their complete executable
graph is checked against the current channel content revision, including
condition channels, operation channels, and pre-multiplier bucket IDs. One
invalid reference rejects the whole graph; authored unknown/deleted channels
never use the tolerant runtime `untyped` fallback. Cache identity includes the
current channel revision, so every real channel-content change revalidates
materialized data even when a template reload fails. Template lookup remains
read-only and does not imply server execution readiness.

Third-party conditions that contain nested conditions implement
`CompositeDamageRuleCondition` and return a stable immutable child list. The
framework uses that contract for semantic node/depth/cycle budgets and strict
reference traversal. A Java condition that wraps children without the contract
is treated as a trusted leaf, not as a data-driven composite.

The server is authoritative. Client tooltip placeholders do not prove lookup
or execution readiness. `DamageNexusTemplates.serverExecutionReady()` reports
current server compatibility but is observational, not an execution API.

`DamageAffixBlueprint`, instances, tiers, rolls, and generation specs remain
experimental authoring-only models. Phase 10/11 supports complete static
definitions only—there is no parameter binding, random materialization,
expression language, script, item-level generator, rarity roll, or drop pool.

Diagnostics are optional and rate-limited; disabling them never disables
settlement events, admission, registry readiness, security, or execution.
