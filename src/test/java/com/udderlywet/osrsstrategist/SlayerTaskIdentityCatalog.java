package com.udderlywet.osrsstrategist;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Singleton;

/** Complete canonical assignment census generated from pinned RuneLite. */
@Singleton
public final class SlayerTaskIdentityCatalog
{
    public static final int EXPECTED_IDENTITIES = 151;
    public static final String PROVENANCE =
            "Generated from RuneLite 1.12.35 Slayer Task enum";
    private final List<SlayerTaskIdentity> identities;

    public SlayerTaskIdentityCatalog()
    {
        identities = Collections.unmodifiableList(load());
        if (identities.size() != EXPECTED_IDENTITIES)
            throw new IllegalStateException("Expected " + EXPECTED_IDENTITIES
                    + " Slayer identities, found " + identities.size());
    }

    public List<SlayerTaskIdentity> all() { return identities; }

    private static List<SlayerTaskIdentity> load()
    {
        InputStream stream = SlayerTaskIdentityCatalog.class
                .getResourceAsStream("/content/slayer-tasks.tsv");
        if (stream == null)
            throw new IllegalStateException("Missing Slayer identity evidence");
        List<SlayerTaskIdentity> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                stream, StandardCharsets.UTF_8)))
        {
            String line;
            int number = 0;
            while ((line = reader.readLine()) != null)
            {
                number++;
                if (line.trim().isEmpty() || line.startsWith("#")) continue;
                String[] fields = line.split("\\t", -1);
                if (fields.length < 2 || fields.length > 3)
                    throw new IllegalStateException(
                            "Invalid Slayer identity line " + number);
                List<String> targets = fields.length == 2 || fields[2].isEmpty()
                        ? Collections.emptyList()
                        : Arrays.asList(fields[2].split("\\|"));
                result.add(new SlayerTaskIdentity(fields[0], fields[1], targets));
            }
        }
        catch (IOException ex)
        {
            throw new IllegalStateException("Unable to read Slayer evidence", ex);
        }
        return result;
    }
}
