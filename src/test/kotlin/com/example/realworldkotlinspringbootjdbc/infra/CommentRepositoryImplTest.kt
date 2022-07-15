package com.example.realworldkotlinspringbootjdbc.infra

import arrow.core.Either.Left
import arrow.core.Either.Right
import com.example.realworldkotlinspringbootjdbc.domain.Comment
import com.example.realworldkotlinspringbootjdbc.domain.CommentRepository
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.comment.Body
import com.example.realworldkotlinspringbootjdbc.domain.comment.CommentId
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import java.text.SimpleDateFormat

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("WithLocalDb")
class CommentRepositoryImplTest {
    companion object {
        val namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate
        fun resetDb() {
            val namedParameterJdbcTemplate = DbConnection.namedParameterJdbcTemplate
            val sql = """
                DELETE FROM users;
                DELETE FROM profiles;
                DELETE FROM followings;
                DELETE FROM articles;
                DELETE FROM article_comments;
            """.trimIndent()
            namedParameterJdbcTemplate.update(sql, MapSqlParameterSource())
        }
    }

    @Nested
    @Tag("WithLocalDb")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class `List(コメント一覧を表示)` {
        @BeforeEach
        @AfterAll
        fun reset() {
            resetDb()
        }

        @Test
        fun `正常系、Comment の List が戻り値`() {
            fun localPrepare() {
                val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")

                val insertArticleSql = """
                    INSERT INTO
                        articles (
                            id
                            , author_id
                            , title
                            , slug
                            , body
                            , description
                            , created_at
                            , updated_at
                        )
                    VALUES (
                        :id
                        , :author_id
                        , :title
                        , :slug
                        , :body
                        , :description
                        , :created_at
                        , :updated_at
                    );
                """.trimIndent()
                val insertArticleSqlParams = MapSqlParameterSource()
                    .addValue("id", 1)
                    .addValue("author_id", 1)
                    .addValue("title", "dummy-title")
                    .addValue("slug", "dummy-slug")
                    .addValue("body", "dummy-body")
                    .addValue("description", "dummy-description")
                    .addValue("created_at", date)
                    .addValue("updated_at", date)
                namedParameterJdbcTemplate.update(insertArticleSql, insertArticleSqlParams)

                val insertArticleCommentSql = """
                    INSERT INTO
                        article_comments (
                            id
                            , author_id
                            , article_id
                            , body
                            , created_at
                            , updated_at
                        )
                    VALUES (
                        :id
                        , :author_id
                        , :article_id
                        , :body
                        , :created_at
                        , :updated_at
                    )
                    ;
                """.trimIndent()
                val insertArticleCommentSqlParams1 = MapSqlParameterSource()
                    .addValue("id", 1)
                    .addValue("author_id", 1)
                    .addValue("article_id", 1)
                    .addValue("body", "dummy-body-1")
                    .addValue("created_at", date)
                    .addValue("updated_at", date)
                namedParameterJdbcTemplate.update(insertArticleCommentSql, insertArticleCommentSqlParams1)
                val insertArticleCommentSqlParams2 = MapSqlParameterSource()
                    .addValue("id", 2)
                    .addValue("author_id", 2)
                    .addValue("article_id", 1)
                    .addValue("body", "dummy-body-2")
                    .addValue("created_at", date)
                    .addValue("updated_at", date)
                namedParameterJdbcTemplate.update(insertArticleCommentSql, insertArticleCommentSqlParams2)
            }
            localPrepare()

            val commentRepository = CommentRepositoryImpl(namedParameterJdbcTemplate)

            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00")
            val expected = listOf(
                Comment.newWithoutValidation(
                    CommentId.newWithoutValidation(1),
                    Body.newWithoutValidation("dummy=body-1"),
                    date,
                    date,
                    UserId(1),
                ),
                Comment.newWithoutValidation(
                    CommentId.newWithoutValidation(2),
                    Body.newWithoutValidation("dummy-body-2"),
                    date,
                    date,
                    UserId(2),
                ),
            )
            when (val actual = commentRepository.list(Slug.newWithoutValidation("dummy-slug"), UserId(1))) {
                is Left -> assert(false)
                is Right -> assertThat(actual.value).isEqualTo(expected)
            }
        }

        @Test
        fun `異常系、NotFoundError が戻り値`() {
            val commentRepository = CommentRepositoryImpl(namedParameterJdbcTemplate)
            val expected = CommentRepository.ListError.NotFoundArticleBySlug(Slug.newWithoutValidation("dummy-slug"))
            when (val actual = commentRepository.list(Slug.newWithoutValidation("dummy-slug"), UserId(1))) {
                is Left -> assertThat(actual.value).isEqualTo(expected)
                is Right -> assert(false)
            }
        }
    }
}
