package com.iloveshopping;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Boots the full application context against a real PostgreSQL container.
 *
 * The container publishes a FIXED host port below the kernel's ephemeral range
 * (ip_local_port_range starts at 32768): when the mapped port sits inside that
 * range, concurrent local connections can have their SOURCE port allocated
 * equal to the destination port, which the loopback stack rejects as a
 * self-connect. Fixed low ports make the context load deterministic even on
 * heavily loaded build hosts.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ILoveShoppingApplicationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("iloveshopping_test")
            .withUsername("test")
            .withPassword("test")
            .withUrlParam("stringtype", "unspecified")
            .withCreateContainerCmdModifier(cmd -> cmd.withPortBindings(new PortBinding(Ports.Binding.bindPort(15432), ExposedPort.tcp(5432))));

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void contextLoads() {
        // Verify the application context loads successfully
    }
}
