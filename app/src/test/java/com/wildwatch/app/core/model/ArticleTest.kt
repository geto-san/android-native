package com.wildwatch.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ArticleTest {

    @Test
    fun `fromFirestoreDocument parses body`() {
        val article = Article.fromFirestoreDocument(
            "doc1",
            mapOf(
                "title" to "Elephants return to Bwindi",
                "excerpt" to "A short summary.",
                "body" to "The full article text goes here, across several paragraphs.",
            ),
        )

        assertEquals("The full article text goes here, across several paragraphs.", article.body)
    }

    @Test
    fun `fromFirestoreDocument defaults body to empty string when absent`() {
        val article = Article.fromFirestoreDocument(
            "doc1",
            mapOf("title" to "Elephants return to Bwindi"),
        )

        assertEquals("", article.body)
    }
}
