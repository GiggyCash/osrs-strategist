package compass;

import java.util.*;

import lombok.Getter;

import net.runelite.api.Skill;

@Getter
public final class MinigameDefinition
{
    final String id;
    private final String name;
    private final Skill primarySkill;
    private final int minimumLevel;
    private final boolean freeToPlay;
    private final RiskLevel riskLevel;
    private final AttentionLevel attention;
    private final Set<AccountMode> supportedModes;
    private final String rewardFocus;
    private final boolean combatActivity;

    public MinigameDefinition(String id, String name, Skill primarySkill,
            int minimumLevel, boolean freeToPlay, RiskLevel riskLevel,
            AttentionLevel attention, Set<AccountMode> supportedModes,
            String rewardFocus)
    {
        this(id, name, primarySkill, minimumLevel, freeToPlay, riskLevel,
                attention, supportedModes, rewardFocus, false);
    }

    public MinigameDefinition(String id, String name, Skill primarySkill,
            int minimumLevel, boolean freeToPlay, RiskLevel riskLevel,
            AttentionLevel attention, Set<AccountMode> supportedModes,
            String rewardFocus, boolean combatActivity)
    {
        this.id = id;
        this.name = name;
        this.primarySkill = primarySkill;
        this.minimumLevel = Math.max(1, minimumLevel);
        this.freeToPlay = freeToPlay;
        this.riskLevel = riskLevel == null ? RiskLevel.NONE : riskLevel;
        this.attention = attention == null ? AttentionLevel.MODERATE : attention;
        this.supportedModes = supportedModes == null || supportedModes.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(supportedModes));
        this.rewardFocus = rewardFocus;
        this.combatActivity = combatActivity;
    }

    public boolean supports(AccountMode mode) { return supportedModes.contains(mode); }
}
