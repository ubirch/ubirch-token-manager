package com.ubirch.models

import java.util.UUID

import com.ubirch.services.cluster.ConnectionService
import io.getquill.{ CassandraStreamContext, SnakeCase }
import monix.reactive.Observable

import javax.inject.Inject

trait PlatformAccessTokenRowsQueries extends TablePointer[PlatformAccessTokenRow] {

  import db._

  //These represent query descriptions only

  implicit val pointingAt: db.SchemaMeta[PlatformAccessTokenRow] = schemaMeta[PlatformAccessTokenRow]("platform_access_tokens")

  def insertQ(patRow: PlatformAccessTokenRow): db.Quoted[db.Insert[PlatformAccessTokenRow]] = quote {
    query[PlatformAccessTokenRow].insert(lift(patRow))
  }

  def deleteQ(ownerId: UUID, tokenHashValue: String): db.Quoted[db.Delete[PlatformAccessTokenRow]] = quote {
    query[PlatformAccessTokenRow].filter(x => x.ownerId == lift(ownerId) && x.tokenHashValue == lift(tokenHashValue)).delete
  }

}

class PlatformAccessTokenDAO @Inject() (val connectionService: ConnectionService) extends PlatformAccessTokenRowsQueries {
  val db: CassandraStreamContext[SnakeCase.type] = connectionService.context

  import db._

  def insert(patRow: PlatformAccessTokenRow): Observable[Unit] = run(insertQ(patRow))

  def delete(ownerId: UUID, tokenHashValue: String): Observable[Unit] = run(deleteQ(ownerId, tokenHashValue))

}
