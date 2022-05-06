package com.mbagrov.routes

import cats.effect.IO
import cats.effect.testing.specs2.CatsEffect
import com.mbagrov.conf.ServiceConfig
import com.mbagrov.services.{BlackBoxService, SystemProcessService, WordsService}
import org.http4s._
import org.http4s.implicits._
import org.mockito.specs2.Mockito
import org.specs2.mutable.Specification

class ZivergeTechChallengeRoutesSpec extends Specification with CatsEffect with Mockito {

  "ZivergeTechChallengeRoutes" should {

    "#words/list" in {
      val listWordsRequest = Request[IO](Method.GET, uri"/words/list")
      implicit val serviceConfig: ServiceConfig = mock[ServiceConfig]

      val systemProcessService = mock[SystemProcessService[IO]]

      systemProcessService.fetch returns IO.pure {
        Vector(
          "{ \"event_type\": \"baz\", \"data\": \"sit\", \"timestamp\": 1651755188 }",
          "{ \"event_type\": \"baz\", \"data\": \"ipsum\", \"timestamp\": 1651755188 }",
          "{ \"event_type\": \"foo\", \"data\": \"sit\", \"timestamp\": 1651755188 }"
        )
      }

      val blackBoxService = BlackBoxService.impl[IO](systemProcessService)
      val wordsService = WordsService.impl[IO](blackBoxService)

      val resource = ZivergeTechChallengeRoutes.wordsRoutes(wordsService).orNotFound(listWordsRequest)

      for {
        f <- wordsService.init.start
        _ <- f.cancel
        response <- resource
        body <- response.as[String]
      } yield {
        response.status must_== Status.Ok
        body must_== """[{"event_type":"baz","data":"sit","timestamp":1651755188,"count":2},{"event_type":"baz","data":"ipsum","timestamp":1651755188,"count":2},{"event_type":"foo","data":"sit","timestamp":1651755188,"count":1}]"""
      }
    }
  }
}
