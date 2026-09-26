package kr.kro.tomchi.tomserver.order

import org.springframework.stereotype.Component

@Component
class PlaceOrderUseCase(
    private val orderService: OrderService
) {
    @Synchronized
    fun execute(userId: Long, skuId: Long, quantity: Int): Order {
        return orderService.placeOrder(userId, skuId, quantity)
    }

    @Synchronized
    fun cancelOrder(orderId: Long) {
        orderService.cancelOrder(orderId)
    }
}
