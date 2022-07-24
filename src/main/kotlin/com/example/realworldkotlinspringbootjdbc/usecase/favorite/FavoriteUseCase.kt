package com.example.realworldkotlinspringbootjdbc.usecase.favorite

import arrow.core.Either
import com.example.realworldkotlinspringbootjdbc.domain.CreatedArticle
import com.example.realworldkotlinspringbootjdbc.domain.RegisteredUser
import com.example.realworldkotlinspringbootjdbc.util.MyError

interface FavoriteUseCase {
    fun execute(slug: String?, currentUser: RegisteredUser): Either<Error, CreatedArticle> = TODO()
    sealed interface Error : MyError {
        data class InvalidSlug(override val errors: List<MyError.ValidationError>) : Error, MyError.ValidationErrors
        data class NotFound(override val cause: MyError) : Error, MyError.MyErrorWithMyError
    }
}