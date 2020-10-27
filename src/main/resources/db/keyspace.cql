-- local
CREATE KEYSPACE token_system WITH replication = {'class': 'SimpleStrategy','replication_factor': '1'};

-- dev/prod
CREATE KEYSPACE token_system WITH replication = {'class': 'NetworkTopologyStrategy','DC_AZURE_WEST_EUROPE_01': '3'};

-- helper to modify replication
ALTER KEYSPACE token_system WITH REPLICATION = {'class': 'NetworkTopologyStrategy','DC_AZURE_WEST_EUROPE_01': '3'};
