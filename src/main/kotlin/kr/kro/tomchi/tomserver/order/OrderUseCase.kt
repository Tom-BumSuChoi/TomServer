package kr.kro.tomchi.tomserver.order

import org.springframework.stereotype.Component

@Component
class OrderUseCase(
    private val orderService: OrderService
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
    fun confirmPayment(orderId: Long) {
        orderService.confirmPayment(orderId = orderId)
    }
}
