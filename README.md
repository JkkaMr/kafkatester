# Kafka performance tester

Original Kafka performance tester (https://github.com/apache/kafka) modified by Jukka Markkanen 2022

## Run kafka cluster

Start docker cluster locally:
```
docker-compose up
```

Create topic (with latest kafka sources found at https://kafka.apache.org/downloads):
```
./kafka_2.13-3.4.0/bin/kafka-topics.sh --bootstrap-server localhost:29092 --create --replication-factor 3 --partitions 9 --topic perftest
```

## Build

Build Java classes with Maven:
```
mvn clean package
```

## Run testers
Different semantics can not be run at the same time

### At most once

Consumer:
```
mvn exec:java -Dexec.mainClass="com.juvimark.gradu.ConsumerTester" -Dexec.args="./kafkaconfigs/consumer.atmost.properties perftest"
```

Producer (after consumer start up)
```
mvn exec:java -Dexec.mainClass="com.juvimark.gradu.ProducerTester" -Dexec.args="--producer.config ./kafkaconfigs/producer.atmost.properties --topic perftest --num-records 100000 --throughput -1 --record-size <RECORD SIZE>"
```

### At least once

Consumer:
```
mvn exec:java -Dexec.mainClass="com.juvimark.gradu.ConsumerTester" -Dexec.args="./kafkaconfigs/consumer.atleast.properties perftest"
```

Producer (after consumer start up)
```
mvn exec:java -Dexec.mainClass="com.juvimark.gradu.ProducerTester" -Dexec.args="--producer.config ./kafkaconfigs/producer.atleast.properties --topic perftest --num-records 100000 --throughput -1 --record-size <RECORD SIZE>"
```

### Exactly once

Consumer:
```
mvn exec:java -Dexec.mainClass="com.juvimark.gradu.ConsumerTester" -Dexec.args="./kafkaconfigs/consumer.exactly.properties perftest"
```

Producer (after consumer start up)
```
mvn exec:java -Dexec.mainClass="com.juvimark.gradu.ProducerTester" -Dexec.args="--producer.config ./kafkaconfigs/producer.exactly.properties --topic perftest --num-records 100000 --throughput -1 --transactional-id test-producer --record-size <RECORD SIZE> --transaction-duration-ms <TRANSACTION MS>"
```