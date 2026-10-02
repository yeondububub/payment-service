# Wallet Service 신뢰성 향상 (feat: Dead Letter Queue) 

## Dead Letter Queue 가 필요한 이유

메시지 처리 작업에서 실패는 허용될 수 있지만, 누락은 결코 발생해서는 안된다.  
처리되지 않은 메시지로 인해 판매자는 판매한 제품에 대한 보상을 받지 못할 수 있고, 결제 시스템 역시 정상적인 결제 완료가 어려워질 수 있기 때문이다.

이러한 메세지 처리 누락을 방지하고 메시지 처리 시스템에 신뢰성을 제공해주는 방법은 Retry Queue 와 Dead Letter Queue 를 활용하는 것이다.  
메시지 처리에 실패한 메시지는 Retry Queue 에 넣어서 재시도 하고, 재시도를 통해 해결되지 않은 메시지는 Dead Letter Queue 에 따로 보관하는 방법이다.  
만약 Dead Letter Queue 가 없다면, 실패한 메시지는 계속 재시도 되어서 전체 시스템의 처리 성능을 저하시킬 수 있다.   
그러므로 일정 횟수 이상 실패한 메시지는 별도의 Dead Letter Queue 에 보관하는 것이다. 이렇게 보관된 메시지는 수동으로라도 처리할 수 있게 된다.

## Spring Cloud Stream: Transactional Rollback Strategies

Spring Cloud Stream 에서 Kafka Transaction 을 사용하는 경우, 트랜잭션 처리 중 예외가 발생한다면 `AfterRollbackProcessor` 에 의해 에외는 핸들링된다.

특별히 복구 전략을 설정하지 않은 경우, 기본적으로 `maxAttempts` 설정에 따라서 실패한 메시지들을 다시 가져와서 처리한다.

재시도 횟수가 모두 소진되었다면 메시지는 처리되었다고 가정하고 커밋한다. 즉, 메시지 처리에 유실이 생길 수 있다.

메시지 처리 과정에서 유실을 원치 않는다면 DLQ (Dead Letter Queue) 토픽에 메시지를 전송하도록 설정 할 수 있다.   
DLQ 토픽으로 메시지가 전송되면, 원본 Kafka 토픽에는 메시지가 처리되었다고 커밋을 하게된다.

만약 DLQ 토픽으로 보내는데 실패하면 무한 재시도가 이뤄지게 된다.

## Spring Cloud Stream 에서 DLQ 적용하기

Spring Cloud Stream Kafka Binder 에서 다음 설정을 넣으면 DLQ (Dead-Letter-Queue) 기능을 간단하게 적용시킬 수 있다.

- `spring.cloud.stream.kafka.bindings.[FUNCTION_NAME]-in-0.consumer.enableDlq=true`
- `spring.cloud.stream.kafka.bindings.[FUNCTION_NAME]-in-0.consumer.dlqName=[DLQ_NAME]`

`dlqName` 을 입력하지 않는다면 `error.<destination>.<group>` 으로 DLQ 토픽 이름이 지정된다.

### Example: Retry Queue + Dead Letter Queue 를 적용하는 방법

실패한 메시지를 처리하는 일반적인 방법은 Retry Queue 에 저장해두고, Retry Queue 에서 Original Topic 으로 메시지를 다시 가져와서 재시도 하는 방법이다.  
만약에 이렇게 몇 번 했는데도 실패한다면 메시지는 이제 Retry Queue 로 보내지 않고 Dead Letter Queue 로 보내서 보관하는 방식이다.

```java
@Bean
public Function<Message<?>, Message<?>> reRoute() {
    return failed -> {
        processed.incrementAndGet();
        Integer retries = failed.getHeaders().get(X_RETRIES_HEADER, Integer.class);
        if (retries == null) {
            System.out.println("First retry for " + failed);
            return MessageBuilder.fromMessage(failed)
                    .setHeader(X_RETRIES_HEADER, 1)
                    .setHeader(BinderHeaders.PARTITION_OVERRIDE,
                            failed.getHeaders().get(KafkaHeaders.RECEIVED_PARTITION_ID))
                    .build();
        }
        else if (retries < 3) {
            System.out.println("Another retry for " + failed);
            return MessageBuilder.fromMessage(failed)
                    .setHeader(X_RETRIES_HEADER, retries + 1)
                    .setHeader(BinderHeaders.PARTITION_OVERRIDE,
                            failed.getHeaders().get(KafkaHeaders.RECEIVED_PARTITION_ID))
                    .build();
        }
        else {
            System.out.println("Retries exhausted for " + failed);
            streamBridge.send("parkingLot", MessageBuilder.fromMessage(failed)
                .setHeader(BinderHeaders.PARTITION_OVERRIDE,
                        failed.getHeaders().get(KafkaHeaders.RECEIVED_PARTITION_ID))
                .build());
        }
        return null;
    };
}
```

Note: DLQ 토픽으로 전달되는 메시지는 기존에 처리를 시도했던 메시지와 같은 토픽 파티션으로 결정된다. 그래서 DLQ 토픽과 원본 토픽은 파티션 개수가 동일해야 함을 의미한다.  
만약 두 토픽의 파티션 개수가 다르거나 파티션을 변경하고자 한다면, `DlqPartitionFunction` 빈을 새로 정의해서 파티션을 결정할 수 있다.

```java
@Bean
public DlqPartitionFunction partitionFunction() {
    return (group, record, ex) -> 0;
}
```

