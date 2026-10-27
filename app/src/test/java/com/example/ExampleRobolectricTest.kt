package com.example

import com.example.actions.DeviceAction
import com.example.ai.AaruAiService
import com.example.ai.AiBrainConfig
import com.example.ai.UserEmotion
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExampleRobolectricTest {

    private val aiService = AaruAiService()

    @Test
    fun emotionalIntelligence_detectsSadHappyFrustratedAndTiredCues() {
        assertEquals(
            UserEmotion.SAD,
            aiService.detectEmotionFromText("Yaar aaj main bahut udaas aur lonely feel kar raha hoon")
        )
        assertEquals(
            UserEmotion.HAPPY,
            aiService.detectEmotionFromText("Yay! Mujhe promotion mil gaya, super happy today!")
        )
        assertEquals(
            UserEmotion.FRUSTRATED,
            aiService.detectEmotionFromText("Yaar ye kaam nahi kar raha, dimag kharab ho gaya")
        )
        assertEquals(
            UserEmotion.TIRED,
            aiService.detectEmotionFromText("Main bahut thak gaya hoon, neend aa rahi hai")
        )
        assertEquals(
            UserEmotion.CONFUSED,
            aiService.detectEmotionFromText("Mujhe samajh nahi aa raha kaise shuru karu")
        )
    }

    @Test
    fun aaruAiService_parsesYouTubeWhatsAppInstagramTimerAndMemoryCommands() = runBlocking {
        val config = AiBrainConfig(userNickname = "Boss")

        // 1. YouTube search command in Hinglish
        val ytRes = aiService.processUserMessage(
            userPrompt = "Aaru search Arijit Singh songs on YouTube",
            memories = emptyList(),
            recentChat = emptyList(),
            preferredLanguage = "hi",
            memoryEnabled = true,
            brainConfig = config
        )
        assertTrue(ytRes.detectedAction is DeviceAction.YouTubeSearch)
        val ytAction = ytRes.detectedAction as DeviceAction.YouTubeSearch
        assertTrue(ytAction.query.contains("Arijit Singh", ignoreCase = true))

        // 2. WhatsApp message command
        val waRes = aiService.processUserMessage(
            userPrompt = "WhatsApp par message bhejo: Main 10 minute mein aa raha hoon",
            memories = emptyList(),
            recentChat = emptyList(),
            preferredLanguage = "hi",
            memoryEnabled = true,
            brainConfig = config
        )
        assertTrue(waRes.detectedAction is DeviceAction.SendWhatsApp)

        // 3. Instagram open command
        val igRes = aiService.processUserMessage(
            userPrompt = "Aaru Instagram kholo @virat.kohli",
            memories = emptyList(),
            recentChat = emptyList(),
            preferredLanguage = "hi",
            memoryEnabled = true,
            brainConfig = config
        )
        assertTrue(igRes.detectedAction is DeviceAction.OpenInstagram)

        // 4. Timer command
        val timerRes = aiService.processUserMessage(
            userPrompt = "Aaru 10 minute ka timer laga do",
            memories = emptyList(),
            recentChat = emptyList(),
            preferredLanguage = "hi",
            memoryEnabled = true,
            brainConfig = config
        )
        assertTrue(timerRes.detectedAction is DeviceAction.SetTimer)
        assertEquals(600, (timerRes.detectedAction as DeviceAction.SetTimer).seconds)

        // 5. Long-term memory save command
        val memRes = aiService.processUserMessage(
            userPrompt = "Aaru yaad rakhna mera office gate code 7788 hai",
            memories = emptyList(),
            recentChat = emptyList(),
            preferredLanguage = "hi",
            memoryEnabled = true,
            brainConfig = config
        )
        assertTrue(memRes.newMemories.isNotEmpty())
        assertTrue(memRes.newMemories.first().content.contains("7788"))
    }
}
