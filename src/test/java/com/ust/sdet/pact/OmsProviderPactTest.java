package com.ust.sdet.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

@Provider("oms-provider")
@PactBroker(url = "http://localhost:9292",
        enablePendingPacts = "true",
        providerTags = "main",
        includeWipPactsSince = "2026-06-26")
public class OmsProviderPactTest {

    @BeforeEach
    void before(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", 4010));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("Order exists")
    void orderExists() {
        System.out.println("Order exists");
    }

    @State("inventory available for SKU-9")
    void inventoryAvailable() {
        System.out.println("Inventory available for SKU-9");
    }

    @State("SKU-9 has stock")
    void skuHasStock() {
        System.out.println("SKU-9 has stock");
    }
}