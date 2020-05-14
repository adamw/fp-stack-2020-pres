package com.softwaremill.fp

import java.time.Instant
import java.util.UUID

import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.implicits.legacy.instant._
import cats.implicits._
import com.typesafe.scalalogging.StrictLogging

import sttp.tapir._
import sttp.tapir.json.circe._
import io.circe.generic.auto._
import org.http4s.HttpRoutes
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.kleisli._
import sttp.tapir.swagger.http4s.SwaggerHttp4s
import zio._
import zio.interop.catz._
import zio.interop.catz.implicits._

object Main extends App with StrictLogging {
  def upsertLanguage(name: String): ConnectionIO[UUID] =
    sql"""INSERT INTO languages(id, name) VALUES
         |(${UUID.randomUUID()}, $name)
         |ON CONFLICT ON CONSTRAINT languages_name_key
         |DO UPDATE SET name=languages.name RETURNING (id)
         |""".stripMargin.update.withUniqueGeneratedKeys[UUID]("id")

  def insertLike(who: String, what: String): ConnectionIO[Unit] =
    upsertLanguage(what).flatMap { id =>
      sql"""INSERT INTO likes(id, language_id, username, created)
           |VALUES(${UUID.randomUUID()}, $id, $who, ${Instant.now()})
           |""".stripMargin.update.run.void
    }

  //

  case class LikeData(username: String, language: String)
  val likeEndpoint: Endpoint[(String, LikeData), String, Unit, Nothing] = endpoint.post
    .in(auth.bearer[String])
    .in("api" / "like")
    .in(jsonBody[LikeData])
    .errorOut(stringBody)

  //

  val transactor: Transactor[Task] = Transactor.fromDriverManager(
    "org.postgresql.Driver",
    "jdbc:postgresql:fp",
    "postgres",
    ""
  )

  val likeRoute: HttpRoutes[Task] = likeEndpoint.toZioRoutes {
    case (token, likeData) =>
      if (token == "1234") {
        insertLike(likeData.username, likeData.language)
          .transact(transactor)
          .mapError { t =>
            logger.error("Exception when talking to the DB", t)
            "Internal server error"
          }
      } else {
        IO.fail("Invalid token")
      }
  }

  //

  val yaml: String = {
    import sttp.tapir.docs.openapi._
    import sttp.tapir.openapi.circe.yaml._
    List(likeEndpoint).toOpenAPI("Best languages of 2020", "1.0").toYaml
  }

  val swaggerRoute: HttpRoutes[Task] = new SwaggerHttp4s(yaml).routes[Task]

  //

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] = {
    ZIO.runtime.flatMap { implicit runtime: Runtime[Any] =>
      BlazeServerBuilder[Task](runtime.platform.executor.asEC)
        .bindHttp(8080, "localhost")
        .withHttpApp(Router("/" -> (likeRoute <+> swaggerRoute)).orNotFound)
        .serve
        .compile
        .drain
        .map(_ => 0)
        .catchAll(
          t =>
            UIO(logger.error("Exception when starting server", t))
              .map(_ => 1)
        )
    }
  }
}
