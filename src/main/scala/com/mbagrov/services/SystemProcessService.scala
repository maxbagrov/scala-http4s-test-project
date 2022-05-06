package com.mbagrov.services

import cats.effect.Concurrent
import cats.syntax.functor._
import com.mbagrov.conf.ServiceConfig

import java.io.File
import scala.collection.mutable.ArrayBuffer
import scala.sys.process.{Process, ProcessLogger}

trait SystemProcessService[F[_]] {

  def init: F[Unit]

  def fetch: F[Vector[String]]
}

object SystemProcessService {

  def apply[F[_]](implicit ev: SystemProcessService[F]): SystemProcessService[F] = ev

  def impl[F[_]: Concurrent](implicit conf: ServiceConfig): SystemProcessService[F] = new SystemProcessService[F] {

    object RowsLock

    private val rowsBuffer = ArrayBuffer.empty[String]

    private def startProcess() = {
      val process = Process(s"./${conf.app}", new File(conf.paths))
      val processLogger = ProcessLogger { output =>
        val rows = output.split("\n").toVector
        add(rows)
      }

      process.run(processLogger)
    }

    private def add(rows: Vector[String]): Unit = {
      RowsLock.synchronized {
        rowsBuffer.addAll(rows)
        ()
      }
    }

    private def fetchRecords() = {
      RowsLock.synchronized {
        val rows = rowsBuffer.toVector
        rowsBuffer.clear()
        rows
      }
    }

    override def fetch: F[Vector[String]] = {
      Concurrent[F].unit.map { _ =>
        fetchRecords()
      }
    }

    override def init: F[Unit] = {
      Concurrent[F].unit.map { _ =>
        startProcess()
        ()
      }
    }
  }
}


