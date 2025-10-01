package dev2next.langchain4j

import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.document.splitter.DocumentSplitters
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.store.embedding.EmbeddingStore
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import io.quarkus.logging.Log
import io.quarkus.runtime.Startup
import jakarta.annotation.PostConstruct
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jsoup.Jsoup
import java.util.concurrent.CompletableFuture

/**
 * Document ingestion service that loads airline loyalty program documents from URLs,
 * processes them, and stores them in an in-memory embedding store for RAG.
 * 
 * Documents are loaded asynchronously in parallel for improved performance.
 */
@ApplicationScoped
@Startup
class DocumentIngestionService {

    @Inject
    lateinit var embeddingModel: EmbeddingModel

    @Inject
    lateinit var embeddingStore: EmbeddingStore<TextSegment>

    companion object {
        // Airline loyalty program URLs to ingest
        private val DOCUMENT_URLS = listOf(
            "https://www.delta.com/us/en/skymiles/medallion-program/how-to-qualify",
            "https://www.united.com/en/us/fly/mileageplus/premier/qualify.html"
        )
    }

    @PostConstruct
    fun ingestDocuments() {
        Log.info("Starting document ingestion from ${DOCUMENT_URLS.size} URLs...")
        
        try {
            // Load documents from URLs in parallel
            val documents = loadDocumentsInParallel()
            
            if (documents.isEmpty()) {
                Log.warn("No documents were loaded. RAG will not be available.")
                return
            }

            Log.info("Loaded ${documents.size} documents. Starting embedding and ingestion...")

            // Create ingestor with recursive splitter
            val ingestor = EmbeddingStoreIngestor.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .documentSplitter(DocumentSplitters.recursive(500, 50))
                .build()

            // Ingest all documents
            ingestor.ingest(documents)

            Log.info("Document ingestion completed successfully. RAG is ready.")
        } catch (e: Exception) {
            Log.error("Failed to ingest documents", e)
            throw e
        }
    }

    /**
     * Loads documents from URLs in parallel using CompletableFuture.
     * Each document includes metadata with the source URL for attribution.
     */
    private fun loadDocumentsInParallel(): List<Document> {
        // Create futures for parallel loading
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

        // Wait for all futures to complete and filter out nulls
        return CompletableFuture.allOf(*futures.toTypedArray())
            .thenApply { 
                futures.mapNotNull { it.get() }
            }
            .get()
    }

    /**
     * Loads a single document from a URL using Jsoup.
     * Extracts text content and includes source URL in metadata.
     */
    private fun loadDocumentFromUrl(url: String): Document {
        Log.info("Loading document from: $url")
        
        // Fetch and parse HTML using Jsoup
        val jsoupDoc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (compatible; AirlineLoyaltyBot/1.0)")
            .timeout(30000)
            .get()

        // Extract text content from body
        val text = jsoupDoc.body().text()

        // Extract title for better context
        val title = jsoupDoc.title()

        // Create metadata with source URL and title
        val metadata = Metadata()
        metadata.put("source", url)
        metadata.put("title", title)
        metadata.put("airline", extractAirlineName(url))

        Log.info("Loaded document from $url: $title (${text.length} characters)")

        return Document.from(text, metadata)
    }

    /**
     * Extracts airline name from URL for easier identification.
     */
    private fun extractAirlineName(url: String): String {
        return when {
            url.contains("delta.com") -> "Delta"
            url.contains("united.com") -> "United"
            else -> "Unknown"
        }
    }
}
