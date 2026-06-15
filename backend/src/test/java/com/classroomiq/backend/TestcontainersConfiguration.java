package com.classroomiq.backend;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer<?> postgresContainer() {
		// pgvector image so Flyway's `CREATE EXTENSION vector` works in integration tests
		return new PostgreSQLContainer<>(
				DockerImageName.parse("pgvector/pgvector:pg16")
						.asCompatibleSubstituteFor("postgres"));
	}

}
