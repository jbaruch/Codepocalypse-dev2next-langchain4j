package dev2next.langchain4j

import io.quarkiverse.mcp.server.Tool
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import org.jsoup.Jsoup

/**
 * MCP Server tools for airline loyalty program information.
 * These tools fetch current information from Delta and United websites
 * and make it available to the AI assistant via Model Context Protocol.
 */
@ApplicationScoped
class AirlineMcpTools {

    companion object {
        private const val DELTA_URL = "https://www.delta.com/us/en/skymiles/medallion-program/how-to-qualify"
        private const val UNITED_URL = "https://www.united.com/en/us/fly/mileageplus/premier/qualify.html"
    }

    /**
     * Fetches current Delta SkyMiles Medallion qualification requirements.
     * Returns comprehensive information about earning Medallion status tiers.
     */
    @Tool(description = "Fetches current Delta SkyMiles Medallion qualification requirements and status tier information")
    fun getDeltaMedallionQualification(): String {
        Log.info("Fetching Delta Medallion qualification information")
        return try {
            val doc = Jsoup.connect(DELTA_URL)
                .userAgent("Mozilla/5.0 (compatible; AirlineLoyaltyBot/1.0)")
                .timeout(30000)
                .get()

            val text = doc.body().text()
            val title = doc.title()

            Log.info("Successfully fetched Delta information: $title (${text.length} characters)")
            
            // Return structured information
            """
            Source: $DELTA_URL
            Title: $title
            
            Content:
            ${text.take(5000)} // Limit to 5000 chars to avoid token overflow
            
            Note: This information is from Delta's official website and represents current qualification requirements.
            """.trimIndent()
        } catch (e: Exception) {
            Log.error("Failed to fetch Delta qualification information", e)
            "Error: Unable to fetch Delta qualification information. ${e.message}"
        }
    }

    /**
     * Fetches current United MileagePlus Premier qualification requirements.
     * Returns comprehensive information about earning Premier status tiers.
     */
    @Tool(description = "Fetches current United MileagePlus Premier qualification requirements and status tier information")
    fun getUnitedPremierQualification(): String {
        Log.info("Fetching United Premier qualification information")
        return try {
            val doc = Jsoup.connect(UNITED_URL)
                .userAgent("Mozilla/5.0 (compatible; AirlineLoyaltyBot/1.0)")
                .timeout(30000)
                .get()

            val text = doc.body().text()
            val title = doc.title()

            Log.info("Successfully fetched United information: $title (${text.length} characters)")
            
            // Return structured information
            """
            Source: $UNITED_URL
            Title: $title
            
            Content:
            ${text.take(5000)} // Limit to 5000 chars to avoid token overflow
            
            Note: This information is from United's official website and represents current qualification requirements.
            """.trimIndent()
        } catch (e: Exception) {
            Log.error("Failed to fetch United qualification information", e)
            "Error: Unable to fetch United qualification information. ${e.message}"
        }
    }

    /**
     * Compares qualification requirements between Delta and United programs.
     * Fetches information from both airlines and provides a comparative analysis.
     */
    @Tool(description = "Compares Delta SkyMiles Medallion and United MileagePlus Premier qualification requirements")
    fun compareAirlinePrograms(): String {
        Log.info("Comparing Delta and United loyalty programs")
        
        val deltaInfo = getDeltaMedallionQualification()
        val unitedInfo = getUnitedPremierQualification()
        
        return """
        COMPARISON OF AIRLINE LOYALTY PROGRAMS
        
        === DELTA SKYMILES MEDALLION ===
        $deltaInfo
        
        === UNITED MILEAGEPLUS PREMIER ===
        $unitedInfo
        
        Use this information to provide a detailed comparison based on the customer's needs.
        """.trimIndent()
    }
}
