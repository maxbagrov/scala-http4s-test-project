package com.mbagrov.services

import cats.effect.IO
import cats.effect.testing.specs2.CatsEffect
import BlackBoxService.Row
import org.mockito.specs2.Mockito
import org.specs2.mutable.Specification

class BlackBoxServiceSpec extends Specification with CatsEffect with Mockito {

  "BlackBoxService" should {

    "#list with correctly produced data" in {
      val systemProcessService = mock[SystemProcessService[IO]]

      systemProcessService.fetch returns IO.pure {
        Vector(
          "{ \"event_type\": \"foo\", \"data\": \"ipsum\", \"timestamp\": 1651875088 }",
          "{ \"event_type\": \"bar\", \"data\": \"lorem\", \"timestamp\": 1651875088 }"
        )
      }

      val blackBoxService = BlackBoxService.impl[IO](systemProcessService)

      blackBoxService.list.map { result =>
        result must have size 2
        result must contain(Row("foo", "ipsum", 1651875088))
        result must contain(Row("bar", "lorem", 1651875088))
      }
    }

    "#list with incorrectly produced data" in {
      val systemProcessService = mock[SystemProcessService[IO]]

      systemProcessService.fetch returns IO.pure {
        Vector(
          "{ \"event_type\": \"foo\", \"data\": \"ipsum\", \"timestamp\": 1651875088 }",
          "{ \"?QO?????\""
        )
      }

      val blackBoxService = BlackBoxService.impl[IO](systemProcessService)

      blackBoxService.list.map { result =>
        result must have size 1
        result must contain(Row("foo", "ipsum", 1651875088))
      }
    }
  }
}
