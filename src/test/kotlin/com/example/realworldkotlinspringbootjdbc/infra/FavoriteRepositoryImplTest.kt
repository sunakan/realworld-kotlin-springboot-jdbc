package com.example.realworldkotlinspringbootjdbc.infra

import arrow.core.Either.Left
import arrow.core.Either.Right
import com.example.realworldkotlinspringbootjdbc.domain.ArticleId
import com.example.realworldkotlinspringbootjdbc.domain.CreatedArticle
import com.example.realworldkotlinspringbootjdbc.domain.article.Body
import com.example.realworldkotlinspringbootjdbc.domain.article.Description
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.article.Title
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.core.api.dataset.ExpectedDataSet
import com.github.database.rider.junit5.api.DBRider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import java.text.SimpleDateFormat
import com.example.realworldkotlinspringbootjdbc.domain.article.Tag as ArticleTag

class FavoriteRepositoryImplTest {
    companion object {
        val namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate

        fun resetSequence() {
            val sql = """
                SELECT
                    setval('favorites_id_seq', 10000)
                ;
            """.trimIndent()
            DbConnection.namedParameterJdbcTemplate.queryForRowSet(sql, MapSqlParameterSource())
        }
    }

    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DBRider
    @DisplayName("お気に入り登録")
    class Favorite {
        @BeforeAll
        fun reset() = resetSequence()

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/articles.yml",
                "datasets/yml/given/tags.yml",
            ],
        )
        @ExpectedDataSet(
            value = ["datasets/yml/then/favorite_repository/favorite-success.yml"],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        fun `成功-「slug に該当する articles テーブルに作成済記事が存在する」「favorites テーブルにお気に入り登録済でない」場合は、お気に入り登録（favorites テーブルに挿入）され 作成済記事（CreatedArticle）が戻り値`() {
            /**
             * given:
             */
            val favoriteRepository = FavoriteRepositoryImpl(namedParameterJdbcTemplate)

            /**
             * when:
             */
            val actual = favoriteRepository.favorite(Slug.newWithoutValidation("rust-vs-scala-vs-kotlin"), UserId(3))

            /**
             * then:
             */
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
            val expected = CreatedArticle.newWithoutValidation(
                id = ArticleId(1),
                title = Title.newWithoutValidation("Rust vs Scala vs Kotlin"),
                slug = Slug.newWithoutValidation("rust-vs-scala-vs-kotlin"),
                body = Body.newWithoutValidation("dummy-body"),
                createdAt = date,
                updatedAt = date,
                description = Description.newWithoutValidation("dummy-description"),
                tagList = listOf(ArticleTag.newWithoutValidation("rust"), ArticleTag.newWithoutValidation("scala")),
                authorId = UserId(1),
                favorited = true,
                favoritesCount = 2
            )
            when (actual) {
                is Left -> assert(false)
                is Right -> {
                    assertThat(actual.value.id).isEqualTo(expected.id)
                    assertThat(actual.value.title).isEqualTo(expected.title)
                    assertThat(actual.value.slug).isEqualTo(expected.slug)
                    assertThat(actual.value.body).isEqualTo(expected.body)
                    assertThat(actual.value.description).isEqualTo(expected.description)
                    assertThat(actual.value.authorId).isEqualTo(expected.authorId)
                    assertThat(actual.value.favorited).isEqualTo(expected.favorited)
                    assertThat(actual.value.favoritesCount).isEqualTo(expected.favoritesCount)
                }
            }
        }

        @Test
        @DataSet(
            value = [
                "datasets/yml/given/articles.yml",
                "datasets/yml/given/tags.yml",
            ],
        )
        @ExpectedDataSet(
            value = ["datasets/yml/given/articles.yml", "datasets/yml/given/tags.yml"],
            ignoreCols = ["id", "created_at", "updated_at"],
            orderBy = ["id"]
        )
        fun `成功-「articles テーブルに slug に該当する作成済記事が存在」「favorites テーブルにお気に入り登録済」の場合は、お気に入り登録されず（テーブルに変更なし）作成済記事（CreatedArticle）が戻り値`() {
            /**
             * given:
             */
            val favoriteRepository = FavoriteRepositoryImpl(namedParameterJdbcTemplate)

            /**
             * when:
             */
            val actual = favoriteRepository.favorite(Slug.newWithoutValidation("rust-vs-scala-vs-kotlin"), UserId(2))

            /**
             * then:
             */
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
            val expected = CreatedArticle.newWithoutValidation(
                id = ArticleId(1),
                title = Title.newWithoutValidation("Rust vs Scala vs Kotlin"),
                slug = Slug.newWithoutValidation("rust-vs-scala-vs-kotlin"),
                body = Body.newWithoutValidation("dummy-body"),
                createdAt = date,
                updatedAt = date,
                description = Description.newWithoutValidation("dummy-description"),
                tagList = listOf(ArticleTag.newWithoutValidation("rust"), ArticleTag.newWithoutValidation("scala")),
                authorId = UserId(1),
                favorited = true,
                favoritesCount = 1
            )
            when (actual) {
                is Left -> assert(false)
                is Right -> {
                    assertThat(actual.value.id).isEqualTo(expected.id)
                    assertThat(actual.value.title).isEqualTo(expected.title)
                    assertThat(actual.value.slug).isEqualTo(expected.slug)
                    assertThat(actual.value.body).isEqualTo(expected.body)
                    assertThat(actual.value.description).isEqualTo(expected.description)
                    assertThat(actual.value.authorId).isEqualTo(expected.authorId)
                    assertThat(actual.value.favorited).isEqualTo(expected.favorited)
                    assertThat(actual.value.favoritesCount).isEqualTo(expected.favoritesCount)
                }
            }
        }
    }
}
