package com.example.realworldkotlinspringbootjdbc.infra

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.ArticleId
import com.example.realworldkotlinspringbootjdbc.domain.CreatedArticle
import com.example.realworldkotlinspringbootjdbc.domain.FavoriteRepository
import com.example.realworldkotlinspringbootjdbc.domain.article.Body
import com.example.realworldkotlinspringbootjdbc.domain.article.Description
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.article.Tag
import com.example.realworldkotlinspringbootjdbc.domain.article.Title
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.text.SimpleDateFormat

class FavoriteRepositoryImpl(val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) : FavoriteRepository {
    override fun favorite(slug: Slug, currentUserId: UserId): Either<FavoriteRepository.FavoriteError, CreatedArticle> {
        /**
         * article を取得
         */
        val selectArticleSql = """
            SELECT
                id
            FROM
                articles
            WHERE
                slug = :slug
        """.trimIndent()
        val selectArticleSqlParams = MapSqlParameterSource()
            .addValue("slug", slug.value)
        val articleFromDb = try {
            namedParameterJdbcTemplate.queryForList(selectArticleSql, selectArticleSqlParams)
        } catch (e: Throwable) {
            return FavoriteRepository.FavoriteError.Unexpected(e, slug, currentUserId).left()
        }

        /**
         * article が存在しなかったとき NotFoundError
         */
        if (articleFromDb.isEmpty()) {
            return FavoriteRepository.FavoriteError.ArticleNotFoundBySlug(slug).left()
        }
        val articleId = try {
            val it = articleFromDb.first()
            ArticleId(it["id"].toString().toInt())
        } catch (e: Throwable) {
            return FavoriteRepository.FavoriteError.Unexpected(e, slug, currentUserId).left()
        }

        /**
         * お気に入りではないとき、お気に入りに追加する
         */
        val insertFavoritesSql = """
            INSERT INTO favorites
                (
                    user_id
                    , article_id
                    , created_at
                )
            SELECT
                user_id
                , article_id
                , created_at
            FROM
                (
                    SELECT
                        :user_id AS user_id
                        , :article_id AS article_id
                        , NOW() AS created_at
                ) AS tmp
            WHERE
                NOT EXISTS (
                    SELECT
                        1
                    FROM
                        favorites
                    WHERE
                        user_id = :user_id
                        AND article_id = :article_id
                )
            ;
        """.trimIndent()
        val insertFavoritesSqlParams = MapSqlParameterSource()
            .addValue("user_id", currentUserId.value)
            .addValue("article_id", articleId.value)
        try {
            namedParameterJdbcTemplate.update(insertFavoritesSql, insertFavoritesSqlParams)
        } catch (e: Throwable) {
            return FavoriteRepository.FavoriteError.Unexpected(e, slug, currentUserId).left()
        }

        /**
         * Slug に該当する、作成済記事を取得する
         */
        val selectBySlugSql = """
            SELECT
                articles.id
                , articles.title
                , articles.slug
                , articles.body
                , articles.created_at
                , articles.updated_at
                , articles.description
                , COALESCE((
                    SELECT
                        STRING_AGG(tags.name, ',')
                    FROM
                        tags
                    JOIN
                        article_tags
                    ON
                        article_tags.tag_id = tags.id
                        AND article_tags.article_id = articles.id
                    GROUP BY
                        article_tags.article_id
                ), '') AS tags
                , articles.author_id
                , (
                    SELECT
                        CASE COUNT(favorites.id)
                            WHEN 0 THEN 0
                            ELSE 1
                        END
                    FROM
                        favorites
                    WHERE
                        favorites.article_id = articles.id
                        AND favorites.user_id = :user_id
                ) AS favorited
                , (
                    SELECT
                        COUNT(favorites.id)
                    FROM
                        favorites
                    WHERE
                        favorites.article_id = articles.id
                ) AS favoritesCount
            FROM
                articles
            WHERE
                articles.slug = :slug
            ;
        """.trimIndent()
        val sqlParams = MapSqlParameterSource()
            .addValue("slug", slug.value)
            .addValue("user_id", currentUserId.value)
        val articleMap = namedParameterJdbcTemplate.queryForList(selectBySlugSql, sqlParams).first()
        return try {
            CreatedArticle.newWithoutValidation(
                id = ArticleId(articleMap["id"].toString().toInt()),
                title = Title.newWithoutValidation(articleMap["title"].toString()),
                slug = Slug.newWithoutValidation(articleMap["slug"].toString()),
                body = Body.newWithoutValidation(articleMap["body"].toString()),
                createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(articleMap["created_at"].toString()),
                updatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(articleMap["updated_at"].toString()),
                description = Description.newWithoutValidation(articleMap["description"].toString()),
                tagList = articleMap["tags"].toString().split(",").map { Tag.newWithoutValidation(it) },
                authorId = UserId(articleMap["author_id"].toString().toInt()),
                favorited = articleMap["favorited"].toString() == "1",
                favoritesCount = articleMap["favoritesCount"].toString().toInt()
            ).right()
        } catch (e: Throwable) {
            FavoriteRepository.FavoriteError.Unexpected(e, slug, currentUserId).left()
        }
    }
}
