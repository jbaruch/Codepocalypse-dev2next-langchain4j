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

/**
 * Configuration for RAG (Retrieval Augmented Generation) components.
 * Provides beans for embedding store and retrieval augmentor.
 */
@ApplicationScoped
class RagConfiguration {

    /**
     * Provides an in-memory embedding store as a singleton.
     * This will be shared across all RAG operations.
     */
    @Produces
    @Singleton
    fun embeddingStore(): EmbeddingStore<TextSegment> {
        return InMemoryEmbeddingStore()
    }
}

/**
 * RetrievalAugmentor that retrieves relevant document segments and augments
 * the LLM prompt with retrieved context from Delta and United loyalty programs.
 * 
 * Since this is the only RetrievalAugmentor bean, it will be automatically
 * used by all @RegisterAiService interfaces.
 */
@ApplicationScoped
class DocumentRetriever(
    embeddingStore: EmbeddingStore<TextSegment>,
    embeddingModel: EmbeddingModel
) : RetrievalAugmentor {

    private val augmentor: RetrievalAugmentor

    init {
        // Create content retriever with embedding store
        val contentRetriever = EmbeddingStoreContentRetriever.builder()
            .embeddingStore(embeddingStore)
            .embeddingModel(embeddingModel)
            .maxResults(5) // Retrieve top 5 most relevant segments
            .minScore(0.6) // Only include segments with similarity score >= 0.6
            .build()

        // Create the retrieval augmentor
        augmentor = DefaultRetrievalAugmentor.builder()
            .contentRetriever(contentRetriever)
            .build()
    }

    override fun augment(augmentationRequest: AugmentationRequest): AugmentationResult {
        return augmentor.augment(augmentationRequest)
    }
}
