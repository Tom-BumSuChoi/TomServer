package kr.kro.tomchi.tomserver.catalog

import org.springframework.data.jpa.repository.JpaRepository

interface SkuRepository : JpaRepository<Sku, Long>