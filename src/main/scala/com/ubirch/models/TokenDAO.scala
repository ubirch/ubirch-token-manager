package com.ubirch.models

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

}

class TokensDAO @Inject() (val connectionService: ConnectionService) extends TokenRowsQueries {
  val db: CassandraStreamContext[SnakeCase.type] = connectionService.context

  import db._

  def selectAll: Observable[TokenRow] = run(selectAllQ)

  def insert(tokenRow: TokenRow): Observable[Unit] = run(insertQ(tokenRow))

}
