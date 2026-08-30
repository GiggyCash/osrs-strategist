package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

/**
 * Readiness assessments for bosses, raids, and other PvM activities.
 *
 * <p>A future PvM analyzer can populate this from combat stats, prayers,
 * spellbooks, equipment, supplies, quest access, kill count, and Combat
 * Achievements. Keeping the assessment separate from the UI makes that logic
 * testable with fake accounts.</p>
 */
public final class PvmSnapshot
{
    @Getter
    private final Map<String, PvmReadiness> readinessByActivity;

    public PvmSnapshot(Map<String, PvmReadiness> readinessByActivity)
    {
        this.readinessByActivity = Collections.unmodifiableMap(
                readinessByActivity == null
                        ? new HashMap<>()
                        : new HashMap<>(readinessByActivity)
        );
    }

    public static PvmSnapshot unknown()
    {
        return new PvmSnapshot(Collections.emptyMap());
    }

    public PvmReadiness readinessFor(String activityId)
    {
        return readinessByActivity.get(activityId);
    }

}
