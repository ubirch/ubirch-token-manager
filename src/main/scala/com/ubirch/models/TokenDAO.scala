package com.ubirch.models

import java.util.UUID
import com.ubirch.services.cluster.ConnectionService
import io.getquill.{ CassandraStreamContext, SnakeCase }
import monix.reactive.Observable

import javax.inject.Inject

/**
  * @important
  * Since at least quill 3.12, dynamic query might leads to OutOfMemory.
  * Therefore, we need to avoid using it.
  * @see [[https://github.com/zio/zio-quill/issues/2484]]
  */
trait TokenRowsQueries extends CassandraBase {

  import db._

  def insertQ(tokenRow: TokenRow) = quote {
    querySchema[TokenRow]("tokens").insertValue(lift(tokenRow))
  }

  def selectAllQ = quote(querySchema[TokenRow]("tokens"))

  def byOwnerIdQ(ownerId: UUID) = quote {
    querySchema[TokenRow]("tokens")
      .filter(_.ownerId == lift(ownerId))
      .map(x => x)
  }

  def byOwnerIdAndIdQ(ownerId: UUID, id: UUID) = quote {
    querySchema[TokenRow]("tokens")
      .filter(_.ownerId == lift(ownerId))
      .filter(_.id == lift(id))
      .map(x => x)
  }

  def deleteQ(ownerId: UUID, tokenId: UUID) = quote {
    querySchema[TokenRow]("tokens").filter(x => x.ownerId == lift(ownerId) && x.id == lift(tokenId)).delete
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
