package com.mbagrov.server

import cats.effect.{Async, Resource}
import cats.syntax.all._
import com.comcast.ip4s._
import fs2.Stream
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.middleware.Logger
import pureconfig.ConfigSource
import com.mbagrov.conf.ServiceConfig
import com.mbagrov.routes.ZivergeTechChallengeRoutes
import com.mbagrov.services.{BlackBoxService, SystemProcessService, WordsService}
import pureconfig.generic.auto._

object ZivergeTechChallengeServer {

  def stream[F[_] : Async]: Stream[F, Nothing] = {
    implicit val conf: ServiceConfig = ConfigSource.default.loadOrThrow[ServiceConfig]
    val systemProcessService = SystemProcessService.impl[F]
    val blackboxAlg = BlackBoxService.impl[F](systemProcessService)
    val wordsAlg = WordsService.impl[F](blackboxAlg)

    val httpApp = ZivergeTechChallengeRoutes.wordsRoutes[F](wordsAlg).orNotFound
    val finalHttpApp = Logger.httpApp(true, true)(httpApp)

    Stream.resource(
      EmberServerBuilder.default[F]
        .withHost(ipv4"0.0.0.0")
        .withPort(port"8080")
        .withHttpApp(finalHttpApp)
        .build >>
        Resource.eval(systemProcessService.init) >>
        Resource.eval(wordsAlg.init) >>
        Resource.eval(Async[F].never)
    )
  }.drain
}
