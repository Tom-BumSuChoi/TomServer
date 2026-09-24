package kr.kro.tomchi.tomserver.order

import kr.kro.tomchi.tomserver.catalog.SkuRepository
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
        val sku = skuRepository.findById(skuId)
            .orElseThrow { IllegalArgumentException("없는 SKU: $skuId") }

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
}