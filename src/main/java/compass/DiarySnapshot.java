package compass;

import java.util.*;

import lombok.Getter;

public final class DiarySnapshot
{
    private final Map<String, Integer> completedTasksByRegion;
    private final Map<String, Integer> totalTasksByRegion;
    @Getter
    private final Map<String, Map<DiaryTier, Boolean>> completedTiersByRegion;
    @Getter
    private final Map<String, Boolean> observedTaskCompletion;

    public DiarySnapshot(
            Map<String, Integer> completedTasksByRegion,
            Map<String, Integer> totalTasksByRegion)
    {
        this(completedTasksByRegion, totalTasksByRegion,
                Collections.emptyMap(), Collections.emptyMap());
    }

    public DiarySnapshot(
            Map<String, Integer> completedTasksByRegion,
            Map<String, Integer> totalTasksByRegion,
            Map<String, Map<DiaryTier, Boolean>> completedTiersByRegion)
    {
        this(completedTasksByRegion, totalTasksByRegion,
                completedTiersByRegion, Collections.emptyMap());
    }

    public DiarySnapshot(
            Map<String, Integer> completedTasksByRegion,
            Map<String, Integer> totalTasksByRegion,
            Map<String, Map<DiaryTier, Boolean>> completedTiersByRegion,
            Map<String, Boolean> observedTaskCompletion)
    {
        this.completedTasksByRegion = Collections.unmodifiableMap(
                completedTasksByRegion == null
                        ? new HashMap<>()
                        : new HashMap<>(completedTasksByRegion)
        );
        this.totalTasksByRegion = Collections.unmodifiableMap(
                totalTasksByRegion == null
                        ? new HashMap<>()
                        : new HashMap<>(totalTasksByRegion)
        );
        Map<String, Map<DiaryTier, Boolean>> tiers = new HashMap<>();
        if (completedTiersByRegion != null)
        {
            for (Map.Entry<String, Map<DiaryTier, Boolean>> entry
                    : completedTiersByRegion.entrySet())
            {
                EnumMap<DiaryTier, Boolean> copy = new EnumMap<>(DiaryTier.class);
                if (entry.getValue() != null) copy.putAll(entry.getValue());
                tiers.put(entry.getKey(), Collections.unmodifiableMap(copy));
            }
        }
        this.completedTiersByRegion = Collections.unmodifiableMap(tiers);
        this.observedTaskCompletion = Collections.unmodifiableMap(
                observedTaskCompletion == null
                        ? new HashMap<>()
                        : new HashMap<>(observedTaskCompletion));
    }

    public int completedIn(String region)
    {
        return completedTasksByRegion.getOrDefault(region, 0);
    }

    public int totalIn(String region)
    {
        return totalTasksByRegion.getOrDefault(region, 0);
    }

    public Set<String> getRegions()
    {
        Set<String> result = new HashSet<>(completedTasksByRegion.keySet());
        result.addAll(totalTasksByRegion.keySet());
        result.addAll(completedTiersByRegion.keySet());
        return Collections.unmodifiableSet(result);
    }

    public boolean isTierComplete(String region, DiaryTier tier)
    {
        var tiers = completedTiersByRegion.get(region);
        return tiers != null && Boolean.TRUE.equals(tiers.get(tier));
    }

    public Map<DiaryTier, Boolean> tiersFor(String region)
    {
        var tiers = completedTiersByRegion.get(region);
        return tiers == null ? Collections.emptyMap() : tiers;
    }


    /** Null means the individual task row has not been observed. */
    public Boolean taskCompletion(String taskId)
    {
        return observedTaskCompletion.get(taskId);
    }

}
