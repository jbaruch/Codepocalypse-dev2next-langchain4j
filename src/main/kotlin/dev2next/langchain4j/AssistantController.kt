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
 */
@Path("/")
class AssistantController {

    @Inject
    lateinit var assistant: AirlineLoyaltyAssistant

    @Inject
    @io.quarkus.qute.Location("AssistantController/index.html")
    lateinit var index: Template

    /**
     * Renders the main page with an empty form.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    fun showForm(): TemplateInstance {
        return index.data("question", "")
            .data("answer", "")
            .data("error", "")
    }

    /**
     * Processes the user's question and returns the response.
     * Handles form submission with question parameter.
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
        }

        return try {
            Log.info("Processing question: ${question.take(50)}...")
            val answer = assistant.chat(question)
            Log.info("Response generated successfully")
            
            index.data("question", question)
                .data("answer", answer)
                .data("error", "")
        } catch (e: Exception) {
            Log.error("Error processing question", e)
            index.data("question", question)
                .data("answer", "")
                .data("error", "Sorry, I encountered an error: ${e.message}. Please try again.")
        }
    }
}
