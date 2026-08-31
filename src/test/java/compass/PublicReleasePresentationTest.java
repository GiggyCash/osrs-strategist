package compass;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PublicReleasePresentationTest
{
    @Test
    public void readmeLeadsWithPlayerValuePrivacyAndLimitations()
            throws Exception
    {
        String readme = text("README.md");
        assertTrue(readme.startsWith("# Gielinor Compass"));
        assertTrue(readme.contains("Your account. Your next move."));
        assertTrue(readme.contains("## What it does"));
        assertTrue(readme.contains("## How DO NEXT works"));
        assertTrue(readme.contains("## Supported accounts"));
        assertTrue(readme.contains("## Privacy and safety"));
        assertTrue(readme.contains("## Known limitations"));
        assertFalse(readme.toLowerCase().contains("controlled beta"));
    }

    @Test
    public void issueTemplatesNeverRequestSensitiveAccountData()
            throws Exception
    {
        String bad = text(".github/ISSUE_TEMPLATE/bad-recommendation.md");
        String bug = text(".github/ISSUE_TEMPLATE/bug-report.md");
        assertTrue(bad.contains("Account mode"));
        assertTrue(bad.contains("Selected goal"));
        assertTrue(bad.contains("Had Compass observed the bank?"));
        assertTrue(bad.contains("Never include passwords"));
        assertTrue(bug.contains("Steps to reproduce"));
        assertTrue(bug.contains("RuneLite version"));
        assertTrue(bug.contains("never include passwords"));
    }

    @Test
    public void releaseAndRoadmapDocumentsDoNotClaimForecastIsShipped()
            throws Exception
    {
        assertTrue(text("CHANGELOG.md").contains("## [0.2.0] - 2026-08-25"));
        String forecast = text("docs/GOAL_FORECAST.md");
        assertTrue(forecast.contains("REQUIRED"));
        assertTrue(forecast.contains("RECOMMENDED"));
        assertTrue(forecast.contains("RNG-DEPENDENT WORK"));
        assertTrue(forecast.contains("not part of the `0.2.0` release"));
    }

    private static String text(String path) throws Exception
    {
        return new String(Files.readAllBytes(Paths.get(path)),
                StandardCharsets.UTF_8);
    }
}
