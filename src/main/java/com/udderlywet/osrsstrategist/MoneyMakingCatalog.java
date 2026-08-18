package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/**
 * Stable money/resource-producing activities. GP/hour is intentionally not
 * frozen here because prices and drop values change; live-price evaluation is
 * a separate concern for Main accounts.
 */
@Singleton
public class MoneyMakingCatalog
{
    private final List<MoneyMakingDefinition> methods = new ArrayList<>();

    public MoneyMakingCatalog()
    {
        seedF2p();
        seedMain();
        seedIronFriendly();
        seedHighLevel();
    }

    public List<MoneyMakingDefinition> all()
    {
        return Collections.unmodifiableList(methods);
    }

    public List<MoneyMakingDefinition> forAccount(AccountMode mode)
    {
        List<MoneyMakingDefinition> result = new ArrayList<>();
        for (MoneyMakingDefinition method : methods)
            if (method.supports(mode)) result.add(method);
        return Collections.unmodifiableList(result);
    }

    private void seedF2p()
    {
        add("money:f2p-ogresses", "Ogress drops",
                "Safespot ogress warriors/shamans and bank or alch useful rune drops.",
                Skill.RANGED, 40, true, allModes(), RiskLevel.LOW,
                AttentionLevel.LOW, false, false);
        add("money:f2p-iron", "Mine iron ore",
                "Mine and bank iron when ore value/resources matter more than pure Mining XP.",
                Skill.MINING, 15, true, allModes(), RiskLevel.NONE,
                AttentionLevel.ACTIVE, false, true);
        add("money:f2p-high-alch", "High-alch verified items",
                "High-alch only items that Strategist has confirmed are safe to consume and economically sensible.",
                Skill.MAGIC, 55, true, allModes(), RiskLevel.NONE,
                AttentionLevel.LOW, false, true);
        add("money:f2p-crafting", "F2P jewellery/crafting margin",
                "Use current prices before buying inputs; irons instead treat the products as useful alch/resource conversion.",
                Skill.CRAFTING, 20, true, allModes(), RiskLevel.NONE,
                AttentionLevel.MODERATE, false, true);
        add("money:f2p-boss-keys", "Obor/Bryophyta/Brutus drops",
                "Use already-earned access/keys for F2P boss drops when the account is combat-ready.",
                Skill.ATTACK, 40, true, allModes(), RiskLevel.MEDIUM,
                AttentionLevel.ACTIVE, false, false);
    }

    private void seedMain()
    {
        add("money:herb-runs", "Herb runs",
                "Run the most profitable/strategically useful verified herb seeds and patches using live price data for Main accounts.",
                Skill.FARMING, 32, false, allModes(), RiskLevel.NONE,
                AttentionLevel.LOW, false, true);
        add("money:birdhouses", "Birdhouse runs",
                "Complete ready birdhouse runs for nests, seeds, Hunter XP, and tradeable value.",
                Skill.HUNTER, 5, false, allModes(), RiskLevel.NONE,
                AttentionLevel.LOW, false, true);
        add("money:wealthy-citizens", "Varlamore wealthy citizens",
                "Use the Varlamore pickpocket/house-robbery loop for low-intensity Thieving and coin/value generation.",
                Skill.THIEVING, 50, false, allModes(), RiskLevel.LOW,
                AttentionLevel.LOW, false, false);
        add("money:ardy-knights", "Ardougne knights",
                "Pickpocket knights when success rate, healing, and coin generation fit the session.",
                Skill.THIEVING, 55, false, allModes(), RiskLevel.LOW,
                AttentionLevel.LOW, false, false);
        add("money:vyres", "Pickpocket vyres",
                "Pickpocket vyres only after Darkmeyer access and a sustainable healing/teleport setup are confirmed.",
                Skill.THIEVING, 82, false, nonUimModes(), RiskLevel.MEDIUM,
                AttentionLevel.MODERATE, false, false);
        add("money:elves", "Pickpocket elves",
                "Pickpocket elves after Prifddinas access when current crystal/shard and tradeable rewards justify it.",
                Skill.THIEVING, 85, false, nonUimModes(), RiskLevel.MEDIUM,
                AttentionLevel.MODERATE, false, false);
        add("money:blast-furnace", "Blast Furnace processing",
                "Use live prices before buying ore on a Main; irons can use banked ore when bars are strategically valuable.",
                Skill.SMITHING, 40, false, allModes(), RiskLevel.NONE,
                AttentionLevel.ACTIVE, false, true);
        add("money:blood-runes", "Blood rune crafting",
                "Craft blood runes when the unlocked route provides useful runes or strong tradeable value.",
                Skill.RUNECRAFT, 77, false, allModes(), RiskLevel.NONE,
                AttentionLevel.LOW, false, true);
        add("money:sepulchre", "Hallowed Sepulchre loot",
                "Run the deepest safe unlocked Sepulchre floors for Agility XP and valuable loot.",
                Skill.AGILITY, 52, false, allModes(), RiskLevel.MEDIUM,
                AttentionLevel.ACTIVE, false, false);
    }

    private void seedIronFriendly()
    {
        add("money:agility-pyramid", "Agility Pyramid cash",
                "Trade pyramid tops for coins when the desert route, food/water protection, and Agility level make it sensible.",
                Skill.AGILITY, 30, false, ironModes(), RiskLevel.MEDIUM,
                AttentionLevel.ACTIVE, false, false);
        add("money:giants-foundry", "Giants' Foundry contracts",
                "Turn available metal into Smithing XP, outfit progress, and direct coin rewards.",
                Skill.SMITHING, 15, false, ironModes(), RiskLevel.NONE,
                AttentionLevel.MODERATE, false, false);
        add("money:slayer-alchs", "Slayer alch drops",
                "Accumulate and safely high-alch suitable Slayer drops while progressing combat and Slayer.",
                Skill.SLAYER, 40, false, allModes(), RiskLevel.MEDIUM,
                AttentionLevel.MODERATE, false, false);
        add("money:battlestaves", "Battlestaff conversion",
                "Use unlocked daily battlestaves/orb supply only when the account can source the components efficiently.",
                Skill.CRAFTING, 54, false, allModes(), RiskLevel.NONE,
                AttentionLevel.MODERATE, false, true);
        add("money:contracts-seeds", "Farming contracts and seed value",
                "Treat Farming contracts as seed/resource generation on irons rather than forcing a GP/hour comparison.",
                Skill.FARMING, 45, false, ironModes(), RiskLevel.NONE,
                AttentionLevel.LOW, false, false);
    }

    private void seedHighLevel()
    {
        add("money:vorkath", "Vorkath",
                "Use the PvM readiness engine and target-specific gear before considering Vorkath as a money method.",
                Skill.RANGED, 75, false, nonF2pModes(), RiskLevel.HIGH,
                AttentionLevel.ACTIVE, false, true);
        add("money:zulrah", "Zulrah",
                "Use verified Zulrah access, combat readiness, supplies, and current drop values.",
                Skill.RANGED, 75, false, nonF2pModes(), RiskLevel.HIGH,
                AttentionLevel.ACTIVE, false, true);
        add("money:gauntlet", "Corrupted Gauntlet",
                "Use after Song of the Elves and only when current skills and risk settings support repeated Gauntlet attempts.",
                Skill.RANGED, 75, false, nonF2pModes(), RiskLevel.HIGH,
                AttentionLevel.ACTIVE, false, false);
        add("money:slayer-bosses", "Slayer bosses",
                "Use task-compatible bosses only when the account is realistically ready and the risk/reward fits its mode.",
                Skill.SLAYER, 75, false, nonF2pModes(), RiskLevel.HIGH,
                AttentionLevel.ACTIVE, false, true);
        add("money:raids", "Raids",
                "Treat raids as profit only after raid-specific readiness, team/solo plan, supplies, and gear switches are verified.",
                Skill.RANGED, 80, false, mainAndStandardIronModes(), RiskLevel.HIGH,
                AttentionLevel.ACTIVE, false, true);
        add("money:wilderness-bosses", "Wilderness bosses",
                "High-value Wilderness PvM. Never surface without explicit Wilderness acceptance; never default for Hardcore.",
                Skill.ATTACK, 70, false, nonHardcoreModes(), RiskLevel.HIGH,
                AttentionLevel.ACTIVE, true, true);
    }

    private void add(String id, String name, String description, Skill skill,
            int level, boolean f2p, EnumSet<AccountMode> modes, RiskLevel risk,
            AttentionLevel attention, boolean wilderness, boolean prices)
    {
        methods.add(new MoneyMakingDefinition(id, name, description, skill,
                level, f2p, modes, risk, attention, wilderness, prices));
    }

    private static EnumSet<AccountMode> allModes()
    {
        return EnumSet.of(AccountMode.MAIN, AccountMode.IRONMAN,
                AccountMode.ULTIMATE_IRONMAN, AccountMode.HARDCORE_IRONMAN,
                AccountMode.GROUP_IRONMAN, AccountMode.HARDCORE_GROUP_IRONMAN,
                AccountMode.UNRANKED_GROUP_IRONMAN);
    }

    private static EnumSet<AccountMode> nonUimModes()
    {
        EnumSet<AccountMode> set = allModes();
        set.remove(AccountMode.ULTIMATE_IRONMAN);
        return set;
    }

    private static EnumSet<AccountMode> ironModes()
    {
        return EnumSet.of(AccountMode.IRONMAN, AccountMode.ULTIMATE_IRONMAN,
                AccountMode.HARDCORE_IRONMAN, AccountMode.GROUP_IRONMAN,
                AccountMode.HARDCORE_GROUP_IRONMAN, AccountMode.UNRANKED_GROUP_IRONMAN);
    }

    private static EnumSet<AccountMode> nonF2pModes()
    {
        return allModes();
    }

    private static EnumSet<AccountMode> mainAndStandardIronModes()
    {
        return EnumSet.of(AccountMode.MAIN, AccountMode.IRONMAN,
                AccountMode.GROUP_IRONMAN, AccountMode.UNRANKED_GROUP_IRONMAN);
    }

    private static EnumSet<AccountMode> nonHardcoreModes()
    {
        EnumSet<AccountMode> set = allModes();
        set.remove(AccountMode.HARDCORE_IRONMAN);
        set.remove(AccountMode.HARDCORE_GROUP_IRONMAN);
        return set;
    }
}
