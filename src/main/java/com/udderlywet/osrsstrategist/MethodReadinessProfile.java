package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Account-readiness model for one training method.
 *
 * <p>Item requirements can be verified automatically from observed account
 * state. Other checks remain explicit strings until a dedicated quest/access/
 * transport/capability reader can prove them. This mixed model lets Strategist
 * improve incrementally without turning unknown facts into assumptions.</p>
 */
public final class MethodReadinessProfile
{
    private final String methodId;
    private final List<NamedResourceRequirement> itemRequirements;
    private final List<String> otherChecks;

    public MethodReadinessProfile(
            String methodId,
            List<NamedResourceRequirement> itemRequirements,
            List<String> otherChecks)
    {
        this.methodId = methodId;
        this.itemRequirements = Collections.unmodifiableList(
                itemRequirements == null
                        ? new ArrayList<>()
                        : new ArrayList<>(itemRequirements));
        this.otherChecks = Collections.unmodifiableList(
                otherChecks == null
                        ? new ArrayList<>()
                        : new ArrayList<>(otherChecks));
    }

    public String getMethodId() { return methodId; }
    public List<NamedResourceRequirement> getItemRequirements() { return itemRequirements; }
    public List<String> getOtherChecks() { return otherChecks; }
}
