package compass;

import java.util.*;

import lombok.Getter;

/** Ordered resource route from current ownership to the requested quantity. */
@Getter
public final class ResourceAcquisitionChain
{
    private final ResourceNeed need;
    private final int shortfall;
    private final List<ResourceAcquisitionStep> steps;

    public ResourceAcquisitionChain(ResourceNeed need, int shortfall,
            List<ResourceAcquisitionStep> steps)
    {
        this.need = need;
        this.shortfall = Math.max(0, shortfall);
        this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
    }

    public ResourceAcquisitionStep nextStep()
    {
        return steps.isEmpty() ? null : steps.get(0);
    }
}
