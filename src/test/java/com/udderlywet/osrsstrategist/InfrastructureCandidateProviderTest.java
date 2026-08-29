package com.udderlywet.osrsstrategist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ObjectID;
import org.junit.Test;

public class InfrastructureCandidateProviderTest
{
    private final InfrastructureMilestoneCatalog catalog =
            new InfrastructureMilestoneCatalog();
    private final InfrastructureCandidateProvider provider =
            new InfrastructureCandidateProvider(catalog,
                    new InfrastructureUnlockValueService());

    @Test
    public void unknownPohGetsOneOwnershipSafeVerificationAction()
    {
        StrategyCandidate candidate = provider.candidates(context(
                AccountMode.ULTIMATE_IRONMAN, MembershipStatus.P2P,
                46, 25, null)).get(0);

        assertEquals("verify:poh-build-mode", candidate.getId());
        assertTrue(candidate.getGuidance().getAction().contains("Build mode"));
        assertTrue(new RecommendationActionabilityPolicy().canLeadQueue(
                candidate.toRecommendation()));
    }

    @Test
    public void observedMissingArmourCaseBecomesExactPreparation()
    {
        PohSnapshot roomOnly = LivePohStateReader.snapshotForObjectIds(
                java.util.Collections.singleton(
                        ObjectID.POH_COS_ROOM_ARMOUR_CASE_HOTSPOT));
        List<StrategyCandidate> candidates = provider.candidates(context(
                AccountMode.ULTIMATE_IRONMAN, MembershipStatus.P2P,
                46, 25, roomOnly));
        StrategyCandidate armour = candidates.stream()
                .filter(value -> value.getId().endsWith("poh-armour-case"))
                .findFirst().orElseThrow(AssertionError::new);

        assertEquals("Build Oak armour case", armour.getTitle());
        assertTrue(armour.getGuidance().getSupplies().contains("3 oak planks"));
        assertEquals(RecommendationConfidence.CHECK_NEEDED,
                armour.getConfidence());
        assertTrue(new RecommendationActionabilityPolicy().canLeadQueue(
                armour.toRecommendation()));
    }

    @Test
    public void identicalInfrastructurePropertiesCarryMoreUimValue()
    {
        PohSnapshot empty = LivePohStateReader.snapshotForObjectIds(
                java.util.Collections.emptySet());
        StrategyCandidate main = find(provider.candidates(context(
                AccountMode.MAIN, MembershipStatus.P2P, 50, 25, empty)),
                "poh-costume-room");
        StrategyCandidate uim = find(provider.candidates(context(
                AccountMode.ULTIMATE_IRONMAN, MembershipStatus.P2P,
                50, 25, empty)), "poh-costume-room");

        assertTrue(uim.getScore() > main.getScore());
        assertTrue(uim.getStrategicValue().getAccountModeFit()
                > main.getStrategicValue().getAccountModeFit());
    }

    @Test
    public void f2pNeverReceivesPohCandidates()
    {
        assertTrue(provider.candidates(context(AccountMode.MAIN,
                MembershipStatus.F2P, 99, 99, null)).isEmpty());
    }

    private static StrategyCandidate find(List<StrategyCandidate> values,
            String suffix)
    {
        return values.stream().filter(value -> value.getId().endsWith(suffix))
                .findFirst().orElseThrow(AssertionError::new);
    }

    private static StrategyContext context(AccountMode mode,
            MembershipStatus membership, int construction, int magic,
            PohSnapshot poh)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 1);
            xp.put(skill, 0);
        }
        levels.put(Skill.CONSTRUCTION, construction);
        levels.put(Skill.MAGIC, magic);
        AccountSnapshot account = new AccountSnapshot("Infrastructure", 88L,
                type(mode), mode.name(), membership, 1, 1, 0L, levels, xp);
        StrategyDataBundle data = StrategyDataBundle.builder(account)
                .poh(poh).build();
        return new StrategyContext(data, StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME, QuestTolerance.NORMAL,
                GoalType.AUTOMATIC, false, false, new PreferenceProfile());
    }

    private static int type(AccountMode mode)
    {
        for (int type = 0; type <= 6; type++)
            if (AccountMode.fromTypeCode(type) == mode) return type;
        return -1;
    }
}
