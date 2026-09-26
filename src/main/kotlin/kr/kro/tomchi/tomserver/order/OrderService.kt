package kr.kro.tomchi.tomserver.order

import kr.kro.tomchi.tomserver.catalog.SkuRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val skuRepository: SkuRepository
) {
    @Transactional
    fun placeOrder(userId: Long, skuId: Long, quantity: Int): Order {
        // 조회
        val sku = skuRepository.findByIdOrNull(skuId)
            ?: throw IllegalArgumentException("없는 SKU: $skuId")

        // 조건
        if (sku.stock < quantity) {
            throw IllegalStateException("재고 부족: 남은 ${sku.stock}, 요청 $quantity")
        }

        // 재고 차감
        sku.stock -= quantity

        // 저장
        skuRepository.save(sku)

        // 주문 생성
        val order = Order(userId = userId, skuId = skuId, quantity = quantity)

        // 주문 저장
        return orderRepository.save(order)
    }

    @Transactional
    fun cancelOrder(orderId: Long) {
        val order = orderRepository.findByIdOrNull(orderId)
            ?: throw IllegalArgumentException("없는 주문: $orderId")

        if (order.status == OrderStatus.CANCELLED) {
            return
        }
        check(order.status == OrderStatus.PENDING_PAYMENT) {
            "취소할 수 없는 주문 상태: ${order.status}"
        }

        val sku = skuRepository.findByIdOrNull(order.skuId)
            ?: throw IllegalArgumentException("없는 SKU: ${order.skuId}")

        sku.stock += order.quantity
        order.status = OrderStatus.CANCELLED
    }

    @Transactional
    fun confirmPayment(orderId: Long) {
        val order = orderRepository.findByIdOrNull(orderId)
            ?: throw IllegalArgumentException("없는 주문: $orderId")

        if (order.status == OrderStatus.PAYMENT_CONFIRMED) {
            return
        }
        check(order.status == OrderStatus.PENDING_PAYMENT) {
            "결제 확정할 수 없는 주문 상태: ${order.status}"
        }

        order.status = OrderStatus.PAYMENT_CONFIRMED
    }
}
