package compass;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.*;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.VarPlayerID;

@Singleton
@lombok.RequiredArgsConstructor(onConstructor_ = @Inject)
public class AccountReader
{
    private final Client client;

    public AccountSnapshot read()
    {
        if (client.getGameState() != GameState.LOGGED_IN
                || client.getLocalPlayer() == null)
        {
            return null;
        }

        var playerName = client.getLocalPlayer().getName();

        if (playerName == null || playerName.isEmpty())
        {
            playerName = "Unknown Player";
        }

        int accountTypeCode =
                client.getVarbitValue(VarbitID.IRONMAN);

        String accountTypeName =
                formatAccountType(accountTypeCode);

        // RuneLite itself uses ACCOUNT_CREDIT > 0 as the account-level member
        // signal. A members world is retained as a safety proof because an F2P
        // account cannot be logged into one.
        int membershipCredit = client.getVarpValue(
                VarPlayerID.ACCOUNT_CREDIT
        );

        boolean membersWorld = client.getWorldType() != null
                && client.getWorldType().contains(WorldType.MEMBERS);

        MembershipStatus membershipStatus =
                membershipCredit > 0 || membersWorld
                        ? MembershipStatus.P2P
                        : MembershipStatus.F2P;

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
                client.getAccountHash(),
                accountTypeCode,
                accountTypeName,
                membershipStatus,
                membershipCredit,
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
                return Text.get(1606);

            case 3:
                return Text.get(1607);

            case 4:
                return "Group Ironman";

            case 5:
                return Text.get(1108);

            case 6:
                return Text.get(1109);

            default:
                return "Unknown";
        }
    }
}
