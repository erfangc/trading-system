package com.example.tradingsystem.oms

import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/v1")
class OrderManagementController(
    private val getOrdersService: GetOrdersService,
    private val createOrderService: CreateOrderService,
) {

    @PostMapping("orders")
    fun createOrder(
        @RequestParam(required = false) accountNumber: String? = null,
        @RequestParam(required = false) securityId: String? = null,
        @RequestParam(required = false) type: String? = null,
        @RequestParam(required = false) timeInForce: String? = null,
        @RequestParam(required = false) limitPrice: Double? = null,
        @RequestParam(required = false) qty: Double? = null,
    ): Order {
        val order = createOrderService.createOrder(
            Order(
                accountNumber = accountNumber,
                securityId = securityId,
                type = type,
                timeInForce = timeInForce,
                limitPrice = limitPrice,
                qty = qty,
                timestamp = Instant.now(),
            )
        )
        return order
    }

    @GetMapping("orders")
    fun getOrders(@RequestParam accountNumber: String): List<Order> {
        return getOrdersService.getOrders(accountNumber)
    }

    @GetMapping("orders/{orderId}")
    fun getOrder(@PathVariable orderId: String): Order? {
        return getOrdersService.getOrder(orderId)
    }

}