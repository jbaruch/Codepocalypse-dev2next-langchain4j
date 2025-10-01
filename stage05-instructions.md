# Stage 05 Instructions: Local Model + LLM-Based Guardrails

## Objective
Migrate from OpenAI cloud to a local Docker Desktop Model Runner and implement LLM-based input guardrails to restrict questions to airline loyalty topics only.

## Prerequisites

### Required Software
- Docker Desktop installed and running
- Docker Desktop Model Runner enabled
- Model `ai/mistral:7B-Q4_K_M` pulled and ready

### Verify Prerequisites
```bash
# Check Docker Desktop is running
docker ps

# Verify Model Runner is accessible (should return OpenAI-compatible API)
curl http://localhost:12434/engines/llama.cpp/v1/models

# Verify Mistral model is available
# Should show ai/mistral:7B-Q4_K_M in the list
```

## Step 1: Configure Local Model Connection

### Update application.properties

**File**: `src/main/resources/application.properties`

Comment out cloud OpenAI configuration and add local model configuration:

```properties
# ============================================
# CLOUD OPENAI (DISABLED - Using Local Model)
# ============================================
#quarkus.langchain4j.openai.api-key=${OPENAI_API_KEY}
#quarkus.langchain4j.openai.chat-model.model-name=gpt-4o-mini
#quarkus.langchain4j.openai.timeout=60s

# ============================================
# LOCAL MODEL (Docker Desktop Model Runner)
# ============================================
# OpenAI-compatible API at http://localhost:12434/engines/llama.cpp/v1
quarkus.langchain4j.openai.base-url=http://localhost:12434/engines/llama.cpp/v1
quarkus.langchain4j.openai.api-key=not-needed
quarkus.langchain4j.openai.chat-model.model-name=ai/mistral:7B-Q4_K_M
quarkus.langchain4j.openai.timeout=60s
```

**Key Points**:
- Port **12434** (not 11434 - that's Ollama)
- Base URL includes `/engines/llama.cpp/v1` path
- API key is "not-needed" for local runner
- Model name format: `ai/mistral:7B-Q4_K_M`

## Step 2: Add Guardrail Dependencies

### Update pom.xml

**File**: `pom.xml`

Add vanilla LangChain4j core dependency:

```xml
<!-- Vanilla LangChain4j Core for Guardrails -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-core</artifactId>
</dependency>
```

**Remove** if present:
```xml
<!-- NOT NEEDED - Remove if present -->
<dependency>
    <groupId>io.quarkiverse.langchain4j</groupId>
    <artifactId>quarkus-langchain4j-ollama</artifactId>
</dependency>
```

## Step 3: Implement LLM-Based Input Guardrail

### Create Guardrail Class

**File**: `src/main/kotlin/dev2next/langchain4j/AirlineLoyaltyInputGuardrail.kt`

```kotlin
package dev2next.langchain4j

import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.guardrail.InputGuardrail
import dev.langchain4j.guardrail.InputGuardrailResult
import dev.langchain4j.model.chat.ChatModel
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

/**
 * Input guardrail that validates user questions are related to airline loyalty programs.
 * 
 * This is a VANILLA LangChain4j InputGuardrail implementation (not Quarkus-specific) as per project instructions.
 * Uses the Quarkus-managed ChatModel to invoke the LLM for intelligent validation.
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
- Miles and points earning/redeeming
- Upgrades and benefits
- Airline-specific programs (Delta SkyMiles, United MileagePlus, American AAdvantage, etc.)
- Award flights and redemptions
- Status challenges and promotions
- Lounge access and perks
- Partner airlines and alliances
- Credit card partnerships and benefits

If the question is about ANY of these topics, respond with exactly "YES".
If the question is about ANYTHING else (weather, cooking, general knowledge, etc.), respond with exactly "NO".

Be strict - only airline loyalty program questions should get "YES"."""

        private const val REJECTION_MESSAGE = """I'm sorry, but I can only answer questions about airline loyalty programs.

I specialize in topics like:
- Elite status levels and benefits
- Earning and redeeming miles/points
- Airline loyalty programs (Delta SkyMiles, United MileagePlus, etc.)
- Upgrades, award flights, and redemptions
- Lounge access and travel perks

Please ask me a question about airline loyalty programs!"""
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
```

**Key Implementation Details**:
- Uses **vanilla LangChain4j** `InputGuardrail` interface (not Quarkus-specific)
- Injects **Quarkus-managed** `ChatModel` (correct: `dev.langchain4j.model.chat.ChatModel`)
- Uses `chatModel.chat(String)` method (not `generate()`)
- Uses helper methods: `success()` and `fatal(String)` from InputGuardrail
- **Fail-open**: On errors, allows question through (avoids blocking users)
- Extensive logging with emojis for visibility

## Step 4: Integrate Guardrail in Controller

### Update AssistantController

**File**: `src/main/kotlin/dev2next/langchain4j/AssistantController.kt`

Add guardrail injection and validation:

```kotlin
@Inject
lateinit var guardrail: AirlineLoyaltyInputGuardrail

@POST
fun askQuestion(@FormParam("query") question: String?): TemplateInstance {
    // Existing validation...
    
    // Guardrail validation - Convert to vanilla LangChain4j UserMessage
    val userMessage = dev.langchain4j.data.message.UserMessage.from(question)
    val guardrailResult = guardrail.validate(userMessage)
    val result = guardrailResult.result()
    
    if (result != dev.langchain4j.guardrail.GuardrailResult.Result.SUCCESS) {
        Log.warnf("🚫 Guardrail blocked question: %s", question.take(50))
        val errorMessage = guardrailResult.errorMessage() ?: "Question not allowed"
        return error(errorMessage)
    }
    
    Log.infof("✅ Guardrail passed, proceeding with question: %s", question.take(50))
    
    // Proceed to call assistant...
}
```

**Integration Notes**:
- Manual validation in controller (not annotation-based like `@InputGuardrails`)
- Convert to vanilla `UserMessage` for guardrail
- Check result: `GuardrailResult.Result.SUCCESS`
- Return error template if guardrail fails
- Log guardrail decision for debugging

## Step 5: Update UI

### Update Template

**File**: `src/main/resources/templates/AssistantController/index.html`

Add badges and update messaging:

```html
<!-- In the header badges section -->
<span class="badge">🏠 Local Model</span>
<span class="badge">🛡️ Guardrails</span>

<!-- Update subtitle -->
<p class="subtitle">
  Running on Docker Model Runner (Mistral 7B) with guardrails! 
  I fetch real-time airline data via MCP, remember our conversation, 
  and only answer airline loyalty questions.
</p>

<!-- Update footer -->
<p class="footer">
  Powered by Quarkus + Kotlin + LangChain4j + Docker Model Runner (Mistral 7B) + MCP + Guardrails
</p>

<!-- Update tips -->
<p class="tip">💡 <strong>Guardrails Active:</strong> I only answer questions about airline loyalty programs...</p>
```

## Step 6: Build and Test

### Compile the Application

```bash
./mvnw clean compile
```

**Expected**: BUILD SUCCESS

### Start Development Server

```bash
# Option 1: Direct (may have console issues in VS Code terminal)
./mvnw quarkus:dev

# Option 2: Background with disabled console (recommended for VS Code)
./mvnw quarkus:dev -Dquarkus.console.enabled=false

# Option 3: Persistent background (survives terminal close)
nohup ./mvnw quarkus:dev -Dquarkus.console.enabled=false > /tmp/quarkus.log 2>&1 &
```

**Expected**: Server starts on http://localhost:8080

### Test Guardrails

#### Test 1: Airline Loyalty Question (Should PASS)

```bash
curl -X POST http://localhost:8080/ \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "query=What are the benefits of United MileagePlus Premier Gold status?"
```

**Expected**: 
- Guardrail passes (✅ in logs)
- Assistant provides answer about Premier Gold benefits

#### Test 2: Non-Airline Question (Should REJECT)

```bash
curl -X POST http://localhost:8080/ \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "query=What is the weather in New York today?"
```

**Expected**:
- Guardrail blocks (❌ in logs)
- Error message explaining only airline questions are allowed

#### Test 3: Edge Case (Empty Question)

```bash
curl -X POST http://localhost:8080/ \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "query="
```

**Expected**: 
- Validation error before guardrail (empty question check)

### Verify in Browser

1. Open http://localhost:8080
2. Check for badges: "🏠 Local Model" and "🛡️ Guardrails"
3. Try airline question: "How do I earn Delta SkyMiles?"
4. Verify it answers normally
5. Try non-airline question: "How do I bake a cake?"
6. Verify it rejects with helpful message

## Step 7: Check Logs

View guardrail activity:

```bash
# If using nohup
tail -f /tmp/quarkus.log | grep -E "(🛡️|🤖|✅|❌)"

# If running in terminal, logs appear directly
```

**Expected Log Output**:
```
🛡️ Guardrail validating with LLM: What are the benefits of United MileagePlus...
🤖 LLM validation response: YES
✅ Guardrail PASSED - LLM confirmed question is airline loyalty related
```

## Common Issues & Solutions

### Issue 1: Cannot Connect to localhost:12434

**Symptom**: Connection refused or timeout

**Solution**:
1. Verify Docker Desktop is running
2. Check Model Runner is enabled in Docker Desktop settings
3. Verify model is pulled: Docker Desktop → AI → Models
4. Test endpoint: `curl http://localhost:12434/engines/llama.cpp/v1/models`

### Issue 2: Compilation Error - Unresolved reference 'ChatModel'

**Symptom**: `Unresolved reference: ChatModel`

**Solution**:
- Use `dev.langchain4j.model.chat.ChatModel` (correct interface)
- NOT `dev.langchain4j.model.chat.ChatLanguageModel` (different interface)
- Ensure `langchain4j-core` dependency is added

### Issue 3: Compilation Error - Unresolved reference 'chat'

**Symptom**: `Unresolved reference: chat`

**Solution**:
- Use `chatModel.chat(String)` (Quarkus ChatModel interface)
- NOT `chatModel.generate(String)` (different method)

### Issue 4: Guardrail Always Allows Questions

**Symptom**: Non-airline questions not being blocked

**Solution**:
1. Check logs for "🛡️ Guardrail validating" messages
2. Verify guardrail is injected in controller
3. Check validation logic is before assistant call
4. Ensure error result returns error template

### Issue 5: Quarkus Dev Mode NPE in VS Code

**Symptom**: `NullPointerException: Cannot invoke "RuntimeUpdatesProcessor.doScan"`

**Solution**: Use `-Dquarkus.console.enabled=false` flag when starting dev mode

## Validation Checklist

Before moving to next stage, verify:

- [ ] Docker Model Runner accessible on port 12434
- [ ] Application compiles without errors
- [ ] Server starts successfully
- [ ] UI shows "Local Model" and "Guardrails" badges
- [ ] Airline questions answered normally
- [ ] Non-airline questions rejected with message
- [ ] Logs show guardrail validation (🛡️ emoji)
- [ ] MCP integration still working
- [ ] Memory still working (conversation context)

## Architecture Notes

### Why Vanilla LangChain4j Guardrails?

Per project instructions: **"This is the first (and probably the last) time you're clearly instructed by the documentation to favor langchain4j vanilla implementation over quarkus."**

- Uses vanilla `InputGuardrail` interface
- But injects Quarkus-managed `ChatModel` for LLM access
- Hybrid approach: vanilla API with Quarkus DI

### Why Manual Controller Integration?

Annotation-based guardrails (`@InputGuardrails`) have compatibility issues:
- Requires Quarkus-specific guardrail interfaces
- Less flexible for custom error handling
- Manual integration gives full control

### Why LLM-Based Validation?

Original requirement: **"use LLM for determining weather the question is related or not"**

Benefits over keyword matching:
- Understands paraphrasing and context
- Handles variations (e.g., "status" vs "tier")
- More intelligent classification
- Adapts to edge cases

Trade-offs:
- Adds ~1-2 seconds per request
- Uses model resources for validation
- But: More accurate than keywords

## Documentation References

- **Vanilla LangChain4j Guardrails**: https://docs.langchain4j.dev/tutorials/guardrails/
- **Quarkus LangChain4j**: https://docs.quarkiverse.io/quarkus-langchain4j/dev/
- **Docker Model Runner API**: https://docs.docker.com/ai/model-runner/api-reference/
- **Project Instructions**: `.github/copilot-instructions.md`

## Next Steps

After completing Stage 05:
1. Commit changes with descriptive message
2. Create branch `stage-05-local-guardrails`
3. Push both main and branch to GitHub
4. Document in INSTRUCTION-FILES-SUMMARY.md
5. Consider Stage 06: RAG, streaming, or production deployment

## Summary

Stage 05 adds:
- ✅ Local model (Docker Desktop Model Runner - Mistral 7B)
- ✅ LLM-based input guardrails (airline loyalty topics only)
- ✅ Vanilla LangChain4j guardrail implementation
- ✅ UI updates (badges, messaging)
- ✅ All previous features still working (MCP, memory, templates)

**Result**: Fully local, cost-free, privacy-respecting airline loyalty assistant with intelligent guardrails!
