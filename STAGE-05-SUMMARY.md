# Stage 05 Summary: Local Model + LLM-Based Guardrails

## Overview
Stage 05 successfully migrated from OpenAI cloud to a local Docker Desktop Model Runner (Mistral 7B) and implemented LLM-based input guardrails to restrict questions to airline loyalty topics only.

## Key Accomplishments

### 1. Local Model Integration (Docker Desktop Model Runner)
- **Infrastructure**: Docker Desktop Model Runner (not Ollama)
- **Model**: `ai/mistral:7B-Q4_K_M` (quantized Mistral 7B)
- **Port**: 12434 (OpenAI-compatible API at `/engines/llama.cpp/v1`)
- **Connection**: Reused `quarkus-langchain4j-openai` extension (OpenAI-compatible API)
- **Configuration**: Updated `application.properties` to point to local model

### 2. LLM-Based Input Guardrails
- **Implementation**: Vanilla LangChain4j `InputGuardrail` interface
- **Validation Method**: LLM-based (not keyword matching)
- **Integration**: Declarative via `@InputGuardrails` annotation on AI Service interface
- **Exception Handling**: Catches `InputGuardrailException` with formatted error messages
- **Behavior**: 
  - Airline loyalty questions → Pass guardrail → Answer with assistant
  - Non-airline questions → Blocked with helpful message (HTML-formatted with line breaks)
  - Validation errors → Fail-open (allow question through)

### 3. UI Updates
- Added badges: "🏠 Local Model" and "🛡️ Guardrails"
- Updated subtitle: "Running on Docker Model Runner (Mistral 7B) with guardrails!"
- Updated footer to reflect all technologies
- Added tip about guardrails in the interface

## Technical Implementation

### Docker Model Runner Configuration
```properties
# Local Docker Desktop Model Runner (OpenAI-compatible API)
quarkus.langchain4j.openai.base-url=http://localhost:12434/engines/llama.cpp/v1
quarkus.langchain4j.openai.api-key=not-needed
quarkus.langchain4j.openai.chat-model.model-name=ai/mistral:7B-Q4_K_M
quarkus.langchain4j.openai.timeout=60s
```

### Guardrail Architecture
```kotlin
// Guardrail implementation with Quarkus CDI injection
@ApplicationScoped
class AirlineLoyaltyInputGuardrail @Inject constructor(
    private val chatModel: ChatModel // Quarkus-managed ChatModel
) : InputGuardrail {
    
    override fun validate(userMessage: UserMessage): InputGuardrailResult {
        val question = userMessage.singleText()
        val validationPrompt = "$VALIDATION_SYSTEM_PROMPT\n\nUser question: $question"
        val answer = chatModel.chat(validationPrompt).trim().uppercase()
        
        if (answer.startsWith("YES")) {
            success() // Interface default method
        } else {
            fatal(REJECTION_MESSAGE) // Interface default method
        }
    }
}

// AI Service with declarative guardrails
@RegisterAiService
@ApplicationScoped
@InputGuardrails(AirlineLoyaltyInputGuardrail::class) // Annotation-based!
interface AirlineLoyaltyAssistant {
    fun chat(@MemoryId memoryId: String, @UserMessage message: String): String
}
```

### Controller Exception Handling
```kotlin
try {
    val answer = assistant.chat(MEMORY_ID, question)
    // Display answer...
} catch (e: InputGuardrailException) {
    // Extract friendly message from: "The guardrail <class> failed with this message: <msg>"
    val friendlyMessage = e.message?.let { msg ->
        val prefix = "failed with this message: "
        val index = msg.indexOf(prefix)
        if (index != -1) msg.substring(index + prefix.length) else msg
    } ?: "Your question is not related to airline loyalty programs."
    
    // Convert newlines to HTML <br> tags for proper display
    val htmlFormattedMessage = friendlyMessage.replace("\n", "<br>")
    // Display error with {error.raw} in Qute template
}
```

## Critical Decisions

### 1. Vanilla LangChain4j Guardrails (Quarkus Implementation Deprecated)
**Rationale**: Quarkus LangChain4j guardrails are deprecated. Quarkus instructs users to use vanilla LangChain4j guardrails instead.

- Uses vanilla `dev.langchain4j.guardrail.InputGuardrail` interface
- Uses vanilla `dev.langchain4j.service.guardrail.@InputGuardrails` annotation
- Uses vanilla `dev.langchain4j.guardrail.InputGuardrailException` for error handling
- Injects Quarkus-managed `ChatModel` via CDI for LLM invocation
- Uses interface default methods: `success()` and `fatal(String)`
- Declarative integration via `@InputGuardrails` annotation on AI Service interface

### 2. LLM-Based Validation (Not Keyword Matching)
**Rationale**: Original requirement was to "use LLM for determining weather the question is related or not."

- System prompt defines airline loyalty topics clearly
- LLM responds with YES/NO for each question
- More intelligent than keyword matching (handles paraphrasing, context)
- Fail-open on errors to avoid blocking legitimate users

### 3. Docker Model Runner (Not Ollama)
**Rationale**: User specified "Local docker model runner via docker desktop" with explicit correction that it's "not ollama, native docker image runner."

- Port 12434 (Ollama uses 11434)
- OpenAI-compatible API at `/engines/llama.cpp/v1`
- No need to install/configure Ollama separately
- Integrated into Docker Desktop

## Dependencies Added
```xml
<!-- Vanilla LangChain4j Core for Guardrails -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-core</artifactId>
</dependency>
```

## Dependencies Removed
```xml
<!-- Removed: quarkus-langchain4j-ollama (not needed) -->
```

## Files Modified

### New Files
- `src/main/kotlin/dev2next/langchain4j/AirlineLoyaltyInputGuardrail.kt` - LLM-based input guardrail

### Modified Files
- `src/main/resources/application.properties` - Local model configuration
- `src/main/kotlin/dev2next/langchain4j/AssistantController.kt` - Guardrail integration
- `src/main/resources/templates/AssistantController/index.html` - UI updates
- `pom.xml` - Dependency changes

### Unchanged Files
- `AirlineLoyaltyAssistant.kt` - Still uses MCP tools, memory, system message
- `AirlineMcpTools.kt` - MCP integration still working

## Performance Considerations

### Two LLM Calls Per Request
1. **Guardrail Validation** (fast, simple YES/NO)
2. **Assistant Response** (main query)

**Impact**: Minimal - guardrail validation is quick (single YES/NO response)

**Optimization Opportunities**:
- Cache guardrail results for similar questions
- Use streaming for assistant response only
- Consider async guardrail validation

### Local Model Benefits
- **No API costs**: Free to run as many queries as needed
- **Privacy**: All data stays local (no cloud transmission)
- **Speed**: Low latency (no network round-trip)
- **Offline**: Works without internet connection

### Local Model Limitations
- **Quality**: Mistral 7B smaller than GPT-4 (less knowledgeable)
- **Resources**: Requires Docker Desktop and RAM for model
- **Context**: Smaller context window than cloud models

## Testing

### Airline Loyalty Questions (Should Pass)
✅ "What are the benefits of United MileagePlus Premier Gold status?"
✅ "How do I earn Delta SkyMiles?"
✅ "What's the difference between Platinum and Diamond status?"
✅ "Can I transfer miles between programs?"

### Non-Airline Questions (Should Reject)
❌ "What's the weather in New York?"
❌ "How do I bake a cake?"
❌ "What is Python programming?"
❌ "Tell me about quantum physics"

### Edge Cases
- Empty questions → Validation error (handled)
- Very long questions → Processed normally
- Ambiguous questions → LLM decides
- Guardrail errors → Fail-open (allow through)

## Lessons Learned

### 1. Quarkus vs Vanilla LangChain4j APIs
- **ChatModel vs ChatLanguageModel**: Different interfaces
- **Method**: `chatModel.chat(String)` not `generate(String)`
- **Injection**: Use Quarkus-managed `ChatModel` for proper CDI integration

### 2. Guardrail API Documentation
- Official docs at https://docs.langchain4j.dev/tutorials/guardrails/
- Helper methods: `success()`, `fatal(String)`, `failure(String)`, `reprompt(String, String)`
- Result check: `guardrailResult.result() == GuardrailResult.Result.SUCCESS`

### 3. Docker Model Runner Discovery
- Documentation: https://docs.docker.com/ai/model-runner/
- OpenAI-compatible API (reuse existing extension)
- Different port from Ollama (12434 vs 11434)

## Known Issues & Limitations

### 1. Quarkus Dev Mode NPE in VS Code Terminal
**Workaround**: Use `-Dquarkus.console.enabled=false` flag or run in external terminal.

### 2. Guardrail Validation Latency
**Impact**: Adds ~1-2 seconds per request for guardrail LLM call.
**Mitigation**: Acceptable for demo, could optimize with caching in production.

### 3. No Circular Dependency
**Verification**: Both guardrail and assistant use same `ChatModel` bean without conflicts.
**Reason**: Quarkus CDI properly manages singleton injection.

## Future Enhancements

### Potential Improvements
1. **Output Guardrails**: Validate assistant responses for safety/quality
2. **Guardrail Caching**: Cache YES/NO results for similar questions
3. **Multiple Guardrails**: Chain guardrails (profanity filter, etc.)
4. **Streaming**: Stream assistant responses (guardrails already validated)
5. **Metrics**: Track guardrail pass/fail rates
6. **Fine-tuning**: Fine-tune local model on airline loyalty data

### Next Stages (Suggested)
- **Stage 06**: RAG (Retrieval Augmented Generation) with document store
- **Stage 07**: Streaming responses with Server-Sent Events
- **Stage 08**: Production deployment (containerization, monitoring)

## Key Takeaways

### ✅ What Worked Well
- Docker Model Runner seamless integration (OpenAI-compatible)
- LLM-based guardrails more intelligent than keyword matching
- Vanilla LangChain4j API clean and well-documented
- Fail-open error handling prevents blocking users
- UI badges clearly communicate new features

### 🔧 What Was Challenging
- API confusion: ChatModel vs ChatLanguageModel
- Method naming: `chat()` vs `generate()`
- Dependency resolution: `langchain4j-core` vs `langchain4j-open-ai`
- Documentation: Needed to consult both Quarkus and vanilla docs

### 📚 Resources Used
- Quarkus LangChain4j docs: https://docs.quarkiverse.io/quarkus-langchain4j/dev/
- Vanilla LangChain4j guardrails: https://docs.langchain4j.dev/tutorials/guardrails/
- Docker Model Runner API: https://docs.docker.com/ai/model-runner/api-reference/
- Context7 MCP for documentation lookup

## Conclusion

Stage 05 successfully achieved both goals:
1. ✅ **Local Model**: Running Mistral 7B via Docker Desktop Model Runner
2. ✅ **Guardrails**: LLM-based input validation restricts questions to airline loyalty topics

The application now runs completely locally (no cloud API costs), uses intelligent LLM-based guardrails (not simple keyword matching), and maintains all previous features (MCP, memory, Qute templates).

**Status**: Stage 05 COMPLETE ✅
**Next**: Ready for Stage 06 (RAG, streaming, or production deployment)
