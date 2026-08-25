package com.udderlywet.osrsstrategist;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Repository-backed checks for current RuneLite Plugin Hub blockers. */
public class PluginHubComplianceTest
{
    private static final List<String> FORBIDDEN_PRODUCTION_TOKENS = Arrays.asList(
            "import java.lang.reflect",
            "com.google.gson.reflect.TypeToken",
            "ProcessBuilder",
            "Runtime.getRuntime().exec",
            "Class.forName",
            "ObjectInputStream",
            "ObjectOutputStream",
            "java.net.HttpURLConnection",
            "java.net.http.HttpClient",
            "java.awt.Robot",
            "sun.misc.Unsafe",
            "com.sun.jna",
            "org.lwjgl");

    @Test
    public void productionSourceAvoidsForbiddenLanguageAndRuntimeFeatures()
            throws Exception
    {
        try (Stream<Path> paths = Files.walk(Paths.get("src/main")))
        {
            for (Path path : (Iterable<Path>) paths.filter(Files::isRegularFile)
                    .filter(value -> value.toString().endsWith(".java"))::iterator)
            {
                String source = new String(Files.readAllBytes(path),
                        StandardCharsets.UTF_8);
                for (String forbidden : FORBIDDEN_PRODUCTION_TOKENS)
                    assertFalse(path + " contains " + forbidden,
                            source.contains(forbidden));
            }
        }
    }

    @Test
    public void pluginMetadataPackagingAndLicenseAreReviewable()
            throws Exception
    {
        String properties = text("runelite-plugin.properties");
        assertTrue(properties.contains("displayName=Gielinor Compass"));
        assertTrue(properties.contains("plugins=com.udderlywet.osrsstrategist.OsrsStrategistPlugin"));
        assertTrue(properties.contains("build=standard"));
        assertTrue(text("build.gradle").contains("options.release.set(11)"));
        assertTrue(text("settings.gradle")
                .contains("rootProject.name = 'osrs-strategist'"));
        assertTrue(text("LICENSE").startsWith("BSD 2-Clause License"));
        assertFalse(Files.exists(Paths.get("src/main/resources/META-INF/services/net.runelite.client.plugins.Plugin")));
    }

    private static String text(String path) throws Exception
    {
        return new String(Files.readAllBytes(Paths.get(path)),
                StandardCharsets.UTF_8);
    }
}
