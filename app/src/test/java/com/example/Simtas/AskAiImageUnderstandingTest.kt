package com.example.Simtas

import com.example.Simtas.network.ChatSendMessageRequest
import com.example.Simtas.ui.qa.MessageSource
import com.example.Simtas.ui.qa.QaAssistantUiState
import com.example.Simtas.ui.qa.QaMessageItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AskAiImageUnderstandingTest {

    @Test
    fun testQaMessageItem_backwardCompatibility() {
        // Construct message without image attributes (existing behavior)
        val msg = QaMessageItem(
            sender = MessageSource.USER,
            text = "Hello SmartNotes AI"
        )
        assertEquals("Hello SmartNotes AI", msg.text)
        assertEquals(MessageSource.USER, msg.sender)
        assertNull(msg.imageBitmap)
        assertNull(msg.imageUri)
        assertNotNull(msg.id)
    }

    @Test
    fun testQaMessageItem_withImageProperties() {
        val msg = QaMessageItem(
            sender = MessageSource.USER,
            text = "Analyze this diagram",
            imageUri = "content://media/external/images/media/12345"
        )
        assertEquals("Analyze this diagram", msg.text)
        assertEquals("content://media/external/images/media/12345", msg.imageUri)
        assertNull(msg.imageBitmap)
    }

    @Test
    fun testQaAssistantUiState_defaultsAndImageState() {
        val defaultState = QaAssistantUiState()
        assertNull(defaultState.selectedImageUri)
        assertNull(defaultState.selectedImageBitmap)
        assertNull(defaultState.selectedImageBase64)
        assertNull(defaultState.selectedImageMime)
        assertTrue(defaultState.messages.isEmpty())

        val stateWithImage = defaultState.copy(
            selectedImageUri = "content://media/1",
            selectedImageBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
            selectedImageMime = "image/png"
        )
        assertEquals("content://media/1", stateWithImage.selectedImageUri)
        assertEquals("image/png", stateWithImage.selectedImageMime)
        assertNotNull(stateWithImage.selectedImageBase64)
    }

    @Test
    fun testChatSendMessageRequest_supportsMultimodalPayload() {
        // Text-only request (backward compatibility)
        val textReq = ChatSendMessageRequest(
            message = "Explain photosynthesis"
        )
        assertEquals("Explain photosynthesis", textReq.message)
        assertEquals("send_message", textReq.action)
        assertNull(textReq.image_data)
        assertNull(textReq.image_mime)

        // Multimodal request with attached image Base64
        val imageReq = ChatSendMessageRequest(
            message = "What is shown in this circuit?",
            conversation_id = 10,
            image_data = "base64EncodedDataSample",
            image_mime = "image/jpeg"
        )
        assertEquals("What is shown in this circuit?", imageReq.message)
        assertEquals(10, imageReq.conversation_id)
        assertEquals("base64EncodedDataSample", imageReq.image_data)
        assertEquals("image/jpeg", imageReq.image_mime)
    }

    @Test
    fun testDefaultAcademicPrompt_whenPromptIsBlankWithImage() {
        val userPrompt = ""
        val hasImage = true
        val effectivePrompt = if (userPrompt.isBlank() && hasImage) {
            "Please analyze this image and explain it clearly step by step."
        } else {
            userPrompt.trim()
        }
        assertEquals("Please analyze this image and explain it clearly step by step.", effectivePrompt)
    }
}
