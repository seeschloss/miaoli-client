package im.miaoli.ui

import androidx.compose.ui.graphics.Color
import im.miaoli.ui.theme.MiaoliDarkPrimary
import im.miaoli.ui.theme.MiaoliLightPrimary
import org.junit.Assert.assertEquals
import org.junit.Test

class ReadableColorsTest {
    @Test
    fun lightTribuneColorsUseBlackTextAndDarkLinks() {
        assertEquals(Color.Black, readableTextColor(Color(0xFFFDE096)))
        assertEquals(Color.Black, readableTextColor(Color(0xFF67C6F2)))
        assertEquals(Color.Black, readableTextColor(Color(0xFFB8E0C2)))
        assertEquals(MiaoliLightPrimary, readableLinkColor(Color(0xFFFDE096)))
        assertEquals(MiaoliLightPrimary, readableLinkColor(Color(0xFF67C6F2)))
        assertEquals(MiaoliLightPrimary, readableLinkColor(Color(0xFFB8E0C2)))
    }

    @Test
    fun darkTribuneColorsUseWhiteTextAndLightLinks() {
        assertEquals(Color.White, readableTextColor(Color(0xFF121212)))
        assertEquals(Color.White, readableTextColor(Color(0xFF1976D2)))
        assertEquals(MiaoliDarkPrimary, readableLinkColor(Color(0xFF121212)))
        assertEquals(MiaoliDarkPrimary, readableLinkColor(Color(0xFF1976D2)))
    }

    @Test
    fun blackAndWhiteUseTheOppositeTextColor() {
        assertEquals(Color.White, readableTextColor(Color.Black))
        assertEquals(Color.Black, readableTextColor(Color.White))
    }
}
