package com.vymalo.keycloak.kafka;

import com.vymalo.keycloak.services.AbstractCodeGeneratingSmsGateway;
import com.vymalo.keycloak.services.SmsRequestContext;
import com.vymalo.keycloak.services.SmsServiceConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class KafkaSmsGateway extends AbstractCodeGeneratingSmsGateway {
    private static final Logger LOGGER = Logger.getLogger(KafkaSmsGateway.class.getName());
    private static final String PROVIDER = "KAFKA";
    private static final String DEFAULT_TOPIC = "keycloak.sms.events";

    private final String bootstrapServers;
    private final String topic;

    public KafkaSmsGateway(SmsServiceConfig config) {
        super(PROVIDER);
        this.bootstrapServers = config.smsApiUrl();
        this.topic = readEnvOrProperty("SMS_KAFKA_TOPIC", DEFAULT_TOPIC);
    }

    @Override
    protected boolean dispatch(String payload, SmsRequestContext requestContext, String phoneNumber, String code, String hash) {
        final var props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");

        try (var producer = new KafkaProducer<String, String>(props)) {
            producer.send(new ProducerRecord<>(topic, phoneNumber, payload)).get(10, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to dispatch SMS event via Kafka", e);
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

    private static boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
