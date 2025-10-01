# Stage 04: Model Context Protocol (MCP) Integration - Instructions

This guide walks you through implementing MCP (Model Context Protocol) integration to replace the RAG system with real-time data fetching from airline websites.

## Prerequisites

Before starting Stage 04, ensure you have completed Stage 03 (RAG) or are starting from the `stage-03-rag` branch.

## Step-by-Step Implementation

### Step 1: Add MCP Dependencies

Update `pom.xml` to add MCP server and client dependencies:

```xml
<!-- Add after existing Quarkus LangChain4j dependencies -->

<!-- MCP Server with SSE Transport -->
<dependency>
    <groupId>io.quarkiverse.mcp</groupId>
    <artifactId>quarkus-mcp-server-sse</artifactId>
    <version>1.6.0</version>
</dependency>

<!-- MCP Client for LangChain4j -->
<dependency>
    <groupId>io.quarkiverse.langchain4j</groupId>
    <artifactId>quarkus-langchain4j-mcp</artifactId>
    <!-- Version managed by quarkus-langchain4j-bom (1.2.0) -->
</dependency>
```

**Note**: Keep the `jsoup` dependency from Stage 03 - we'll use it for web scraping.

### Step 2: Create MCP Server Tools

Create `src/main/kotlin/dev2next/langchain4j/AirlineMcpTools.kt`:

```kotlin
package dev2next.langchain4j

import io.quarkiverse.mcp.server.Tool
import jakarta.enterprise.context.ApplicationScoped
import org.jsoup.Jsoup

@ApplicationScoped
class AirlineMcpTools {

    companion object {
        private const val DELTA_URL = "https://www.delta.com/us/en/skymiles/medallion-program/qualify-for-status"
        private const val UNITED_URL = "https://www.united.com/en/us/fly/mileageplus/premier/qualify.html"
    }

    @Tool(description = "Fetches the current Delta SkyMiles Medallion qualification requirements including tiers, miles needed, and benefits from the official Delta website")
    fun getDeltaMedallionQualification(): String {
        return fetchWebContent(DELTA_URL)
    }

    @Tool(description = "Fetches the current United MileagePlus Premier qualification requirements including tiers, miles needed, and benefits from the official United website")
    fun getUnitedPremierQualification(): String {
        return fetchWebContent(UNITED_URL)
    }

    @Tool(description = "Compares Delta and United airline loyalty programs by fetching current information from both official websites and presenting a side-by-side comparison")
    fun compareAirlinePrograms(): String {
        val deltaInfo = getDeltaMedallionQualification()
        val unitedInfo = getUnitedPremierQualification()
        
        return """
            === DELTA SKYMILES MEDALLION PROGRAM ===
            $deltaInfo
            
            === UNITED MILEAGEPLUS PREMIER PROGRAM ===
            $unitedInfo
        """.trimIndent()
    }

    private fun fetchWebContent(url: String): String {
        return try {
            Jsoup.connect(url)
                .timeout(30000)
                .get()
                .text()
                .take(5000) // Limit to 5000 characters to avoid overwhelming the LLM context
        } catch (e: Exception) {
            "Error fetching content from $url: ${e.message}"
        }
    }
}
```

**Key Points**:
- `@ApplicationScoped` makes it a CDI bean
- `@Tool` annotation exposes methods as MCP tools
- Each tool has a clear description for the LLM
- Uses Jsoup to fetch and parse HTML
- Character limit prevents token overflow

### Step 3: Configure MCP Client

Add MCP client configuration to `src/main/resources/application.properties`:

```properties
# MCP Client Configuration
quarkus.langchain4j.mcp.airline-tools.transport-type=http
quarkus.langchain4j.mcp.airline-tools.url=http://localhost:8080/mcp/sse
quarkus.langchain4j.mcp.airline-tools.log-requests=true
quarkus.langchain4j.mcp.airline-tools.log-responses=true
```

**Important**: 
- `airline-tools` is the MCP client name (you'll use this in code)
- `transport-type=http` uses HTTP/SSE transport
- URL points to the same application (`localhost:8080/mcp/sse`)
- Logging helps with debugging

### Step 4: Update AI Service to Use MCP

Modify `src/main/kotlin/dev2next/langchain4j/AirlineLoyaltyAssistant.kt`:

1. **Add the import**:

```kotlin
import io.quarkiverse.langchain4j.mcp.runtime.McpToolBox
```

2. **Update the system message**:

```kotlin
@SystemMessage("""
    You are a helpful airline loyalty program assistant with access to real-time information 
    via MCP tools about Delta SkyMiles Medallion and United MileagePlus Premier programs.
    
    You can fetch current qualification requirements, compare programs, and provide 
    up-to-date information directly from airline websites.
    
    Always be friendly, clear, and help users understand:
    - How to earn elite status
    - Qualification requirements
    - Program benefits
    - Differences between programs
""")
```

3. **Add `@McpToolBox` annotation to the chat method**:

```kotlin
fun chat(
    @MemoryId memberId: Int,
    @UserMessage message: String,
    @McpToolBox("airline-tools") // Enable MCP tools from airline-tools client
): String
```

**Complete method should look like**:

```kotlin
@RegisterAiService(
    chatMemoryProviderSupplier = RegisterAiService.BeanChatMemoryProviderSupplier::class
)
interface AirlineLoyaltyAssistant {

    @SystemMessage("""
        You are a helpful airline loyalty program assistant with access to real-time information 
        via MCP tools about Delta SkyMiles Medallion and United MileagePlus Premier programs.
        
        You can fetch current qualification requirements, compare programs, and provide 
        up-to-date information directly from airline websites.
        
        Always be friendly, clear, and help users understand:
        - How to earn elite status
        - Qualification requirements
        - Program benefits
        - Differences between programs
    """)
    fun chat(
        @MemoryId memberId: Int,
        @UserMessage message: String,
        @McpToolBox("airline-tools")
    ): String
}
```

### Step 5: Remove RAG Components

Delete the following files (no longer needed):

1. `src/main/kotlin/dev2next/langchain4j/DocumentIngestionService.kt`
2. `src/main/kotlin/dev2next/langchain4j/RagConfiguration.kt`

These are replaced by real-time MCP tools.

### Step 6: Update UI

Modify `src/main/resources/templates/AssistantController/index.html`:

1. **Change the badge**:

```html
<!-- Replace RAG badge -->
<span class="badge">&#x2699;&#xFE0F; MCP Enabled</span>
```

2. **Update the subtitle**:

```html
<p class="subtitle">
    I use Model Context Protocol to fetch real-time Delta and United loyalty program information.
</p>
```

3. **Update the tip**:

```html
<p class="tip">
    💡 <strong>Tip:</strong> I fetch real-time information from airline websites using MCP tools. 
    Ask me to compare programs or get current qualification requirements!
</p>
```

4. **Update the footer**:

```html
<div class="footer">
    Powered by Quarkus + Kotlin + LangChain4j + MCP
</div>
```

### Step 7: Build and Test

1. **Build the application**:

```bash
./mvnw clean compile
```

2. **Start in dev mode**:

```bash
./mvnw quarkus:dev
```

3. **Test in browser**:
   - Open http://localhost:8080
   - Try questions like:
     - "What are Delta's Medallion qualification requirements?"
     - "How do I earn United Premier status?"
     - "Compare Delta and United loyalty programs"

4. **Watch the logs**:
   - Look for MCP server startup messages
   - Watch MCP requests/responses (if logging enabled)
   - Check for tool execution

### Step 8: Test MCP Server with Inspector

Verify the MCP server is working:

```bash
npx @modelcontextprotocol/inspector http://localhost:8080/mcp/sse
```

You should see three tools:
- `getDeltaMedallionQualification`
- `getUnitedPremierQualification`
- `compareAirlinePrograms`

### Step 9: Build Docker Image

1. **Package the application**:

```bash
./mvnw clean package -DskipTests
```

2. **Build Docker image**:

```bash
docker build -f src/main/docker/Dockerfile.jvm \
  -t codepocalypse-airline-assistant:latest \
  -t codepocalypse-airline-assistant:mcp-stage-04 .
```

3. **Test Docker image locally**:

```bash
docker run -p 8080:8080 \
  -e QUARKUS_LANGCHAIN4J_OPENAI_API_KEY=your-openai-api-key \
  codepocalypse-airline-assistant:latest
```

### Step 10: Push to Docker Hub (Optional)

If you want to share your image:

1. **Tag for Docker Hub**:

```bash
docker tag codepocalypse-airline-assistant:latest \
  yourusername/codepocalypse-airline-assistant:latest

docker tag codepocalypse-airline-assistant:mcp-stage-04 \
  yourusername/codepocalypse-airline-assistant:mcp-stage-04
```

2. **Push to Docker Hub**:

```bash
docker push yourusername/codepocalypse-airline-assistant:latest
docker push yourusername/codepocalypse-airline-assistant:mcp-stage-04
```

### Step 11: Create Documentation

Create `DOCKER.md` with Docker deployment instructions (see STAGE-04-SUMMARY.md for template).

### Step 12: Update README

Update `README.md` to include:
- Docker quick start section
- MCP features in the features list
- MCP architecture section
- Updated demo stages list

### Step 13: Commit and Push

```bash
# Switch to main branch
git checkout main

# Add all changes
git add -A

# Commit with descriptive message
git commit -m "Stage 04: MCP Integration - Replace RAG with Model Context Protocol

- Added MCP server with airline loyalty program tools
- Integrated MCP client with LangChain4j AI service
- Removed RAG implementation
- Updated UI to show MCP badge and messaging
- Added Docker deployment
- Updated documentation"

# Push to main
git push origin main

# Create and push stage branch
git checkout -b stage-04-mcp
git push -u origin stage-04-mcp
```

## Troubleshooting

### Problem: Cannot find McpToolBox

**Error**: `Unresolved reference: McpToolBox`

**Solution**: Use the correct import:
```kotlin
import io.quarkiverse.langchain4j.mcp.runtime.McpToolBox
```

### Problem: Configuration warnings about unknown keys

**Error**: `Unrecognized configuration key "quarkus.langchain4j.mcp.log-requests"`

**Solution**: Use client-scoped keys:
```properties
# Wrong
quarkus.langchain4j.mcp.log-requests=true

# Correct
quarkus.langchain4j.mcp.airline-tools.log-requests=true
```

### Problem: MCP tools not being called

**Checklist**:
1. ✅ `@McpToolBox("airline-tools")` annotation present on chat method
2. ✅ MCP client configuration in `application.properties`
3. ✅ `AirlineMcpTools.kt` class is `@ApplicationScoped`
4. ✅ Methods have `@Tool` annotation
5. ✅ OpenAI API key is valid

### Problem: Web scraping fails

**Possible causes**:
- Network connectivity issues
- Airline website structure changed
- Timeout too short (increase from 30000ms)
- Website blocking automated requests

**Solution**: Check logs and adjust timeout or implement retry logic.

### Problem: Response too slow

**Causes**:
- Web scraping takes 3-5 seconds per tool
- Multiple tools called = multiple web requests

**Solutions**:
- Implement caching (Redis, Caffeine)
- Pre-fetch data periodically
- Use shorter character limits

## Verification Checklist

Before considering Stage 04 complete, verify:

- [ ] Application compiles without errors
- [ ] MCP server visible in MCP Inspector
- [ ] All three tools listed in Inspector
- [ ] Tools can be executed from Inspector
- [ ] Web UI loads successfully
- [ ] MCP badge shows in UI
- [ ] Questions trigger tool usage
- [ ] Responses include real-time data
- [ ] Docker image builds successfully
- [ ] Docker container runs correctly
- [ ] Documentation is complete
- [ ] Code is committed and pushed

## Testing Questions

Try these questions to verify MCP tools work:

1. **Delta-specific**:
   - "What are the Delta Medallion tier levels?"
   - "How many miles do I need for Delta Gold status?"

2. **United-specific**:
   - "What are United's Premier qualification requirements?"
   - "How do I earn United Premier Platinum?"

3. **Comparison**:
   - "Compare Delta and United loyalty programs"
   - "Which program is better for earning status?"

4. **General**:
   - "What are the benefits of elite status?"
   - "How do airline loyalty programs work?"

## Next Steps

After completing Stage 04, potential enhancements:

1. **Add more airlines**: American, Southwest, Alaska
2. **Implement caching**: Reduce web scraping frequency
3. **Structured data**: Parse HTML into JSON instead of text
4. **Multi-modal**: Add image support for tier comparison charts
5. **Rate limiting**: Protect against excessive requests
6. **Multi-module**: Separate MCP server for independent deployment

## Resources

- [Quarkus LangChain4j Documentation](https://docs.quarkiverse.io/quarkus-langchain4j/dev/)
- [Quarkus MCP Server Documentation](https://docs.quarkiverse.io/quarkus-mcp-server/dev/)
- [Model Context Protocol Specification](https://modelcontextprotocol.io/)
- [MCP Inspector Tool](https://github.com/modelcontextprotocol/inspector)
- [Jsoup Documentation](https://jsoup.org/)

## Summary

Stage 04 demonstrates:
- ✅ MCP server implementation with Quarkus
- ✅ MCP client integration with LangChain4j
- ✅ Real-time data fetching from external sources
- ✅ Replacing static RAG with dynamic tools
- ✅ Docker deployment of complete application
- ✅ Tool annotation and CDI integration

The application now fetches current airline information on-demand, ensuring users always get up-to-date data!
