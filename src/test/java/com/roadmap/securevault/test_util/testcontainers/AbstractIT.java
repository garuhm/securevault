package com.roadmap.securevault.test_util.testcontainers;

import com.roadmap.securevault.common.scheduling.RefreshTokenCleaner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Base Integration Test with a PostgreSQL Testcontainer **/
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIT extends AbstractTestContainersUtilizingTest {
    @MockitoBean
    RefreshTokenCleaner refreshTokenCleaner;
}

