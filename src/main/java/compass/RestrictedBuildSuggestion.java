package compass;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class RestrictedBuildSuggestion
{
    private final RestrictedBuildType type;
    private final Confidence confidence;
    private final String evidence;


}
