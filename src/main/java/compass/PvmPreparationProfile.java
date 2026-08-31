package compass;

import java.util.*;

import lombok.Getter;

/** Reviewable preparation evidence that may produce actions but never READY. */
@Getter
public final class PvmPreparationProfile
{
    private String activityId;
    private String style;
    private List<String> checks;
    private String accountValue;
    private String provenance;
    private int attack, strength, defence, ranged, magic, prayer, slayer;
    private String preferredStyle;
    private String requiredQuest;
    private boolean questMayBeInProgress;
    private boolean requiresSupplies;


}
