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
    private val orderUseCase: OrderUseCase,
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
        val order = orderUseCase.placeOrder(userId = 1, skuId = skuId, quantity = 3)
        val orderId = requireNotNull(order.id)

        // when
        orderUseCase.cancelOrder(orderId)

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
        val order = orderUseCase.placeOrder(userId = 1, skuId = skuId, quantity = 3)
        val orderId = requireNotNull(order.id)

        // when
        orderUseCase.cancelOrder(orderId)
        orderUseCase.cancelOrder(orderId)

        // then
        val actualSku = skuRepository.findByIdOrNull(skuId) ?: error("SKU를 찾을 수 없음")
        assertThat(actualSku.stock).isEqualTo(10)
    }

    @Test
    fun `결제 대기 주문을 확정하면 결제 완료 상태가 되고 재고는 복구되지 않는다`() {
        // given
        val testSku = Sku(name = "testSKU", stock = 10)
        val sku = skuRepository.save(testSku)
        val skuId = requireNotNull(testSku.id)
        val testOrder = Order(userId = 1, skuId = skuId, quantity = 4, status = OrderStatus.PENDING_PAYMENT)
        val order = orderRepository.save(testOrder)
        val orderId = requireNotNull(order.id)

        // when
        orderUseCase.confirmPayment(orderId)

        // then
        val actualOrder = orderRepository.findByIdOrNull(orderId)
        val actualSku = skuRepository.findByIdOrNull(skuId)
        requireNotNull(actualOrder)
        requireNotNull(actualSku)

        assertThat(actualSku.stock).isEqualTo(10)
        assertThat(actualOrder.status).isEqualTo(OrderStatus.PAYMENT_CONFIRMED)
    }

    @Test
    fun `취소된 주문은 결제 확정할 수 없다`() {
        TODO()
    }

    @Test
    fun `결제 완료된 주문은 취소할 수 없다`() {
        TODO()
    }
}
