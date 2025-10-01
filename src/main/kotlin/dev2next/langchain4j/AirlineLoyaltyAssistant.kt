package dev2next.langchain4j

import dev.langchain4j.service.SystemMessage
import dev.langchain4j.service.UserMessage
import io.quarkiverse.langchain4j.RegisterAiService
import jakarta.enterprise.context.ApplicationScoped

/**
 * AI Service for airline loyalty program assistance.
 * Uses Quarkus LangChain4j with CDI to provide AI-powered responses.
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
        
        Provide clear, concise, and accurate information. Be friendly and professional.
        If you don't know something, acknowledge it honestly.
        """
    )
    @UserMessage("{question}")
    fun chat(question: String): String
}
