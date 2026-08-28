package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Singleton;

/**
 * Small audited location catalog. Method identifiers are lookup keys only;
 * route evidence and travel burden determine which location wins.
 */
@Singleton
public final class MethodLocationCatalog
{
    public static final String ECTOFUNTUS_SOURCE =
            "https://oldschool.runescape.wiki/w/Ectofuntus";
    public static final String FRUIT_TREE_SOURCE =
            "https://oldschool.runescape.wiki/w/Fruit_tree_patch/Patches";
    public static final String TREE_PATCH_SOURCE =
            "https://oldschool.runescape.wiki/w/Tree_patch";
    public static final String FLY_FISHING_SOURCE =
            "https://oldschool.runescape.wiki/w/Free-to-play_Fishing_training";

    private final Map<String, MethodLocationProfile> profiles =
            new LinkedHashMap<>();

    public MethodLocationCatalog()
    {
        add(new MethodLocationProfile("prayer_ectofuntus",
                Collections.singletonList(option("ectofuntus",
                        "Ectofuntus, north of Port Phasmatys", 9,
                        "ectophial", 2, true, false)), ECTOFUNTUS_SOURCE));
        add(new MethodLocationProfile("farming_fruit_trees", Arrays.asList(
                option("catherby-fruit-tree", "Fruit tree patch east of Catherby",
                        6, null, 6, true, false),
                option("gnome-stronghold-fruit-tree",
                        "Tree Gnome Stronghold fruit tree patch", 9,
                        "spirit-tree-gnome-stronghold", 2, true, false)),
                FRUIT_TREE_SOURCE));
        add(new MethodLocationProfile("farming_trees", Arrays.asList(
                option("falador-tree", "Falador Park tree patch", 5,
                        null, 5, true, false),
                option("gnome-stronghold-tree",
                        "Tree Gnome Stronghold tree patch", 9,
                        "spirit-tree-gnome-stronghold", 2, true, false)),
                TREE_PATCH_SOURCE));
        add(new MethodLocationProfile("fishing_f2p_fly",
                Collections.singletonList(option("barbarian-village-fly",
                        "Barbarian Village fishing spots", 2, null, 2,
                        false, false)), FLY_FISHING_SOURCE));
    }

    public MethodLocationProfile forMethod(String methodId)
    {
        return methodId == null ? null : profiles.get(methodId);
    }

    public Map<String, MethodLocationProfile> all()
    {
        return Collections.unmodifiableMap(profiles);
    }

    private void add(MethodLocationProfile profile)
    {
        if (profiles.put(profile.getMethodId(), profile) != null)
            throw new IllegalStateException("Duplicate method location profile "
                    + profile.getMethodId());
    }

    private static MethodLocationOption option(String id, String name,
            int burden, String route, int routedBurden, boolean members,
            boolean wilderness)
    {
        return new MethodLocationOption(id, name, burden, route,
                routedBurden, members, wilderness);
    }
}
