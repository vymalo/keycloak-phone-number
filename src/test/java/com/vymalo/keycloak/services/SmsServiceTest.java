package com.vymalo.keycloak.services;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
    }

    @Test
    void fallsBackToBasicAuthWhenTokenRequestFails() {
        String user = "user";
        String pass = "pass";
        String basicAuth = "Basic " + Base64.getEncoder().encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));

        server.stubFor(post(urlEqualTo("/token")).willReturn(aResponse().withStatus(500)));

        server.stubFor(post(urlEqualTo("/sms/send"))
                .withHeader("Authorization", equalTo(basicAuth))
                .willReturn(okJson("{\"status\":\"SENT\",\"hash\":\"xyz\"}")));

        System.setProperty("SMS_API_URL", server.baseUrl());
        System.setProperty("SMS_API_AUTH_USERNAME", user);
        System.setProperty("SMS_API_AUTH_PASSWORD", pass);
        System.setProperty("OAUTH2_CLIENT_ID", "client");
        System.setProperty("OAUTH2_CLIENT_SECRET", "secret");
        System.setProperty("OAUTH2_TOKEN_ENDPOINT", server.baseUrl() + "/token");

        Optional<String> hash = SmsService.getInstance().sendSmsAndGetHash("+1234567890");
        assertTrue(hash.isPresent());

        server.verify(postRequestedFor(urlEqualTo("/sms/send"))
                .withHeader("Authorization", equalTo(basicAuth)));
    }
}
