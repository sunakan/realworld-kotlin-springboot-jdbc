package com.example.realworldkotlinspringbootjdbc.util

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.Some
import arrow.core.left
import arrow.core.right
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.domain.UserRepository
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 *
 * authorize のテスト
 *
 * */
class MyAuthTest {
    @Test
    fun `引数のAuthorization Headerがnullの場合、Jwt認証は「Bearer Tokenを要求する」旨のエラーを返す`() {
        val notImplementedUserRepository = object : UserRepository {}
        val notImplementedMySessionJwt = object : MySessionJwt {}

        val actual = MyAuthImpl(notImplementedUserRepository, notImplementedMySessionJwt).authorize(null)
        val expected = MyAuth.Unauthorized.RequiredBearerToken.left()
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `引数のAuthorization HeaderがBearer tokenとしての形式が異なった場合、Jwt認証は「Parseに失敗した」旨のエラーを返す`() {
        val notImplementedMySessionJwt = object : MySessionJwt {}
        val notImplementedUserRepository = object : UserRepository {}

        // TODO: ThrowableのCompare方法
        when (val actual = MyAuthImpl(notImplementedUserRepository, notImplementedMySessionJwt).authorize("dummy-empty")) {
            is Left -> when (val error = actual.value) {
                is MyAuth.Unauthorized.FailedParseBearerToken -> assertThat(error.authorizationHeader).isEqualTo("dummy-empty")
                else -> assert(false)
            }
            is Right -> assert(false)
        }
    }

    @Test
    fun `Jwtデコード時に「失敗した」旨のエラーが返ってきた場合、Jwt認証は「Decodeに失敗した」旨のエラーを返す`() {
        val decodeError = MySessionJwt.DecodeError.NothingRequiredClaim("dummy")
        val notImplementedUserRepository = object : UserRepository {}
        val decodeReturnError = object : MySessionJwt {
            override fun decode(token: String): Either<MySessionJwt.DecodeError, MySession> =
                decodeError.left()
        }

        val actual = MyAuthImpl(notImplementedUserRepository, decodeReturnError).authorize("Bearer: abc")
        val expected = MyAuth.Unauthorized.FailedDecodeToken(decodeError, "abc").left()
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Jwtデコードは成功するが、UserRepositoryがユーザー検索時にエラーを返した場合、Jwt認証は「見つからなかった」旨のエラーを返す`() {
        val userId = UserId(1)
        val decodeReturnSuccess = object : MySessionJwt {
            override fun decode(token: String): Either<MySessionJwt.DecodeError, MySession> =
                MySession(
                    userId,
                    object : Email { override val value: String get() = "dummy@example.com" }
                ).right()
        }
        val notFoundError = UserRepository.FindByUserIdError.NotFound(userId)
        val findByUserIdReturnError = object : UserRepository {
            override fun findByUserId(id: UserId): Either<UserRepository.FindByUserIdError, RegisteredUser> =
                notFoundError.left()
        }

        val actual = MyAuthImpl(findByUserIdReturnError, decodeReturnSuccess).authorize("Bearer: dummy")
        val expected = MyAuth.Unauthorized.NotFound(notFoundError, userId).left()
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `UserRepositoryがユーザー検索時にエラーを返した場合、Jwt認証は「見つからなかった」旨のエラーを返す`() {
        val userId = UserId(1)
        val decodeReturnSuccess = object : MySessionJwt {
            override fun decode(token: String): Either<MySessionJwt.DecodeError, MySession> =
                MySession(
                    userId,
                    object : Email { override val value: String get() = "dummy@example.com" }
                ).right()
        }
        val notFoundError = UserRepository.FindByUserIdError.NotFound(userId)
        val findByUserIdReturnError = object : UserRepository {
            override fun findByUserId(id: UserId): Either<UserRepository.FindByUserIdError, RegisteredUser> =
                notFoundError.left()
        }

        val actual = MyAuthImpl(findByUserIdReturnError, decodeReturnSuccess).authorize("Bearer: dummy")
        val expected = MyAuth.Unauthorized.NotFound(notFoundError, userId).left()
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `UserRepositoryがユーザー検索時に謎のエラーを返した場合、Jwt認証は「謎のエラー」を返す`() {
        val userId = UserId(1)
        val dummyEmail = Email.newWithoutValidation("dummy@example.com")
        val decodeReturnSuccess = object : MySessionJwt {
            override fun decode(token: String): Either<MySessionJwt.DecodeError, MySession> = MySession(userId, dummyEmail).right()
        }
        val unexpectedError = UserRepository.FindByUserIdError.Unexpected(Throwable(), userId)
        val findByUserIdReturnUnexpectedError = object : UserRepository {
            override fun findByUserId(id: UserId): Either<UserRepository.FindByUserIdError, RegisteredUser> =
                unexpectedError.left()
        }
        val input = "Bearer: dummy"

        val actual = MyAuthImpl(findByUserIdReturnUnexpectedError, decodeReturnSuccess).authorize(input)
        val expected = MyAuth.Unauthorized.Unexpected(unexpectedError, Some(input)).left()
        assertThat(actual).isEqualTo(expected)
    }
}
