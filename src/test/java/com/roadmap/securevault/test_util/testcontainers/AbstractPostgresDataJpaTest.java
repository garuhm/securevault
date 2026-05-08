package com.roadmap.securevault.test_util.testcontainers;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.properties.TestcontainersPropertySourceAutoConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;

/** Base {@link DataJpaTest} with a PostgreSQL Testcontainer **/
@DataJpaTest(
        excludeAutoConfiguration = {
                TestcontainersPropertySourceAutoConfiguration.class,
                ServiceConnectionAutoConfiguration.class
        })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractPostgresDataJpaTest extends AbstractTestContainersUtilizingTest {
}
