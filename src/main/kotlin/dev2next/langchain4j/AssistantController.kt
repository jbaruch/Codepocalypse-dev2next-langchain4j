package dev2next.langchain4j

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
            val answer = assistant.chat(MEMORY_ID, question)
            Log.info("Response generated successfully with conversation context")
            
            index.data("question", question)
                .data("answer", answer)
                .data("error", "")
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
