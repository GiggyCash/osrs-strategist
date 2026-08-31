package compass;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FarmingPatchStateDecoderTest
{
    private final FarmingPatchStateDecoder decoder = new FarmingPatchStateDecoder();

    @Test
    public void herbPatchTurnsCompleteWhenGrowing()
    {
        assertEquals(FarmingPatchCycleState.EMPTY,
                decoder.decode(FarmingPatchKind.HERB, 0));
        assertEquals(FarmingPatchCycleState.GROWING,
                decoder.decode(FarmingPatchKind.HERB, 4));
        assertEquals(FarmingPatchCycleState.READY,
                decoder.decode(FarmingPatchKind.HERB, 8));
        assertEquals(FarmingPatchCycleState.DISEASED,
                decoder.decode(FarmingPatchKind.HERB, 128));
        assertEquals(FarmingPatchCycleState.DEAD,
                decoder.decode(FarmingPatchKind.HERB, 170));
    }

    @Test
    public void treePatchTracksPlantAndReplantCycle()
    {
        assertEquals(FarmingPatchCycleState.EMPTY,
                decoder.decode(FarmingPatchKind.TREE, 0));
        assertEquals(FarmingPatchCycleState.GROWING,
                decoder.decode(FarmingPatchKind.TREE, 8));
        assertEquals(FarmingPatchCycleState.READY,
                decoder.decode(FarmingPatchKind.TREE, 13));
        assertEquals(FarmingPatchCycleState.DISEASED,
                decoder.decode(FarmingPatchKind.TREE, 73));
        assertEquals(FarmingPatchCycleState.DEAD,
                decoder.decode(FarmingPatchKind.TREE, 137));
    }
}
