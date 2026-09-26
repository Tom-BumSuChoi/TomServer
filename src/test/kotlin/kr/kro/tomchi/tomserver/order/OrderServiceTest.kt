package kr.kro.tomchi.tomserver.order

import kr.kro.tomchi.tomserver.catalog.Sku
import kr.kro.tomchi.tomserver.catalog.SkuRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.mysql.MySQLContainer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
@Testcontainers
class OrderServiceTest @Autowired constructor(
    private val placeOrderUseCase: PlaceOrderUseCase,
    private val orderRepository: OrderRepository,
    private val skuRepository: SkuRepository
) {

    private data class ScenarioResult(
        val successCount: Int,
        val finalStocks: List<Int>,
        val orderCount: Long
    )

    companion object {
        private const val INITIAL_STOCK = 100
        private const val CONCURRENT_REQUESTS = 1_000

        @Container
        @JvmStatic
        val mysql = MySQLContainer("mysql:8.0")

        @DynamicPropertySource
        @JvmStatic
        fun datasourceProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { mysql.jdbcUrl }
            registry.add("spring.datasource.username") { mysql.username }
            registry.add("spring.datasource.password") { mysql.password }
            // 기본값 10이면 실제 동시성이 커넥션 수만큼으로 잘려 경합이 잘 안 난다
            registry.add("spring.datasource.hikari.maximum-pool-size") { 50 }
        }
    }

    @Test
    fun `한 SKU에 1,000건을 동시에 주문해도 초과 판매하지 않는다`() {
        val result = runScenario(skuCount = 1, requestsPerSku = CONCURRENT_REQUESTS)

        assertThat(result.successCount).isEqualTo(INITIAL_STOCK)
        assertThat(result.finalStocks).containsExactly(0)
        assertThat(result.orderCount).isEqualTo(result.successCount.toLong())
    }

    @Test
    fun `열 SKU의 재고가 충분하면 모든 주문이 성공한다`() {
        val result = runScenario(skuCount = 10, requestsPerSku = 100)

        assertThat(result.successCount).isEqualTo(1_000)
        assertThat(result.finalStocks).containsExactlyElementsOf(List(10) { 0 })
        assertThat(result.orderCount).isEqualTo(result.successCount.toLong())
    }

    private fun runScenario(skuCount: Int, requestsPerSku: Int): ScenarioResult {
        orderRepository.deleteAll()
        skuRepository.deleteAll()

        val skuIds = (1..skuCount).map { index ->
            skuRepository.save(Sku(name = "테스트 SKU $index", stock = INITIAL_STOCK)).id!!
        }
        val requestCount = skuCount * requestsPerSku
        val executor = Executors.newFixedThreadPool(requestCount)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(requestCount)
        val success = AtomicInteger()

        repeat(requestCount) { requestIndex ->
            executor.submit {
                try {
                    startLatch.await()
                    val skuId = skuIds[requestIndex / requestsPerSku]
                    placeOrderUseCase.execute(userId = requestIndex.toLong(), skuId = skuId, quantity = 1)
                    success.incrementAndGet()
                } catch (_: Exception) {
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        startLatch.countDown()
        doneLatch.await()
        executor.shutdown()

        return ScenarioResult(
            successCount = success.get(),
            finalStocks = skuIds.map { skuId -> skuRepository.findById(skuId).orElseThrow().stock },
            orderCount = orderRepository.count()
        )
    }
}
