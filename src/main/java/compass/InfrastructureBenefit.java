package compass;

/** A reusable property supplied by an account infrastructure milestone. */
public enum InfrastructureBenefit
{
    INVENTORY_RELIEF(AccountStrategicDimension.INVENTORY_PRESSURE),
    POH_PLATFORM(AccountStrategicDimension.POH_VALUE),
    STORAGE(AccountStrategicDimension.STORAGE_VALUE),
    TRAVEL_NETWORK(AccountStrategicDimension.TELEPORT_INFRASTRUCTURE_VALUE),
    SETUP_REUSE(AccountStrategicDimension.SETUP_COST_SENSITIVITY),
    SELF_SUFFICIENCY(AccountStrategicDimension.SELF_SOURCING_BURDEN),
    SHARED_UTILITY(AccountStrategicDimension.SHARED_INFRASTRUCTURE_VALUE),
    RISK_REDUCTION(AccountStrategicDimension.DEATH_RISK_SENSITIVITY),
    RESOURCE_SUSTAINABILITY(
            AccountStrategicDimension.CONSUMABLE_REPLACEMENT_DIFFICULTY),
    STORABLE_EQUIPMENT(AccountStrategicDimension.STORABLE_EQUIPMENT_VALUE),
    GP_LIQUIDITY(AccountStrategicDimension.GP_LIQUIDITY_STORAGE_VALUE);

    private final AccountStrategicDimension dimension;

    InfrastructureBenefit(AccountStrategicDimension dimension)
    {
        this.dimension = dimension;
    }

    public AccountStrategicDimension getDimension() { return dimension; }
}
