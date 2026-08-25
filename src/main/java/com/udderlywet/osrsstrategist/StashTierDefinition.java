package com.udderlywet.osrsstrategist;

import net.runelite.api.Skill;

/** Current-live construction and material rules shared by every STASH tier. */
public enum StashTierDefinition
{
    BEGINNER(ClueTier.BEGINNER, 12, "plank", false),
    EASY(ClueTier.EASY, 27, "plank", false),
    MEDIUM(ClueTier.MEDIUM, 42, "oak plank", false),
    HARD(ClueTier.HARD, 55, "teak plank", false),
    ELITE(ClueTier.ELITE, 77, "mahogany plank", false),
    MASTER(ClueTier.MASTER, 88, "mahogany plank", true);

    private final ClueTier clueTier;
    private final int constructionLevel;
    private final String plank;
    private final boolean goldLeaf;

    StashTierDefinition(ClueTier clueTier, int constructionLevel,
            String plank, boolean goldLeaf)
    {
        this.clueTier = clueTier;
        this.constructionLevel = constructionLevel;
        this.plank = plank;
        this.goldLeaf = goldLeaf;
    }

    public ClueTier getClueTier() { return clueTier; }
    public int getConstructionLevel() { return constructionLevel; }
    public Skill getSkill() { return Skill.CONSTRUCTION; }
    public String getPlank() { return plank; }
    public boolean requiresGoldLeaf() { return goldLeaf; }

    public ItemRequirementExpression materials()
    {
        ItemRequirementExpression planks = ItemRequirementExpression.item(
                plank, 2, ItemRequirementScope.IMMEDIATELY_USABLE);
        ItemRequirementExpression nails = ItemRequirementExpression.itemClass(
                ItemRequirementClass.NAILS, 10,
                ItemRequirementScope.IMMEDIATELY_USABLE, "dragon nails");
        return goldLeaf
                ? ItemRequirementExpression.allOf(planks, nails,
                        ItemRequirementExpression.item("gold leaf", 1,
                                ItemRequirementScope.IMMEDIATELY_USABLE))
                : ItemRequirementExpression.allOf(planks, nails);
    }
}
