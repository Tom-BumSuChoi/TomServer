package kr.kro.tomchi.tomserver.order

import kr.kro.tomchi.tomserver.catalog.Sku
import kr.kro.tomchi.tomserver.catalog.SkuRepository
import kr.kro.tomchi.tomserver.payment.PaymentAttemptResult
import kr.kro.tomchi.tomserver.payment.PaymentGateway
import kr.kro.tomchi.tomserver.payment.PaymentMethod
import kr.kro.tomchi.tomserver.support.IntegrationTestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.util.*

@MockitoBean(types = [PaymentGateway::class])
@SpringBootTest
class OrderServiceTest @Autowired constructor(
    private val orderUseCase: OrderUseCase,
    private val orderService: OrderService,
    private val orderRepository: OrderRepository,
    private val skuRepository: SkuRepository,
    private val paymentGateway: PaymentGateway
) : IntegrationTestBase() {

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
        orderService.confirmPayment(orderId)

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
        // given
        val sku = skuRepository.save(Sku(name = "testSKU", stock = 10))
        val skuId = requireNotNull(sku.id)
        val order = orderRepository.save(
            Order(userId = 1, skuId = skuId, quantity = 4, status = OrderStatus.CANCELLED)
        )
        val orderId = requireNotNull(order.id)

        // when
        assertThrows<IllegalStateException> {
            orderService.confirmPayment(orderId)
        }

        // then
        val actualOrder = orderRepository.findByIdOrNull(orderId) ?: error("주문을 찾을 수 없음")
        val actualSku = skuRepository.findByIdOrNull(skuId) ?: error("SKU를 찾을 수 없음")
        assertThat(actualOrder.status).isEqualTo(OrderStatus.CANCELLED)
        assertThat(actualSku.stock).isEqualTo(10)
    }

    @Test
    fun `결제 완료된 주문은 취소할 수 없다`() {
        // given
        val sku = skuRepository.save(Sku(name = "testSKU", stock = 10))
        val skuId = requireNotNull(sku.id)
        val order = orderRepository.save(
            Order(userId = 1, skuId = skuId, quantity = 4, status = OrderStatus.PAYMENT_CONFIRMED)
        )
        val orderId = requireNotNull(order.id)

        // when
        assertThrows<IllegalStateException> {
            orderUseCase.cancelOrder(orderId)
        }

        // then
        val actualOrder = orderRepository.findByIdOrNull(orderId) ?: error("주문을 찾을 수 없음")
        val actualSku = skuRepository.findByIdOrNull(skuId) ?: error("SKU를 찾을 수 없음")
        assertThat(actualOrder.status).isEqualTo(OrderStatus.PAYMENT_CONFIRMED)
        assertThat(actualSku.stock).isEqualTo(10)
    }

    @Test
    fun `결제 시도가 실패해도 선점 만료 전에는 주문과 재고를 유지한다`() {
        // given
        val sku = skuRepository.save(Sku(name = "testSKU", stock = 10))
        val skuId = requireNotNull(sku.id)
        val order = orderUseCase.placeOrder(userId = 1, skuId = skuId, quantity = 3)
        val orderId = requireNotNull(order.id)
        Mockito.doReturn(PaymentAttemptResult.DECLINED)
            .`when`(paymentGateway)
            .requestPayment(
                Mockito.eq(orderId),
                Mockito.eq(PaymentMethod.CREDIT_CARD) ?: PaymentMethod.CREDIT_CARD,
                Mockito.any(UUID::class.java) ?: UUID.randomUUID()
            )


        // when
        val result = orderUseCase.attemptPayment(orderId = orderId, paymentMethod = PaymentMethod.CREDIT_CARD)

        // then
        assertThat(result).isEqualTo(PaymentAttemptResult.DECLINED)
        val actualSku = skuRepository.findByIdOrNull(skuId) ?: error("SKU를 찾을 수 없음 SKU ID: $skuId")
        val actualOrder = orderRepository.findByIdOrNull(orderId) ?: error("Order를 찾을 수 없음: $orderId")
        assertThat(actualOrder.status).isEqualTo(OrderStatus.PENDING_PAYMENT)
        assertThat(actualSku.stock).isEqualTo(7)
    }

    @Test
    fun `결제 실패 후 다른 수단으로 재시도해 성공하면 기존 주문을 확정하고 재고를 중복 차감하지 않는다`() {
        // given
        val sku = skuRepository.save(Sku(name = "testSKU", stock = 10))
        val skuId = requireNotNull(sku.id)
        val order = orderUseCase.placeOrder(userId = 1, skuId = skuId, quantity = 4)
        val orderId = requireNotNull(order.id)
        Mockito.doReturn(PaymentAttemptResult.DECLINED)
            .`when`(paymentGateway)
            .requestPayment(
                Mockito.eq(orderId),
                Mockito.eq(PaymentMethod.CREDIT_CARD) ?: PaymentMethod.CREDIT_CARD,
                Mockito.any(UUID::class.java) ?: UUID.randomUUID()
            )
        Mockito.doReturn(PaymentAttemptResult.SUCCESS)
            .`when`(paymentGateway)
            .requestPayment(
                Mockito.eq(orderId),
                Mockito.eq(PaymentMethod.DEBIT_CARD) ?: PaymentMethod.DEBIT_CARD,
                Mockito.any(UUID::class.java) ?: UUID.randomUUID()
            )

        // when
        val firstResult = orderUseCase.attemptPayment(orderId, PaymentMethod.CREDIT_CARD)
        val retryResult = orderUseCase.attemptPayment(orderId, PaymentMethod.DEBIT_CARD)

        // then
        val actualOrder = orderRepository.findByIdOrNull(orderId) ?: error("주문을 찾을 수 없음")
        val actualSku = skuRepository.findByIdOrNull(skuId) ?: error("SKU를 찾을 수 없음")
        assertThat(firstResult).isEqualTo(PaymentAttemptResult.DECLINED)
        assertThat(retryResult).isEqualTo(PaymentAttemptResult.SUCCESS)
        assertThat(actualOrder.status).isEqualTo(OrderStatus.PAYMENT_CONFIRMED)
        assertThat(actualSku.stock).isEqualTo(6)
    }

    @Test
    fun `결제 결과가 불확실하면 주문과 재고 선점을 유지한다`() {
        // given
        val sku = skuRepository.save(Sku(name = "testSKU", stock = 10))
        val skuId = requireNotNull(sku.id)
        val order = orderUseCase.placeOrder(userId = 1, skuId = skuId, quantity = 4)
        val orderId = requireNotNull(order.id)
        Mockito.doReturn(PaymentAttemptResult.UNKNOWN)
            .`when`(paymentGateway)
            .requestPayment(
                orderId = Mockito.eq(orderId),
                paymentMethod = Mockito.eq(PaymentMethod.CREDIT_CARD) ?: PaymentMethod.CREDIT_CARD,
                attemptKey = Mockito.any(UUID::class.java) ?: UUID.randomUUID()
            )

        // when
        val paymentResult = orderUseCase.attemptPayment(orderId, PaymentMethod.CREDIT_CARD)

        // then
        val actualOrder = orderRepository.findByIdOrNull(orderId) ?: error("주문을 찾을 수 없음")
        val actualSku = skuRepository.findByIdOrNull(skuId) ?: error("SKU를 찾을 수 없음")
        assertThat(paymentResult).isEqualTo(PaymentAttemptResult.UNKNOWN)
        assertThat(actualOrder.status).isEqualTo(OrderStatus.PENDING_PAYMENT)
        assertThat(actualSku.stock).isEqualTo(6)
    }

    @Test
    fun `결제하지 않은 주문의 선점이 만료되면 주문을 취소하고 재고를 복구한다`() {
        TODO()
    }
}
