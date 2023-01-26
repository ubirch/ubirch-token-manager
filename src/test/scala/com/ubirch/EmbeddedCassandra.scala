package com.ubirch

import com.github.nosan.embedded.cassandra.commons.ClassPathResource
import com.github.nosan.embedded.cassandra.cql.{ CqlScript, ResourceCqlScript, StringCqlScript }

object EmbeddedCassandra {

  def truncateScript: StringCqlScript = {
    new StringCqlScript("truncate token_system.tokens;")
  }

  def creationScripts: Seq[CqlScript] = List(
    new StringCqlScript("drop keyspace IF EXISTS token_system;"),
    new StringCqlScript("CREATE KEYSPACE token_system WITH replication = {'class': 'SimpleStrategy','replication_factor': '1'};"),
    new StringCqlScript("USE token_system;"),
    new ResourceCqlScript(new ClassPathResource("db/migrations/v1_Adding_basic_token_table.cql"))
  )
}
