package compass;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FarmingPatchStateDecoderTest
{
    private final FarmingPatchStateDecoder decoder = new FarmingPatchStateDecoder();

    @Test
    public void herbPatchTurnsCompleteWhenGrowing()
    {
        assertEquals(PatchState.EMPTY,
                decoder.decode(FarmingPatchKind.HERB, 0));
        assertEquals(PatchState.GROWING,
                decoder.decode(FarmingPatchKind.HERB, 4));
        assertEquals(PatchState.READY,
                decoder.decode(FarmingPatchKind.HERB, 8));
        assertEquals(PatchState.DISEASED,
                decoder.decode(FarmingPatchKind.HERB, 128));
        assertEquals(PatchState.DEAD,
                decoder.decode(FarmingPatchKind.HERB, 170));
    }

    @Test
    public void treePatchTracksPlantAndReplantCycle()
    {
        assertEquals(PatchState.EMPTY,
                decoder.decode(FarmingPatchKind.TREE, 0));
        assertEquals(PatchState.GROWING,
                decoder.decode(FarmingPatchKind.TREE, 8));
        assertEquals(PatchState.READY,
                decoder.decode(FarmingPatchKind.TREE, 13));
        assertEquals(PatchState.DISEASED,
                decoder.decode(FarmingPatchKind.TREE, 73));
        assertEquals(PatchState.DEAD,
                decoder.decode(FarmingPatchKind.TREE, 137));
    }
}
