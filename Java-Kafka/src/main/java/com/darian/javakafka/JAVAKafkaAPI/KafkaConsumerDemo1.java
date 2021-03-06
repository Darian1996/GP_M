package com.darian.javakafka.JAVAKafkaAPI;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.IntegerDeserializer;

import java.util.Collections;
import java.util.Properties;

public class KafkaConsumerDemo1 extends Thread {
    private final KafkaConsumer kafkaConsumer;

    public KafkaConsumerDemo1(String topic) {
        // 设置属性
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                "192.168.40.128:9092,192.168.40.129:9092,192.168.40.131:9092");
        // 消费组
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "KafkaConsumerDemo");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        // 间隔时间
        properties.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
//        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
//                "org.apache.kafka.common.serialization.IntegerDeserializer");

        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
        // 从最早的开始
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        kafkaConsumer = new KafkaConsumer(properties);
        kafkaConsumer.subscribe(Collections.singletonList(topic));
    }

    @Override
    public void run() {
        while (true) {
            ConsumerRecords<Integer, String> consumerRecords = kafkaConsumer.poll(1000); // 超时时间
            consumerRecords.forEach(consumerRecord -> {
                System.err.println("[partition]:" + consumerRecord.partition() + "[message receive]:" + consumerRecord.value());
                kafkaConsumer.commitSync();
            });
        }
    }

    public static void main(String[] args) {
        new KafkaConsumerDemo1("test").start();
    }
}
