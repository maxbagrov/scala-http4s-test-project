package com.mbagrov.routes

import cats.effect.Sync
import cats.implicits._
import com.mbagrov.services.WordsService
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object ZivergeTechChallengeRoutes {

  def wordsRoutes[F[_]: Sync](words: WordsService[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "words" / "list" =>
        for {
          _words <- words.list
          resp <- Ok(_words)
        } yield resp
    }
  }
}
