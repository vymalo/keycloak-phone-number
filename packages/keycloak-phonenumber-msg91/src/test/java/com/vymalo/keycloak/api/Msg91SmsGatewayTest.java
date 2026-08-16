package com.vymalo.keycloak.msg91;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.vymalo.keycloak.services.SmsRequestContext;
import com.vymalo.keycloak.services.SmsServiceConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Msg91SmsGatewayTest {

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
    void sendsMsg91OtpWithQueryParamsAndJsonBody() {
        server.stubFor(post(urlPathEqualTo("/api/v5/otp"))
                .withQueryParam("otp", matching("\\d{6}"))
                .withQueryParam("mobile", matching("1234567890"))
                .withQueryParam("authkey", matching("abcxyz"))
                .withQueryParam("template_id", matching("abcdef"))
                .withRequestBody(equalToJson("{\"Param1\":\"realm1\"}"))
                .willReturn(okJson("{\"type\":\"success\",\"request_id\":\"rid-1\"}")));

        Msg91SmsGateway gateway = new Msg91SmsGateway(new SmsServiceConfig(
                server.baseUrl(),
                SmsServiceConfig.SmsAuthMode.NONE,
                null,
                null,
                "abcxyz",
                "abcdef",
                null
        ));

        Optional<String> hash = gateway.sendSmsAndGetHash(
                new SmsRequestContext("realm1", "client1", "127.0.0.1", "ua", "sid", "trace1", Map.of()),
                "+1 234 567 890"
        );
        assertTrue(hash.isPresent());
        assertEquals("rid-1", hash.get());

        server.verify(postRequestedFor(urlPathEqualTo("/api/v5/otp")));
    }
}
