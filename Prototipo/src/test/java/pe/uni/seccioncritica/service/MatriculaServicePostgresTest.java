package pe.uni.seccioncritica.service;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Las mismas reglas y carreras contra PostgreSQL real. Se omite si Docker no está disponible. */
@SpringBootTest(properties = "DB_PASSWORD=no-usada")
@ActiveProfiles("postgres")
@Testcontainers(disabledWithoutDocker = true)
class MatriculaServicePostgresTest extends ReglasMatriculaTestBase {
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");
}
