package com.example.realworldkotlinspringbootjdbc.presentation

import arrow.core.Either
import arrow.core.Option
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.Comment
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.article.Slug
import com.example.realworldkotlinspringbootjdbc.domain.comment.CommentId
import com.example.realworldkotlinspringbootjdbc.domain.user.Bio
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.Image
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import com.example.realworldkotlinspringbootjdbc.domain.user.Username
import com.example.realworldkotlinspringbootjdbc.usecase.comment.CreateCommentUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.comment.DeleteCommentUseCase
import com.example.realworldkotlinspringbootjdbc.usecase.comment.ListCommentUseCase
import com.example.realworldkotlinspringbootjdbc.util.MyAuth
import com.example.realworldkotlinspringbootjdbc.util.MyError
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.text.SimpleDateFormat
import java.util.stream.Stream
import com.example.realworldkotlinspringbootjdbc.domain.comment.Body as CommentBody

class CommentControllerTest {
    @Nested
    class ListComment {
        private val requestHeader = "hoge-authorize"
        private val pathParam = "hoge-slug"
        val dummyRegisteredUser = RegisteredUser.newWithoutValidation(
            UserId(1),
            Email.newWithoutValidation("dummy@example.com"),
            Username.newWithoutValidation("dummy-name"),
            Bio.newWithoutValidation("dummy-bio"),
            Image.newWithoutValidation("dummy-image"),
        )

        private fun commentController(
            myAuth: MyAuth,
            commentsUseCase: ListCommentUseCase,
            createCommentUseCase: CreateCommentUseCase,
            deleteCommentUseCase: DeleteCommentUseCase
        ): CommentController =
            CommentController(myAuth, commentsUseCase, createCommentUseCase, deleteCommentUseCase)

        data class TestCase(
            val title: String,
            val useCaseExecuteResult: Either<ListCommentUseCase.Error, List<Comment>>,
            val expected: ResponseEntity<String>
        )

        @TestFactory
        fun listTest(): Stream<DynamicNode> {
            return Stream.of(
                TestCase(
                    "UseCase:?????????List<Comment>?????????????????????200????????????????????????",
                    listOf(
                        Comment.newWithoutValidation(
                            CommentId.newWithoutValidation(1),
                            CommentBody.newWithoutValidation("hoge-body-1"),
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                            UserId(1),
                        ),
                        Comment.newWithoutValidation(
                            CommentId.newWithoutValidation(2),
                            CommentBody.newWithoutValidation("hoge-body-2"),
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-02-02T00:00:00+09:00"),
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-02-02T00:00:00+09:00"),
                            UserId(1),
                        ),
                    ).right(),
                    ResponseEntity(
                        """{"comments":[{"id":1,"body":"hoge-body-1","createdAt":"2021-12-31T15:00:00.000Z","updatedAt":"2021-12-31T15:00:00.000Z","authorId":1},{"id":2,"body":"hoge-body-2","createdAt":"2022-02-01T15:00:00.000Z","updatedAt":"2022-02-01T15:00:00.000Z","authorId":1}]}""",
                        HttpStatus.valueOf(200)
                    )
                ),
                TestCase(
                    "UseCase:?????????NotFound?????????????????????404 ?????????????????????????????????",
                    ListCommentUseCase.Error.NotFound(object : MyError {}).left(),
                    ResponseEntity("""{"errors":{"body":["???????????????????????????????????????"]}}""", HttpStatus.valueOf(404)),
                ),
                TestCase(
                    "UseCase:?????????ValidationError?????????????????????404 ?????????????????????????????????",
                    ListCommentUseCase.Error.InvalidSlug(
                        listOf(object : MyError.ValidationError {
                            override val message: String get() = "DummyValidationError"
                            override val key: String get() = "DummyKey"
                        })
                    ).left(),
                    ResponseEntity("""{"errors":{"body":["???????????????????????????????????????"]}}""", HttpStatus.valueOf(404)),
                ),
                TestCase(
                    "UseCase:?????????Unexpected?????????????????????500 ?????????????????????????????????",
                    ListCommentUseCase.Error.Unexpected(object : MyError {}).left(),
                    ResponseEntity("""{"errors":{"body":["?????????????????????????????????????????????"]}}""", HttpStatus.valueOf(500)),
                )
            ).map { testCase ->
                dynamicTest(testCase.title) {
                    val actual =
                        commentController(
                            object : MyAuth {
                                override fun authorize(bearerToken: String?): Either<MyAuth.Unauthorized, RegisteredUser> {
                                    return dummyRegisteredUser.right()
                                }
                            },
                            object : ListCommentUseCase {
                                override fun execute(
                                    slug: String?,
                                    currentUser: Option<RegisteredUser>
                                ): Either<ListCommentUseCase.Error, List<Comment>> {
                                    return testCase.useCaseExecuteResult
                                }
                            },
                            object : CreateCommentUseCase {},
                            object : DeleteCommentUseCase {}
                        ).list(
                            slug = pathParam,
                            rawAuthorizationHeader = requestHeader
                        )
                    assertThat(actual).isEqualTo(testCase.expected)
                }
            }
        }
    }

    @Nested
    class ListCommentUnauthorized {
        private val requestHeader = "hoge-authorize"
        private val pathParam = "hoge-slug"
        private fun commentController(
            myAuth: MyAuth,
            commentsUseCase: ListCommentUseCase,
            createCommentUseCase: CreateCommentUseCase,
            deleteCommentUseCase: DeleteCommentUseCase
        ): CommentController =
            CommentController(myAuth, commentsUseCase, createCommentUseCase, deleteCommentUseCase)

        data class TestCase(
            val title: String,
            val useCaseExecuteResult: Either<ListCommentUseCase.Error, List<Comment>>,
            val expected: ResponseEntity<String>
        )

        @TestFactory
        fun unauthorizedListTest(): Stream<DynamicNode> {
            return Stream.of(
                TestCase(
                    "UseCase:?????????LIst<Comment>?????????????????????200 ????????????????????????",
                    listOf(
                        Comment.newWithoutValidation(
                            CommentId.newWithoutValidation(1),
                            CommentBody.newWithoutValidation("hoge-body-1"),
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                            UserId(1),
                        ),
                        Comment.newWithoutValidation(
                            CommentId.newWithoutValidation(2),
                            CommentBody.newWithoutValidation("hoge-body-2"),
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-02-02T00:00:00+09:00"),
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-02-02T00:00:00+09:00"),
                            UserId(1),
                        ),
                    ).right(),
                    ResponseEntity(
                        """{"comments":[{"id":1,"body":"hoge-body-1","createdAt":"2021-12-31T15:00:00.000Z","updatedAt":"2021-12-31T15:00:00.000Z","authorId":1},{"id":2,"body":"hoge-body-2","createdAt":"2022-02-01T15:00:00.000Z","updatedAt":"2022-02-01T15:00:00.000Z","authorId":1}]}""",
                        HttpStatus.valueOf(200)
                    )
                ),
                TestCase(
                    "UseCase:?????????NotFound?????????????????????404 ????????????????????????",
                    ListCommentUseCase.Error.NotFound(object : MyError {}).left(),
                    ResponseEntity("""{"errors":{"body":["???????????????????????????????????????"]}}""", HttpStatus.valueOf(404)),
                ),
                TestCase(
                    "UseCase:?????????ValidationError?????????????????????404 ????????????????????????",
                    ListCommentUseCase.Error.InvalidSlug(
                        listOf(object : MyError.ValidationError {
                            override val message: String get() = "DummyValidationError"
                            override val key: String get() = "DummyKey"
                        })
                    ).left(),
                    ResponseEntity(
                        """{"errors":{"body":["???????????????????????????????????????"]}}""",
                        HttpStatus.valueOf(404)
                    )
                ),
                TestCase(
                    "UseCase:?????????Unexpected??????????????????404 ????????????????????????",
                    ListCommentUseCase.Error.Unexpected(object : MyError {}).left(),
                    ResponseEntity("""{"errors":{"body":["?????????????????????????????????????????????"]}}""", HttpStatus.valueOf(500))
                )
            ).map { testCase ->
                dynamicTest(testCase.title) {
                    val actual =
                        commentController(
                            object : MyAuth {
                                override fun authorize(bearerToken: String?): Either<MyAuth.Unauthorized, RegisteredUser> {
                                    return MyAuth.Unauthorized.RequiredBearerToken.left()
                                }
                            },
                            object : ListCommentUseCase {
                                override fun execute(
                                    slug: String?,
                                    currentUser: Option<RegisteredUser>
                                ): Either<ListCommentUseCase.Error, List<Comment>> = testCase.useCaseExecuteResult
                            },
                            object : CreateCommentUseCase {},
                            object : DeleteCommentUseCase {}
                        ).list(
                            slug = pathParam,
                            rawAuthorizationHeader = requestHeader
                        )
                    assertThat(actual).isEqualTo(testCase.expected)
                }
            }
        }
    }

    @Nested
    class Create {
        private val requestHeader = "hoge-authorize"
        private val pathParam = "hoge-slug"
        private val requestBody = """
                {
                    "comment": {
                        "body": "hoge-body"
                    }
                }
        """.trimIndent()
        val dummyRegisteredUser = RegisteredUser.newWithoutValidation(
            UserId(1),
            Email.newWithoutValidation("dummy@example.com"),
            Username.newWithoutValidation("dummy-name"),
            Bio.newWithoutValidation("dummy-bio"),
            Image.newWithoutValidation("dummy-image"),
        )

        private fun commentController(
            myAuth: MyAuth,
            listCommentUseCase: ListCommentUseCase,
            createCommentUseCase: CreateCommentUseCase,
            deleteCommentUseCase: DeleteCommentUseCase
        ): CommentController =
            CommentController(myAuth, listCommentUseCase, createCommentUseCase, deleteCommentUseCase)

        data class TestCase(
            val title: String,
            val useCaseExecuteResult: Either<CreateCommentUseCase.Error, Comment>,
            val expected: ResponseEntity<String>,
        )

        @TestFactory
        fun createTest(): Stream<DynamicNode> {
            return Stream.of(
                TestCase(
                    "UseCase:?????????????????????Comment?????????????????????200 ????????????????????????",
                    Comment.newWithoutValidation(
                        CommentId.newWithoutValidation(1),
                        CommentBody.newWithoutValidation("hoge-body"),
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse("2022-01-01T00:00:00+09:00"),
                        UserId(1),
                    ).right(),
                    ResponseEntity(
                        """{"Comment":{"id":1,"body":"hoge-body","createdAt":"2021-12-31T15:00:00.000Z","updatedAt":"2021-12-31T15:00:00.000Z","authorId":1}}""",
                        HttpStatus.valueOf(200)
                    )
                ),
                TestCase(
                    "UseCase:?????????Slug ??????????????????ValidationError?????????????????????422 ?????????????????????????????????",
                    CreateCommentUseCase.Error.InvalidSlug(
                        listOf(
                            object : MyError.ValidationError {
                                override val message: String get() = "DummyValidationError because Invalid Slug"
                                override val key: String get() = "DummyKey"
                            }
                        )
                    ).left(),
                    ResponseEntity(
                        """{"errors":{"body":[{"key":"DummyKey","message":"DummyValidationError because Invalid Slug"}]}}""",
                        HttpStatus.valueOf(422)
                    )
                ),
                TestCase(
                    "UseCase:?????????CommentBody ?????????????????? ValidationError?????????????????????422 ?????????????????????????????????",
                    CreateCommentUseCase.Error.InvalidCommentBody(
                        listOf(object : MyError.ValidationError {
                            override val message: String get() = "DummyValidationError because invalid CommentBody"
                            override val key: String get() = "DummyKey"
                        })
                    ).left(),
                    ResponseEntity(
                        """{"errors":{"body":[{"key":"DummyKey","message":"DummyValidationError because invalid CommentBody"}]}}""",
                        HttpStatus.valueOf(422)
                    ),
                ),
                TestCase(
                    "UseCase:?????????Unexpected?????????????????????500 ?????????????????????????????????",
                    CreateCommentUseCase.Error.Unexpected(object : MyError {}).left(),
                    ResponseEntity("""{"errors":{"body":["?????????????????????????????????????????????"]}}""", HttpStatus.valueOf(500)),
                )
            ).map { testCase ->
                dynamicTest(testCase.title) {
                    val actual =
                        commentController(
                            object : MyAuth {
                                override fun authorize(bearerToken: String?): Either<MyAuth.Unauthorized, RegisteredUser> {
                                    return dummyRegisteredUser.right()
                                }
                            },
                            object : ListCommentUseCase {},
                            object : CreateCommentUseCase {
                                override fun execute(
                                    slug: String?,
                                    body: String?
                                ): Either<CreateCommentUseCase.Error, Comment> {
                                    return testCase.useCaseExecuteResult
                                }
                            },
                            object : DeleteCommentUseCase {}
                        ).create(requestHeader, pathParam, requestBody)
                    assertThat(actual).isEqualTo(testCase.expected)
                }
            }
        }
    }

    @Nested
    class Delete {
        private val requestHeader = "hoge-authorize"
        private val pathParamSlug = "hoge-slug"
        private val pathParamCommentId = "1"
        private val dummyRegisteredUser = RegisteredUser.newWithoutValidation(
            UserId(1),
            Email.newWithoutValidation("dummy@example.com"),
            Username.newWithoutValidation("dummy-name"),
            Bio.newWithoutValidation("dummy-bio"),
            Image.newWithoutValidation("dummy-image"),
        )

        private fun commentController(
            myAuth: MyAuth,
            listCommentUseCase: ListCommentUseCase,
            createCommentUseCase: CreateCommentUseCase,
            deleteCommentUseCase: DeleteCommentUseCase
        ): CommentController =
            CommentController(myAuth, listCommentUseCase, createCommentUseCase, deleteCommentUseCase)

        data class TestCase(
            val title: String,
            val useCaseExecuteResult: Either<DeleteCommentUseCase.Error, Unit>,
            val expected: ResponseEntity<String>,
        )

        @TestFactory
        fun deleteTest(): Stream<DynamicNode> {
            return Stream.of(
                TestCase(
                    "UseCase:?????????Unit?????????????????????200 ????????????????????????",
                    Unit.right(),
                    ResponseEntity("", HttpStatus.valueOf(200))
                ),
                TestCase(
                    "UseCase:?????????Slug ??????????????????ValidationError?????????????????????422 ?????????????????????????????????",
                    DeleteCommentUseCase.Error.InvalidSlug(
                        listOf(object : MyError.ValidationError {
                            override val message: String get() = "DummyValidationError because Invalid Slug"
                            override val key: String get() = "DummyKey"
                        })
                    ).left(),
                    ResponseEntity(
                        """{"errors":{"body":[{"key":"DummyKey","message":"DummyValidationError because Invalid Slug"}]}}""",
                        HttpStatus.valueOf(422)
                    ),
                ),
                TestCase(
                    "UseCase:?????????CommentId ?????????????????? ValidationError?????????????????????422 ?????????????????????????????????",
                    DeleteCommentUseCase.Error.InvalidCommentId(
                        listOf(object : MyError.ValidationError {
                            override val message: String get() = "DummyValidationError because Invalid CommentId"
                            override val key: String get() = "DummyKey"
                        })
                    ).left(),
                    ResponseEntity(
                        """{"errors":{"body":[{"key":"DummyKey","message":"DummyValidationError because Invalid CommentId"}]}}""",
                        HttpStatus.valueOf(422)
                    ),
                ),
                TestCase(
                    "UseCase:?????????slug?????????????????????????????????????????????????????? NotFound?????????????????????404 ?????????????????????????????????",
                    DeleteCommentUseCase.Error.ArticleNotFoundBySlug(
                        object : MyError {},
                        Slug.newWithoutValidation(pathParamSlug)
                    ).left(),
                    ResponseEntity("""{"errors":{"body":["???????????????????????????????????????"]}}""", HttpStatus.valueOf(404)),
                ),
                TestCase(
                    "UseCase:?????????commentId ?????????????????????????????????????????????????????? NotFound?????????????????????404 ?????????????????????????????????",
                    DeleteCommentUseCase.Error.CommentsNotFoundByCommentId(
                        object : MyError {},
                        CommentId.newWithoutValidation(pathParamCommentId.toInt())
                    ).left(),
                    ResponseEntity("""{"errors":{"body":["?????????????????????????????????????????????"]}}""", HttpStatus.valueOf(404)),
                ),
                TestCase(
                    "UseCase:?????????Undefined?????????????????????500 ?????????????????????????????????",
                    DeleteCommentUseCase.Error.Unexpected(object : MyError {}).left(),
                    ResponseEntity("""{"errors":{"body":["?????????????????????????????????????????????"]}}""", HttpStatus.valueOf(500)),
                )
            ).map { testCase ->
                dynamicTest(testCase.title) {
                    val actual =
                        commentController(
                            object : MyAuth {
                                override fun authorize(bearerToken: String?): Either<MyAuth.Unauthorized, RegisteredUser> {
                                    return dummyRegisteredUser.right()
                                }
                            },
                            object : ListCommentUseCase {},
                            object : CreateCommentUseCase {},
                            object : DeleteCommentUseCase {
                                override fun execute(
                                    slug: String?,
                                    commentId: Int?
                                ): Either<DeleteCommentUseCase.Error, Unit> {
                                    return testCase.useCaseExecuteResult
                                }
                            },
                        ).delete(requestHeader, pathParamSlug, pathParamCommentId)
                    assertThat(actual).isEqualTo(testCase.expected)
                }
            }
        }
    }
}
