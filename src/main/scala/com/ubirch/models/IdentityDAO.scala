package com.ubirch.models

import com.ubirch.services.cluster.ConnectionService
import io.getquill.{CassandraStreamContext, SnakeCase}
import javax.inject.Inject
import monix.reactive.Observable

/**
  * Represents the queries for the Identity Column Family.
  */
trait IdentitiesQueries extends TablePointer[IdentityRow] {

  import db._

  //These represent query descriptions only

  implicit val eventSchemaMeta: db.SchemaMeta[IdentityRow] = schemaMeta[IdentityRow]("identities")

  def byOwnerIdAndIdentityIdAndDataIdQ(ownerId: String, identityId: String, dataId: String): db.Quoted[db.EntityQuery[IdentityRow]] = quote {
    query[IdentityRow]
      .filter(x => x.ownerId == lift(ownerId))
      .filter(x => x.identityId == lift(identityId))
      .filter(x => x.dataId == lift(dataId))
      .map(x => x)
  }

  def insertQ(identityRow: IdentityRow): db.Quoted[db.Insert[IdentityRow]] = quote {
    query[IdentityRow].insert(lift(identityRow))
  }

  def selectAllQ: db.Quoted[db.EntityQuery[IdentityRow]] = quote(query[IdentityRow])

}

/**
  * Represents the Data Access Object for the Identity Queries
  * @param connectionService Represents the Connection to Cassandra
  */
class IdentitiesDAO @Inject() (val connectionService: ConnectionService) extends IdentitiesQueries {
  val db: CassandraStreamContext[SnakeCase.type] = connectionService.context

  import db._

  def insertWithState(identityRow: IdentityRow): Observable[Unit] = {

    for {
      _ <- run(insertQ(identityRow))
    } yield ()

  }

  def selectAll: Observable[IdentityRow] = run(selectAllQ)

  def byOwnerIdAndIdentityIdAndDataId(ownerId: String, identityId: String, dataId: String): Observable[IdentityRow] = run(byOwnerIdAndIdentityIdAndDataIdQ(ownerId, identityId, dataId))

}
