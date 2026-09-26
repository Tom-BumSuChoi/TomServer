package kr.kro.tomchi.tomserver.payment

import java.util.*

interface PaymentGateway {
    fun requestPayment(orderId: Long, paymentMethod: PaymentMethod, attemptKey: UUID): PaymentAttemptResult
}

enum class PaymentMethod {
    CREDIT_CARD,
    DEBIT_CARD
}

enum class PaymentAttemptResult {
    SUCCESS,
    DECLINED,
    UNKNOWN
}