package com.mbagrov.services

import cats.effect.Concurrent
import cats.syntax.functor._
import io.circe.generic.semiauto.deriveDecoder
import io.circe.parser.decode
import io.circe.Decoder
import org.http4s.circe.jsonOf
import org.http4s.EntityDecoder

trait BlackBoxService[F[_]] {

  def list: F[Vector[BlackBoxService.Row]]
}

object BlackBoxService {

  def apply[F[_]](implicit ev: BlackBoxService[F]): BlackBoxService[F] = ev

  final case class Row(event_type: String, data: String, timestamp: Int)

  object Row {
    implicit val rowDecoder: Decoder[Row] = deriveDecoder[Row]
    implicit def rowEntityDecoder[F[_]: Concurrent]: EntityDecoder[F, Row] = jsonOf
  }

  def impl[F[_]: Concurrent](systemProcessService: SystemProcessService[F]): BlackBoxService[F] = new BlackBoxService[F] {

    private def parseOutput(output: String): Option[Row] = {
      decode[Row](output).toOption
    }

    override def list: F[Vector[BlackBoxService.Row]] = {
      systemProcessService.fetch.map { records =>
        records.flatMap(parseOutput)
      }
    }
  }
}
