package kr.kro.tomchi.tomserver.order

import kr.kro.tomchi.tomserver.payment.PaymentAttemptResult
import kr.kro.tomchi.tomserver.payment.PaymentGateway
import kr.kro.tomchi.tomserver.payment.PaymentMethod
import org.springframework.stereotype.Component
import java.util.*

@Component
class OrderUseCase(
    private val orderService: OrderService,
    private val paymentGateway: PaymentGateway
) {
    @Synchronized
    fun placeOrder(userId: Long, skuId: Long, quantity: Int): Order {
        return orderService.placeOrder(userId, skuId, quantity)
    }

    @Synchronized
    fun cancelOrder(orderId: Long) {
        orderService.cancelOrder(orderId)
    }

    @Synchronized
    fun attemptPayment(orderId: Long, paymentMethod: PaymentMethod): PaymentAttemptResult {
        val attemptKey = UUID.randomUUID()
        return paymentGateway.requestPayment(orderId = orderId, paymentMethod = paymentMethod, attemptKey = attemptKey)
    }

    @Synchronized
    fun confirmPayment(orderId: Long) {
        orderService.confirmPayment(orderId = orderId)
    }
}
