package compass;

import lombok.Getter;

import net.runelite.api.Skill;

/** One action exposed by RuneLite's maintained skill-calculator data. */
@Getter
public final class ActionDef
{
    private final Skill skill;
    final String id;
    private final String name;
    private final int level;
    private final float xp;
    private final String category;
    private final MembershipStatus membership;
    private final int itemId;

    public ActionDef(Skill skill, String id, String name,
            int level, float xp, String category, MembershipStatus membership)
    {
        this(skill, id, name, level, xp, category, membership, -1);
    }

    public ActionDef(Skill skill, String id, String name,
            int level, float xp, String category, MembershipStatus membership,
            int itemId)
    {
        this.skill = skill;
        this.id = id;
        this.name = name;
        this.level = level;
        this.xp = xp;
        this.category = category;
        this.membership = membership == null ? MembershipStatus.UNKNOWN : membership;
        this.itemId = itemId;
    }

}
