package kr.kro.tomchi.tomserver.order

import kr.kro.tomchi.tomserver.catalog.Sku
import kr.kro.tomchi.tomserver.catalog.SkuRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.mysql.MySQLContainer

@SpringBootTest
@Testcontainers
class OrderServiceTest @Autowired constructor(
    private val placeOrderUseCase: PlaceOrderUseCase,
    private val orderRepository: OrderRepository,
    private val skuRepository: SkuRepository
) {

    companion object {
        @Container
        @JvmStatic
        val mysql = MySQLContainer("mysql:8.0")

        @DynamicPropertySource
        @JvmStatic
        fun datasourceProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { mysql.jdbcUrl }
            registry.add("spring.datasource.username") { mysql.username }
            registry.add("spring.datasource.password") { mysql.password }
        }
    }

    @Test
    fun `주문 취소 시 재고를 복구한다`() {
        // given
        val sku = skuRepository.save(Sku(name = "testSKU", stock = 10))
        val skuId = requireNotNull(sku.id)
        val order = placeOrderUseCase.execute(userId = 1, skuId = skuId, quantity = 3)
        val orderId = requireNotNull(order.id)

        // when
        placeOrderUseCase.cancelOrder(orderId)

        // then
        val actualSku = skuRepository.findByIdOrNull(skuId) ?: error("SKU를 찾을 수 없음")
        val actualOrder = orderRepository.findByIdOrNull(orderId) ?: error("주문을 찾을 수 없음")

        assertThat(actualSku.stock).isEqualTo(10)
        assertThat(actualOrder.status).isEqualTo(OrderStatus.CANCELLED)
    }

    @Test
    fun `이미 취소된 주문을 다시 취소해도 재고는 한 번만 복구한다`() {
        // given
        val sku = skuRepository.save(Sku(name = "testSKU", stock = 10))
        val skuId = requireNotNull(sku.id)
        val order = placeOrderUseCase.execute(userId = 1, skuId = skuId, quantity = 3)
        val orderId = requireNotNull(order.id)

        // when
        placeOrderUseCase.cancelOrder(orderId)
        placeOrderUseCase.cancelOrder(orderId)

        // then
        val actualSku = skuRepository.findByIdOrNull(skuId) ?: error("SKU를 찾을 수 없음")
        assertThat(actualSku.stock).isEqualTo(10)
    }
}
