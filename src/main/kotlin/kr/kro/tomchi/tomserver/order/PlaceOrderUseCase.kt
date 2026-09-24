package kr.kro.tomchi.tomserver.order

import org.springframework.stereotype.Component

@Component
class PlaceOrderUseCase(
    private val orderService: OrderService
) {
    // 락이 트랜잭션보다 바깥에 있어야 한다.
    // @Transactional 은 프록시가 메서드 호출을 감싸므로, 같은 클래스 안에서 락을 걸면
    // 락이 먼저 풀리고 커밋이 나중이 되어 다음 스레드가 미커밋 재고를 읽는다.
    @Synchronized
    fun execute(userId: Long, skuId: Long, quantity: Int): Order {
        return orderService.placeOrder(userId, skuId, quantity)
    }
}
