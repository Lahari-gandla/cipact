package com.ust.sdet.pact;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;

import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "oms-provider")
public class PosOmsConsumerPactTest {

    @Pact(consumer = "pos-consumer")
    public V4Pact getOrder(PactDslWithProvider builder) {

        return builder
                .given("Order exists")
                .uponReceiving("Get order details")
                .path("/orders/1001")
                .method("GET")
                .willRespondWith()
                .status(200)
                .body(new PactDslJsonBody()
                        .integerType("orderId", 1001)
                        .stringType("status", "CONFIRMED"))
                .toPact(V4Pact.class);
    }

    @Pact(consumer = "pos-consumer")
    public V4Pact createOrder(PactDslWithProvider builder) {

        return builder
                .given("inventory available for SKU-9")
                .uponReceiving("a request to create an order")
                .path("/orders")
                .method("POST")
                .headers(Map.of("Content-Type", "application/json"))
                .body(new PactDslJsonBody()
                        .stringType("sku", "SKU-9")
                        .integerType("qty", 1))
                .willRespondWith()
                .status(201)
                .matchHeader("Location",
                        "/orders/\\d+",
                        "/orders/777")
                .body(new PactDslJsonBody()
                        .integerType("id", 777)
                        .stringType("status", "PENDING"))
                .toPact(V4Pact.class);
    }

    @Pact(consumer = "pos-consumer")
    public V4Pact getInventory(PactDslWithProvider builder) {

        return builder
                .given("SKU-9 has stock")
                .uponReceiving("a request for SKU-9 inventory")
                .path("/inventory/SKU-9")
                .method("GET")
                .willRespondWith()
                .status(200)
                .body(new PactDslJsonBody()
                        .stringType("sku", "SKU-9")
                        .integerType("qty", 5))
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "getOrder")
    void getOrderTest(MockServer mockServer) {

        Response response =
                given()
                        .baseUri(mockServer.getUrl())
                        .when()
                        .get("/orders/1001")
                        .then()
                        .extract()
                        .response();

        assertEquals(200, response.statusCode());
        assertEquals(1001,
                response.jsonPath().getInt("orderId"));
        assertEquals("CONFIRMED",
                response.jsonPath().getString("status"));
    }

    @Test
    @PactTestFor(pactMethod = "createOrder")
    void createOrderTest(MockServer mockServer) {

        String requestBody = """
                {
                  "sku":"SKU-9",
                  "qty":1
                }
                """;

        Response response =
                given()
                        .baseUri(mockServer.getUrl())
                        .contentType("application/json")
                        .body(requestBody)
                        .when()
                        .post("/orders")
                        .then()
                        .extract()
                        .response();

        assertEquals(201, response.statusCode());

        assertNotNull(response.header("Location"));

        assertEquals("PENDING",
                response.jsonPath().getString("status"));
    }

    @Test
    @PactTestFor(pactMethod = "getInventory")
    void getInventoryTest(MockServer mockServer) {

        Response response =
                given()
                        .baseUri(mockServer.getUrl())
                        .when()
                        .get("/inventory/SKU-9")
                        .then()
                        .extract()
                        .response();

        assertEquals(200, response.statusCode());

        assertEquals("SKU-9",
                response.jsonPath().getString("sku"));

        assertTrue(
                response.jsonPath().getInt("qty") >= 0
        );
    }
}