package compass;

import java.util.*;
import javax.inject.Singleton;
import lombok.Getter;

/** Single access boundary for optional hosted capabilities; local safety is unconditional. */
@Singleton
public class StrategistFeatureAccessPolicy
{
    public boolean canUse(Feature feature, Snapshot entitlement)
    {
        return feature != null && (feature.isCoreLocal()
                || entitlement != null && entitlement.has(feature));
    }

    public void requireHosted(Feature feature, Snapshot entitlement)
    {
        if (feature == null || feature.isCoreLocal())
            throw new IllegalArgumentException(Text.get(730));
        if (!canUse(feature, entitlement))
            throw new HostedFeatureUnavailableException(feature);
    }

    public enum Feature
    {
        CORE_PLANNER(false), LOCAL_PROFILE_MEMORY(false),
        LOCAL_METHOD_GUIDANCE(false), LOCAL_BUILD_SAFETY(false),
        LOCAL_RESOURCE_PLANNING(false), LOCAL_RECOMMENDATION_HISTORY(false),
        CLOUD_PROFILE_SYNC(true), CROSS_DEVICE_HISTORY(true),
        GIM_TEAM_PLANNING(true), REMOTE_REMINDERS(true), WEB_DASHBOARD(true),
        ONLINE_REASONING(true), ADVANCED_CLOUD_ANALYTICS(true);

        @Getter private final boolean hostedPremium;
        Feature(boolean hosted) { hostedPremium = hosted; }
        public boolean isCoreLocal() { return !hostedPremium; }
    }

    @Getter
    public static final class Snapshot
    {
        private final Set<Feature> hostedFeatures;
        private final Confidence confidence;
        private final String source;

        public Snapshot(Set<Feature> features, Confidence confidence, String source)
        {
            var copy = EnumSet.noneOf(Feature.class);
            if (features != null) for (Feature feature : features)
                if (feature != null && feature.isHostedPremium()) copy.add(feature);
            hostedFeatures = Collections.unmodifiableSet(copy);
            this.confidence = confidence == null ? Confidence.CHECK_NEEDED : confidence;
            this.source = source == null ? "unknown" : source;
        }

        public static Snapshot none()
        {
            return new Snapshot(Collections.emptySet(), Confidence.CHECK_NEEDED,
                    "not-connected");
        }

        boolean has(Feature feature)
        {
            return confidence == Confidence.VERIFIED
                    && feature.isHostedPremium() && hostedFeatures.contains(feature);
        }
    }

    public static final class HostedFeatureUnavailableException
            extends IllegalStateException
    {
        @Getter private final Feature feature;
        HostedFeatureUnavailableException(Feature feature)
        {
            super(Text.get(731) + feature);
            this.feature = feature;
        }
    }
}
