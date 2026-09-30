package compass;

import java.util.*;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReviewedExecutionProfileTest
{
    private final MethodExecutionProfileCatalog profiles = new MethodExecutionProfileCatalog();
    private final UniversalActionRecipeResolver resolver = new UniversalActionRecipeResolver();

    @Test
    public void migratedProfilesUseReviewedIdsWithoutDuplicateRules()
    {
        String[] ids = {"fletching_darts", "cooking_f2p_fish", "cooking_f2p_fish_baseline", "cooking_wines"};
        String[] names = {"Bronze dart", "Salmon", "Salmon", "Jug of wine"};
        for (int i = 0; i < ids.length; i++)
        {
            MethodProfile profile = profiles.forMethod(ids[i]);
            Skill skill = i == 0 ? Skill.FLETCHING : Skill.COOKING;
            ActionDef action = action(skill, names[i], 1);
            assertNotNull(profile.reviewedRecipes);
            assertTrue(profile.inputs.isEmpty());
            List<MethodInput> inputs = resolver.profileInputs(profile, action, 10, Membership.P2P);
            List<MethodInput> universal = resolver.resolve(action, 10, Membership.P2P).inputs;
            assertEquals(universal.size(), inputs.size());
            for (int j = 0; j < inputs.size(); j++)
            {
                assertTrue(inputs.get(j).itemId > 0);
                assertEquals(universal.get(j).itemId, inputs.get(j).itemId);
                assertEquals(universal.get(j).quantity, inputs.get(j).quantity);
            }
        }
    }

    @Test
    public void unknownActionsMembershipAndInlineOverridesCannotBypassReview()
    {
        MethodProfile profile = profiles.forMethod("fletching_darts");
        assertNull(resolver.profileInputs(profile, action(Skill.FLETCHING, "Future bronze dart", 1), 10, Membership.P2P));
        for (Membership membership : new Membership[] {Membership.F2P, Membership.UNKNOWN})
            assertNull(resolver.profileInputs(profile, action(Skill.FLETCHING, "Bronze dart", 1), 10, membership));
        profile.inputs = Collections.singletonList(new MethodInputRule(MethodProfile.InputMode.FIXED, "Feather", 1));
        assertNull(resolver.profileInputs(profile, action(Skill.FLETCHING, "Bronze dart", 1), 10, Membership.P2P));
    }

    @Test
    public void adaptiveSelectionRejectsUnknownNamesEvenWhenProfileTermsMatch()
    {
        MethodProfile profile = profiles.forMethod("fletching_darts");
        ActionDef unknown = action(Skill.FLETCHING, "Future bronze dart", 10000);
        ActionDef known = action(Skill.FLETCHING, "Bronze dart", 1.8f);
        AdaptiveActionSelector selector = new AdaptiveActionSelector();
        for (int mode : new int[] {0, 1, 2})
        {
            GameData data = MethodIntelligenceTest.data(mode, -1, Collections.emptyList(), Collections.emptyList());
            assertNull(selector.select(data, profile, Collections.singletonList(unknown), 80,
                    Membership.P2P, 0, 10000, 1, false));
            assertSame(known, selector.select(data, profile, Arrays.asList(unknown, known), 80,
                    Membership.P2P, 0, 10000, 1, false));
        }
    }

    @Test
    public void unresolvedKarambwanProfileCannotInventIngredientsOrWinSelection()
    {
        MethodProfile profile = profiles.forMethod("cooking_karambwan_1t");
        assertNotNull(profile.reviewedRecipes);
        assertTrue(profile.inputs.isEmpty());
        for (String name : Arrays.asList("Cooked karambwan", "Poison karambwan", "Future cooked karambwan"))
        {
            ActionDef action = action(Skill.COOKING, name, 190);
            for (Membership membership : Membership.values())
                assertNull(resolver.profileInputs(profile, action, 10, membership));
            for (int mode : new int[] {0, 1, 2})
            {
                GameData data = MethodIntelligenceTest.data(mode, -1, Collections.emptyList(), Collections.emptyList());
                assertNull(new AdaptiveActionSelector().select(data, profile,
                        Collections.singletonList(action), 80, Membership.P2P, 0, 10000, 1, false));
            }
        }
    }

    @Test
    public void reviewedKnowledgeCannotLeakAcrossMethodContracts()
    {
        ReviewedActionRecipeCatalog.Recipe[] recipes = BundledCatalogLoader.array(
                "/content/catalogs/reviewed-action-recipes.json", ReviewedActionRecipeCatalog.Recipe[].class);
        for (MethodProfile profile : profiles.all().values())
        {
            if (profile.reviewedRecipes == null) continue;
            for (ReviewedActionRecipeCatalog.Recipe recipe : recipes)
            {
                ActionDef action = action(Skill.valueOf(recipe.skill), recipe.match, 1);
                boolean allowed = profile.reviewedRecipes.contains(recipe.skill + ":" + recipe.match);
                assertEquals(profile.methodId + " / " + recipe.match, allowed,
                        resolver.profileInputs(profile, action, 10, Membership.P2P) != null);
            }
        }
    }

    @Test
    public void misleadingSearchTermsCannotAdmitAnotherReviewedActivity()
    {
        MethodProfile profile = profiles.forMethod("fletching_darts");
        ActionDef otherActivity = new ActionDef(Skill.FLETCHING, "test:bronze_dart", "Arrow shaft",
                1, 1000, "bronze_dart", Membership.P2P);
        ActionDef supported = action(Skill.FLETCHING, "Bronze dart", 1.8f);
        assertTrue(resolver.resolve(otherActivity, 10, Membership.P2P).hasExactInputs());
        for (int mode = 0; mode <= 6; mode++)
        {
            GameData data = MethodIntelligenceTest.data(mode, -1, Collections.emptyList(), Collections.emptyList());
            assertSame(supported, new AdaptiveActionSelector().select(data, profile,
                    Arrays.asList(otherActivity, supported), 99, Membership.P2P, 0, 10000, 1, false));
        }
    }

    private static ActionDef action(Skill skill, String name, float xp)
    {
        return new ActionDef(skill, "test:" + name.toLowerCase(Locale.ROOT).replace(' ', '_'),
                name, 1, xp, "test", Membership.F2P);
    }
}
