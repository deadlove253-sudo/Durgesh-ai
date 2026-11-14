package com.example

import com.example.domain.AppTarget
import com.example.domain.CommandIntent
import com.example.domain.HindiCommandProcessor
import com.example.domain.SettingsTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HindiCommandProcessorTest {

  private val processor = HindiCommandProcessor()

  @Test
  fun `parses Hindi app launch commands`() {
    assertEquals(CommandIntent.OpenApp(AppTarget.YOUTUBE), processor.parse("यूट्यूब खोलो"))
    assertEquals(CommandIntent.OpenApp(AppTarget.WHATSAPP), processor.parse("व्हाट्सएप चालू करो"))
    assertEquals(CommandIntent.OpenApp(AppTarget.CHROME), processor.parse("chrome kholo"))
    assertEquals(CommandIntent.OpenApp(AppTarget.CAMERA), processor.parse("कैमरा खोलो"))
  }

  @Test
  fun `parses settings commands and flags restricted direct toggle attempts`() {
    val wifi = processor.parse("वाई-फ़ाई सेटिंग्स खोलो")
    assertEquals(CommandIntent.OpenSettings(SettingsTarget.WIFI, false), wifi)

    val wifiToggle = processor.parse("wifi on karo")
    assertEquals(CommandIntent.OpenSettings(SettingsTarget.WIFI, true), wifiToggle)

    val bt = processor.parse("ब्लूटूथ सेटिंग्स")
    assertEquals(CommandIntent.OpenSettings(SettingsTarget.BLUETOOTH, false), bt)

    val settings = processor.parse("फ़ोन सेटिंग्स खोलो")
    assertEquals(CommandIntent.OpenSettings(SettingsTarget.PHONE_SETTINGS, false), settings)
  }

  @Test
  fun `parses offline queries torch and math`() {
    assertEquals(CommandIntent.QueryBattery, processor.parse("बैटरी कितनी है"))
    assertEquals(CommandIntent.QueryTime, processor.parse("समय क्या हुआ है"))
    assertEquals(CommandIntent.ToggleTorch(true), processor.parse("टॉर्च जलाओ"))
    assertEquals(CommandIntent.ToggleTorch(false), processor.parse("फ़्लैशलाइट बंद करो"))

    val math = processor.parse("12 गुना 8")
    assertTrue(math is CommandIntent.CalculateMath)
    assertEquals("96", (math as CommandIntent.CalculateMath).formattedResult)
  }

  @Test
  fun `returns unsupported for unknown commands`() {
    val result = processor.parse("मेरे बैंक का पासवर्ड बदल दो")
    assertTrue(result is CommandIntent.Unsupported)
  }
}
