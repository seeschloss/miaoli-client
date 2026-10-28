package im.miaoli.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class TotozTest {
    @Test
    fun triplesSmallImages() {
        assertEquals(45f, totozDisplayLongEdgeDp(15f), 0.01f)
        assertEquals(90f, totozDisplayLongEdgeDp(30f), 0.01f)
        assertEquals(147f, totozDisplayLongEdgeDp(49f), 0.01f)
    }

    @Test
    fun usesLogarithmicRangeForMediumImages() {
        assertEquals(150f, totozDisplayLongEdgeDp(50f), 0.01f)
        assertEquals(175f, totozDisplayLongEdgeDp(100f), 0.01f)
        assertEquals(200f, totozDisplayLongEdgeDp(200f), 0.01f)
    }

    @Test
    fun capsLargeImagesAtTwoHundredDp() {
        assertEquals(200f, totozDisplayLongEdgeDp(400f), 0.01f)
    }
}
