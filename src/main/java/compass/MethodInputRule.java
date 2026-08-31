package compass;

import lombok.RequiredArgsConstructor;
import lombok.Getter;

/** One material rule consumed by a deterministic training action. */
@RequiredArgsConstructor
@Getter
public final class MethodInputRule
{
    private final MethodProfile.InputMode mode;
    private final String fixedName;
    private final double quantityPerAction;


}
