# Stage 02: Implementing Conversation Memory

## Prerequisites

- ✅ Completed Stage 01 (Basic chat functionality working)
- ✅ Quarkus application running in dev mode
- ✅ OpenAI API key configured in `.env` file
- ✅ Basic understanding of Quarkus LangChain4j extension

## Overview

In this stage, we'll add conversation memory to the Airline Loyalty Assistant, enabling it to remember user details, previous questions, and maintain context across multiple interactions.

**Key Principle:** Use Quarkus LangChain4j's **built-in memory management** rather than writing custom memory code. The framework provides automatic memory handling through configuration.

## Objectives

1. Enable conversation memory using `@MemoryId` annotation
2. Configure memory settings in `application.properties`
3. Update AI service interface to accept memory ID
4. Modify controller to pass memory ID to assistant
5. Enhance UI to show memory is active
6. Test memory functionality

## Step 1: Update AI Service Interface

### File: `src/main/kotlin/dev2next/langchain4j/AirlineLoyaltyAssistant.kt`

**Add Memory Parameter:**

```kotlin
package dev2next.langchain4j

import dev.langchain4j.service.MemoryId  // ADD THIS IMPORT
import dev.langchain4j.service.SystemMessage
import dev.langchain4j.service.UserMessage
import io.quarkiverse.langchain4j.RegisterAiService

@RegisterAiService
interface AirlineLoyaltyAssistant {

    @SystemMessage("""
        You are a helpful airline loyalty program assistant with expertise in:
        - Earning and redeeming miles/points
        - Elite status qualification and benefits
        - Partner airlines and alliances
        - Credit card rewards and bonuses
        - Award travel booking strategies
        
        Provide clear, accurate, and friendly advice. Keep responses concise but informative.
        Use examples when helpful. If you're not sure about something, say so.
        
        IMPORTANT: Remember information from our conversation:
        - User names and personal details they share
        - Their membership status and tier
        - Travel preferences and history they mention
        - Questions they've asked before
        - Any context from earlier in our conversation
        
        When users refer to previous parts of our conversation, reference that context naturally.
        Address users by name if they've introduced themselves.
    """)
    fun chat(@MemoryId memoryId: String, @UserMessage question: String): String
}
```

**What Changed:**

1. **Added Import:** `dev.langchain4j.service.MemoryId`
2. **Added Parameter:** `@MemoryId memoryId: String` before the `question` parameter
3. **Enhanced System Message:** Added explicit instructions to remember conversation details

**Why This Works:**

- The `@MemoryId` annotation tells Quarkus LangChain4j to:
  - Look up conversation memory by the provided ID
  - Inject previous messages into the LLM context
  - Store new messages after generating a response
- Framework handles all memory management automatically
- No custom `ChatMemoryProvider` code needed!

## Step 2: Configure Memory in application.properties

### File: `src/main/resources/application.properties`

**Add Memory Configuration:**

```properties
# Conversation Memory Configuration
# Use message window memory to retain recent conversation history
quarkus.langchain4j.chat-memory.type=message-window
# Keep last 20 messages in memory (10 exchanges)
quarkus.langchain4j.chat-memory.memory-window.max-messages=20
```

**Configuration Options Explained:**

### Memory Type Options:

1. **`message-window`** (Recommended for demo):
   - Keeps last N messages
   - Simple and predictable
   - No token counting needed

2. **`token-window`** (For production):
   - Keeps messages up to N tokens
   - Respects LLM context limits
   - Requires more configuration

### Max Messages:

- `20 messages` = approximately 10 conversation exchanges
- 1 exchange = 1 user message + 1 AI response = 2 messages
- Adjust based on your needs:
  - Shorter conversations: 10-20 messages
  - Longer conversations: 40-100 messages
  - Consider LLM context limits (e.g., GPT-4o-mini: 128K tokens)

### Storage:

- **Default:** `InMemoryChatMemoryStore` (automatically provided)
- **Production Options:**
  - Redis: Add `quarkus-langchain4j-redis` extension
  - Database: Implement custom `ChatMemoryStore`

**What Happens:**

When you add this configuration, Quarkus LangChain4j automatically:

1. Creates a `ChatMemoryProvider` bean
2. Instantiates `InMemoryChatMemoryStore`
3. Configures `MessageWindowChatMemory` with your settings
4. Wires everything together via CDI

## Step 3: Update Controller to Pass Memory ID

### File: `src/main/kotlin/dev2next/langchain4j/AssistantController.kt`

**Add Memory ID Constant:**

```kotlin
package dev2next.langchain4j

import io.quarkus.logging.Log
import io.quarkus.qute.Template
import io.quarkus.qute.TemplateInstance
import jakarta.inject.Inject
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType

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

    @GET
    @Produces(MediaType.TEXT_HTML)
    fun showForm(): TemplateInstance {
        return index.data("question", "")
            .data("answer", "")
            .data("error", "")
            .data("hasMemory", true)  // ADD THIS
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    fun askQuestion(@FormParam("question") question: String?): TemplateInstance {
        if (question.isNullOrBlank()) {
            Log.warn("Empty question submitted")
            return index.data("question", "")
                .data("answer", "")
                .data("error", "Please enter a question about airline loyalty programs.")
                .data("hasMemory", true)  // ADD THIS
        }

        return try {
            Log.info("Processing question with memory: ${question.take(50)}...")
            // Pass memory ID to maintain conversation context
            val answer = assistant.chat(MEMORY_ID, question)  // UPDATED: Added MEMORY_ID
            Log.info("Response generated successfully with conversation context")
            
            index.data("question", question)
                .data("answer", answer)
                .data("error", "")
                .data("hasMemory", true)  // ADD THIS
        } catch (e: Exception) {
            Log.error("Error processing question", e)
            index.data("question", question)
                .data("answer", "")
                .data("error", "Sorry, I encountered an error processing your question. Please try again.")
                .data("hasMemory", true)  // ADD THIS
        }
    }
}
```

**What Changed:**

1. **Added Companion Object:** Contains the constant `MEMORY_ID`
2. **Updated assistant.chat() Call:** Now passes `MEMORY_ID` as first parameter
3. **Added hasMemory Flag:** Set to `true` in all template data calls
4. **Updated Logging:** Mentions memory/context in log messages

**Memory ID Strategy:**

### For Demo (Current):

```kotlin
private const val MEMORY_ID = "demo-conversation"
```

- Single memory for all users
- Simple and predictable
- Good for demonstrations and testing

### For Production (Future):

```kotlin
// Option 1: Session-based memory
private fun getMemoryId(session: HttpSession): String = session.id

// Option 2: User-based memory
private fun getMemoryId(user: User): String = "user-${user.id}"

// Usage:
val answer = assistant.chat(getMemoryId(session), question)
```

## Step 4: Enhance UI with Memory Indicators

### File: `src/main/resources/templates/AssistantController/index.html`

**Add Memory Badge CSS:**

Find the `<style>` section and add this CSS before the closing `</style>` tag:

```css
.memory-badge {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    padding: 5px 15px;
    border-radius: 20px;
    font-size: 0.5em;
    font-weight: 600;
    display: inline-flex;
    align-items: center;
    gap: 5px;
}
```

**Update Header with Memory Badge:**

Find the `<h1>` tag and update it:

```html
<h1>
    ✈️ Airline Loyalty Assistant
    {#if hasMemory}<span class="memory-badge">🧠 Memory Active</span>{/if}
</h1>
```

**Update Subtitle:**

```html
<p class="subtitle">I remember our conversation! Ask me anything about airline loyalty programs, miles, and rewards.</p>
```

**Update Examples Section:**

Find the examples section and update it:

```html
<div class="examples">
    <h3>Try asking:</h3>
    <ul>
        <li>Hello, my name is Bob. How can I earn elite status?</li>
        <li>What are the benefits of elite status?</li>
        <li>How do I use miles for award flights?</li>
        <li>What's my name? (test memory!)</li>
    </ul>
    <p style="margin-top: 15px; font-style: italic; color: #666;">
        💡 Tip: I'll remember your name and preferences throughout our conversation!
    </p>
</div>
```

**Add Follow-Up Prompts (After Answer Section):**

After the answer section (the `{#if answer}...{/if}` block), add:

```html
{#if answer}
<div class="answer-section">
    <h2>💬 Answer</h2>
    <div class="answer-content">{answer}</div>
</div>

<!-- ADD THIS SECTION -->
<div class="examples">
    <h3>Continue the conversation:</h3>
    <ul>
        <li>What about [related question]?</li>
        <li>Can you tell me more about that?</li>
        <li>What's my name? (test memory!)</li>
    </ul>
</div>
{/if}
```

## Step 5: Test the Implementation

### Start Quarkus Dev Mode

If not already running:

```bash
./mvnw quarkus:dev -Dquarkus.console.enabled=false
```

**Remember:** Quarkus dev mode has **live reload**. Once started, code changes are automatically picked up. Don't restart unless necessary!

### Verify Application Started

Look for:

```
Listening on: http://localhost:8080
Profile dev activated. Live Coding activated.
```

### Test Memory with curl

**Test 1: Introduce yourself**

```bash
curl -X POST http://localhost:8080 \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode 'question=Hello, my name is Bob. How can I earn elite status?'
```

Expected: Response addresses you as "Bob" and provides information about earning elite status.

**Test 2: Check if name is remembered**

```bash
curl -X POST http://localhost:8080 \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode 'question=What is my name?'
```

Expected: Response says "Your name is Bob" (or similar).

**Test 3: Check topic memory**

```bash
curl -X POST http://localhost:8080 \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode 'question=What was I asking about before?'
```

Expected: Response references the previous question about elite status.

### Test in Browser

1. Open: http://localhost:8080
2. Look for "🧠 Memory Active" badge in header
3. Enter: "Hello, my name is Alice. How do I redeem miles?"
4. Submit and read response (should address you as Alice)
5. Enter: "What's my name?"
6. Submit and verify it remembers "Alice"
7. Ask follow-up questions about miles redemption
8. Verify it references previous conversation

### Verify Memory Behavior

✅ **What Should Work:**

- Assistant remembers user names
- Assistant recalls previous questions
- Assistant maintains conversation context
- Visual badge shows memory is active
- Follow-up questions make sense

❌ **What Won't Work (By Design):**

- Memory across application restarts (in-memory storage)
- Separate memory per user (single memory ID)
- Memory older than 20 messages (message window limit)

## Troubleshooting

### Memory Not Working - Check These:

**1. Check Configuration:**

```bash
# Verify memory settings are present
cat src/main/resources/application.properties | grep memory
```

Should show:

```
quarkus.langchain4j.chat-memory.type=message-window
quarkus.langchain4j.chat-memory.memory-window.max-messages=20
```

**2. Check @MemoryId Parameter:**

Verify the AI service has `@MemoryId` parameter:

```kotlin
fun chat(@MemoryId memoryId: String, @UserMessage question: String): String
```

**3. Check Memory ID is Passed:**

Verify controller passes memory ID:

```kotlin
val answer = assistant.chat(MEMORY_ID, question)  // NOT: assistant.chat(question)
```

**4. Check Build Logs:**

Look for memory-related DEBUG logs during startup:

```
DEBUG [io.qua.lan.dep.AiServicesProcessor] Annotation dev.langchain4j.service.MemoryId matches...
```

**5. Test with Simple Conversation:**

```bash
# First request
curl -X POST http://localhost:8080 -d 'question=My name is Test'

# Second request (should remember)
curl -X POST http://localhost:8080 -d 'question=What is my name?'
```

### Common Issues:

**Issue:** Assistant doesn't remember anything

**Cause:** Missing `@MemoryId` parameter or not passing memory ID

**Solution:** Double-check Step 1 and Step 3 above

---

**Issue:** Application won't start after changes

**Cause:** Syntax error in Kotlin code

**Solution:** Check terminal for compilation errors, fix syntax

---

**Issue:** Memory works but forgets too quickly

**Cause:** `max-messages` too low for conversation length

**Solution:** Increase in `application.properties`:

```properties
quarkus.langchain4j.chat-memory.memory-window.max-messages=40
```

---

**Issue:** "Template not found" error

**Cause:** Quarkus can't find the Qute template

**Solution:** Ensure `@Location("AssistantController/index.html")` annotation is present

## Architecture Notes

### How Memory Works in Quarkus LangChain4j

1. **Request Arrives:** Controller receives question
2. **Memory Lookup:** Framework looks up memory by ID
3. **Context Building:** Previous messages added to LLM context
4. **LLM Call:** Question + history sent to OpenAI
5. **Response Generated:** LLM responds with context awareness
6. **Memory Update:** New user message and AI response stored
7. **Response Returned:** Answer sent back to user

### Memory Flow Diagram:

```
User Question
     ↓
Controller (passes MEMORY_ID)
     ↓
AI Service Interface (@MemoryId)
     ↓
Quarkus LangChain4j Framework
     ↓
ChatMemoryProvider.get(memoryId)
     ↓
ChatMemory (InMemoryChatMemoryStore)
     ↓
Retrieve: [Previous messages]
     ↓
Build Context: System + History + New Question
     ↓
OpenAI API Call
     ↓
Response Generated
     ↓
Store: User Message + AI Response
     ↓
Return Answer to Controller
```

### Memory Storage Structure:

```
InMemoryChatMemoryStore
├── "demo-conversation"
│   ├── UserMessage("Hello, my name is Bob...")
│   ├── AiMessage("Hello Bob! Earning elite status...")
│   ├── UserMessage("What is my name?")
│   ├── AiMessage("Your name is Bob...")
│   └── ... (up to 20 messages)
```

## Production Considerations

### For Real Applications, Consider:

**1. Per-User Memory:**

```kotlin
// Use session ID or user ID
val memoryId = "user-${session.id}"
val answer = assistant.chat(memoryId, question)
```

**2. Persistent Storage:**

```xml
<!-- Add Redis for persistence -->
<dependency>
    <groupId>io.quarkiverse.langchain4j</groupId>
    <artifactId>quarkus-langchain4j-redis</artifactId>
</dependency>
```

```properties
# Configure Redis memory store
quarkus.langchain4j.chat-memory.store=redis
quarkus.redis.hosts=redis://localhost:6379
```

**3. Memory Management:**

- Add "Clear Memory" button in UI
- Implement memory expiration (TTL)
- Add privacy controls
- Provide data export functionality

**4. Token Window for Better Control:**

```properties
quarkus.langchain4j.chat-memory.type=token-window
quarkus.langchain4j.chat-memory.token-window.max-tokens=2000
```

## Key Learnings

### ✅ Do This:

1. **Use Built-in Memory:** Leverage framework defaults
2. **Configure, Don't Code:** Use `application.properties`
3. **Enhance System Message:** Explicitly instruct AI to remember
4. **Test Incrementally:** Verify each change before moving on
5. **Document Memory Behavior:** Explain limitations to users

### ❌ Avoid This:

1. **Custom ChatMemoryProvider:** Not needed for basic use cases
2. **Complex Memory Logic:** Framework handles it better
3. **Forgetting @MemoryId:** Required for memory to work
4. **Restarting Server:** Use live reload instead
5. **Production Memory in Demo:** Keep it simple for demos

## Verification Checklist

Before moving to Stage 03, verify:

- [ ] Memory configuration in `application.properties`
- [ ] `@MemoryId` parameter in AI service interface
- [ ] Enhanced system message with memory instructions
- [ ] Memory ID constant in controller
- [ ] All `assistant.chat()` calls pass memory ID
- [ ] `hasMemory` flag added to all template data
- [ ] Memory badge visible in UI
- [ ] Name memory test passes (introduce name, ask for it back)
- [ ] Topic memory test passes (ask about previous question)
- [ ] Application runs without errors

## Success Criteria

✅ **Stage 02 Complete When:**

- Assistant remembers user names across requests
- Assistant recalls previous conversation topics
- Visual indicators show memory is active
- Tests pass (name memory, topic memory)
- Code is clean and well-commented
- Documentation is complete

## Next Steps

After completing Stage 02:

1. **Create Stage 02 Summary:** Document what was accomplished
2. **Commit Changes:** Save Stage 02 to version control
3. **Update Branch:** Push to `stage-02-conversation-memory` branch
4. **Consider Stage 03 Options:**
   - Function Calling/Tools (e.g., lookup loyalty account data)
   - RAG (Retrieval Augmented Generation with policy documents)
   - Multi-modal (image understanding for boarding passes)

## Additional Resources

- [Quarkus LangChain4j Documentation](https://docs.quarkiverse.io/quarkus-langchain4j/dev/index.html)
- [Quarkus Dev Mode Guide](https://quarkus.io/guides/dev-mode-differences)
- [LangChain4j Memory Documentation](https://github.com/langchain4j/langchain4j)

---

**Estimated Time:** 30-45 minutes (excluding testing)
**Difficulty:** Moderate
**Prerequisites:** Stage 01 complete, basic Quarkus knowledge
