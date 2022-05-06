package com.mbagrov

import cats.effect.{ExitCode, IO, IOApp}
import com.mbagrov.server.ZivergeTechChallengeServer

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    ZivergeTechChallengeServer.stream[IO].compile.drain.as(ExitCode.Success)
  }
}
