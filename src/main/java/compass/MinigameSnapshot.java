package compass;

import java.util.*;

import lombok.Getter;

/**
 * Minigame unlocks and currencies observed on the account.
 *
 * <p>This covers progression systems such as Guardians of the Rift,
 * Tempoross, Wintertodt, Giants' Foundry, Mahogany Homes, Barbarian Assault,
 * and future minigames without teaching the strategy engine each minigame's
 * internal storage format.</p>
 */
@Getter
public final class MinigameSnapshot
{
    private final Set<String> unlocked;
    private final Map<String, Integer> currencies;

    public MinigameSnapshot(
            Set<String> unlocked,
            Map<String, Integer> currencies)
    {
        this.unlocked = Collections.unmodifiableSet(
                unlocked == null
                        ? new HashSet<>()
                        : new HashSet<>(unlocked)
        );
        this.currencies = Collections.unmodifiableMap(
                currencies == null
                        ? new HashMap<>()
                        : new HashMap<>(currencies)
        );
    }

    public static MinigameSnapshot unknown()
    {
        return new MinigameSnapshot(
                Collections.emptySet(),
                Collections.emptyMap()
        );
    }

    public boolean isUnlocked(String minigameId)
    {
        return minigameId != null && unlocked.contains(minigameId);
    }

    public int currency(String currencyId)
    {
        return currencies.getOrDefault(currencyId, 0);
    }

}
