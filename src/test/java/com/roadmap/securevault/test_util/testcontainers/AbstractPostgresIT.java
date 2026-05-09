package com.roadmap.securevault.test_util.testcontainers;

import org.springframework.boot.test.context.SpringBootTest;

/** Base Integration Test with a PostgreSQL Testcontainer **/
@SpringBootTest
public abstract class AbstractPostgresIT extends AbstractTestContainersUtilizingTest {
}

