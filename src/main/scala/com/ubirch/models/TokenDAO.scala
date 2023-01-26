package com.ubirch.models

import java.util.UUID
import com.ubirch.services.cluster.ConnectionService
import io.getquill.{ CassandraStreamContext, Delete, EntityQuery, Insert, Quoted, SnakeCase }
import monix.reactive.Observable

import javax.inject.Inject

trait TokenRowsQueries extends TablePointer[TokenRow] {

  import db._

  //These represent query descriptions only

  implicit val pointingAt: db.SchemaMeta[TokenRow] = schemaMeta[TokenRow]("tokens")

  def insertQ(tokenRow: TokenRow): Quoted[Insert[TokenRow]] = quote {
    query[TokenRow].insertValue(lift(tokenRow))
  }

  def selectAllQ: Quoted[EntityQuery[TokenRow]] = quote(query[TokenRow])

  def byOwnerIdQ(ownerId: UUID): Quoted[EntityQuery[TokenRow]] = quote {
    query[TokenRow]
      .filter(_.ownerId == lift(ownerId))
      .map(x => x)
  }

  def byOwnerIdAndIdQ(ownerId: UUID, id: UUID): Quoted[EntityQuery[TokenRow]] = quote {
    query[TokenRow]
      .filter(_.ownerId == lift(ownerId))
      .filter(_.id == lift(id))
      .map(x => x)
  }

  def deleteQ(ownerId: UUID, tokenId: UUID): Quoted[Delete[TokenRow]] = quote {
    query[TokenRow].filter(x => x.ownerId == lift(ownerId) && x.id == lift(tokenId)).delete
  }

}

class TokensDAO @Inject() (val connectionService: ConnectionService) extends TokenRowsQueries {
  val db: CassandraStreamContext[SnakeCase] = connectionService.context

  import db._

  def selectAll: Observable[TokenRow] = run(selectAllQ)

  def insert(tokenRow: TokenRow): Observable[Unit] = run(insertQ(tokenRow))

  def byOwnerId(ownerId: UUID): Observable[TokenRow] = run(byOwnerIdQ(ownerId))

  def byOwnerIdAndId(ownerId: UUID, id: UUID): Observable[TokenRow] = run(byOwnerIdAndIdQ(ownerId, id))

  def delete(ownerId: UUID, tokenId: UUID): Observable[Unit] = run(deleteQ(ownerId, tokenId))

}
