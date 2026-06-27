package com.ust.sdet.pact;


import java.util.Map;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(PactConsumerTestExt.class)

@PactTestFor(providerName = "oms-provider",pactVersion = PactSpecVersion.V4)

class PosOmsConsumerPactTest {

    @Pact(provider = "oms-provider", consumer = "pos-consumer")
    V4Pact getOrder(PactDslWithProvider builder) {
        return builder
                .given("Order 245 exists")
                .uponReceiving("a request for order 123")
                .path("/order/123")
                .method("GET")
                .willRespondWith()
                .status(200)
                .matchHeader("Content-Type", "application/json(;.*)?", "application/json")
                .body(new PactDslJsonBody()
                        .integerType("orderId", 123)
                        .stringType("status", "CONFIRMED")
                        .numberType("total", 42.0))
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "getOrder")
    void testGetOrder(MockServer mockServer) {

        var order = new OmsClient(mockServer.getUrl()).getOrder(123);
        assertEquals(123, order.orderId());
        assertEquals("CONFIRMED", order.status());
        assertEquals(42.0, order.total(),0);

    }

    @Pact(provider = "oms-provider", consumer = "pos-consumer")
    V4Pact createOrder(PactDslWithProvider builder){
        return builder
                .given("Creating a new order")
                .uponReceiving("Upon New order Creating")
                .body( new PactDslJsonBody()
                        .integerType("statusCode",0)
                        .integerType("orderId",0)
                        .stringType("status","NEW")
                        .decimalType("total",2000.0)
                )
                .path("/orders/")
                .headers(Map.of("Content-Type", "application/json"))
                .method("POST")
                .willRespondWith()
                .status(201)
                .matchHeader("Content-Type", "application/json(;.*)?", "application/json")
                .body(
                        new PactDslJsonBody()
                                .integerType("statusCode",201)
                                .integerType("orderId",101)
                                .stringType("status","CREATED")
                                .integerType("total",2000)
                ).toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "createOrder")
    void testcreateOrder(MockServer mockServer){
        var newOrder = new OmsClient.Order(201,0,"NEW",2000);
        var createdorder = new OmsClient(mockServer.getUrl()).createOrder(newOrder);
        assertEquals(createdorder.orderId(), 101);
        assertEquals(createdorder.status(), "CREATED");
        assertEquals(newOrder.total(), createdorder.total(),0);
    }

    @Pact(provider = "oms-provider", consumer = "pos-consumer")
    V4Pact getInventory(PactDslWithProvider builder){
        return builder
                .given("SKU-9 has Stock")
                .uponReceiving("a request for SKU-9 inventory")
                .path("/inventory/SKU-9")
                .method("GET")
                .willRespondWith()
                .status(200)
                .body(
                        new PactDslJsonBody()
                                .stringType("sku","SKU-9")
                                .integerType("qty",5)
                ).toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "getInventory")
    void testgetInventory(MockServer mockServer){
        var inventoryResponse = new OmsClient(mockServer.getUrl()).getInventory();
        assertEquals(inventoryResponse.path("sku"), "SKU-9");
        assertEquals((int)inventoryResponse.path("qty"), 5);
    }
}
