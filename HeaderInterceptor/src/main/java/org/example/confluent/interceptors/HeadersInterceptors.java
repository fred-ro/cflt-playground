package org.example.confluent.interceptors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class HeadersInterceptors<V> implements ConsumerInterceptor<byte[], V> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * This is called just before the records are returned by
     * {@link KafkaConsumer#poll(Duration)}
     * <p>
     * This method is allowed to modify consumer records, in which case the new records will be
     * returned. There is no limitation on number of records that could be returned from this
     * method. I.e., the interceptor can filter the records or generate new records.
     * <p>
     * Any exception thrown by this method will be caught by the caller, logged, but not propagated to the client.
     * <p>
     * Since the consumer may run multiple interceptors, a particular interceptor's onConsume() callback will be called
     * in the order specified by {@link ConsumerConfig#INTERCEPTOR_CLASSES_CONFIG}.
     * The first interceptor in the list gets the consumed records, the following interceptor will be passed the records returned
     * by the previous interceptor, and so on. Since interceptors are allowed to modify records, interceptors may potentially get
     * the records already modified by other interceptors. However, building a pipeline of mutable interceptors that depend on the output
     * of the previous interceptor is discouraged, because of potential side-effects caused by interceptors potentially failing
     * to modify the record and throwing an exception. If one of the interceptors in the list throws an exception from onConsume(),
     * the exception is caught, logged, and the next interceptor is called with the records returned by the last successful interceptor
     * in the list, or otherwise the original consumed records.
     *
     * @param records records to be consumed by the client or records returned by the previous interceptors in the list.
     * @return records that are either modified by the interceptor or same as records passed to this method.
     */
    @Override
    public ConsumerRecords<byte[], V> onConsume(ConsumerRecords<byte[], V> records) {
        LOGGER.debug("onConsume() processing records");
        Map<TopicPartition, List<ConsumerRecord<byte[], V>>> result = new HashMap<>();
        for (var record : records) {
            LOGGER.debug("onConsume() converting record key={}", record.key());
            var newRecord = processRecordHeaders(record);
            TopicPartition tp = new TopicPartition(record.topic(), record.partition());
            result.computeIfAbsent(tp, k -> new ArrayList<>()).add(newRecord);
        }
        return new ConsumerRecords<>(result);
    }


    protected ConsumerRecord<byte[], V> processRecordHeaders(ConsumerRecord<byte[], V> record) {
        var hh = record.headers().toArray();
        if (hh.length == 0) {
            LOGGER.debug("processRecordHeaders() topic={} partition={} offset={}: no headers", record.topic(), record.partition(), record.offset());
            return record;
        }
        var headers = Arrays.stream(hh).collect(Collectors.toMap(Header::key, h -> new String(h.value(), StandardCharsets.UTF_8)));

        byte[] key;
        try {
            HashMap<String, Object> map = new HashMap<>();
            map.put("headers", headers);
            byte[] origKey = record.key();
            if (origKey != null) {
                map.put("id", Base64.getEncoder().encodeToString(origKey));
            }
            String headerStr = mapper.writeValueAsString(map);
            LOGGER.debug("processRecordHeaders() topic={} partition={} offset={} headers={}", record.topic(), record.partition(), record.offset(), headerStr);
            key = headerStr.getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            key = new byte[0];
        }

        //add the headers into the key field of the new  consumer record; the other fields are copied from the original ConsumerRecord
        return new ConsumerRecord<>(record.topic(),
                record.partition(),
                record.offset(),
                record.timestamp(),
                record.timestampType(),
                key.length,
                record.serializedValueSize(),
                key,
                record.value(),
                record.headers(),
                record.leaderEpoch());
    }


    /**
     * This is called when offsets get committed.
     * <p>
     * Any exception thrown by this method will be ignored by the caller.
     *
     * @param offsets A map of offsets by partition with associated metadata
     */
    @Override
    public void onCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {

    }

    /**
     * This is called when interceptor is closed
     */
    @Override
    public void close() {

    }

    /**
     * Configure this class with the given key-value pairs
     *
     * @param configs
     */
    @Override
    public void configure(Map<String, ?> configs) {
        LOGGER.info("ConsumerInterceptor loaded");
    }
}
