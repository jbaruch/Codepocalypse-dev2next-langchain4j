package dev2next.langchain4j

import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.guardrail.InputGuardrail
import dev.langchain4j.guardrail.InputGuardrailResult
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

/**
 * Input guardrail that validates user questions are related to airline loyalty programs.
 * 
 * This is a vanilla LangChain4j InputGuardrail implementation (Quarkus deprecated their guardrails).
 * Uses Quarkus CDI to inject the ChatModel for LLM-based validation.
 * 
 * Questions about Delta SkyMiles, United MileagePlus, elite status, miles, points, etc. are allowed.
 * Questions about unrelated topics are rejected with a helpful message.
 */
@ApplicationScoped
class AirlineLoyaltyInputGuardrail @Inject constructor(
    private val chatModel: ChatModel
) : InputGuardrail {

    companion object {
        private const val VALIDATION_SYSTEM_PROMPT = """You are a strict classifier that determines if a user question is related to airline loyalty programs.

Airline loyalty program topics include:
- Elite status (Medallion, Premier, Diamond, Platinum, Gold, Silver, etc.)
- Earning and redeeming miles/points
- Status qualification requirements (MQMs, MQDs, PQPs, PQFs, etc.)
- Tier benefits and perks
- Upgrades and priority services
- Specific airlines (Delta, United, American, Southwest, etc.)
- Partnerships and co-branded credit cards
- Travel-related questions about loyalty programs
- Comparing loyalty programs

You must respond with ONLY one word:
- "YES" if the question is about airline loyalty programs
- "NO" if the question is about anything else

Do not provide explanations. Only answer YES or NO."""

        private const val REJECTION_MESSAGE = """I'm sorry, but I can only help with questions about airline loyalty programs, specifically Delta SkyMiles and United MileagePlus.

I can assist you with:
- Elite status qualification requirements
- Earning and spending miles/points
- Tier benefits and perks
- Comparing Delta and United programs
- Understanding how to achieve or maintain elite status

Please ask me a question related to airline loyalty programs."""
    }

    override fun validate(userMessage: UserMessage): InputGuardrailResult {
        val question = userMessage.singleText()
        
        Log.infof("🛡️ Guardrail validating with LLM: %s", question.take(50))
        
        return try {
            // Use the Quarkus ChatModel to determine if the question is airline loyalty related
            val validationPrompt = "$VALIDATION_SYSTEM_PROMPT\n\nUser question: $question"
            val answer = chatModel.chat(validationPrompt).trim().uppercase()
            
            Log.infof("🤖 LLM validation response: %s", answer)
            
            if (answer.startsWith("YES")) {
                Log.info("✅ Guardrail PASSED - LLM confirmed question is airline loyalty related")
                success()
            } else {
                Log.info("❌ Guardrail FAILED - LLM determined question is not airline loyalty related")
                // Use fatal() helper method from InputGuardrail interface to prevent LLM call
                fatal(REJECTION_MESSAGE)
            }
        } catch (e: Exception) {
            Log.errorf(e, "⚠️ Error during LLM guardrail validation: %s", e.message)
            // On error, allow the question through (fail open) to avoid blocking legitimate requests
            Log.warn("Guardrail validation failed, allowing question through (fail-open)")
            success()
        }
    }
}
