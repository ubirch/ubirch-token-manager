/* Cassandra migration for keyspace token_system.
   Version 1 - 2020-10-27T11:17:34.805802+00:00

   Adding basic token table */

drop table if exists tokens;

create table if not exists tokens (id UUID,
                                   owner_id UUID,
                                   token_value text,
                                   created_at timestamp,
                                   PRIMARY KEY (owner_id, id));
