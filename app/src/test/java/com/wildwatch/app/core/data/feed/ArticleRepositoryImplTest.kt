package com.wildwatch.app.core.data.feed

import com.wildwatch.app.core.database.ArticleDao
import com.wildwatch.app.core.database.ArticleEntity
import com.wildwatch.app.core.database.ArticleTheme
import com.wildwatch.app.core.model.Article
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArticleRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var dao: ArticleDao
    private lateinit var feedRemote: FeedRemoteDataSource
    private lateinit var changes: MutableSharedFlow<FeedRemoteChange>

    private fun buildRepository(): ArticleRepositoryImpl {
        every { feedRemote.observeFeedChanges() } returns changes
        return ArticleRepositoryImpl(
            dao,
            feedRemote,
            testDispatcher,
            CoroutineScope(testDispatcher),
        )
    }

    @Before
    fun setUp() {
        dao = mockk(relaxUnitFun = true)
        feedRemote = mockk()
        changes = MutableSharedFlow()
    }

    private fun entity(
        id: String = "art-1",
        body: String = "Full article body text.",
    ) = ArticleEntity(
        id = id,
        category = "News",
        theme = ArticleTheme.FOREST,
        title = "Elephants return to Bwindi",
        excerpt = "A short summary.",
        body = body,
        readTime = "3 min",
        source = "Uganda Wildlife Authority",
        likes = 4,
        comments = 1,
        publishedAt = 1_700_000_000_000L,
    )

    private fun article(
        id: String = "remote-1",
        body: String = "Full remote article body.",
    ) = Article(
        id = id,
        category = "News",
        theme = ArticleTheme.WILDLIFE,
        title = "Snare removed near Nkuringo",
        excerpt = "A short summary.",
        body = body,
        readTime = "4 min",
        source = "Uganda Wildlife Authority",
        likes = 0,
        comments = 0,
        publishedAt = 1_700_000_100_000L,
    )

    @Test
    fun `observeAll maps Room entities to domain articles including body`() = runTest(testDispatcher) {
        every { dao.observeAll() } returns flowOf(listOf(entity()))
        val repository = buildRepository()

        val result = repository.observeAll()

        result.collect { articles ->
            assertEquals(1, articles.size)
            assertEquals("Full article body text.", articles.first().body)
        }
    }

    @Test
    fun `observeById returns null when the row is absent`() = runTest(testDispatcher) {
        every { dao.observeById("missing") } returns flowOf(null)
        val repository = buildRepository()

        repository.observeById("missing").collect { article ->
            assertNull(article)
        }
    }

    @Test
    fun `observeById maps a present row to a domain article`() = runTest(testDispatcher) {
        every { dao.observeById("art-1") } returns flowOf(entity(id = "art-1"))
        val repository = buildRepository()

        repository.observeById("art-1").collect { article ->
            assertEquals("art-1", article?.id)
            assertEquals("Full article body text.", article?.body)
        }
    }

    @Test
    fun `a new remote article change is upserted into Room with its body`() = runTest(testDispatcher) {
        buildRepository() // the init block starts collecting feedRemote on construction

        runCurrent()
        changes.emit(FeedRemoteChange(article = article(id = "remote-1"), isRemoved = false))
        advanceUntilIdle()

        coVerify {
            dao.upsert(
                match { it.id == "remote-1" && it.body == "Full remote article body." },
            )
        }
    }

    @Test
    fun `a remote removal deletes the row from Room instead of upserting`() = runTest(testDispatcher) {
        buildRepository() // the init block starts collecting feedRemote on construction

        runCurrent()
        changes.emit(FeedRemoteChange(article = article(id = "remote-2"), isRemoved = true))
        advanceUntilIdle()

        coVerify { dao.deleteById("remote-2") }
        coVerify(exactly = 0) { dao.upsert(match { it.id == "remote-2" }) }
    }
}
