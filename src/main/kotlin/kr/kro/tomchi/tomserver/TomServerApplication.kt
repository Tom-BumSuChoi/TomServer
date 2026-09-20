package kr.kro.tomchi.tomserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TomServerApplication

fun main(args: Array<String>) {
    runApplication<TomServerApplication>(*args)
}
