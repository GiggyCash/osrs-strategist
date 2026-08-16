package com.udderlywet.osrsstrategist;

import java.util.EnumMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.Varbits;

@Singleton
public class AccountReader
{
    private final Client client;

    @Inject
    public AccountReader(Client client)
    {
        this.client = client;
    }

    public AccountSnapshot read()
    {
        if (client.getGameState() != GameState.LOGGED_IN
                || client.getLocalPlayer() == null)
        {
            return null;
        }

        String playerName = client.getLocalPlayer().getName();

        if (playerName == null || playerName.isEmpty())
        {
            playerName = "Unknown Player";
        }

        int accountTypeCode =
                client.getVarbitValue(Varbits.ACCOUNT_TYPE);

        String accountTypeName =
                formatAccountType(accountTypeCode);

        Map<Skill, Integer> skillLevels =
                new EnumMap<>(Skill.class);

        Map<Skill, Integer> skillExperience =
                new EnumMap<>(Skill.class);

        for (Skill skill : Skill.values())
        {
            int level =
                    client.getRealSkillLevel(skill);

            int experience =
                    client.getSkillExperience(skill);

            skillLevels.put(skill, level);
            skillExperience.put(skill, experience);
        }

        return new AccountSnapshot(
                playerName,
                accountTypeCode,
                accountTypeName,
                client.getTotalLevel(),
                client.getOverallExperience(),
                skillLevels,
                skillExperience
        );
    }

    private String formatAccountType(int type)
    {
        switch (type)
        {
            case 0:
                return "Main";

            case 1:
                return "Ironman";

            case 2:
                return "Ultimate Ironman";

            case 3:
                return "Hardcore Ironman";

            case 4:
                return "Group Ironman";

            case 5:
                return "Hardcore Group Ironman";

            case 6:
                return "Unranked Group Ironman";

            default:
                return "Unknown";
        }
    }
}