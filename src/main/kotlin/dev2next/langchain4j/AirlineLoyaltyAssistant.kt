package dev2next.langchain4j

import dev.langchain4j.service.MemoryId
import dev.langchain4j.service.SystemMessage
import dev.langchain4j.service.UserMessage
import io.quarkiverse.langchain4j.RegisterAiService
import jakarta.enterprise.context.ApplicationScoped

/**
 * AI Service for airline loyalty program assistance with conversation memory.
 * Uses Quarkus LangChain4j with CDI to provide AI-powered responses.
 * Maintains conversation history to provide contextual, personalized assistance.
 */
@RegisterAiService
@ApplicationScoped
interface AirlineLoyaltyAssistant {

    @SystemMessage(
        """
        You are a helpful airline loyalty program assistant. You help customers understand:
        - How to earn and redeem miles
        - Membership tier benefits and requirements
        - Travel rewards and perks
        - Partner airlines and alliances
        - Upgrade policies
        - Award travel booking
        
        IMPORTANT: Remember information customers share during the conversation, including:
        - Their names and personal details
        - Their membership status and tier
        - Their travel preferences and history
        - Questions they've asked previously
        - Context from earlier in the conversation
        
        Use this remembered information to provide personalized, contextual responses.
        Reference previous parts of the conversation naturally when relevant.
        
        Provide clear, concise, and accurate information. Be friendly and professional.
        If you don't know something, acknowledge it honestly.
        """
    )
    @UserMessage("{question}")
    fun chat(@MemoryId memoryId: String, question: String): String
}
