package com.vymalo.keycloak.sns_sqs;

import com.vymalo.keycloak.services.AbstractCodeGeneratingSmsGateway;
import com.vymalo.keycloak.services.SmsRequestContext;
import com.vymalo.keycloak.services.SmsServiceConfig;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SnsSqsSmsGateway extends AbstractCodeGeneratingSmsGateway {
    private static final Logger LOGGER = Logger.getLogger(SnsSqsSmsGateway.class.getName());
    private static final String PROVIDER = "SNS_SQS";

    private final String endpointOverride;
    private final String region;
    private final String topicArn;
    private final String queueUrl;

    public SnsSqsSmsGateway(SmsServiceConfig config) {
        super(PROVIDER);
        this.endpointOverride = config.smsApiUrl();
        this.region = readEnvOrProperty("AWS_REGION", "us-east-1");
        this.topicArn = readEnvOrProperty("SMS_SNS_TOPIC_ARN", null);
        this.queueUrl = readEnvOrProperty("SMS_SQS_QUEUE_URL", null);
    }

    @Override
    protected boolean dispatch(String payload, SmsRequestContext requestContext, String phoneNumber, String code, String hash) {
        if (notBlank(topicArn)) {
            return dispatchToSns(payload);
        }
        if (notBlank(queueUrl)) {
            return dispatchToSqs(payload);
        }

        LOGGER.severe("Neither SMS_SNS_TOPIC_ARN nor SMS_SQS_QUEUE_URL is configured");
        return false;
    }

    private boolean dispatchToSns(String payload) {
        try {
            var builder = SnsClient.builder().region(Region.of(region));
            if (isHttpUri(endpointOverride)) {
                builder = builder.endpointOverride(URI.create(endpointOverride));
            }

            try (var snsClient = builder.build()) {
                snsClient.publish(PublishRequest.builder().topicArn(topicArn).message(payload).build());
                return true;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to dispatch SMS event via SNS", e);
            return false;
        }
    }

    private boolean dispatchToSqs(String payload) {
        try {
            var builder = SqsClient.builder().region(Region.of(region));
            if (isHttpUri(endpointOverride)) {
                builder = builder.endpointOverride(URI.create(endpointOverride));
            }

            try (var sqsClient = builder.build()) {
                sqsClient.sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageBody(payload).build());
                return true;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to dispatch SMS event via SQS", e);
            return false;
        }
    }

    private static String readEnvOrProperty(String key, String defaultValue) {
        final var fromEnv = System.getenv(key);
        if (notBlank(fromEnv)) {
            return fromEnv.trim();
        }

        final var fromProperty = System.getProperty(key);
        if (notBlank(fromProperty)) {
            return fromProperty.trim();
        }
        return defaultValue;
    }

    private static boolean isHttpUri(String value) {
        return value != null && (value.startsWith("http://") || value.startsWith("https://"));
    }

    private static boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
