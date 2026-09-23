package kr.kro.tomchi.tomserver.order

import jakarta.persistence.*

@Entity
@Table(name = "orders")
class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var userId: Long = 0,
    var skuId: Long = 0,
    var quantity: Int = 0
)