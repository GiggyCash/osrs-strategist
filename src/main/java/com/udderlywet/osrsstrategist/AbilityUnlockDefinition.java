package com.udderlywet.osrsstrategist;

import lombok.RequiredArgsConstructor;
import lombok.AccessLevel;
import lombok.Getter;

import net.runelite.api.Skill;

/** Verified prerequisites for a prayer, spell, or spellbook unlock. */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public final class AbilityUnlockDefinition
{
    private final String id;
    private final String name;
    private final GoalNodeKind kind;
    private final String quest;
    private final Skill skill;
    private final int level;
    private final Skill secondarySkill;
    private final int secondaryLevel;
    private final String requiredItem;
    private final String encounterId;
    private final String accessCheck;


}
