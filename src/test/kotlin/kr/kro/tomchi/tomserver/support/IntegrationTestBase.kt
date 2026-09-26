package kr.kro.tomchi.tomserver.support

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.jdbc.Sql
import org.testcontainers.mysql.MySQLContainer

@Sql(
    statements = ["DELETE FROM orders", "DELETE FROM sku"],
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS
)
abstract class IntegrationTestBase {
    companion object {
        private val mysql = MySQLContainer("mysql:8.0").apply { start() }

        @DynamicPropertySource
        @JvmStatic
        fun datasourceProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { mysql.jdbcUrl }
            registry.add("spring.datasource.username") { mysql.username }
            registry.add("spring.datasource.password") { mysql.password }
        }
    }
}
