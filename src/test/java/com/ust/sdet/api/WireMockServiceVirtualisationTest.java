package com.ust.sdet.api;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WireMockServiceVirtualisationTest {

    @RegisterExtension
    static WireMockExtension wireMock =
            WireMockExtension.newInstance()
                    .options(wireMockConfig())
                    .build();

    @Test
    void inventoryCheck() {

        wireMock.stubFor(get(urlEqualTo("/inventory/SKU-9"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withFixedDelay(3000)
                        .withBody("""
                                {
                                  "sku":"SKU-9",
                                  "qty":5
                                }
                                """)));

        wireMock.stubFor(get(urlEqualTo("/inventory/SKU-0"))
                .willReturn(aResponse()
                        .withStatus(409)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "error":"OUT_OF_STOCK"
                                }
                                """)));

        given()
                .baseUri(wireMock.getRuntimeInfo().getHttpBaseUrl())
                .when()
                .get("/inventory/SKU-9")
                .then()
                .statusCode(200)
                .header("Content-Type", equalTo("application/json"))
                .body("sku", equalTo("SKU-9"))
                .body("qty", equalTo(5));

        given()
                .baseUri(wireMock.getRuntimeInfo().getHttpBaseUrl())
                .when()
                .get("/inventory/SKU-0")
                .then()
                .statusCode(409)
                .header("Content-Type", equalTo("application/json"))
                .body("error", equalTo("OUT_OF_STOCK"));

        wireMock.verify(
                exactly(1),
                getRequestedFor(urlEqualTo("/inventory/SKU-9"))
        );

        wireMock.verify(
                exactly(1),
                getRequestedFor(urlEqualTo("/inventory/SKU-0"))
        );
    }

    @Test
    void timeoutTest() throws Exception {

        wireMock.stubFor(get(urlEqualTo("/orders/slow"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(3000)
                        .withBody("Success")));

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        wireMock.getRuntimeInfo().getHttpBaseUrl()
                                + "/orders/slow"))
                .timeout(Duration.ofSeconds(1))
                .GET()
                .build();

        assertThrows(
                HttpTimeoutException.class,
                () -> client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString())
        );

        HttpRequest request2 = HttpRequest.newBuilder()
                .uri(URI.create(
                        wireMock.getRuntimeInfo().getHttpBaseUrl()
                                + "/orders/slow"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(
                        request2,
                        HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("Success", response.body());

        wireMock.verify(
                exactly(2),
                getRequestedFor(urlEqualTo("/orders/slow"))
        );
    }

    @Test
    void fulfilmentScenario() {

        wireMock.stubFor(get(urlEqualTo("/orders/42"))
                .inScenario("fulfilment")
                .whenScenarioStateIs(STARTED)
                .willSetStateTo("CONFIRMED")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "status":"PENDING"
                                }
                                """)));

        wireMock.stubFor(get(urlEqualTo("/orders/42"))
                .inScenario("fulfilment")
                .whenScenarioStateIs("CONFIRMED")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "status":"CONFIRMED"
                                }
                                """)));

        given()
                .baseUri(wireMock.getRuntimeInfo().getHttpBaseUrl())
                .when()
                .get("/orders/42")
                .then()
                .statusCode(200)
                .header("Content-Type", equalTo("application/json"))
                .body("status", equalTo("PENDING"));

        given()
                .baseUri(wireMock.getRuntimeInfo().getHttpBaseUrl())
                .when()
                .get("/orders/42")
                .then()
                .statusCode(200)
                .header("Content-Type", equalTo("application/json"))
                .body("status", equalTo("CONFIRMED"));

        wireMock.verify(
                exactly(2),
                getRequestedFor(urlEqualTo("/orders/42"))
        );
    }
}