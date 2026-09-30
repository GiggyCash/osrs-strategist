package compass;

import java.util.*;
import java.util.function.Consumer;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

public class MethodIntelligenceCatalogTest
{
    @Test
    public void productionContractsValidate()
    {
        assertEquals(8, new MethodIntelligenceCatalog().methodIds().size());
    }

    @Test
    public void accessFlagsMustBeExplicit()
    {
        rejects(bundle -> bundle.methods.get(0).membersOnly = null);
        rejects(bundle -> bundle.methods.get(0).bankLoop = null);
        rejects(bundle -> bundle.methods.get(0).purchaseAllowed = null);
    }

    @Test
    public void diaryGateNeedsBothRegionAndTier()
    {
        rejects(bundle -> bundle.methods.get(0).diaryRegion = "Kourend & Kebos");
        rejects(bundle -> bundle.methods.get(0).diaryTier = DiaryTier.EASY);
        rejects(bundle -> bundle.methods.get(0).requiredQuest = " ");
        rejects(bundle -> bundle.methods.get(0).observedAccess = " ");
    }

    @Test
    public void recipeCannotOmitRetainedOutputsOrInvertBatchThresholds()
    {
        rejects(bundle -> bundle.recipes.get(0).outputsPerAction = null);
        rejects(bundle -> bundle.recipes.get(0).outputsPerAction = -1);
        rejects(bundle -> bundle.recipes.get(0).outputsPerAction = Integer.MAX_VALUE);
        rejects(bundle -> bundle.recipes.get(0).efficientBatch = 1);
        rejects(bundle -> bundle.recipes.get(0).level = 100);
    }

    @Test
    public void identitiesAndGuidanceMustBeNonblankAndUnique()
    {
        rejects(bundle -> bundle.recipes.get(0).id = " ");
        rejects(bundle -> bundle.recipes.get(0).action = " ");
        rejects(bundle -> bundle.recipes.get(0).outputDescription = " ");
        rejects(bundle -> bundle.recipes.get(0).inputs.get(0).name = " ");
        rejects(bundle -> bundle.methods.get(0).ids = Collections.singletonList(" "));
        rejects(bundle -> bundle.methods.get(0).location = " ");
        rejects(bundle -> bundle.methods.add(bundle.methods.get(0)));
        rejects(bundle -> bundle.recipes.add(bundle.recipes.get(0)));
    }

    @Test
    public void recipeReferencesMustResolveOnceWithinOneSkill()
    {
        rejects(bundle -> bundle.methods.get(0).recipes = Collections.singletonList("missing"));
        rejects(bundle -> bundle.methods.get(0).recipes = Arrays.asList("shrimp", "shrimp"));
        rejects(bundle -> bundle.recipes.get(0).skill = Skill.FISHING);
    }

    @Test
    public void toolsAndInputsCannotOverlapOrRecycleOutputs()
    {
        rejects(bundle -> bundle.recipes.get(0).tools.add(bundle.recipes.get(0).inputs.get(0)));
        rejects(bundle -> bundle.recipes.get(0).outputItemId = bundle.recipes.get(0).inputs.get(0).itemId);
        rejects(bundle -> bundle.recipes.get(0).inputs.get(0).equippedAllowed = true);
    }

    @Test
    public void nullRowsAndMissingProvenanceFailClosed()
    {
        rejects(bundle -> bundle.recipes.set(0, null));
        rejects(bundle -> bundle.methods.set(0, null));
        rejects(bundle -> bundle.methods.get(0).sourceRevision = 0);
        rejects(bundle -> bundle.methods.get(0).reviewed = "9999-01-01");
        rejects(bundle -> bundle.methods.get(0).recipes.clear());
    }

    private static void rejects(Consumer<MethodIntelligenceCatalog.Bundle> mutation)
    {
        MethodIntelligenceCatalog.Bundle bundle = BundledCatalogLoader.array(
                "/content/catalogs/method-intelligence.json", MethodIntelligenceCatalog.Bundle[].class)[0];
        // Keep inline-input validator coverage independent of reference resolution.
        MethodIntelligenceCatalog.Recipe inline = new MethodIntelligenceCatalog().recipe("shrimp");
        inline.inputRecipe = null;
        bundle.recipes.set(0, inline);
        mutation.accept(bundle);
        assertThrows(IllegalStateException.class, () -> new MethodIntelligenceCatalog(bundle));
    }
}
