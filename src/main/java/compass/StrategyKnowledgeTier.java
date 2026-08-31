package compass;

/** Evidence hierarchy for strategy candidate generation, not a score bonus. */
public enum StrategyKnowledgeTier
{
    VERIFIED_ACCOUNT_SPECIFIC,
    VERIFIED_SHARED,
    MECHANICALLY_VERIFIED_FALLBACK,
    SAFE_RECOVERY
}
