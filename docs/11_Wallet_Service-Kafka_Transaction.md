# Wallet Service - Kafka Transaction

## Kafka Transaction 이란?

카프카 트랜잭션은 여러 파티션에 대한 원자적 쓰기를 보장한다.  
이를 통해 여러 토픽으로 동시에 메시지를 발행하거나, 메시지 처리 커밋과 토픽에 메시지를 발행하는 작업을 원자적으로 정확히 한 번 수행할 수 있다.

카프카 트랜잭션은 시스템 장애가 발생했을 때 여러 토픽에 메시지를 발행하는 작업이 원자적으로 이뤄지지 못하고 부분적으로만 이루어지는 것을 방지할 수 있다.

단, 카프카 트랜잭션은 카프카 토픽 내에서만 유효하다. 외부 데이터베이스나 시스템에서 일어나는 처리 작업에 대해서는 트랜잭션을 보장하지 않는다. 
따라서 카프카 트랜잭션을 통해 메시지가 정확히 한 번만 발행된다 해도, 수신한 메시지가 정확히 한 번 처리된다는 보장은 없다. 
메시지를 수신하여 애플리케이션에서 외부 시스템에 대한 상태 변경 작업을 수행하는 경우, 이 작업은 여러 번 처리될 가능성이 있으므로 이에 대비할 필요가 있다.

## Wallet Service 에서 Kafka Transaction 이 필요한 이유는?

Kafka 트랜잭션이 필요한 이유는 정산 처리와 같이 중요한 작업의 성공 메시지가 절대 누락되지 않게 하기 위해서다.
만약 메시지가 누락된다면 Payment Service 는 이 메시지를 수신할 수 없으므로 결제 완료 작업을 수행할 수 없게 된다.

여기서 메시지를 안정적으로 발행하기 위해서 Kafka 커밋과 메시지 전달을 원자적으로 할 수 있는 Kafka 트랜잭션을 적용해서 해결해보겠다.

## Kafka Transaction 작동 원리

Transaction Coordinator 에 의해 카프카 트랜잭션 상태가 관리된다.

- 트랜잭션 상태는 총 3가지가 있으며, `Ongoing` -> `Prepare commit` -> `Completed` 순서대로 전환된다.
- 트랜잭션 상태는 transaction log 라는 토픽에서 관리가 된다. 이 토픽을 관리할 수 있는 것은 Transaction Coordinator 뿐이며, 이것도 다른 토픽 파티션과 마찬가지로 복제가 된다.

구체적으로 동작하는 과정은 크게 A -> B -> C -> D 순으로 이뤄진다:

- A) 프로듀서는 트랜잭션 기능을 사용하기 위해서 `initTransactions API` 를 호출한다. 이를 통해 프로듀서는 자신의 transactional.id 를 트랜잭션을 Coordinator 에 등록시켜서 트랜잭션 프로듀서로 업그레이드 된다. 그리고 이 시점에서 이전에 완료하지 못했던 트랜잭션 작업이 있다면 마무리 시킨다.
- B) 프로듀서는 트랜잭션을 시작할 때 이를 Coordinator 에게 알려준다. Transaction Coordinator 는 transaction log 의 트랜잭션 상태를 `Ongoing` 으로 변경시킨다.
- C) 이 과정은 일반 프로듀서가 토픽 파티션에 데이터를 보내는 것과 거의 동일하다. 추가되는 점은 브로커에서 프로듀서가 보낸 메시지를 보고 좀비 프로듀서가 보낸 건 아닌지 검사하는 과정이 추가된다.
- D) 프로듀서가 모든 데이터를 쓰고 나서 `commitTransaction API` 를 호출하면 Transaction Coordinator 는 Transaction Log 에 상태를 `prepare commit` 으로 변경시킨다. 일단 이 상태가 되면 트랜잭션은 완료될 수 있다. 그 후 Transaction Coordinator는 컨슈머가 커밋된 데이터를 읽어갈 수 있도록 토픽 파티션에 commit marker 를 남긴다. Commit Marker 를 모두 남기게 되면 Transaction log 에 롼료 상태인 `Completed` 를 기록하고 마무리한다.

## Kafka Transaction 사용법

### (1) Producer Configuration

```java
public KafkaProducer<Integer, String> createKafkaProducer() {
    Properties props = new Properties();

    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ProducerConfig.CLIENT_ID_CONFIG, "client-" + UUID.randomUUID());
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

    if (transactionTimeoutMs > 0) {
        props.put(ProducerConfig.TRANSACTION_TIMEOUT_CONFIG, transactionTimeoutMs);
    }
    if (transactionalId != null) {
        props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, transactionalId);
    }

    props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, enableIdempotency);
    return new KafkaProducer<>(props);
}
```

- `props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, transactionalId);` 설정을 통해 transaction.id 를 등록해야 트랜잭션 프로듀서를 사용할 수 있다.
- `props.put(ProducerConfig.TRANSACTION_TIMEOUT_CONFIG, transactionTimeoutMs);`가능한 transactionTimeoutMs 값을 짧게 잡아줘서 트랜잭션 작업이 지연되지 않도록 만드는 것이 좋다.

### (2) Consumer Configuration

```java
public KafkaConsumer<Integer, String> createKafkaConsumer() {
    Properties props = new Properties();

    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.CLIENT_ID_CONFIG, "client-" + UUID.randomUUID());
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

    instanceId.ifPresent(id -> props.put(ConsumerConfig.GROUP_INSTANCE_ID_CONFIG, id));
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, readCommitted ? "false" : "true");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    
		if (readCommitted) {
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
    }
    
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    return new KafkaConsumer<>(props);
}
```

- `props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, readCommitted ? "false" : "true");` Transactional Producer 를 통해서 커밋을 할테니 기본적으로 자동 커밋은 꺼야한다.
- 트랜잭션으로 발행된 메시지를 읽을 때는 다음 설정을 해줘야한다.  `props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed")`

### (3) Transaction Demo

```java
public void run() {
    int processedRecords = 0;
    long remainingRecords = Long.MAX_VALUE;
    // it is recommended to have a relatively short txn timeout in order to clear pending offsets faster
    int transactionTimeoutMs = 10_000;
    // consumer must be in read_committed mode, which means it won't be able to read uncommitted data
    boolean readCommitted = true;
    try (KafkaProducer<Integer, String> producer = new Producer("processor-producer", bootstrapServers, outputTopic, true, transactionalId, true, -1, transactionTimeoutMs, null).createKafkaProducer();

    KafkaConsumer<Integer, String> consumer = new Consumer("processor-consumer", bootstrapServers, inputTopic, "processor-group", Optional.of(groupInstanceId), readCommitted, -1, null).createKafkaConsumer()) {

        // called first and once to fence zombies and abort any pending transaction
        producer.initTransactions();

        consumer.subscribe(singleton(inputTopic), this);

        Utils.printOut("Processing new records");
        while (!closed && remainingRecords > 0) {
            try {
                ConsumerRecords<Integer, String> records = consumer.poll(ofMillis(200));
                if (!records.isEmpty()) {
                    // begin a new transaction session
                    producer.beginTransaction();

                    for (ConsumerRecord<Integer, String> record : records) {
                        // process the record and send downstream
                        ProducerRecord<Integer, String> newRecord = new ProducerRecord<>(outputTopic, record.key(), record.value() + "-ok");
                            producer.send(newRecord);
                        }

                        // checkpoint the progress by sending offsets to group coordinator broker
                        // note that this API is only available for broker >= 2.5
                        producer.sendOffsetsToTransaction(getOffsetsToCommit(consumer), consumer.groupMetadata());

                        // commit the transaction including offsets
                        producer.commitTransaction();
                        processedRecords += records.count();
                    }
            } catch (AuthorizationException | UnsupportedVersionException | ProducerFencedException
                         | FencedInstanceIdException | OutOfOrderSequenceException | SerializationException e) {
                // we can't recover from these exceptions
                Utils.printErr(e.getMessage());
                shutdown();
            } catch (OffsetOutOfRangeException | NoOffsetForPartitionException e) {
                // invalid or no offset found without auto.reset.policy
                Utils.printOut("Invalid or no offset found, using latest");
                consumer.seekToEnd(emptyList());
                consumer.commitSync();
            } catch (KafkaException e) {
                // abort the transaction and try to continue
                Utils.printOut("Aborting transaction: %s", e);
                producer.abortTransaction();
            }
            remainingRecords = getRemainingRecords(consumer);
            if (remainingRecords != Long.MAX_VALUE) {
                Utils.printOut("Remaining records: %d", remainingRecords);
            }
        }
    } catch (Throwable e) {
        Utils.printOut("Unhandled exception");
        e.printStackTrace();
    }
    Utils.printOut("Processed %d records", processedRecords);
    shutdown();
}
```

1.  `producer.initTransactions();`를 통해 프로듀서를 트랜잭션 프로듀서로 업그레이드 시킨다.
2.  `consumer.subscribe(singleton(inputTopic), this);`를 통해서 메시지를 수신할 토픽 파티션을 설정한다.
3.  `producer.beginTransaction();`를 통해서 트랜잭션 처리를 시작한다.
4.  `producer.send(newRecord);`를 통해서 다음 스트림 처리를 위해 토픽 파티션에 메시지를 발행한다.
5.  `producer.sendOffsetsToTransaction(getOffsetsToCommit(consumer), consumer.groupMetadata());`를 통해서 오프셋을 커밋한다.
6.  `producer.commitTransaction();`를 통해서 트랜잭션을 커밋한다.
7.  만약 처리하다가 예외가 발생한다면`producer.abortTransaction();`를 통해서 트랜잭션 처리를 롤백시키는게 중요하다.

## Spring Cloud Stream 에서의 Kafka Transaction 사용법

`*spring.cloud.stream.kafka.binder.transaction.transactionIdPrefix`* 값을 설정함으로써 트랜잭셔널 프로듀서를 활용하게 되며 메시지를 처리할 때마다 Kafka Transaction 이 사용된다.

Kafka Transaction 기능을 사용할 때 주의할 점은 어플리케이션을 수평적으로 확장할 경우 각 어플리케이션마다 `transactionIdPrefix` 값을 유니크하게 설정해야 한다는 점이다.