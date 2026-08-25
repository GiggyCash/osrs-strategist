package com.udderlywet.osrsstrategist;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class QuestItemEvidenceParserTest
{
    @Test
    public void sourceAndPhaseAnnotationsDoNotHideExactRequirements()
    {
        QuestItemEvidenceParser parser = new QuestItemEvidenceParser();
        QuestItemEvidenceParser.Result result = parser.parse(
                "*[[Rope]] (obtainable during the quest)\n"
                        + "*5 [[iron bar]]s (unnoted)\n"
                        + "*A [[tinderbox]] (part 2)");
        assertTrue(result.getUnresolved().toString(), result.isFullyExecutable());
        assertNotNull(result.getExpression());
        assertEquals(3, result.getParsedLineCount());
    }

    @Test
    public void semanticParentheticalsRemainUnresolved()
    {
        QuestItemEvidenceParser.Result result = parser.parse(
                "*[[Rope]] (consumed during the quest)\n"
                        + "*[[Ghostspeak amulet]] ([[Morytania legs 2]] or better also work)");
        assertNull(result.getExpression());
        assertEquals(2, result.getUnresolved().size());
    }

    @Test
    public void exactParentheticalSubstitutesBecomeAnyOf()
    {
        QuestItemEvidenceParser.Result result = parser.parse(
                "*[[Secateurs]] (obtainable during quest) "
                        + "([[Magic secateurs]] also work)\n"
                        + "*[[Saw]] (obtainable during the quest) or [[Amy's saw]]");

        assertTrue(result.getUnresolved().toString(), result.isFullyExecutable());
        assertEquals(2, result.getParsedLineCount());
        assertEquals("(Secateurs or Magic secateurs) and (Saw or Amy's saw)",
                result.getExpression().label());
    }

    @Test
    public void parsesNestedExactRequirementsAndIgnoresHeadings()
    {
        QuestItemEvidenceParser.Result result = parser.parse(
                "*TRIP 1 (starting the quest):\n"
                        + "**3 unnoted [[papyrus]]\n"
                        + "**A [[ball of wool]]\n"
                        + "*Farming:\n"
                        + "**A [[spade]] (part 2)");

        assertTrue(result.isFullyExecutable());
        assertEquals(3, result.getParsedLineCount());
        assertEquals("3 × Papyrus and Ball of wool and Spade",
                result.getExpression().label());
    }

    @Test
    public void acquisitionProseDoesNotHideConcreteRequirements()
    {
        QuestItemEvidenceParser.Result result = parser.parse(
                "*[[Swamp paste]] (made by mixing [[flour]] with [[swamp tar]]; "
                        + "can also be bought from a trader or a general store)\n"
                        + "*[[Ice gloves]] ''for defeating Flambeed''\n"
                        + "*[[Menaphite remedy]] or [[restore potion]]s "
                        + "''since Karamel lowers stats''");

        assertTrue(result.isFullyExecutable());
        assertEquals(3, result.getParsedLineCount());
        assertEquals(ItemRequirementExpression.Kind.ALL_OF,
                result.getExpression().getKind());
    }

    @Test
    public void parsesBareCoinQuantitiesAndKeepsConditionalCostsExplicit()
    {
        QuestItemEvidenceParser.Result result = parser.parse(
                "*500,000 coins\n"
                        + "*208 [[coins]] to buy all the required alcohol\n"
                        + "*1,000 [[coins]] if you used the gate entrance");

        assertEquals(3, result.getParsedLineCount());
        assertTrue(result.getUnresolved().isEmpty());
        assertTrue(result.getExpression().label()
                .contains("if so, bring 1000 × Coins"));
    }

    @Test
    public void optionalItemsDoNotBecomeMandatory()
    {
        QuestItemEvidenceParser.Result result = parser.parse(
                "*100 [[coins]] required each time the banker is used (optional)");

        assertTrue(result.isFullyExecutable());
        assertNull(result.getExpression());
        assertTrue(result.getUnresolved().isEmpty());
    }

    @Test
    public void genericRequirementsRemainSemanticClasses()
    {
        QuestItemEvidenceParser.Result result = parser.parse(
                "*Any [[axe]] (a [[bronze axe]] is obtainable during the quest)\n"
                        + "*A [[Light sources|light source]] ([[Kandarin headgear]], "
                        + "[[Firemaking cape]], or a [[bruma torch]] are recommended)\n"
                        + "*Any [[cat]] except an [[overgrown cat]]\n"
                        + "*Something to cut webs ([[knife]] or a [[slash weapon]])");

        assertTrue(result.isFullyExecutable());
        assertEquals(4, result.getParsedLineCount());
        assertEquals(ItemRequirementExpression.Kind.ITEM_CLASS,
                result.getExpression().getChildren().get(0).getKind());
        assertEquals(ItemRequirementClass.AXE,
                result.getExpression().getChildren().get(0).getItemClass());
        assertEquals("Overgrown cat", result.getExpression().getChildren().get(2)
                .getExcludedItemNames().get(0));
    }

    @Test
    public void conditionalToolClassesRemainUnresolved()
    {
        QuestItemEvidenceParser.Result result = parser.parse(
                "*[[Pickaxe]] (if not using a games necklace)");
        assertTrue(result.isFullyExecutable());
        assertEquals(ItemRequirementExpression.Kind.CHECK_NEEDED,
                result.getExpression().getKind());
        assertTrue(result.getExpression().getCheckAction()
                .contains("if so, bring any usable pickaxe"));
    }

    @Test
    public void factionRoutesBecomeMutuallyExclusiveAlternatives()
    {
        QuestItemEvidenceParser.Result result = parser.parse(
                "*Phoenix Gang: 20 [[coins]]\n"
                        + "*Black Arm Gang: 2 [[Phoenix crossbow]]s "
                        + "(obtained during the quest)");

        assertTrue(result.isFullyExecutable());
        assertEquals(ItemRequirementExpression.Kind.ANY_OF,
                result.getExpression().getKind());
        assertEquals("20 × Coins or 2 × Phoenix crossbow",
                result.getExpression().label());
    }

    @Test
    public void itemOrSkillAccessBecomesAnExplicitVerification()
    {
        QuestItemEvidenceParser.Result result = parser.parse(
                "*[[Dusty key]] (obtainable during the quest) or "
                        + "<span data-skill=\"Agility\" data-level=\"70\">70 Agility</span>");

        assertTrue(result.isFullyExecutable());
        assertEquals(ItemRequirementExpression.Kind.CHECK_NEEDED,
                result.getExpression().getKind());
        assertEquals("Bring Dusty key or verify access using 70 Agility",
                result.getExpression().getCheckAction());
    }

    @Test
    public void parsesConcreteCommaAlternativesAndParenthesizedNames()
    {
        QuestItemEvidenceParser.Result result = parser.parse(
                "*Alternatively, [[Asgarnian ale]], [[Dwarven stout]], or a "
                        + "[[Wizard's mind bomb]]\n"
                        + "*[[Raw cod]] or the [[Ring of Charos(a)]]\n"
                        + "*[[Ice gloves]] or [[Smiths gloves (i)]]");

        assertTrue(result.getUnresolved().toString(), result.isFullyExecutable());
        assertEquals(3, result.getParsedLineCount());
        assertEquals("Asgarnian ale or Dwarven stout or Wizard's mind bomb",
                result.getExpression().getChildren().get(0).label());
        assertEquals("Raw cod or Ring of Charos(a)",
                result.getExpression().getChildren().get(1).label());
    }

    @Test
    public void acquisitionRecipeBecomesNestedAlternative()
    {
        QuestItemEvidenceParser.Result result = parser.parse(
                "*20 [[ecto-token]]s (or 4 [[bones]], 4 [[bucket]]s, "
                        + "and 4 [[pot]]s to gain the tokens)\n"
                        + "*[[Knife]] (or 10 [[coins]] to buy a knife and chisel)");

        assertTrue(result.getUnresolved().toString(), result.isFullyExecutable());
        assertEquals(2, result.getParsedLineCount());
        assertEquals("20 × Ecto-token or (4 × Bones and 4 × Bucket and 4 × Pot)",
                result.getExpression().getChildren().get(0).label());
        assertEquals("Knife or 10 × Coins",
                result.getExpression().getChildren().get(1).label());
    }

    @Test
    public void recommendationDoesNotRaiseMandatoryQuantity()
    {
        QuestItemEvidenceParser.Result result = parser.parse(
                "*1 [[Log]] (2 logs and a tinderbox are recommended)\n"
                        + "*4 [[coal]] (4 more for each additional bar)");
        assertTrue(result.isFullyExecutable());
        assertEquals("Log and 4 × Coal", result.getExpression().label());
    }

    @Test
    public void lowerBoundsAreNotPresentedAsExactQuantities()
    {
        QuestItemEvidenceParser.Result result = parser.parse(
                "*At least 10 normal [[logs]]\n"
                        + "*501+ [[coins]]\n"
                        + "*At least 12 [[nail]]s of any kind");
        assertTrue(result.getUnresolved().toString(), result.isFullyExecutable());
        assertEquals(ItemQuantityMode.AT_LEAST,
                result.getExpression().getChildren().get(0).getQuantityMode());
        assertEquals("at least 10 × Logs and at least 501 × Coins and "
                        + "at least 12 × nails of any usable metal",
                result.getExpression().label());
    }

    @Test
    public void mechanicalPreparationFamiliesStayCheckNeeded()
    {
        QuestItemEvidenceParser.Result result = parser.parse(
                "*Magic or Ranged gear for the enemy fight\n"
                        + "*Means to cast [[Telekinetic Grab]] (air and law rune)\n"
                        + "*Runes to cast [[Fire Bolt]], [[Fire Blast]] or [[Fire Wave]]\n"
                        + "*Five empty inventory slots");
        assertTrue(result.getUnresolved().toString(), result.isFullyExecutable());
        assertEquals(4, result.getParsedLineCount());
        for (ItemRequirementExpression child : result.getExpression().getChildren())
            assertTrue(child.getKind() == ItemRequirementExpression.Kind.ITEM_CLASS
                    || child.getKind() == ItemRequirementExpression.Kind.CHECK_NEEDED);
    }
    private final QuestItemEvidenceParser parser = new QuestItemEvidenceParser();

    @Test
    public void parsesOnlyUnambiguousSingleItemBullets()
    {
        QuestItemEvidenceParser.Result result = parser.parse(
                "* [[Rope]]\n* 5 [[death rune]]s\n* [[Knife]] or a [[slash weapon]]");

        assertNotNull(result.getExpression());
        assertEquals(ItemRequirementExpression.Kind.ALL_OF,
                result.getExpression().getKind());
        assertEquals(2, result.getExpression().getChildren().size());
        assertEquals("Rope", result.getExpression().getChildren().get(0)
                .getItemNames().get(0));
        assertEquals(5, result.getExpression().getChildren().get(1).getQuantity());
        assertEquals(1, result.getUnresolved().size());
        assertFalse(result.isFullyExecutable());
    }

    @Test
    public void parsesBareConcreteAlternativesWithoutGuessing()
    {
        QuestItemEvidenceParser.Result result = parser.parse(
                "* [[Ivandis flail]] or [[Blisterwood flail]]\n"
                        + "* 5 [[death rune]]s or 10 [[chaos rune]]s");

        assertTrue(result.isFullyExecutable());
        assertEquals(ItemRequirementExpression.Kind.ALL_OF,
                result.getExpression().getKind());
        ItemRequirementExpression weapons = result.getExpression()
                .getChildren().get(0);
        assertEquals(ItemRequirementExpression.Kind.ANY_OF, weapons.getKind());
        assertEquals("Ivandis flail", weapons.getChildren().get(0)
                .getItemNames().get(0));
        assertEquals("Blisterwood flail", weapons.getChildren().get(1)
                .getItemNames().get(0));
        ItemRequirementExpression runes = result.getExpression()
                .getChildren().get(1);
        assertEquals(5, runes.getChildren().get(0).getQuantity());
        assertEquals(10, runes.getChildren().get(1).getQuantity());
        assertEquals("5 × Death rune or 10 × Chaos rune", runes.label());
    }

    @Test
    public void explicitNoneNeedsNoOwnershipExpression()
    {
        QuestItemEvidenceParser.Result result = parser.parse("None");
        assertNull(result.getExpression());
        assertTrue(result.getUnresolved().isEmpty());
        assertTrue(result.isFullyExecutable());
    }

    @Test
    public void supportedClassesParseWhileUnknownClassesRemainFailClosed()
    {
        QuestItemEvidenceParser.Result result = parser.parse(
                "* A [[weapon]]\n* Any [[axe]]\n* [[Cat]] or [[kitten]]\n"
                        + "* [[Ghostspeak amulet]] ([[Morytania legs 2]] or better also work)");
        assertNotNull(result.getExpression());
        assertEquals(2, result.getParsedLineCount());
        assertEquals(2, result.getUnresolved().size());
    }

    @Test
    public void authoritativeCatalogExposesExecutableCoverageMetrics()
    {
        ImportedQuestItemRequirementCatalog catalog =
                new ImportedQuestItemRequirementCatalog();
        QuestItemEvidenceParser.Result porcine = catalog.resultFor(
                "A Porcine of Interest");
        assertNotNull(porcine);
        assertNotNull(porcine.getExpression());
        assertTrue(porcine.getParsedLineCount() >= 1);
        assertTrue(catalog.questCount() > 0);
    }
}
