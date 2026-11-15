package compass;

import java.util.*;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

public class SmithingProfileRecipeTest
{
    private final UniversalActionRecipeResolver resolver = new UniversalActionRecipeResolver();
    private final MethodExecutionProfileCatalog profiles = new MethodExecutionProfileCatalog();

    @Test
    public void bronzeProfileCountsActualBarsInsteadOfOneForEveryNonPlatebody()
    {
        MethodProfile profile = profiles.forMethod("smithing_f2p_uim_bronze");
        assertEquals(17, profile.reviewedRecipes.size());
        String[] names = {"Bronze dagger", "Bronze scimitar", "Bronze sq shield", "Bronze platelegs", "Bronze platebody"};
        int[] units = {1, 2, 2, 3, 5};
        for (int i = 0; i < names.length; i++)
        {
            List<MethodInput> needs = resolver.profileInputs(profile, action(names[i]), 7, Membership.F2P);
            assertEquals(1, needs.size());
            assertEquals(2349, needs.get(0).itemId);
            assertEquals(7 * units[i], needs.get(0).quantity);
            assertEquals(needs.get(0).quantity, resolver.resolve(action(names[i]), 7, Membership.F2P).inputs.get(0).quantity);
        }
    }

    @Test
    public void platebodyAndDartProfilesKeepTheirDistinctActionUnits()
    {
        MethodProfile plates = profiles.forMethod("smithing_f2p_platebodies");
        MethodProfile darts = profiles.forMethod("smithing_dart_tips");
        assertEquals(6, plates.reviewedRecipes.size());
        assertEquals(6, darts.reviewedRecipes.size());
        assertEquals(15, resolver.profileInputs(plates, action("Rune platebody"), 3, Membership.F2P).get(0).quantity);
        assertEquals(2363, resolver.profileInputs(plates, action("Rune platebody"), 3, Membership.F2P).get(0).itemId);
        assertEquals(3, resolver.profileInputs(darts, action("Rune dart tip"), 3, Membership.P2P).get(0).quantity);
        for (Membership membership : new Membership[] {Membership.F2P, Membership.UNKNOWN})
            assertNull(resolver.profileInputs(darts, action("Rune dart tip"), 3, membership));
    }

    @Test
    public void broadBronzeTermsCannotInventAnvilInputsForAnotherActivity()
    {
        MethodProfile bronze = profiles.forMethod("smithing_f2p_uim_bronze");
        for (String name : Arrays.asList("Bronze bar", "Bronze cannonball", "Bronze spear", "Bronze claws", "Future bronze platebody"))
            assertNull(name, resolver.profileInputs(bronze, action(name), 10, Membership.P2P));
        assertNull(resolver.profileInputs(profiles.forMethod("smithing_f2p_platebodies"), action("Bronze dagger"), 10, Membership.P2P));
    }

    private static ActionDef action(String name)
    {
        return new ActionDef(Skill.SMITHING, "test:" + name, name, 1, 1, "test", Membership.F2P);
    }
}
