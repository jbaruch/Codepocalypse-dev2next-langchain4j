package dev2next.langchain4j

import dev.langchain4j.guardrail.InputGuardrailException
import io.quarkus.logging.Log
import io.quarkus.qute.Template
import io.quarkus.qute.TemplateInstance
import jakarta.inject.Inject
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType

/**
 * Main controller for the Airline Loyalty Assistant web application.
 * Handles both GET and POST requests for the UI.
 * Uses a single conversation memory for the entire application.
 * Guardrails are applied automatically using vanilla LangChain4j @InputGuardrails annotation.
 */
@Path("/")
class AssistantController {

    @Inject
    lateinit var assistant: AirlineLoyaltyAssistant

    @Inject
    @io.quarkus.qute.Location("AssistantController/index.html")
    lateinit var index: Template

    companion object {
        // Single memory ID for application-wide conversation
        // In a production app, this would be per-user/session
        private const val MEMORY_ID = "demo-conversation"
    }

    /**
     * Renders the main page with an empty form.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    fun showForm(): TemplateInstance {
        return index.data("question", "")
            .data("answer", "")
            .data("error", "")
            .data("hasMemory", true) // Indicate memory is active
    }

    /**
     * Processes the user's question and returns the response.
     * Handles form submission with question parameter.
     * Maintains conversation context using memory.
     * Guardrails are applied automatically by Quarkus LangChain4j before calling the LLM.
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    fun askQuestion(@FormParam("question") question: String?): TemplateInstance {
        // Validate input
        if (question.isNullOrBlank()) {
            Log.warn("Empty question submitted")
            return index.data("question", "")
                .data("answer", "")
                .data("error", "Please enter a question about airline loyalty programs.")
                .data("hasMemory", true)
        }

        return try {
            Log.info("Processing question with memory: ${question.take(50)}...")
            // Pass memory ID to maintain conversation context
            // Guardrails are applied automatically by Quarkus before the LLM is called
            val answer = assistant.chat(MEMORY_ID, question)
            Log.info("Response generated successfully with conversation context")
            
            index.data("question", question)
                .data("answer", answer)
                .data("error", "")
                .data("hasMemory", true)
        } catch (e: InputGuardrailException) {
            // Guardrail rejected the question
            Log.warn("Guardrail rejected question: ${e.message}")
            
            // Extract the friendly message from the exception
            // The exception message format is: "The guardrail <class name> failed with this message: <actual message>"
            val friendlyMessage = e.message?.let { msg ->
                val prefix = "failed with this message: "
                val index = msg.indexOf(prefix)
                if (index != -1) {
                    msg.substring(index + prefix.length)
                } else {
                    msg
                }
            } ?: "Your question is not related to airline loyalty programs."
            
            // Convert newlines to HTML line breaks for proper display
            val htmlFormattedMessage = friendlyMessage.replace("\n", "<br>")
            
            index.data("question", question)
                .data("answer", "")
                .data("error", htmlFormattedMessage)
                .data("hasMemory", true)
        } catch (e: Exception) {
            Log.error("Error processing question", e)
            index.data("question", question)
                .data("answer", "")
                .data("error", "Sorry, I encountered an error: ${e.message}. Please try again.")
                .data("hasMemory", true)
        }
    }
}
