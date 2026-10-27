package im.miaoli.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TribuneConfigTest {
    @Test
    fun allocatesNextIdAfterTheHighestExistingId() {
        val config = TribuneConfig(
            tribunes = listOf(
                Tribunes.moules.copy(id = 3L),
                Tribunes.linuxfr.copy(id = 7L),
            ),
        )

        assertEquals(8L, config.nextTribuneId())
    }

    @Test
    fun allocatesFirstIdWhenThereAreNoTribunes() {
        assertEquals(1L, TribuneConfig(tribunes = emptyList()).nextTribuneId())
    }
}
