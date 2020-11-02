package com.ubirch.models

import java.util.UUID

import com.ubirch.services.cluster.ConnectionService
import io.getquill.{ CassandraStreamContext, SnakeCase }
import javax.inject.Inject
import monix.reactive.Observable

trait TokenRowsQueries extends TablePointer[TokenRow] {

  import db._

  //These represent query descriptions only

  implicit val pointingAt: db.SchemaMeta[TokenRow] = schemaMeta[TokenRow]("tokens")

  def insertQ(tokenRow: TokenRow): db.Quoted[db.Insert[TokenRow]] = quote {
    query[TokenRow].insert(lift(tokenRow))
  }

  def selectAllQ: db.Quoted[db.EntityQuery[TokenRow]] = quote(query[TokenRow])

  def byOwnerIdQ(ownerId: UUID): db.Quoted[db.EntityQuery[TokenRow]] = quote {
    query[TokenRow]
      .filter(_.ownerId == lift(ownerId))
      .map(x => x)
  }

  def deleteQ(ownerId: UUID, tokenId: UUID): db.Quoted[db.Delete[TokenRow]] = quote {
    query[TokenRow].filter(x => x.ownerId == lift(ownerId) && x.id == lift(tokenId)).delete
  }

}

class TokensDAO @Inject() (val connectionService: ConnectionService) extends TokenRowsQueries {
  val db: CassandraStreamContext[SnakeCase.type] = connectionService.context

  import db._

  def selectAll: Observable[TokenRow] = run(selectAllQ)

  def insert(tokenRow: TokenRow): Observable[Unit] = run(insertQ(tokenRow))

  def byOwnerId(ownerId: UUID): Observable[TokenRow] = run(byOwnerIdQ(ownerId))

  def delete(ownerId: UUID, tokenId: UUID): Observable[Unit] = run(deleteQ(ownerId, tokenId))

}
