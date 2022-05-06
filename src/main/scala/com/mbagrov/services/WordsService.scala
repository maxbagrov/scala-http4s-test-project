package com.mbagrov.services

import cats.effect.Concurrent
import io.circe.generic.semiauto._
import io.circe.{Encoder, Json}
import org.http4s._
import org.http4s.circe._
import cats.syntax.functor._
import com.mbagrov.conf.ServiceConfig
import io.circe.syntax._
import WordsService.WordsCounts

import scala.collection.mutable.ArrayBuffer

trait WordsService[F[_]] {

  def list: F[WordsCounts]

  def init: F[Unit]
}

object WordsService {
  def apply[F[_]](implicit ev: WordsService[F]): WordsService[F] = ev

  final case class WordsCount(event_type: String, data: String, timestamp: Int, count: Int)

  object WordsCount {
    def apply(row: BlackBoxService.Row, count: Int): WordsCount = {
      WordsCount(row.event_type, row.data, row.timestamp, count)
    }

    implicit val wordsCountEncoder: Encoder[WordsCount] = deriveEncoder[WordsCount]
    implicit def wordsCountEntityEncoder[F[_]]: EntityEncoder[F, WordsCount] = jsonEncoderOf
  }

  final case class WordsCounts(counts: Seq[WordsCount])

  object WordsCounts {
    implicit val wordsCountsEncoder: Encoder[WordsCounts] = new Encoder[WordsCounts] {
      final def apply(a: WordsCounts): Json = Json.arr(
        a.counts.map(_.asJson): _*
      )
    }

    implicit def wordsCountsEntityEncoder[F[_]]: EntityEncoder[F, WordsCounts] = jsonEncoderOf
  }

  def impl[F[_]: Concurrent](blackboxService: BlackBoxService[F])
                            (implicit conf: ServiceConfig): WordsService[F] = new WordsService[F] {

    object WordsCountLock
    private val wordsCountBuffer = ArrayBuffer.empty[WordsCount]

    private def updateWordsCount(wordsCount: Seq[WordsCount]) = {
      WordsCountLock.synchronized {
        wordsCountBuffer.clear()
        wordsCountBuffer.addAll(wordsCount)
      }
    }

    def list: F[WordsCounts] = {
      Concurrent[F].unit.map(_ => WordsCounts(wordsCountBuffer.toSeq))
    }

    def init: F[Unit] = {
      val update = blackboxService.list.map { rows =>
        val wordsCounts = rows.groupBy(_.event_type).flatMap { case (_, rows) =>
          val count = rows.length
          rows.map(WordsCount(_, count))
        }

        updateWordsCount(wordsCounts.toSeq)
        Thread.sleep(conf.duration)
      }

      Concurrent[F].foreverM(update)
    }
  }
}