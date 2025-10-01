# Stage 04: Model Context Protocol (MCP) Integration - Summary

## Overview

Stage 04 replaces the RAG (Retrieval-Augmented Generation) implementation with **Model Context Protocol (MCP)**, enabling the assistant to fetch real-time airline loyalty program information directly from airline websites. This demonstrates the power of MCP for providing up-to-date, dynamic data to AI assistants.

## What Changed

### Added Files

1. **`AirlineMcpTools.kt`** - MCP server tools implementation
   - `@ApplicationScoped` CDI bean exposing airline data tools
   - Three `@Tool` annotated methods for MCP tool exposure
   - Uses Jsoup to fetch live data from airline websites

2. **`DOCKER.md`** - Docker deployment documentation
   - Instructions for running pre-built Docker images
   - Configuration options and environment variables
   - MCP server access details

### Modified Files

1. **`AirlineLoyaltyAssistant.kt`**
   - Added `@McpToolBox("airline-tools")` annotation to `chat()` method
   - Updated system message to mention MCP tools
   - Removed RAG-specific content

2. **`pom.xml`**
   - Added `io.quarkiverse.mcp:quarkus-mcp-server-sse:1.6.0`
   - Added `io.quarkiverse.langchain4j:quarkus-langchain4j-mcp:1.2.0`
   - Retained `org.jsoup:jsoup:1.18.3` for HTML fetching
   - Removed `dev.langchain4j:langchain4j` core dependency

3. **`application.properties`**
   - Added MCP client configuration for `airline-tools`
   - Configured HTTP/SSE transport to `http://localhost:8080/mcp/sse`
   - Added logging configuration for MCP requests/responses

4. **`index.html`**
   - Changed badge from "📚 RAG Enabled" to "⚙️ MCP Enabled"
   - Updated subtitle and tips to mention MCP
   - Added "+ MCP" to footer

5. **`README.md`**
   - Added Docker quick start section
   - Updated features list with MCP capabilities
   - Added MCP architecture section
   - Updated demo stages list

### Deleted Files

1. **`DocumentIngestionService.kt`** - No longer needed
   - RAG document ingestion removed
   - Replaced with real-time fetching via MCP

2. **`RagConfiguration.kt`** - No longer needed
   - InMemoryEmbeddingStore removed
   - DocumentRetriever removed

## Architecture

### MCP Server

The application exposes an MCP server at `/mcp/sse` using the **HTTP/SSE (Server-Sent Events)** transport protocol.

**Endpoint**: `http://localhost:8080/mcp/sse`

### Available Tools

1. **`getDeltaMedallionQualification`**
   - Fetches current Delta SkyMiles Medallion qualification requirements
   - Scrapes data from Delta's official website
   - Returns up to 5000 characters of formatted text

2. **`getUnitedPremierQualification`**
   - Fetches current United MileagePlus Premier qualification requirements
   - Scrapes data from United's official website
   - Returns up to 5000 characters of formatted text

3. **`compareAirlinePrograms`**
   - Calls both Delta and United tools
   - Returns a side-by-side comparison
   - Useful for users comparing programs

### MCP Client Integration

The AI service uses Quarkus LangChain4j MCP client to connect to the MCP server:

```kotlin
@RegisterAiService(
    chatMemoryProviderSupplier = RegisterAiService.BeanChatMemoryProviderSupplier::class
)
interface AirlineLoyaltyAssistant {
    @SystemMessage("""
        You are a helpful airline loyalty program assistant with access to 
        real-time information via MCP tools about Delta and United programs.
    """)
    fun chat(
        @MemoryId memberId: Int,
        @UserMessage message: String,
        @McpToolBox("airline-tools") // Enables MCP tools from airline-tools client
    ): String
}
```

### Configuration

```properties
# MCP Client Configuration
quarkus.langchain4j.mcp.airline-tools.transport-type=http
quarkus.langchain4j.mcp.airline-tools.url=http://localhost:8080/mcp/sse
quarkus.langchain4j.mcp.airline-tools.log-requests=true
quarkus.langchain4j.mcp.airline-tools.log-responses=true
```

### Same Application Deployment

**Important**: Both the MCP server and client run in the **same Quarkus application**. The client connects to `localhost:8080/mcp/sse` which is served by the same application. This simplifies deployment while demonstrating MCP architecture.

## Key Implementation Details

### Jsoup Web Scraping

Tools use Jsoup to fetch live HTML content:

```kotlin
private fun fetchWebContent(url: String): String {
    return try {
        Jsoup.connect(url)
            .timeout(30000)
            .get()
            .text()
            .take(5000) // Limit to 5000 characters
    } catch (e: Exception) {
        "Error fetching content: ${e.message}"
    }
}
```

### Tool Annotations

MCP tools are exposed using `@Tool` annotation from `io.quarkiverse.mcp.server`:

```kotlin
@Tool(description = "Fetches the current Delta SkyMiles Medallion qualification requirements")
fun getDeltaMedallionQualification(): String {
    return fetchWebContent("https://www.delta.com/us/en/skymiles/medallion-program/qualify-for-status")
}
```

### McpToolBox Annotation

The `@McpToolBox` annotation connects the AI service to MCP tools:

- **Package**: `io.quarkiverse.langchain4j.mcp.runtime.McpToolBox`
- **Parameter**: MCP client name (matches configuration key)
- **Placement**: On the AI service method (e.g., `chat()`)

## Benefits Over RAG

1. **Real-Time Data**: Always fetches current information from airline websites
2. **No Embedding Required**: No need to embed and store documents
3. **Dynamic Updates**: Information automatically reflects website changes
4. **No Stale Data**: RAG required periodic document updates; MCP is always current
5. **Simpler Architecture**: No embedding store or document ingestion needed

## Testing the MCP Server

### Using MCP Inspector

```bash
npx @modelcontextprotocol/inspector http://localhost:8080/mcp/sse
```

This opens a web UI where you can:
- View available tools
- Test tool execution
- Inspect requests/responses

### Manual Testing

1. Start the application: `./mvnw quarkus:dev`
2. Open browser to http://localhost:8080
3. Ask questions like:
   - "What are Delta's Medallion qualification requirements?"
   - "How do I earn United Premier status?"
   - "Compare Delta and United loyalty programs"

## Docker Deployment

### Pre-Built Images

Available on Docker Hub:
- `jbaruchs/codepocalypse-airline-assistant:latest`
- `jbaruchs/codepocalypse-airline-assistant:mcp-stage-04`

### Quick Start

```bash
docker run -p 8080:8080 \
  -e QUARKUS_LANGCHAIN4J_OPENAI_API_KEY=your-openai-api-key \
  jbaruchs/codepocalypse-airline-assistant:latest
```

See [DOCKER.md](DOCKER.md) for complete deployment instructions.

## Dependencies Added

```xml
<!-- MCP Server (SSE Transport) -->
<dependency>
    <groupId>io.quarkiverse.mcp</groupId>
    <artifactId>quarkus-mcp-server-sse</artifactId>
    <version>1.6.0</version>
</dependency>

<!-- MCP Client for LangChain4j -->
<dependency>
    <groupId>io.quarkiverse.langchain4j</groupId>
    <artifactId>quarkus-langchain4j-mcp</artifactId>
    <!-- Version 1.2.0 from BOM -->
</dependency>

<!-- Jsoup for HTML fetching (retained from Stage 03) -->
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
    <version>1.18.3</version>
</dependency>
```

## Troubleshooting

### Import Issue: McpToolBox

**Problem**: Cannot find `McpToolBox` annotation

**Solution**: Use the correct import path:
```kotlin
import io.quarkiverse.langchain4j.mcp.runtime.McpToolBox
```

**Common mistakes**:
- ❌ `io.quarkiverse.langchain4j.McpToolBox`
- ❌ `io.quarkiverse.langchain4j.mcp.McpToolBox`
- ✅ `io.quarkiverse.langchain4j.mcp.runtime.McpToolBox`

### Configuration Warnings

**Problem**: Unknown configuration keys for MCP logging

**Solution**: Use client-scoped keys:
```properties
# ❌ Wrong - global scope
quarkus.langchain4j.mcp.log-requests=true

# ✅ Correct - client-scoped
quarkus.langchain4j.mcp.airline-tools.log-requests=true
```

### MCP Server Not Visible

**Problem**: MCP Inspector can't find server

**Solution**:
1. Verify application is running
2. Check port 8080 is accessible
3. Confirm MCP server dependency is included
4. Check logs for MCP server startup messages

### Tools Not Working

**Problem**: AI doesn't use MCP tools

**Solution**:
1. Verify `@McpToolBox("airline-tools")` annotation is present
2. Check MCP client configuration in `application.properties`
3. Ensure OpenAI API key is valid
4. Check network connectivity to airline websites

## Performance Considerations

### Response Time

- First call: ~3-5 seconds (includes web scraping)
- Cached by OpenAI context: Subsequent queries may be faster
- Timeout: 30 seconds for web fetching

### Rate Limiting

- Tools fetch directly from airline websites
- No rate limiting implemented
- Consider caching for production use

### Character Limits

- Each tool returns max 5000 characters
- Prevents token overflow in LLM context
- Adjust `take(5000)` if needed

## Future Enhancements

### Potential Improvements

1. **Caching**: Add Redis/Caffeine cache for web scraping results
2. **More Airlines**: Add American, Southwest, Alaska tools
3. **Structured Data**: Parse HTML into structured JSON instead of plain text
4. **Error Handling**: More graceful error messages for failed fetches
5. **Rate Limiting**: Implement rate limiting for external requests
6. **Multi-Module**: Separate MCP server into standalone JAR (requires refactor)

### Multi-Module Architecture

For deploying MCP server separately:

```
parent/
├── mcp-server/        (AirlineMcpTools.kt)
├── mcp-client/        (AirlineLoyaltyAssistant.kt, UI)
└── pom.xml            (parent)
```

This requires significant restructuring but enables independent deployment.

## Comparison: Stage 03 vs Stage 04

| Aspect | Stage 03 (RAG) | Stage 04 (MCP) |
|--------|----------------|----------------|
| **Data Source** | Static documents | Live websites |
| **Freshness** | Requires manual updates | Always current |
| **Setup** | Document embedding required | No preprocessing |
| **Dependencies** | Embedding store | MCP server + client |
| **Performance** | Fast (in-memory) | Slower (web fetch) |
| **Complexity** | Higher (embedding pipeline) | Lower (simple tools) |
| **Use Case** | Fixed knowledge base | Dynamic data |

## Git Operations

```bash
# View changes
git status

# Commit Stage 04
git add -A
git commit -m "Stage 04: MCP Integration"

# Push to main
git push origin main

# Create and push stage branch
git checkout -b stage-04-mcp
git push -u origin stage-04-mcp
```

## References

- **Quarkus LangChain4j Docs**: https://docs.quarkiverse.io/quarkus-langchain4j/dev/
- **Quarkus MCP Server Docs**: https://docs.quarkiverse.io/quarkus-mcp-server/dev/
- **Model Context Protocol**: https://modelcontextprotocol.io/
- **MCP Inspector**: https://github.com/modelcontextprotocol/inspector
- **Docker Hub**: https://hub.docker.com/r/jbaruchs/codepocalypse-airline-assistant

## Conclusion

Stage 04 successfully demonstrates MCP integration with Quarkus LangChain4j, replacing static RAG with dynamic real-time data fetching. The application now provides current airline loyalty program information while maintaining the same simple user interface. Docker deployment makes the entire stack (MCP server + client + UI) available as a single containerized application.
