package com.drychan.configuration;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Component;

@Component
public class MigrationConfiguration {

    final DataSource ds;

    public MigrationConfiguration(DataSource ds) {
        this.ds = ds;
    }

    @PostConstruct
    public void migrateWithFlyway() {
        Flyway flyway = Flyway.configure()
                .dataSource(ds)
                .locations("db/migration")
                .load();

        flyway.migrate();
    }
}