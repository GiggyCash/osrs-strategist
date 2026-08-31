package compass;

import javax.inject.Singleton;

/** Compatibility facade; item evidence is now resolved by the shared index. */
@Singleton
public class ResourceReadinessService
{
    public RequirementCheck evaluate(GameData data, ResourceRequirement need)
    {
        return evaluate(data, need, false);
    }

    public RequirementCheck evaluate(GameData data, ResourceRequirement need,
            boolean useGroupStorage)
    {
        return new ItemIndex(data, useGroupStorage).check(need);
    }

    public RequirementCheck evaluate(GameData data, ResourceRequirement need,
            CapabilityState alternate, String evidence)
    {
        return alternate == CapabilityState.VERIFIED
                ? new RequirementCheck(need.getId(), need.getLabel(),
                        RequirementState.VERIFIED, evidence == null
                        ? Text.get(1569) : evidence)
                : evaluate(data, need);
    }

    public int observedQuantity(GameData data, int... itemIds)
    {
        return new ItemIndex(data, false).quantity(itemIds);
    }

    public int observedQuantity(GameData data, boolean group, int... itemIds)
    {
        return new ItemIndex(data, group).quantity(itemIds);
    }
}
