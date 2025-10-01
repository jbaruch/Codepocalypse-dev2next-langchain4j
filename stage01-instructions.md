# Stage 01: Basic Airline Loyalty Assistant Instructions

## Objective

Implement a working AI-powered airline loyalty assistant chatbot with a simple web UI, using Quarkus LangChain4j CDI patterns.

## Prerequisites

- Stage 00 completed (project setup with Quarkus, Kotlin, LangChain4j)
- `.env` file with valid `OPENAI_API_KEY`
- Understanding of Quarkus CDI and Qute templating

## Architecture Overview

**Keep It Simple:**

- 1 AI Service interface (`AirlineLoyaltyAssistant.kt`)
- 1 REST Controller (`AssistantController.kt`)
- 1 Qute template (`index.html`)
- Server-side rendering (NO separate frontend/API)
- Minimal dependencies

## Step 1: Create AI Service Interface

Create `src/main/kotlin/dev2next/langchain4j/AirlineLoyaltyAssistant.kt`:

```kotlin
package dev2next.langchain4j

import io.quarkiverse.langchain4j.RegisterAiService
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
@RegisterAiService
interface AirlineLoyaltyAssistant {
    
    @io.quarkiverse.langchain4j.SystemMessage("""
        You are a helpful airline loyalty program assistant. 
        You help users understand their benefits, status tiers, and how to earn/redeem miles.
        Be friendly, concise, and accurate in your responses.
        If you don't know something specific about the airline's program, say so honestly.
    """)
    fun chat(question: String): String
}
```

**Key Points:**

- Use `@RegisterAiService` annotation (Quarkus LangChain4j pattern)
- Use `@ApplicationScoped` for CDI
- Import from `io.quarkiverse.langchain4j.*` (NOT `dev.langchain4j.*`)
- `@SystemMessage` defines AI behavior and context
- Simple `chat(question: String): String` method signature
- No manual model instantiation - Quarkus handles via config

## Step 2: Create REST Controller

Create `src/main/kotlin/dev2next/langchain4j/AssistantController.kt`:

```kotlin
package dev2next.langchain4j

import io.quarkus.qute.Template
import io.quarkus.qute.Location
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.FormParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.jboss.logging.Logger

@Path("/")
class AssistantController {

    @Inject
    @Location("AssistantController/index.html")
    lateinit var index: Template

    @Inject
    lateinit var assistant: AirlineLoyaltyAssistant

    private val logger = Logger.getLogger(AssistantController::class.java)

    @GET
    @Produces(MediaType.TEXT_HTML)
    fun showForm(): String {
        logger.info("Serving main form")
        return index.data("answer", null).render()
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    fun askQuestion(@FormParam("question") question: String?): String {
        logger.info("Received question: $question")
        
        if (question.isNullOrBlank()) {
            logger.warn("Empty question received")
            return index.data("error", "Please enter a question").render()
        }

        return try {
            val answer = assistant.chat(question)
            logger.info("Response generated successfully")
            index.data("question", question, "answer", answer).render()
        } catch (e: Exception) {
            logger.error("Error processing question", e)
            index.data("error", "Failed to get response: ${e.message}").render()
        }
    }
}
```

**Critical Points:**

- **MUST use `@Location` annotation** - Explicitly specify template path
  - Without this, Qute cannot resolve template location
  - This was the bug discovered during testing
- Inject `AirlineLoyaltyAssistant` via `@Inject` (Quarkus CDI)
- Inject Qute `Template` with `@Location("AssistantController/index.html")`
- Form validation for empty questions
- Error handling with try-catch
- Logging for debugging
- Return HTML directly (server-side rendering)

## Step 3: Create Qute Template

Create `src/main/resources/templates/AssistantController/index.html`:

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Airline Loyalty Assistant</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
            min-height: 100vh;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            display: flex;
            justify-content: center;
            align-items: center;
            padding: 20px;
        }
        
        .container {
            background: white;
            border-radius: 20px;
            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
            padding: 40px;
            max-width: 800px;
            width: 100%;
        }
        
        h1 {
            color: #333;
            margin-bottom: 10px;
            font-size: 2rem;
        }
        
        .subtitle {
            color: #666;
            margin-bottom: 30px;
            font-size: 1rem;
        }
        
        .form-group {
            margin-bottom: 20px;
        }
        
        label {
            display: block;
            margin-bottom: 8px;
            color: #333;
            font-weight: 500;
        }
        
        textarea {
            width: 100%;
            padding: 12px;
            border: 2px solid #e0e0e0;
            border-radius: 8px;
            font-size: 1rem;
            font-family: inherit;
            resize: vertical;
            transition: border-color 0.3s;
        }
        
        textarea:focus {
            outline: none;
            border-color: #667eea;
        }
        
        button {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            border: none;
            padding: 12px 30px;
            border-radius: 8px;
            font-size: 1rem;
            font-weight: 600;
            cursor: pointer;
            transition: transform 0.2s, box-shadow 0.2s;
        }
        
        button:hover {
            transform: translateY(-2px);
            box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
        }
        
        button:active {
            transform: translateY(0);
        }
        
        .examples {
            margin: 20px 0;
            padding: 15px;
            background: #f5f5f5;
            border-radius: 8px;
        }
        
        .examples h3 {
            font-size: 0.9rem;
            color: #666;
            margin-bottom: 10px;
        }
        
        .examples ul {
            list-style: none;
            padding-left: 0;
        }
        
        .examples li {
            color: #667eea;
            margin: 5px 0;
            cursor: pointer;
            font-size: 0.9rem;
        }
        
        .examples li:hover {
            text-decoration: underline;
        }
        
        .answer-section {
            margin-top: 30px;
            padding: 20px;
            background: #f8f9fa;
            border-radius: 8px;
            border-left: 4px solid #667eea;
        }
        
        .answer-section h2 {
            color: #333;
            margin-bottom: 15px;
            font-size: 1.3rem;
        }
        
        .answer-content {
            color: #555;
            line-height: 1.6;
            white-space: pre-wrap;
        }
        
        .error {
            background: #fee;
            border-left-color: #e74c3c;
            color: #c0392b;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>✈️ Airline Loyalty Assistant</h1>
        <p class="subtitle">Ask me anything about your airline loyalty program benefits!</p>
        
        <form method="post" action="/">
            <div class="form-group">
                <label for="question">Your Question:</label>
                <textarea 
                    id="question" 
                    name="question" 
                    rows="4" 
                    placeholder="e.g., What are the benefits of Gold status?"
                    required
                >{question ?: ''}</textarea>
            </div>
            
            <div class="examples">
                <h3>💡 Example Questions:</h3>
                <ul>
                    <li onclick="document.getElementById('question').value='What are the benefits of Gold status?'">
                        What are the benefits of Gold status?
                    </li>
                    <li onclick="document.getElementById('question').value='How do I earn more miles?'">
                        How do I earn more miles?
                    </li>
                    <li onclick="document.getElementById('question').value='What is the difference between Silver and Gold status?'">
                        What is the difference between Silver and Gold status?
                    </li>
                </ul>
            </div>
            
            <button type="submit">Ask Assistant</button>
        </form>
        
        {#if error}
        <div class="answer-section error">
            <h2>⚠️ Error</h2>
            <p class="answer-content">{error}</p>
        </div>
        {/if}
        
        {#if answer}
        <div class="answer-section">
            <h2>💬 Answer</h2>
            <p class="answer-content">{answer}</p>
        </div>
        {/if}
    </div>
</body>
</html>
```

**Template Features:**

- Modern gradient design
- Form with textarea for questions
- Example questions with click-to-fill
- Answer display section
- Error handling display
- Responsive layout
- Server-side rendering (NO JavaScript frameworks)

## Step 4: Test Before Committing

**IMPORTANT: Always test before committing!**

### Start Application

```bash
# Option 1: Standard dev mode (may show NPE in logs but works)
./mvnw quarkus:dev

# Option 2: With console disabled (avoids NPE)
./mvnw quarkus:dev -Dquarkus.console.enabled=false

# Option 3: Background with logging
nohup ./mvnw quarkus:dev -Dquarkus.console.enabled=false > /tmp/quarkus.log 2>&1 &
```

### Verify Startup

Look for:

```
Listening on: http://localhost:8080
```

### Test in Browser

1. Open <http://localhost:8080>
2. Enter a question: "What are the benefits of Gold status?"
3. Click "Ask Assistant"
4. Verify AI response appears

### Test with curl

```bash
# Test POST endpoint
curl -X POST http://localhost:8080/ \
  -d "question=What are the benefits of Gold status?" \
  -H "Content-Type: application/x-www-form-urlencoded"
```

**Expected:** HTML response with AI-generated answer

### Check Logs

```bash
# If using nohup
tail -f /tmp/quarkus.log

# Look for:
# - "Received question: ..."
# - OpenAI API request/response logs
# - "Response generated successfully"
```

## Step 5: Commit and Push

Only after successful testing:

```bash
git add .
git commit -m "feat(stage-01): implement basic airline loyalty assistant

- Add AirlineLoyaltyAssistant AI service with @RegisterAiService
- Add AssistantController with form handling and @Location annotation
- Add Qute template with modern UI
- Configure OpenAI integration via application.properties
- Test and verify functionality"

git push origin main
```

## Known Issues & Solutions

### Issue 1: Template Not Found

**Symptoms:** `TemplateException: Template not found`

**Root Cause:** Qute cannot resolve template path without explicit `@Location`

**Solution:**

```kotlin
@Inject
@Location("AssistantController/index.html")
lateinit var index: Template
```

**DO NOT** rely on automatic path resolution - always use `@Location`.

### Issue 2: Quarkus Dev Mode NPE in VS Code

**Symptoms:**

```
java.lang.NullPointerException: Cannot invoke "...RuntimeUpdatesProcessor.doScan"
because "...RuntimeUpdatesProcessor.INSTANCE" is null
```

**Root Cause:** Quarkus interactive console conflicts with VS Code terminal handling

**Status:** Framework bug, NOT application code issue

**Solutions:**

1. **Disable console** (recommended):

   ```bash
   ./mvnw quarkus:dev -Dquarkus.console.enabled=false
   ```

2. **Use nohup for background**:

   ```bash
   nohup ./mvnw quarkus:dev -Dquarkus.console.enabled=false > /tmp/quarkus.log 2>&1 &
   ```

3. **Run in external terminal**: Open separate terminal outside VS Code

**Note:** Application works perfectly - NPE only affects console interaction.

### Issue 3: Empty AI Responses

**Check:**

1. `OPENAI_API_KEY` in `.env` file
2. API key is valid and has credits
3. Network connectivity to OpenAI
4. Check logs for API errors

### Issue 4: Kotlin Class Proxy Issues

**Symptoms:** CDI injection fails

**Solution:** Verify `all-open` plugin configured in `pom.xml`:

```xml
<plugin>
    <groupId>org.jetbrains.kotlin</groupId>
    <artifactId>kotlin-maven-plugin</artifactId>
    <configuration>
        <compilerPlugins>
            <plugin>all-open</plugin>
        </compilerPlugins>
        <pluginOptions>
            <option>all-open:annotation=jakarta.ws.rs.Path</option>
            <option>all-open:annotation=jakarta.enterprise.context.ApplicationScoped</option>
        </pluginOptions>
    </configuration>
</plugin>
```

## Verification Checklist

Before considering Stage 01 complete:

- ✅ `AirlineLoyaltyAssistant.kt` created with `@RegisterAiService`
- ✅ `AssistantController.kt` created with `@Location` annotation
- ✅ `index.html` template created in correct directory
- ✅ Application starts without errors
- ✅ UI accessible at <http://localhost:8080>
- ✅ Form accepts input
- ✅ AI generates responses
- ✅ Error handling works (test with empty question)
- ✅ Logs show request/response debug info
- ✅ Code tested before committing
- ✅ Changes committed with descriptive message
- ✅ Pushed to GitHub

## Architecture Decisions

### Why Quarkus LangChain4j over Vanilla?

- **CDI Integration:** Automatic dependency injection
- **Configuration:** Declarative via `application.properties`
- **Lifecycle:** Managed by Quarkus runtime
- **Hot Reload:** Dev mode support
- **Build Time:** Optimizations at compile time

### Why Server-Side Rendering?

- **Simplicity:** No separate frontend/backend
- **Less Code:** Single deployment artifact
- **Demo Focus:** Easier to understand and modify
- **No CORS:** All on same origin

### Why Single Controller?

- **Simplicity:** 5-6 files total
- **Maintainability:** Everything in one place
- **Demo Purpose:** Easy to follow flow

## Next Steps

Proceed to **Stage 02: Function Calling/Tools** to add:

- Tool definitions for structured actions
- Flight lookup functionality
- Status check capabilities
- More complex AI interactions

## Key Learnings

1. **Always use `@Location` annotation** for Qute templates
2. **Test before committing** - catches issues early
3. **Quarkus LangChain4j requires CDI patterns** - different from vanilla
4. **NPE in dev mode is framework bug** - workaround with `-Dquarkus.console.enabled=false`
5. **Debug logging is essential** - `quarkus.langchain4j.log-requests=true`
6. **Form validation prevents errors** - check for null/blank input
7. **Error handling in UI** - show friendly messages to users
8. **Document issues immediately** - helps team avoid same problems

## Success Criteria

- ✅ Users can ask questions through web UI
- ✅ AI provides relevant answers about airline loyalty
- ✅ Application starts in < 2 seconds
- ✅ Responses generated in < 10 seconds
- ✅ Error handling works gracefully
- ✅ Code is maintainable and documented
- ✅ All tests pass before commit
