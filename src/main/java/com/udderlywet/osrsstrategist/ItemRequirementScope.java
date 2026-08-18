package com.udderlywet.osrsstrategist;

/** Where an item must be observed before it can satisfy a requirement. */
public enum ItemRequirementScope
{
    CARRIED,
    EQUIPPED,
    CARRIED_OR_EQUIPPED,
    IMMEDIATELY_USABLE,
    OWNED_OR_RETRIEVABLE
}
