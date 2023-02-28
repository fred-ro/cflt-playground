package org.example.consumer;

import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.MetricsReporter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;

public class LocalMetricsReporter implements MetricsReporter {

    private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * This is called when the reporter is first registered to initially register all existing metrics
     *
     * @param metrics All currently existing metrics
     */
    @Override
    public void init(List<KafkaMetric> metrics) {
        LOGGER.info("init started");
        for(KafkaMetric metric: metrics) {
            LOGGER.debug("init metrics: {}", metric.metricName());
        }
    }

    /**
     * This is called whenever a metric is updated or added
     *
     * @param metric
     */
    @Override
    public void metricChange(KafkaMetric metric) {
        LOGGER.debug("Change metrics: {}", metric.metricName());
        switch (metric.metricName().name()) {
            case "connection-count": updateConnectionCount((Double) metric.metricValue());
        }
    }

    /**
     * This is called whenever a metric is removed
     *
     * @param metric
     */
    @Override
    public void metricRemoval(KafkaMetric metric) {
        LOGGER.debug("Remove metrics: {}", metric.metricName());
    }

    /**
     * Called when the metrics repository is closed.
     */
    @Override
    public void close() {
        LOGGER.info("Close");
    }

    /**
     * Configure this class with the given key-value pairs
     *
     * @param configs
     */
    @Override
    public void configure(Map<String, ?> configs) {
        LOGGER.info("Config");
    }

    void updateConnectionCount(Double count) {
        LOGGER.info("Connection count = {}", count);
    }
}
