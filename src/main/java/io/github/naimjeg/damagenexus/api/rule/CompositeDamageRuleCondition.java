package io.github.naimjeg.damagenexus.api.rule;

import java.util.List;

/**
 * Public traversal contract for a condition that owns nested conditions.
 *
 * <p>Implementations must return a stable, immutable, non-null list containing
 * no null elements. Framework structural budgets, cycle/depth checks, and
 * strict authored-reference validation traverse this list. A third-party
 * wrapper that does not implement this interface is treated as a leaf and is
 * therefore trusted Java code rather than a data-driven composite.</p>
 */
public interface CompositeDamageRuleCondition extends DamageRuleCondition {

    List<DamageRuleCondition> childConditions();
}
