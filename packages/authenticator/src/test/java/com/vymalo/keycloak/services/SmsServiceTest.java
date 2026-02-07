package com.vymalo.keycloak.services;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
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

        SmsService service = new SmsService(new SmsServiceConfig(
                server.baseUrl(),
                SmsServiceConfig.SmsAuthMode.AUTO,
                server.baseUrl() + "/token",
                user,
                pass,
                "client",
                "secret"
        ));

        Optional<String> hash = service.sendSmsAndGetHash(
                new SmsRequestContext("test-realm", "test-client", "127.0.0.1", "test-agent", "test-session", "test-trace", Map.of()),
                "+1234567890"
        );
        assertTrue(hash.isPresent());

        server.verify(postRequestedFor(urlEqualTo("/sms/send"))
                .withHeader("Authorization", equalTo(basicAuth)));
    }
}
