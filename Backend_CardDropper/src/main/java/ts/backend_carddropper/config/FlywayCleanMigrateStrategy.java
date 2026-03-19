package ts.backend_carddropper.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Slf4j
@Configuration
@Profile("!prod")
public class FlywayCleanMigrateStrategy {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            log.info("Cleaning database and re-running all migrations (fresh seed)...");
            flyway.clean();
            flyway.migrate();
        };
    }
}
