package org.example.consumer;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.errors.RecordDeserializationException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class Consumer {
    private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    private static final String GROUP_ID = "Consumer";
    private static final String KAFKA_TOPIC = "topic";
    private static final Duration POLL_TIMEOUT = Duration.ofMillis(1000);
    private static final Duration CONNECTION_LOSS_TIMEOUT = Duration.ofSeconds(10);

    @Nullable
    private MetricName connectionCountMetricName = null;

    @Nullable
    private Instant lostConnectionInstant = null;

    private static Properties settings() {
        final Properties settings = new Properties();
        settings.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        settings.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        settings.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        settings.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 100);
        settings.put(ConsumerConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 1000);
        settings.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        settings.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        settings.put(ConsumerConfig.METRIC_REPORTER_CLASSES_CONFIG, LocalMetricsReporter.class.getName());
        return settings;
    }

    public static void main(String[] args) {
        Consumer consumer = new Consumer();
        consumer.run(settings());
    }

    private void run(Properties settings) {
        LOGGER.info("Starting consumer");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(settings)) {
            // Subscribe to our topic
            LOGGER.info("Subscribing to topic " + KAFKA_TOPIC);
            consumer.subscribe(List.of(KAFKA_TOPIC));
            LOGGER.info("Subscribed !");
            //noinspection InfiniteLoopStatement
            while (true) {
                try {
                    final var records = consumer.poll(POLL_TIMEOUT);
                    LOGGER.info("poll() returned {} records", records.count());
                    checkMetrics(consumer.metrics());
                    for (var record : records) {
                        LOGGER.info("Fetch record key={} value={}", record.key(), record.value());
                        // Any processing
                    }
                } catch (RecordDeserializationException re) {
                    long offset = re.offset();
                    Throwable t = re.getCause();
                    LOGGER.error("Failed to consume at partition={} offset={}", re.topicPartition().partition(), offset, t);
                    LOGGER.info("Skipping offset={}", offset);
                    consumer.seek(re.topicPartition(), offset + 1);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Poll loop exiting", e);
        } finally {
            LOGGER.info("Closing consumer");
        }
    }

    private void checkMetrics(Map<MetricName, ? extends Metric> metrics) throws ConnectionTimeoutException {
        Double connectionCount = getConnectionCount(metrics);
        LOGGER.debug("ConnectionCount={}", connectionCount);
        if (connectionCount == 0.0) {
            Instant now = Instant.now();
            if (lostConnectionInstant == null) {
                lostConnectionInstant = now;
            } else {
                if (CONNECTION_LOSS_TIMEOUT.compareTo(Duration.between(lostConnectionInstant, now)) <= 0) {
                    throw new ConnectionTimeoutException("No connection for more than connection timeout");
                }
            }
        } else {
            lostConnectionInstant = null;
        }
    }

    private Double getConnectionCount(Map<MetricName, ? extends Metric> metrics) {
        if (connectionCountMetricName == null) {
            var entry = metrics.entrySet()
                    .stream()
                    .filter(e -> e.getKey().name().equals("connection-count"))
                    .findFirst();
            if (entry.isPresent()) {
                connectionCountMetricName = entry.get().getKey();
                return (Double) entry.get().getValue().metricValue();
            } else {
                return 0.0;
            }
        } else {
            var countMetric = metrics.get(connectionCountMetricName);
            if (countMetric != null) {
                return (Double) countMetric.metricValue();
            } else {
                return 0.0;
            }
        }
    }
}

