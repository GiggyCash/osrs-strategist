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
        assertTrue(result.isFullyExecutable());
        assertNotNull(result.getExpression());
        assertEquals(3, result.getParsedLineCount());
    }

    @Test
    public void semanticParentheticalsRemainUnresolved()
    {
        QuestItemEvidenceParser.Result result = parser.parse(
                "*[[Rope]] (consumed during the quest)\n"
                        + "*[[Saw]] ([[Crystal saw]] also works)");
        assertNull(result.getExpression());
        assertEquals(2, result.getUnresolved().size());
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
    public void genericAndQualifiedAlternativesRemainFailClosed()
    {
        QuestItemEvidenceParser.Result result = parser.parse(
                "* A [[weapon]]\n* Any [[axe]]\n* [[Cat]] or [[kitten]]\n"
                        + "* [[Ghostspeak amulet]] ([[Morytania legs 2]] or better also work)");
        assertNull(result.getExpression());
        assertEquals(4, result.getUnresolved().size());
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
