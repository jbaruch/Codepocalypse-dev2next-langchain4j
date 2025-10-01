package dev2next.langchain4j

import dev.langchain4j.service.MemoryId
import dev.langchain4j.service.SystemMessage
import dev.langchain4j.service.UserMessage
import dev.langchain4j.service.guardrail.InputGuardrails
import io.quarkiverse.langchain4j.RegisterAiService
import io.quarkiverse.langchain4j.mcp.runtime.McpToolBox
import jakarta.enterprise.context.ApplicationScoped

/**
 * AI Service for airline loyalty program assistance with conversation memory, MCP tools, and guardrails.
 * Uses Quarkus LangChain4j with CDI to provide AI-powered responses.
 * Maintains conversation history to provide contextual, personalized assistance.
 * Uses MCP (Model Context Protocol) to fetch current information from airline websites.
 * Uses vanilla LangChain4j input guardrails to limit questions to airline loyalty topics only.
 * 
 * MCP tools are automatically discovered from the configured airline-tools MCP server.
 * Guardrails are applied via @InputGuardrails annotation (vanilla LangChain4j) and executed automatically.
 */
@RegisterAiService
@ApplicationScoped
@InputGuardrails(AirlineLoyaltyInputGuardrail::class)
interface AirlineLoyaltyAssistant {

    @SystemMessage(
        """
        You are a helpful airline loyalty program assistant with access to real-time information 
        about Delta SkyMiles and United MileagePlus loyalty programs via MCP tools.
        
        AVAILABLE TOOLS:
        You have access to tools that fetch current information from:
        - Delta SkyMiles Medallion qualification requirements
        - United MileagePlus Premier qualification requirements
        - Comparison tools for analyzing both programs
        
        When answering questions, you should:
        1. Use the MCP tools to fetch current, accurate information from airline websites
        2. Cite your sources when providing specific facts (e.g., "According to Delta's current website...")
        3. Use comparison tools when asked about differences between airlines
        4. Remember information customers share during the conversation, including:
           - Their names and personal details
           - Their membership status and tier
           - Their travel preferences and history
           - Questions they've asked previously
           - Context from earlier in the conversation
        5. Use this remembered information to provide personalized, contextual responses
        6. Reference previous parts of the conversation naturally when relevant
        7. Call the appropriate MCP tool when you need current airline information
        
        Topics you help with:
        - How to earn and qualify for elite status (Medallion/Premier)
        - Membership tier benefits and requirements
        - Qualifying activities (flights, spending, partnerships)
        - Travel rewards and perks
        - Comparing Delta and United programs
        
        Provide clear, concise, and accurate information. Be friendly and professional.
        If you don't have information, use the MCP tools to fetch it from the airline websites.
        
        ALWAYS cite the airline source when providing specific qualification requirements or benefits.
        """
    )
    @UserMessage("{question}")
    @McpToolBox("airline-tools")
    fun chat(@MemoryId memoryId: String, question: String): String
}
