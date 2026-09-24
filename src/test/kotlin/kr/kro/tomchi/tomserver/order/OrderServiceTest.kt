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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

@SpringBootTest
@Testcontainers
class OrderServiceTest @Autowired constructor(
    private val orderService: OrderService,
    private val orderRepository: OrderRepository,
    private val skuRepository: SkuRepository
) {

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
    fun `재고 100에 1,000건을 동시에 주문하면 초과 판매가 난다`() {
        // given
        val skuId = skuRepository.save(Sku(name = "에티오피아 예가체프", stock = INITIAL_STOCK)).id!!

        val executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS)
        val startLatch = CountDownLatch(1)                    // 출발선
        val doneLatch = CountDownLatch(CONCURRENT_REQUESTS)   // 결승선
        val success = AtomicInteger()
        val failures = ConcurrentHashMap<String, AtomicInteger>()

        repeat(CONCURRENT_REQUESTS) { i ->
            executor.submit {
                try {
                    startLatch.await()
                    orderService.placeOrder(userId = i.toLong(), skuId = skuId, quantity = 1)
                    success.incrementAndGet()
                } catch (e: Exception) {
                    val reason = e::class.simpleName ?: "Unknown"
                    failures.computeIfAbsent(reason) { AtomicInteger() }.incrementAndGet()
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        // when — 묶어둔 스레드를 한 번에 푼다
        val elapsed = measureTimeMillis {
            startLatch.countDown()
            doneLatch.await()
        }
        executor.shutdown()

        // then
        val finalStock = skuRepository.findById(skuId).orElseThrow().stock
        val orderCount = orderRepository.count()

        println("성공 건수: ${success.get()}")
        println("최종 재고: $finalStock")
        println("주문 행 수: $orderCount")
        println("실패: ${failures.mapValues { it.value.get() }}")
        println("소요 시간: ${elapsed}ms")

        assertThat(success.get()).isLessThanOrEqualTo(INITIAL_STOCK)
        assertThat(finalStock).isEqualTo(INITIAL_STOCK - success.get())
    }
}
