# Stage 03: RAG Implementation - Step-by-Step Instructions

## Goal
Implement Retrieval Augmented Generation (RAG) to enhance the Airline Loyalty Assistant with current information from Delta and United airline loyalty program websites.

## Prerequisites
- Stage 02 completed (conversation memory working)
- Quarkus dev server running (or ability to start it)
- OpenAI API key configured
- Internet connection (for fetching airline websites)

## Part 1: Add Dependencies

### Step 1: Add RAG Dependencies to pom.xml

Add the following dependencies to your `pom.xml`:

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
</dependency>
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
    <version>1.18.3</version>
</dependency>
```

**Why these dependencies?**
- `langchain4j`: Core library for document handling, splitting, and ingestion
- `jsoup`: HTML parsing library (recommended by LangChain4j for web content)

**Note**: There is NO `UrlDocumentLoader` in LangChain4j - Jsoup is the official way to load web pages.

## Part 2: Create RAG Components

### Step 2: Create DocumentIngestionService.kt

Create `src/main/kotlin/dev2next/langchain4j/DocumentIngestionService.kt`:

```kotlin
package dev2next.langchain4j

import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.document.splitter.DocumentSplitters
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.store.embedding.EmbeddingStore
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor
import io.quarkus.logging.Log
import io.quarkus.runtime.Startup
import jakarta.annotation.PostConstruct
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jsoup.Jsoup
import java.util.concurrent.CompletableFuture

@ApplicationScoped
@Startup
class DocumentIngestionService {

    @Inject
    lateinit var embeddingModel: EmbeddingModel

    @Inject
    lateinit var embeddingStore: EmbeddingStore<TextSegment>

    companion object {
        private val DOCUMENT_URLS = listOf(
            "https://www.delta.com/us/en/skymiles/medallion-program/how-to-qualify",
            "https://www.united.com/en/us/fly/mileageplus/premier/qualify.html"
        )
    }

    @PostConstruct
    fun ingestDocuments() {
        Log.info("Starting document ingestion from ${DOCUMENT_URLS.size} URLs...")
        
        try {
            val documents = loadDocumentsInParallel()
            
            if (documents.isEmpty()) {
                Log.warn("No documents were loaded. RAG will not be available.")
                return
            }

            Log.info("Loaded ${documents.size} documents. Starting embedding and ingestion...")

            val ingestor = EmbeddingStoreIngestor.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .documentSplitter(DocumentSplitters.recursive(500, 50))
                .build()

            ingestor.ingest(documents)

            Log.info("Document ingestion completed successfully. RAG is ready.")
        } catch (e: Exception) {
            Log.error("Failed to ingest documents", e)
            throw e
        }
    }

    private fun loadDocumentsInParallel(): List<Document> {
        val futures = DOCUMENT_URLS.map { url ->
            CompletableFuture.supplyAsync {
                try {
                    loadDocumentFromUrl(url)
                } catch (e: Exception) {
                    Log.error("Failed to load document from $url", e)
                    null
                }
            }
        }

        return CompletableFuture.allOf(*futures.toTypedArray())
            .thenApply { 
                futures.mapNotNull { it.get() }
            }
            .get()
    }

    private fun loadDocumentFromUrl(url: String): Document {
        Log.info("Loading document from: $url")
        
        val jsoupDoc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (compatible; AirlineLoyaltyBot/1.0)")
            .timeout(30000)
            .get()

        val text = jsoupDoc.body().text()
        val title = jsoupDoc.title()

        val metadata = Metadata()
        metadata.put("source", url)
        metadata.put("title", title)
        metadata.put("airline", extractAirlineName(url))

        Log.info("Loaded document from $url: $title (${text.length} characters)")

        return Document.from(text, metadata)
    }

    private fun extractAirlineName(url: String): String {
        return when {
            url.contains("delta.com") -> "Delta"
            url.contains("united.com") -> "United"
            else -> "Unknown"
        }
    }
}
```

**Key Points**:
- `@Startup`: Runs on application start
- `@PostConstruct`: Method called after injection
- Parallel loading with `CompletableFuture`
- Jsoup for HTML parsing
- Document splitting: 500 tokens, 50 overlap
- Metadata tracking (source, title, airline)

### Step 3: Create RagConfiguration.kt

Create `src/main/kotlin/dev2next/langchain4j/RagConfiguration.kt`:

```kotlin
package dev2next.langchain4j

import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.rag.AugmentationRequest
import dev.langchain4j.rag.AugmentationResult
import dev.langchain4j.rag.DefaultRetrievalAugmentor
import dev.langchain4j.rag.RetrievalAugmentor
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever
import dev.langchain4j.store.embedding.EmbeddingStore
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import jakarta.inject.Singleton

@ApplicationScoped
class RagConfiguration {

    @Produces
    @Singleton
    fun embeddingStore(): EmbeddingStore<TextSegment> {
        return InMemoryEmbeddingStore()
    }
}

@ApplicationScoped
class DocumentRetriever(
    embeddingStore: EmbeddingStore<TextSegment>,
    embeddingModel: EmbeddingModel
) : RetrievalAugmentor {

    private val augmentor: RetrievalAugmentor

    init {
        val contentRetriever = EmbeddingStoreContentRetriever.builder()
            .embeddingStore(embeddingStore)
            .embeddingModel(embeddingModel)
            .maxResults(5)
            .minScore(0.6)
            .build()

        augmentor = DefaultRetrievalAugmentor.builder()
            .contentRetriever(contentRetriever)
            .build()
    }

    override fun augment(augmentationRequest: AugmentationRequest): AugmentationResult {
        return augmentor.augment(augmentationRequest)
    }
}
```

**Key Points**:
- `InMemoryEmbeddingStore`: Simple vector storage
- `DocumentRetriever`: Implements `RetrievalAugmentor`
- Auto-wired to AI service (no explicit configuration needed)
- Max 5 results, minimum score 0.6

## Part 3: Update AI Service

### Step 4: Update AirlineLoyaltyAssistant.kt

Update the system message in `src/main/kotlin/dev2next/langchain4j/AirlineLoyaltyAssistant.kt`:

```kotlin
package dev2next.langchain4j

import dev.langchain4j.service.MemoryId
import dev.langchain4j.service.SystemMessage
import dev.langchain4j.service.UserMessage
import io.quarkiverse.langchain4j.RegisterAiService
import jakarta.enterprise.context.ApplicationScoped

@RegisterAiService
@ApplicationScoped
interface AirlineLoyaltyAssistant {

    @SystemMessage(
        """
        You are a helpful airline loyalty program assistant with access to current information 
        about Delta SkyMiles and United MileagePlus loyalty programs.
        
        DOCUMENT SOURCES:
        You have access to official documentation from:
        - Delta SkyMiles Medallion qualification requirements
        - United MileagePlus Premier qualification requirements
        
        When answering questions, you should:
        1. Use the retrieved document information to provide accurate, up-to-date answers
        2. Cite your sources when providing specific facts (e.g., "According to Delta's program...")
        3. Compare programs when asked about differences between airlines
        4. Remember information customers share during the conversation, including:
           - Their names and personal details
           - Their membership status and tier
           - Their travel preferences and history
           - Questions they've asked previously
           - Context from earlier in the conversation
        5. Use this remembered information to provide personalized, contextual responses
        6. Reference previous parts of the conversation naturally when relevant
        
        Topics you help with:
        - How to earn and qualify for elite status (Medallion/Premier)
        - Membership tier benefits and requirements
        - Qualifying activities (flights, spending, partnerships)
        - Travel rewards and perks
        - Comparing Delta and United programs
        
        Provide clear, concise, and accurate information. Be friendly and professional.
        If the retrieved documents don't contain the answer, acknowledge it honestly
        and offer to help with what you do know.
        
        ALWAYS cite the airline source when providing specific qualification requirements or benefits.
        """
    )
    @UserMessage("{question}")
    fun chat(@MemoryId memoryId: String, question: String): String
}
```

**Changes Made**:
- Added RAG-aware instructions
- Emphasized source citation
- Mentioned document sources explicitly
- Combined memory + RAG instructions
- **No explicit `retrievalAugmentor` parameter** - auto-detected!

## Part 4: Update UI

### Step 5: Update index.html

Update `src/main/resources/templates/AssistantController/index.html`:

Add the RAG badge and update styles:

```html
<!-- In the <style> section, add: -->
.rag-badge {
    background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
}

.tip-text {
    margin-top: 15px;
    color: #667eea;
    font-size: 0.95em;
}

<!-- Update the header: -->
<h1>
    ✈️ Airline Loyalty Assistant
    {#if hasMemory}
    <span class="memory-badge">🧠 Memory Active</span>
    {/if}
    <span class="memory-badge rag-badge">📚 RAG Enabled</span>
</h1>
<p class="subtitle">I have access to current Delta and United loyalty program information and remember our conversation!</p>

<!-- Update example questions: -->
<div class="examples">
    <h3>Try asking about:</h3>
    <ul>
        <li>Hello, my name is Bob. What are the requirements for Delta Medallion status?</li>
        <li>How do I qualify for United Premier Gold?</li>
        <li>Compare the elite status requirements for Delta and United</li>
        <li>What are the differences between Delta and United loyalty programs?</li>
        <li>How does spending on co-branded credit cards count toward elite status?</li>
    </ul>
    <p class="tip-text">
        💡 <strong>Tip:</strong> I have current information from Delta and United websites, and I'll remember our conversation!
    </p>
</div>
```

**UI Enhancements**:
- Added "📚 RAG Enabled" badge (green)
- Updated subtitle to mention airline information
- Changed example questions to airline-specific queries
- Updated tip text

## Part 5: Test the Implementation

### Step 6: Restart Application (if needed)

If Quarkus dev mode is running, it should auto-reload. Otherwise:

```bash
./mvnw quarkus:dev -Dquarkus.console.enabled=false
```

**Watch for startup logs**:
```
INFO  [dev.lan.DocumentIngestionService] Starting document ingestion from 2 URLs...
INFO  [dev.lan.DocumentIngestionService] Loading document from: https://www.delta.com/...
INFO  [dev.lan.DocumentIngestionService] Loading document from: https://www.united.com/...
INFO  [dev.lan.DocumentIngestionService] Loaded document from https://www.delta.com/...
INFO  [dev.lan.DocumentIngestionService] Loaded document from https://www.united.com/...
INFO  [dev.lan.DocumentIngestionService] Document ingestion completed successfully. RAG is ready.
```

### Step 7: Test RAG Functionality

Open browser to `http://localhost:8080` and test these scenarios:

**Test 1: Delta-Specific Query**
```
Query: "What are the requirements for Delta Medallion status?"

Expected: Response includes Delta-specific qualification requirements,
          cites Delta or SkyMiles as source
```

**Test 2: United-Specific Query**
```
Query: "How do I qualify for United Premier Gold?"

Expected: Response includes United-specific requirements,
          cites United or MileagePlus as source
```

**Test 3: Comparison Query**
```
Query: "Compare the elite status requirements for Delta and United"

Expected: Response compares both programs,
          cites both airlines
```

**Test 4: Memory + RAG**
```
Query 1: "Hello, my name is Alice. What are Delta's requirements?"
Query 2: "What about United?"
Query 3: "Which one is better for me?"

Expected: All responses maintain conversation context,
          remember name "Alice",
          provide personalized comparison
```

**Test 5: Out-of-Scope Query**
```
Query: "What is the weather in New York?"

Expected: Response acknowledges lack of information,
          offers to help with airline loyalty topics
```

### Step 8: Verify Logs

Check console for:
1. Successful document ingestion
2. No errors during startup
3. Normal AI service responses

## Troubleshooting

### Issue: "Failed to load document from URL"

**Symptoms**: Logs show errors fetching URLs

**Possible Causes**:
- Network connectivity issues
- Website blocking the bot user agent
- SSL/certificate problems
- Timeout (30 seconds)

**Solutions**:
1. Check internet connection
2. Try accessing URLs in browser
3. Increase timeout if needed
4. Verify Jsoup version is correct

### Issue: "No documents were loaded"

**Symptoms**: Warning "No documents were loaded. RAG will not be available."

**Causes**:
- All URL fetches failed
- Jsoup connection errors

**Solutions**:
1. Check logs for specific URL errors
2. Verify URLs are accessible
3. Check for firewall/proxy issues

### Issue: "Responses don't include airline information"

**Symptoms**: AI responds without citing sources or using retrieved docs

**Possible Causes**:
- Documents not ingested properly
- RetrievalAugmentor not wired
- Similarity scores too low

**Solutions**:
1. Check startup logs for successful ingestion
2. Verify `DocumentRetriever` bean is created
3. Lower `minScore` threshold temporarily
4. Check if query is too generic

### Issue: "Build/Compile Errors"

**Common Errors**:

**Error**: `Unresolved reference: Jsoup`
**Solution**: Ensure jsoup dependency is in pom.xml, run `./mvnw clean compile`

**Error**: `Unresolved reference: DocumentSplitters`
**Solution**: Ensure langchain4j dependency is added

**Error**: `Cannot find symbol: EmbeddingStoreIngestor`
**Solution**: Import from `dev.langchain4j.store.embedding`

### Issue: "Quarkus won't start"

**Symptoms**: Application crashes on startup

**Check**:
1. OpenAI API key is set
2. No port conflicts (8080)
3. All dependencies are valid
4. No syntax errors in Kotlin files

## Understanding the Code

### Document Loading Flow

```
1. @Startup triggers DocumentIngestionService creation
2. @PostConstruct calls ingestDocuments()
3. loadDocumentsInParallel() starts:
   - CompletableFuture for each URL
   - Jsoup.connect() fetches HTML
   - Extract text and metadata
   - Create Document objects
4. EmbeddingStoreIngestor:
   - Splits documents (500 tokens, 50 overlap)
   - Embeds each segment (OpenAI)
   - Stores in InMemoryEmbeddingStore
5. RAG ready!
```

### Query-Time Flow

```
1. User submits query
2. AssistantController calls assistant.chat()
3. DocumentRetriever automatically invoked:
   - Embed user query
   - Search embedding store (similarity)
   - Retrieve top 5 segments (score >= 0.6)
4. DefaultRetrievalAugmentor:
   - Injects retrieved content into prompt
5. LLM generates response with context
6. Response returned to user
```

### Auto-Wiring Magic

**How RetrievalAugmentor Gets Connected**:

1. Quarkus scans for `RetrievalAugmentor` beans
2. Finds `DocumentRetriever` (only one)
3. Automatically injects into `@RegisterAiService`
4. No explicit configuration needed!

**Multiple Augmentors**:
If you had multiple `RetrievalAugmentor` beans, specify:
```kotlin
@RegisterAiService(retrievalAugmentor = DocumentRetriever::class)
```

## Configuration Options

### Document Splitting

**Current**: 500 tokens, 50 overlap
**Adjust**:
```kotlin
DocumentSplitters.recursive(
    maxSegmentSize = 300,  // Smaller segments = more precise
    maxOverlapSize = 30     // Less overlap = less redundancy
)
```

**Trade-offs**:
- Smaller segments: More precise, but less context
- Larger segments: More context, but less precise
- More overlap: Better continuity, more storage

### Retrieval Configuration

**Current**: 5 results, 0.6 min score
**Adjust**:
```kotlin
EmbeddingStoreContentRetriever.builder()
    .maxResults(3)     // Fewer results = faster
    .minScore(0.7)     // Higher score = stricter matching
```

**Trade-offs**:
- More results: Better recall, longer prompts
- Fewer results: Faster, risk missing info
- Higher min score: More relevant, but may filter good matches

### Adding More URLs

Update `DOCUMENT_URLS`:
```kotlin
companion object {
    private val DOCUMENT_URLS = listOf(
        "https://www.delta.com/...",
        "https://www.united.com/...",
        "https://www.americanairlines.com/...",  // Add more!
        "https://www.southwest.com/..."
    )
}
```

## Advanced: Why Not EasyRAG?

**Question**: Why not use `quarkus-langchain4j-easy-rag` extension?

**Answer**: EasyRAG is simpler BUT only works with **local filesystem/classpath**.

**EasyRAG Example**:
```xml
<dependency>
    <groupId>io.quarkiverse.langchain4j</groupId>
    <artifactId>quarkus-langchain4j-easy-rag</artifactId>
</dependency>
```

```properties
quarkus.langchain4j.easy-rag.path=/path/to/documents
```

**That's it - zero code!**

**BUT** our requirements specify **URLs**, not local files. EasyRAG doesn't support URL loading, so we had to implement custom loading with Jsoup.

**If you download files first**, you could use EasyRAG:
```bash
# Download files
curl -o delta.html "https://www.delta.com/..."
curl -o united.html "https://www.united.com/..."

# Configure EasyRAG
quarkus.langchain4j.easy-rag.path=./documents
```

## Next Steps

### Immediate
- ✅ Test all query scenarios
- ✅ Verify source attribution
- ✅ Check memory + RAG integration

### Future Enhancements
- **Persistent Storage**: Migrate to pgvector or Redis
- **Source Display**: Show retrieved snippets in UI
- **Document Refresh**: Scheduled re-ingestion
- **More Airlines**: Add American, Southwest, etc.
- **Function Calling**: Real-time flight lookups
- **Local Models**: Ollama for offline operation

## Summary

You've successfully implemented RAG! Your assistant now:

✅ Loads current information from airline websites
✅ Performs semantic search over documents
✅ Augments LLM prompts with relevant context
✅ Cites sources in responses
✅ Maintains conversation memory
✅ Compares programs across airlines

**Stage 03: Complete!** 🎉
