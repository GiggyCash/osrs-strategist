package compass;

/**
 * Account properties that change the value of an activity or unlock.
 *
 * <p>These are deliberately not method identities. Candidate metadata can be
 * matched to these dimensions without teaching the selector that a named
 * account mode should always choose a named method.</p>
 */
public enum AccountStrategicDimension
{
    INVENTORY_PRESSURE(AccountStrategicDimensionRole.BURDEN_WEIGHT),
    BANK_AVAILABILITY(AccountStrategicDimensionRole.CAPABILITY_GATE),
    GRAND_EXCHANGE_AVAILABILITY(AccountStrategicDimensionRole.CAPABILITY_GATE),
    SELF_SOURCING_BURDEN(AccountStrategicDimensionRole.BURDEN_WEIGHT),
    SHARED_RESOURCE_VALUE(AccountStrategicDimensionRole.BENEFIT_WEIGHT),
    SHARED_INFRASTRUCTURE_VALUE(AccountStrategicDimensionRole.BENEFIT_WEIGHT),
    STORAGE_VALUE(AccountStrategicDimensionRole.BENEFIT_WEIGHT),
    POH_VALUE(AccountStrategicDimensionRole.BENEFIT_WEIGHT),
    TELEPORT_INFRASTRUCTURE_VALUE(AccountStrategicDimensionRole.BENEFIT_WEIGHT),
    SETUP_COST_SENSITIVITY(AccountStrategicDimensionRole.BURDEN_WEIGHT),
    DEATH_RISK_SENSITIVITY(AccountStrategicDimensionRole.BURDEN_WEIGHT),
    CONSUMABLE_REPLACEMENT_DIFFICULTY(
            AccountStrategicDimensionRole.BURDEN_WEIGHT),
    STORABLE_EQUIPMENT_VALUE(AccountStrategicDimensionRole.BENEFIT_WEIGHT),
    DUPLICATE_GRIND_PENALTY(AccountStrategicDimensionRole.BURDEN_WEIGHT),
    GP_LIQUIDITY_STORAGE_VALUE(AccountStrategicDimensionRole.BENEFIT_WEIGHT);

    private final AccountStrategicDimensionRole role;

    AccountStrategicDimension(AccountStrategicDimensionRole role)
    {
        this.role = role;
    }

    public AccountStrategicDimensionRole getRole() { return role; }
}
