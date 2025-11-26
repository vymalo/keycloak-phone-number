package com.vymalo.keycloak.services;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.vymalo.keycloak.constants.ConfigKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmsServiceTest {

    private WireMockServer server;

    @BeforeEach
    void setup() {
        server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        server.start();
    }

    @AfterEach
    void teardown() {
        server.stop();
        System.clearProperty(ConfigKey.CONF_PRP_SMS_URL);
    }

    @Test
    void sendSmsSuccessfully() {
        // Setup WireMock to receive the request
        // URL pattern: /Accounts/{AUTH_TOKEN}/SMSes/
        // AUTH_TOKEN is hardcoded in SmsService: ed3dxCIPJ0WMD1ZUbvz6gIOsfTpQsV5pKNVrEgnS
        String expectedPath = "/Accounts/ed3dxCIPJ0WMD1ZUbvz6gIOsfTpQsV5pKNVrEgnS/SMSes/";
        
        server.stubFor(post(urlEqualTo(expectedPath))
                .willReturn(okJson("{\"Success\":\"True\",\"Message\":\"Sent\",\"JobId\":\"123\"}")));

        // Configure SmsService to use WireMock URL
        // IMPORTANT: This relies on SmsService class NOT being initialized yet.
        // If it was initialized, we can't change the URL easily without reflection or refactoring.
        System.setProperty(ConfigKey.CONF_PRP_SMS_URL, server.baseUrl());

        // Trigger the service
        Optional<String> hash = SmsService.getInstance().sendSmsAndGetHash("+1234567890");

        // Verify
        assertTrue(hash.isPresent());

        server.verify(postRequestedFor(urlEqualTo(expectedPath))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(matchingJsonPath("$.Text", containing("iHeal")))
                .withRequestBody(matchingJsonPath("$.Number", equalTo("+1234567890")))
                .withRequestBody(matchingJsonPath("$.SenderId", equalTo("iheal"))));
    }
}
