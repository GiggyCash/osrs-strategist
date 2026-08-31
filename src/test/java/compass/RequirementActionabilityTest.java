package compass;

import java.util.Collections;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RequirementActionabilityTest
{
    @Test
    public void knownSupplyShortfallCanStillBeDoNext()
    {
        TrainingMethod method = method("smithing_test", Skill.SMITHING);
        TrainingPlan plan = new TrainingPlan(
                method,
                "test",
                Confidence.CHECK_NEEDED,
                Collections.singletonList(new RequirementCheck(
                        "generic:Steel bar supply",
                        "Steel bar supply",
                        RequirementState.CHECK_NEEDED,
                        "Acquire the missing bars.")));
        Recommendation recommendation = new Recommendation(
                "skill:smithing",
                "Train Smithing to 50",
                "test",
                10.0,
                plan,
                Confidence.CHECK_NEEDED,
                40,
                50,
                new Guidance(
                        "Smith steel platebodies until the milestone.",
                        "Need 500 steel bars. Buy or self-source the missing amount.",
                        "Varrock West anvils, directly south of the bank.",
                        "The route is known; only supplies remain."));

        ActionabilityPolicy policy =
                new ActionabilityPolicy();
        assertTrue(RequirementActionability.isActionablePreparation(
                plan, recommendation.getGuidance()));
        assertTrue(policy.canLeadQueue(recommendation));
        assertTrue(Presentation.compactText(recommendation)
                .contains("Need 500 steel bars"));
        assertTrue(Presentation.compactText(recommendation)
                .contains("BRING"));
    }

    @Test
    public void unknownAccessCanNeverBeDoNext()
    {
        TrainingMethod method = method("minigame_test", Skill.MINING);
        TrainingPlan plan = new TrainingPlan(
                method,
                "test",
                Confidence.CHECK_NEEDED,
                Collections.singletonList(new RequirementCheck(
                        "generic:Volcanic Mine access",
                        "Volcanic Mine access",
                        RequirementState.CHECK_NEEDED,
                        "Access has not been proven.")));
        Recommendation recommendation = new Recommendation(
                "skill:mining",
                "Train Mining",
                "test",
                1000.0,
                plan,
                Confidence.CHECK_NEEDED,
                70,
                80,
                new Guidance(
                        "Run Volcanic Mine.",
                        "Bring your best pickaxe.",
                        "Volcanic Mine.",
                        "Access must be verified."));

        assertFalse(RequirementActionability.isActionablePreparation(
                plan, recommendation.getGuidance()));
        assertTrue(RequirementActionability.hasHardUnresolvedRequirement(plan));
        assertFalse(new ActionabilityPolicy()
                .canLeadQueue(recommendation));
        assertFalse(new ActionabilityPolicy()
                .mayAppearAsAlternative(recommendation));
        assertTrue(Presentation.compactText(recommendation)
                .contains("NEEDED"));
    }

    @Test
    public void accessAndSupplyInOneLabelRemainsAHardGate()
    {
        TrainingPlan plan = new TrainingPlan(
                method("runecraft_zmi", Skill.RUNECRAFT),
                "test",
                Confidence.CHECK_NEEDED,
                Collections.singletonList(new RequirementCheck(
                        "generic:Ourania Altar route and essence supply",
                        "Ourania Altar route and essence supply",
                        RequirementState.CHECK_NEEDED,
                        "Route not observed.")));

        assertTrue(RequirementActionability.hasHardUnresolvedRequirement(plan));
        assertFalse(RequirementActionability.isActionablePreparation(plan,
                new Guidance(
                        "Run essence to the altar.",
                        "Bring pure essence.",
                        "Ourania Altar.",
                        "Route access is unknown.")));
    }

    @Test
    public void typedResourceShortfallIsPreparationNotUnknownAccess()
    {
        TrainingPlan plan = new TrainingPlan(
                method("runecraft_f2p_earth", Skill.RUNECRAFT),
                "test", Confidence.CHECK_NEEDED,
                java.util.Arrays.asList(
                        new RequirementCheck("resource:runecraft_essence",
                                "Rune or pure essence",
                                RequirementState.CHECK_NEEDED,
                                "No essence is currently observed."),
                        new RequirementCheck("resource:runecraft_earth_entry",
                                "Earth talisman or earth tiara",
                                RequirementState.CHECK_NEEDED,
                                "No entry item is currently observed.")));
        Guidance guidance = new Guidance(
                "Bank at Varrock East, craft earth runes, and repeat.",
                "Acquire essence and an earth talisman before starting.",
                "Earth Altar northeast of Varrock.", null);

        assertFalse(RequirementActionability.hasHardUnresolvedRequirement(plan));
        assertTrue(RequirementActionability.isActionablePreparation(
                plan, guidance));
    }

    @Test
    public void knownTypedBoatSetupCanLeadPreparation()
    {
        RequirementCheck check = new RequirementCheck(
                "preparation:sailing-trial-boat", "Trial-ready boat",
                RequirementState.CHECK_NEEDED,
                "Fit an iron helm, oak mast, and linen sails.");
        TrainingPlan plan = new TrainingPlan(
                method("sailing_barracuda_tantrum", Skill.SAILING),
                "test", Confidence.CHECK_NEEDED,
                Collections.singletonList(check));

        assertFalse(RequirementActionability.hasHardUnresolvedRequirement(plan));
        assertTrue(RequirementActionability.isActionablePreparation(plan,
                new Guidance("Run Tempor Tantrum.",
                        "Fit an iron helm, oak mast, and linen sails.",
                        "Rum-dashed Ralph north-west of The Storm Tempor.", null)));
    }

    @Test
    public void locationDoesNotMasqueradeAsALogSupply()
    {
        RequirementCheck check = new RequirementCheck(
                "generic:Forestry-enabled tree location",
                "Forestry-enabled tree location",
                RequirementState.CHECK_NEEDED,
                "No location is proven.");
        TrainingPlan plan = new TrainingPlan(
                method("woodcutting_forestry", Skill.WOODCUTTING),
                "test", Confidence.CHECK_NEEDED,
                Collections.singletonList(check));

        assertTrue(RequirementActionability.hasHardUnresolvedRequirement(plan));
    }

    @Test
    public void barbarianFishingDoesNotMasqueradeAsAMetalBarShortfall()
    {
        TrainingPlan plan = new TrainingPlan(
                method("fishing_barbarian", Skill.FISHING),
                "test", Confidence.CHECK_NEEDED,
                Collections.singletonList(new RequirementCheck(
                        "generic:Barbarian Fishing training",
                        "Barbarian Fishing training",
                        RequirementState.CHECK_NEEDED,
                        "Training unlock has not been observed.")));

        assertTrue(RequirementActionability.hasHardUnresolvedRequirement(plan));
        assertFalse(RequirementActionability.isActionablePreparation(plan,
                new Guidance("Catch leaping fish.",
                        "Bring feathers.", "Otto's Grotto.", null)));
    }

    @Test
    public void unknownFarmingPatchIsNotJustASeedShortfall()
    {
        TrainingPlan plan = new TrainingPlan(
                method("farming_allotments_expanded", Skill.FARMING),
                "test", Confidence.CHECK_NEEDED,
                Collections.singletonList(new RequirementCheck(
                        "generic:Reachable allotment patches and supplies",
                        "Reachable allotment patches and supplies",
                        RequirementState.CHECK_NEEDED,
                        "No patch has been proven.")));

        assertTrue(RequirementActionability.hasHardUnresolvedRequirement(plan));
    }

    @Test
    public void retrievalOnlyUimResourceStillRequiresExplicitRoute()
    {
        RequirementCheck check = new RequirementCheck(
                "resource:runecraft_essence", "Rune or pure essence",
                RequirementState.CHECK_NEEDED,
                "Enough is observed only after counting UIM storage with additional access/risk preconditions; verify that route before using the resource.");
        TrainingPlan plan = new TrainingPlan(
                method("runecraft_f2p_earth", Skill.RUNECRAFT),
                "test", Confidence.CHECK_NEEDED,
                Collections.singletonList(check));

        assertTrue(RequirementActionability.hasHardUnresolvedRequirement(plan));
    }

    @Test
    public void blockedRequirementCanNeverBePrep()
    {
        TrainingMethod method = method("blocked_test", Skill.PRAYER);
        TrainingPlan plan = new TrainingPlan(
                method,
                "test",
                Confidence.BLOCKED,
                Collections.singletonList(new RequirementCheck(
                        "build:restriction",
                        "Build restriction",
                        RequirementState.BLOCKED,
                        "This action would violate the protected account build.")));
        Recommendation recommendation = new Recommendation(
                "skill:prayer",
                "Train Prayer",
                "test",
                1000.0,
                plan,
                Confidence.BLOCKED,
                1,
                43,
                new Guidance(
                        "Do the blocked action.",
                        "None.",
                        "Nowhere.",
                        "Blocked."));

        assertFalse(new ActionabilityPolicy()
                .canLeadQueue(recommendation));
    }

    private static TrainingMethod method(String id, Skill skill)
    {
        return new TrainingMethod(
                id,
                skill,
                1,
                99,
                id,
                "test",
                10,
                10,
                10,
                AttentionLevel.MODERATE,
                10,
                1,
                Collections.emptyList(),
                Confidence.CHECK_NEEDED);
    }
}
