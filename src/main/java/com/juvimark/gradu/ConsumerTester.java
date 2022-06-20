/**
 * Copyright 2020 Confluent Inc.
 * Modifications Copyright 2022 Jukka Markkanen <juvimark@student.jyu.fi>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.juvimark.gradu;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class ConsumerTester {

  public static void main(final String[] args) throws Exception {
    if (args.length != 2) {
      System.out.println("Please provide command line arguments: configPath topic");
      System.exit(1);
    }

    final String topic = args[1];

    // Load properties from a local configuration file
    // Create the configuration file (e.g. at '$HOME/.confluent/java.config') with configuration parameters
    // to connect to your Kafka cluster, which can be on your local host, Confluent Cloud, or any other cluster.
    // Follow these instructions to create this file: https://docs.confluent.io/platform/current/tutorials/examples/clients/docs/java.html

    final Properties props = loadConfig(args[0]);

    // Add additional properties.
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArrayDeserializer");
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArrayDeserializer");
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "consumer-tester");
    // props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

    final Consumer<byte[], byte[]> consumer = new KafkaConsumer<byte[], byte[]>(props);
    consumer.subscribe(Arrays.asList(topic));

    List<String> recordIds = new ArrayList<String>();
    List<Long> latencies = new ArrayList<Long>();
    String lastBatchId = null;
    Long batchStartTime = Long.MAX_VALUE;
    Long timeFromBatchStartTime = 0L;

    // TODO: add percentile calculations

    try {
      while (true) {
        ConsumerRecords<byte[], byte[]> records = consumer.poll(Duration.ofMillis(100));
        for (ConsumerRecord<byte[], byte[]> record : records) {
          byte[] value = record.value();
          // System.out.printf("%s%n", value);
          String stringValue = new String(value, StandardCharsets.UTF_8);
          // System.out.printf("%s%n", stringValue);
          String[] parsedValue = stringValue.toString().split(";");

          String id = parsedValue[0];
          String batchId = parsedValue[1];
          Long dateTime = Long.parseLong(parsedValue[2]);
          Long latency = System.currentTimeMillis() - dateTime;
          Long batchSize = Long.parseLong(parsedValue[3]);

          // System.out.printf("id %s batchId %s, latency %d, lastBatchId %s, debug %s%n", id, batchId, latency, lastBatchId, lastBatchId != batchId);
          
          if (lastBatchId != null && !lastBatchId.equals(batchId)) {
            long uniqueIdCount = recordIds.stream().distinct().count();
            long idCount = recordIds.stream().count();
            long duplicateRecords = idCount - uniqueIdCount;
            long droppedRecords = batchSize - uniqueIdCount;
            double avgLatency = latencies.stream().mapToDouble(a -> a).average().orElse(0d);
            int recordSize = record.serializedValueSize();
            double throughput = (((double)idCount * (double)recordSize) / (double)timeFromBatchStartTime) / 1000d;

            System.out.printf("Last batch %s stats: avg latency %s, dropped records %d, duplicate records %d, total time %s, throughput %s%n", lastBatchId, avgLatency + " ms", droppedRecords, duplicateRecords, timeFromBatchStartTime, throughput + " MB/s");

            recordIds.clear();
            latencies.clear();
            batchStartTime = Long.MAX_VALUE;
          }

          recordIds.add(id);
          latencies.add(latency);
          lastBatchId = batchId;
          batchStartTime = Long.min(batchStartTime, dateTime);
          timeFromBatchStartTime = System.currentTimeMillis() - batchStartTime;
        }
      }
    } finally {
      consumer.close();
    }
  }


  public static Properties loadConfig(String configFile) throws IOException {
    if (!Files.exists(Paths.get(configFile))) {
      throw new IOException(configFile + " not found.");
    }
    final Properties cfg = new Properties();
    try (InputStream inputStream = new FileInputStream(configFile)) {
      cfg.load(inputStream);
    }
    return cfg;
  }

}
