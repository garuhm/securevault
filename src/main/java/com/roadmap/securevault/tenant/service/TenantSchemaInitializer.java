package com.roadmap.securevault.tenant.service;

import lombok.RequiredArgsConstructor;
import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@RequiredArgsConstructor
public class TenantSchemaInitializer {

    private final DataSource dataSource;

    public void migrateSchema(String schemaName) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .locations("classpath:db/migration/tenant")
                .table("flyway_schema_history")
                .baselineOnMigrate(true)
                .load();

        flyway.migrate();
    }
}
