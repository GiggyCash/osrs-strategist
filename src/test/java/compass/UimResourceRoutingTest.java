package compass;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UimResourceRoutingTest
{
    @Test
    public void uimReadinessIgnoresNormalBankAndUsesVerifiedSafeStorageContents()
    {
        GameData data = GameData.builder(uim())
                .inventory(new ItemsState(Collections.emptyList()))
                .bank(new ItemsState(Collections.singletonList(
                        new ItemState(100, "Thing", 99)), 1L))
                .storage(storage(StorageKind.STASH, 2))
                .build();

        EvidenceCheck check = new ItemIndex(data, false).check(
                new ResourceRequirement("thing", "Thing", 2, 100));
        assertEquals(RequirementState.VERIFIED, check.getState());
    }

    @Test
    public void deathBasedOrLootingBagStorageDoesNotBecomeReadyWithoutAccessCheck()
    {
        for (StorageKind capability : new StorageKind[] {
                StorageKind.LOOTING_BAG,
                StorageKind.DEATH_STORAGE,
                StorageKind.DEATHPILE})
        {
            GameData data = GameData.builder(uim())
                    .inventory(new ItemsState(Collections.emptyList()))
                    .storage(storage(capability, 2))
                    .build();
            EvidenceCheck check = new ItemIndex(data, false).check(
                    new ResourceRequirement("thing", "Thing", 2, 100));
            assertEquals(capability.name(), RequirementState.CHECK_NEEDED,
                    check.getState());
        }
    }

    private static StorageSnapshot storage(
            StorageKind capability,
            int quantity)
    {
        Map<StorageKind, Capability> states =
                new EnumMap<>(StorageKind.class);
        states.put(capability, Capability.VERIFIED);
        Map<StorageKind, java.util.List<ItemState>> contents =
                new EnumMap<>(StorageKind.class);
        contents.put(capability, Collections.singletonList(
                new ItemState(100, "Thing", quantity)));
        return new StorageSnapshot(states, contents);
    }

    private static StrategyContext context(StorageSnapshot storage)
    {
        GameData data = GameData.builder(uim())
                .inventory(new ItemsState(Collections.emptyList()))
                .storage(storage)
                .build();
        return new StrategyContext(
                data, StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME,
                QuestTolerance.NORMAL, GoalType.MAX, false, false,
                new PreferenceProfile());
    }

    private static AccountSnapshot uim()
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 1);
            xp.put(skill, 0);
        }
        return new AccountSnapshot("UimTester", 0L, 2, "Ultimate Ironman", Membership.P2P, 1, 1, 0L, levels, xp);
    }
}
