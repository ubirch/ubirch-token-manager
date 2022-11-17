drop table if exists platform_access_token;

create table if not exists platform_access_tokens (owner_id UUID,
                                                   token_hash_value text,
                                                   created_at timestamp,
                                                   expire_at timestamp,
                                                   PRIMARY KEY ((owner_id, token_hash_value), expire_at, created_at));
