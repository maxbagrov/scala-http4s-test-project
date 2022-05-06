package com.mbagrov.services

import cats.effect.IO
import cats.effect.testing.specs2.CatsEffect
import com.mbagrov.conf.ServiceConfig
import org.mockito.specs2.Mockito
import org.specs2.mutable.Specification
import BlackBoxService.Row
import WordsService.WordsCount

class WordsServiceSpec extends Specification with CatsEffect with Mockito {

  "WordsService" should {

    "#init" in {
      val blackBoxService = mock[BlackBoxService[IO]]
      blackBoxService.list returns IO.pure(Vector.empty[Row])

      implicit val serviceConfig: ServiceConfig = mock[ServiceConfig]

      val wordsService = WordsService.impl(blackBoxService)

      verifyNoMoreInteractions(blackBoxService)

      wordsService.init.start.onCancel(IO.unit).map { _ =>
        there was one(blackBoxService).list
      }
    }

    "#list" in {
      val blackBoxService = mock[BlackBoxService[IO]]
      blackBoxService.list returns IO.pure{
        Vector(
          Row("baz", "sit", 1651755188),
          Row("baz", "ipsum", 1651755188),
          Row("foo", "sit", 1651755188)
        )
      }

      implicit val serviceConfig: ServiceConfig = mock[ServiceConfig]

      val wordsService = WordsService.impl(blackBoxService)

      for {
        f <- wordsService.init.start
        _ <- f.cancel
        wordsCounts <- wordsService.list
      } yield {
        wordsCounts.counts should have size 3
        wordsCounts.counts should contain(WordsCount("baz", "sit", 1651755188, 2))
        wordsCounts.counts should contain(WordsCount("baz", "ipsum", 1651755188, 2))
        wordsCounts.counts should contain(WordsCount("foo", "sit", 1651755188, 1))
      }
    }
  }
}
