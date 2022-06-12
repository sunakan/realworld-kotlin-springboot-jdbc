package com.example.realworldkotlinspringbootjdbc.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Comments")
class CommentController {
    @GetMapping("/articles/{slug}/comments")
    fun list(): ResponseEntity<String> {
        return ResponseEntity(
            ObjectMapper().writeValueAsString(
                mapOf(
                    "comments" to listOf(
                        mapOf(
                            "id" to 1,
                            "body" to "hoge-body",
                            "createdAt" to "2022-01-01T00:00:00.0+09:00",
                            "updatedAt" to "2022-01-01T00:00:00.0+09:00",
                            "author" to "hoge-author",
                        ),
                    ),
                )
            ),
            HttpStatus.valueOf(200)
        )
    }

    @PostMapping("/articles/{slug}/comments")
    fun create(@RequestBody rawRequestBody: String?): ResponseEntity<String> {
        return ResponseEntity(
            ObjectMapper().writeValueAsString(
                mapOf(
                    "comment" to mapOf(
                        "id" to 1,
                        "body" to "hoge-body",
                        "createdAt" to "2022-01-01T00:00:00.0+09:00",
                        "updatedAt" to "2022-01-01T00:00:00.0+09:00",
                        "author" to "hoge-author",
                    ),
                )
            ),
            HttpStatus.valueOf(200)
        )
    }

    @DeleteMapping("/articles/{slug}/comments/{id}")
    fun delete(): ResponseEntity<String> {
        return ResponseEntity("", HttpStatus.valueOf(200))
    }
}