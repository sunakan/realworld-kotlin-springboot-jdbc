package com.example.realworldkotlinspringbootjdbc.domain

import arrow.core.Either
import com.example.realworldkotlinspringbootjdbc.domain.user.Email
import com.example.realworldkotlinspringbootjdbc.domain.user.Password
import com.example.realworldkotlinspringbootjdbc.util.MyError

interface UserRepository {
    //
    // ユーザー登録
    //
    fun register(user: UnregisteredUser): Either<RegisterError, RegisteredUser> = TODO()
    sealed interface RegisterError : MyError {
        data class AlreadyRegisteredEmail(val email: Email) : RegisterError, MyError.Basic
        data class Unexpected(override val cause: Throwable, val user: UnregisteredUser) : RegisterError, MyError.MyErrorWithThrowable
    }

    //
    // ユーザー検索 by Email with Password
    //
    fun findByEmailWithPassword(email: Email): Either<FindByEmailWithPasswordError, RegisteredWithPassword>
    sealed interface FindByEmailWithPasswordError : MyError {
        data class NotFound(val email: Email) : FindByEmailWithPasswordError, MyError.Basic
        data class Unexpected(override val cause: Throwable, val user: UnregisteredUser) : FindByEmailWithPasswordError, MyError.MyErrorWithThrowable
    }
}

typealias RegisteredWithPassword = Pair<RegisteredUser, Password>