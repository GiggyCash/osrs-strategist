package compass;

import lombok.RequiredArgsConstructor;
import java.util.*;

import lombok.Getter;

/** Data descriptor connecting a training method to legal concrete locations. */
@RequiredArgsConstructor
@Getter
public final class MethodLocationProfile
{
    private final String methodId;
    private final List<MethodLocationOption> locations;
    private final String sourceUrl;


}
