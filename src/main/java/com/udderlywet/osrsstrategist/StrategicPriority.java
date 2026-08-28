package com.udderlywet.osrsstrategist;

/** Qualitative importance; intentionally not a hidden recommendation score. */
public enum StrategicPriority
{
    NONE,
    LOW,
    MODERATE,
    HIGH,
    CRITICAL;

    public boolean isAtLeast(StrategicPriority other)
    {
        return other != null && ordinal() >= other.ordinal();
    }

    public static StrategicPriority higherOf(
            StrategicPriority left,
            StrategicPriority right)
    {
        if (left == null) return right == null ? NONE : right;
        if (right == null) return left;
        return left.ordinal() >= right.ordinal() ? left : right;
    }

    public static StrategicPriority lowerOf(
            StrategicPriority left,
            StrategicPriority right)
    {
        if (left == null || right == null) return NONE;
        return left.ordinal() <= right.ordinal() ? left : right;
    }
}
