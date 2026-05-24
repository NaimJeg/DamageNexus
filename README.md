# DamageNexus

![NeoForge](https://img.shields.io/badge/NeoForge-26.1.x-orange.svg)
![Java](https://img.shields.io/badge/Java-25-blue.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)

DamageNexus is a server-authoritative damage framework mod for Minecraft,
built with NeoForge. It is not a content pack: production built-in entries,
affixes, static templates, equipment rules, gameplay items, entities, effects,
enchantments, and damage types are all zero.

It replaces fragmented damage calculations with a structured, extensible pipeline that allows mods and data packs to define how damage is created, modified, converted, mitigated, and finalized.

## Features

* Multiple damage channels, including physical, fire, cold, lightning, magic, poison, wither, and kinetic damage
* Ordered damage-processing phases for predictable calculations
* Data-driven damage rules
* Item damage entries and affixes
* Damage conversion, extra damage, true damage, multipliers, resistance, and mitigation
* Integration with vanilla melee, projectile, critical-hit, enchantment, armor, and status-effect mechanics
* Public Java API for registering custom rules, conditions, operations, providers, and processors

## Critical decision providers

Phase 7 adds a server-only Java registration point:
`registerCriticalDecisionProvider(id, priority, provider)`. Providers receive a
read-only damage view and a callback-scoped collector; they may contribute
`FORCE_CRITICAL` or `SUPPRESS_CRITICAL`, but cannot modify damage buckets or
mark a hit critical directly. Data-driven FORCE/SUPPRESS operations are not
part of this API.

Critical decisions run after `TYPE_SCALING` and before ordinary
`CRITICAL_HIT` processors and rules. Only the highest-priority contribution
layer is considered; SUPPRESS wins a tie. Contributions are frozen before the
final result is applied, so `is_critical` observes the authoritative result.
Each child request starts an independent decision lifecycle.

The default framework critical multiplier is `1 + 0.5 +
crit_damage_additive`. A captured vanilla melee critical rebuilds its captured
multiplier once and adds `crit_damage_additive` once. A captured projectile
critical rebuilds its captured random flat bonus once, marks the transaction
critical, and applies only the custom additive multiplier to the projectile
base/enchantment buckets—there is no second `0.5` roll. FORCE on a noncritical
projectile uses the generic framework multiplier and does not invent a vanilla
projectile random bonus. SUPPRESS prevents either captured bonus from being
rebuilt. Mace/spear special attacks remain ineligible for the default
melee/attribute roll, while an explicit trusted FORCE contribution may
override that default ineligibility without recalculating special scaling.

## Attribute consumption

Phase 8 applies damage attributes in a private handoff after the complete
`TYPE_SCALING` phase and before critical decision collection. A component
converted to Fire, or newly gained as Fire, therefore reads
`fire_damage_additive`; damage converted away from Fire does not. Values are
additive multiplier increments: `0.25` means `+25%`, so a channel bucket with
attribute `0.25` and rule contribution `0.10` evaluates to
`1 + 0.25 + 0.10 = 1.35`.

| Channel | Damage attribute | Existing pre-multiplier bucket |
| --- | --- | --- |
| fire | `fire_damage_additive` | `fire_damage` |
| cold | `cold_damage_additive` | `cold_damage` |
| lightning | `lightning_damage_additive` | `lightning_damage` |
| magic | `magic_damage_additive` | `magic_damage` |
| wither | `wither_damage_additive` | `wither_damage` |
| poison | `poison_damage_additive` | `poison_damage` |
| kinetic | `kinetic_damage_additive` | `kinetic_damage` |

The mutually exclusive melee/projectile category contributes
`melee_damage_additive` or `projectile_damage_additive` to the existing generic
damage bucket. Projectile signals take precedence. Damage-type tags, the
resolved direct entity, and the final vanilla source profile determine the
category; request kind, action ID, and source metadata tags do not. Attribute
reads use the final `logicalAttacker`, never the equipment owner. True-damage
buckets bypass these channel/global pre-multipliers.

Channel resistance remains data-driven through each channel definition's
`resistance_attribute`. `resistance_melee` or `resistance_projectile` is added
to the channel rating and transaction-local temporary rating before one
formula and one cap:

```text
total = channelRating + temporaryRating + categoryRating
R >= 0: reduction = R / (R + K)
R < 0:  reduction = R / K
reduction = clamp(reduction, -1.0, 0.95)
```

Consumed attributes are `crit_chance`, `crit_damage_additive`, the seven
channel damage attributes above, both category damage attributes, physical /
fire / cold / lightning / magic / wither / poison / kinetic resistance, both
category resistance attributes, and `thorns`. There is no
`physical_damage_additive`; rules can still use the existing
`physical_damage` bucket.

`vulnerable_damage_additive`, `dodge_chance`, and `healing_received` are
reserved and currently not consumed. Vulnerability lacks a stable generic
context signal; dodge needs a dedicated avoidance decision and unique
server-authoritative sample; healing belongs to a separate future healing
framework. Existing IDs, defaults, ranges, and stored modifiers are retained.

With debug commands enabled, manual probes include
`/damagenexus attribute self crit_0`, `/damagenexus attribute self crit_100`,
`/damagenexus attribute target physical_res_50`, and
`/damagenexus attribute target fire_res_50`. Full trace reports attribute
contributions plus channel, temporary, category, total resistance, and final
reduction; normal combat emits no per-hit attribute log.

* Read-only damage origin/lineage views, trusted server attribution resolvers, and owner-bound external item-stack rule sources
* Diagnostic logging and configurable compatibility behavior

Attribution resolvers and external item sources register synchronously through
`DamageNexusRegisterEvent`. Resolvers must prove proxy ownership from server
state; request fields alone never authorize another entity's equipment.
External equipment integrations contribute copied `ItemStack` sources, which
still pass through DamageNexus item security and normal entry/affix matching.

## Source and effect conditions

Effect-ID conditions (`attacker_has_effect`, `target_has_effect`) match one
registered effect. Effect-tag conditions (`attacker_effect_tag`,
`target_effect_tag`) instead inspect the entity's current effect holders against
a reloadable Minecraft `MobEffect` tag. DamageNexus does not supply tag members;
content mods and data packs own those bindings.

`damage_type_tag` checks a Minecraft `DamageType` registry tag. It is unrelated
to `source_tag`, which checks an identifier in the immutable
`DamageOrigin.sourceTags` metadata set. Likewise, `source_action_is` checks the
optional `actionId`. Content mods should use their own namespace for action and
source-tag identifiers. Native damage has neither value by default.

`proc_allowed` reports the current request's final downstream permission to
create a PROC child; it does not mean that the current request is itself PROC.
Request category checks use `request_kind_is`, `is_primary_damage`, and
`is_proc_damage`.

Minimal condition examples (illustrative only; they register no production
rules or tag members):

```json
{ "type": "damagenexus:attacker_effect_tag", "tag": "contentmod:example_effects" }
```

```json
{ "type": "damagenexus:source_action_is", "action": "contentmod:example_action" }
```

```json
{ "type": "damagenexus:source_tag", "tag": "contentmod:example_damage" }
```

```json
{ "type": "damagenexus:request_kind_is", "kind": "proc" }
```

## Stable Phase 9 Java API

DamageNexus is a framework, not a content pack: it ships no production item,
affix, damage-entry template, equipment rule, skill, effect, entity, or damage
type. External integrations should depend on `io.github.naimjeg.damagenexus.api`
and must not import `core`, `internal`, `registry`, diagnostics, or test classes.

Stable identifiers are exposed by `DamageNexusConditionIds`,
`DamageNexusOperationIds`, `DamageNexusPreMultiplierBuckets`, the built-in ID
fields on `DamageChannel`, and the keys/accessors on `DamageNexusAttributes`.
`DamageApplicationBucket.CODEC` uses stable lowercase names. Channel
`index()` and `DamageNexusPreMultiplierBuckets.runtimeIndex(id)` are frozen
runtime indexes only; persist the `Identifier`, never the integer.

Conditions, operations, and pre-multiplier buckets expose one canonical set of
public identifiers. `damage_type_tag` is the serialized condition for a
Minecraft DamageType tag; `source_tag` remains distinct DamageOrigin metadata.

`#damagenexus:is_spear_charge` and `#damagenexus:is_mace_smash` are exposed by
`DamageNexusTags`; neither tag adds content-specific members here.

`DamageNexusIds.id(path)` is only for DamageNexus-owned built-ins. Content mods
must construct full identifiers in their own namespace; rule IDs, action IDs,
source tags, metadata keys, resolver/item-source/critical-provider IDs, and
custom condition/operation/bucket IDs are preserved verbatim.

The Phase 8 formulas remain unchanged. A value of `0.25` means `+25%`;
contributions in one bucket add, while different buckets multiply. Attack
classification freezes once from damage-type tags, the vanilla profile, the
raw direct entity, and the resolver's final authoritative direct entity, with
projectile winning a melee conflict. Resistance uses one saturating total and
one formula/cap; extreme finite totals do not wrap to zero.

## Phase 10 static templates

Phase 10 registers and reloads complete, parameter-free
`DamageEntryDefinition` and `DamageAffixDefinition` templates. A template is a
server-authoritative definition selected by a stable ID; it is not an embedded
payload on the item. Existing materialized `damage_entries` and
`damage_affixes` components remain unchanged and continue to work.

Java integrations register templates during `DamageNexusRegisterEvent` with
`registerEntryTemplate(id, definition)` or
`registerAffixTemplate(id, definition)`. The registration ID must exactly equal
`definition.id()`, and callers retain their own namespace. Datapacks use two
independent directories:

```text
data/<namespace>/damagenexus_entry_templates/<path>.json
data/<namespace>/damagenexus_affix_templates/<path>.json
```

The file-derived ID must equal the definition ID. Reload validates both typed
sets, all nested rules/references, Java conflicts, and aggregate limits before
atomically publishing a new immutable revision. A failed reload retains the
entire previous revision only while its damage-channel dependency is still
compatible; a successful reload lets existing ItemStacks observe the new
definition on their next damage transaction. Java registrations cannot be
overwritten by datapacks. Java-only freeze is lookup-visible but not executable
until the first successful server channel-aware reload. The explicit reload
graph contains `CHANNELS → GLOBAL_RULES` and `CHANNELS → TEMPLATES`. A changed
channel content revision makes stale template/global-rule snapshots fail closed
instead of degrading unknown channels to untyped.

Items store ordered IDs in the separate synchronized
`damage_template_references` component. Materialized definitions execute first,
then entry and affix references resolve in their declaration order against one
revision pinned for the transaction. Duplicates are retained and use existing
entry/affix stacking semantics. Unresolved IDs remain stored and networkable,
execute nothing, and can become active after a later successful reload. They do
not cancel the original damage. All resolved definitions still pass through
the same item security, validation, selection, stacking, collector, and rule
pipeline; references never bypass those boundaries.

The reference component is executable authority. Non-administrator creative
ingress strips it together with materialized executable components. Client
tooltips may display only a bounded, sanitized ID placeholder when the
server-side datapack registry is unavailable; clients do not decide execution.
External equipped-item sources carry ordinary ItemStack snapshots and therefore
use exactly the same resolution path.

Illustrative registration (not built-in content):

```java
event.registerEntryTemplate(
    Identifier.fromNamespaceAndPath("examplemod", "example_entry"),
    completeEntryDefinition
);
```

Illustrative datapack location:

```text
data/examplemod/damagenexus_entry_templates/example_entry.json
```

Illustrative complete entry template (documentation only):

```json
{
  "id": "examplemod:example_entry",
  "display": { "name": "Example entry" },
  "rules": [{
    "id": "examplemod:example_entry_rule",
    "phase": "base_modification",
    "operations": [{
      "type": "damagenexus:add_base_damage",
      "channel": "damagenexus:untyped",
      "value": 1.0
    }]
  }]
}
```

`DamageAffixBlueprint`, `DamageAffixInstance`, generation specs, tiers, roll
ranges, and rolled values remain experimental authoring-only models. Roll keys
have no stable declarative binding to operation parameters, instances have no
versioned mapping to `DamageAffixDefinition.entries`, and there is no codec or
migration contract for parameterized generation. Phase 10 supports complete
static definition templates. Parameterized blueprint generation remains
deferred. External mods must use the public API rather than core/internal
template registry or reload-store implementations.

## Final Phase 11 contract and documentation

The full public contract, including attribution roles, settlement values,
lineage/admission, external providers, conditions, critical decisions,
attributes, templates, readiness, and a compilable registered settlement
callback child-request example, is in [docs/API.md](docs/API.md).

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

```text
Java/datapack definition
→ validated registry snapshot
→ payload-free ItemStack reference
→ server resolution
→ combined item graph validation
→ stacking
→ existing rule pipeline
```

`DamageNexusItemApi.get()` and its explicitly named materialized accessors
inspect only materialized components. `getResolvedMaterialized*()` applies
stacking resolution; it does not perform template-registry lookup. `set()`
preserves template references; `clear()` removes both forms. Storing a reference
reports storage success only, never server readiness.

Diagnostics are rate-limited and optional. They do not gate formal settlement
events, resolver/provider behavior, admission budgets, template readiness, or
security. Client tooltips are non-authoritative displays.

### Release-blocking settlement and authored-reference safety

`DamageSettledEvent` is a synchronous, read-only NeoForge observation event and
never exposes child authority. Directly re-posting the same event object cannot
authorize a child. Secondary damage is authorized only inside a listener
registered with `registerSettlementListener`; its
`DamageSettlementCallback.childAuthority()` is visible only for that exact
APPLIED callback invocation and server lifecycle. Each registered listener gets
a distinct authority instance for the same parent settlement; an authority
saved by one listener is rejected inside every later listener. Listener
priority is limited to `-10000..10000`. Settlement snapshots and
`DamageResult.settlement()` remain observations, NOT_APPLIED callbacks grant no
authority, and parentless roots are rejected during either settlement
observation or callback dispatch.
Direct managed `hurtServer` roots are rejected at the same dispatch boundary
before native admission or settlement creation. Registered callbacks create
secondary damage only through `DamageNexusApi.submitDamage` with their current
`DamageParentRef`; native nesting during an ordinary mutable hurt lifecycle is
not affected.

Public and native completions use one bounded, non-recursive FIFO. A child
submission may return before its observation event is posted; queued child
events are drained only after the currently invoked parent observer callbacks
have exited and all registered DamageNexus settlement callbacks have returned.
NeoForge itself aborts the remaining listener loop when an observer throws, so
observers not yet invoked may be skipped. DamageNexus catches that post failure,
keeps the committed result, runs its independently registered callbacks, and
continues later queued completions.

Materialized item definitions and resolved template references are combined,
then the whole graph is strictly validated against the current damage-channel
content revision before it can execute. Unknown or removed authored channels
fail closed instead of falling back to `untyped`; the execution cache includes
that current revision. `DamageNexusTemplates.entry/affix` lookup is
observational and does not prove server readiness or bypass execution
validation. Third-party composite conditions implement
`CompositeDamageRuleCondition` so node/depth/cycle budgets and nested reference
validation can traverse their immutable children.

## Purpose

DamageNexus is primarily an infrastructure mod for mod developers and modpack authors. It provides a common system for implementing custom damage mechanics without requiring every mod to replace or independently reproduce Minecraft's damage logic.
