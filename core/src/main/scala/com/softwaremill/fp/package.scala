package com.softwaremill

import org.http4s.{EntityBody, HttpRoutes}
import sttp.tapir._
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s._
import zio.interop.catz._
import zio.{IO, Task}

package object fp {
  // extension methods for ZIO; not a strict requirement, but they make working with ZIO much nicer
  implicit class ZioEndpoint[I, E, O](e: Endpoint[I, E, O, EntityBody[Task]]) {
    def toZioRoutes(logic: I => IO[E, O])(implicit serverOptions: Http4sServerOptions[Task]): HttpRoutes[Task] = {
      import sttp.tapir.server.http4s._
      e.toRoutes(i => logic(i).either)
    }

    def zioServerLogic(logic: I => IO[E, O]): ServerEndpoint[I, E, O, EntityBody[Task], Task] = ServerEndpoint(e, logic(_).either)
  }
}
