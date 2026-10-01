# Wallet Service Optimistic Locking

지갑 상태를 동시에 저장할 때 발생하는 문제를 Optimistic Locking 매커니즘으로 해결해보자.

## 쓰기 충돌 문제는 발생할 수 있다

여러 Wallet Service 가 배포된 환경에서 각 서버가 결제 승인 이벤트를 수신해 정산 처리 작업을 수행한다고 생각해보자.  
이때 마지막으로 업데이트된 트랜잭션만 반영되고, 같이 실행된 또 다른 트랜잭션은 무시된다.  
예를 들어, 지갑 잔고가 1000원이었을 때, 2000원을 추가하는 트랜잭션과 3000원을 추가하는 트랜잭션이 동시에 적용될 때,   
2000원 트랜잭션이 먼저 적용되고, 곧바로 3000원 트랜잭션이 적용된다면 최종적으로 지갑의 잔고는 4000( = 1000 + 3000) 원으로 될 것이다.

이처럼 Wallet Service 는 동시에 동일한 판매자의 지갑 잔액을 업데이트할 때 발생할 수 있는 충돌을 고려해야 하며,  
이로 인해 판매자가 받아야 할 금액이 유실되는 상황을 방지해야 한다.   
이를 해결하기 위해, 충돌이 자주 발생하지 않는다는 가정 하에, Optimistic Locking 방식을 채택할 것이다.

## Optimistic Locking 적용 방법

JPA 에서 Optimistic Locking 매커니즘을 사용하는 방법으로 `@Version` 에노테이션을 활용할 수 있다.

`@Version` 은 다음 Entity 와 같이 필드에 붙혀서 사용한다.

- 적용할 수 있는 필드로는 Int, Long, Short, Timestamp 등이 있다.

```kotlin
@Entity
@Table(name = "wallets")
data class JpaWalletEntity (
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "user_id")
  val userId: Long,

  val balance: BigDecimal,

  @Version
  val version: Int,
)
```

### @Version 애노테이션으로 어떻게 Optimistic Locking 이 적용되는걸까?

`@Version` 애노테이션을 필드에 적용하면 해당 엔터티를 데이터베이스에 반영할 때 버전에 대한 검사를 수행한다.  
내가 알고 있던 버전과 다르다면 다른 트랜잭션이 수정을 한 것으로 간주해 실패하고,   
내가 알고 있던 버전이 맞다면 가장 최신 상태의 엔터티이므로 지금 실행중인 트랜잭션은 적용될 수 있다.

이런 Version 에 대한 검사와 데이터베이스 반영은 내부적으로 다음 SQL 문으로 처리된다.

```sql
UPDATE wallets
SET 
	balance = ?,
	version = ? (버전 + 1 증가) 
WHERE
	id = ? 
  version = (내가 알고 있던 버전)
```

## ObjectOptimisticLockingException Handling

`@Version` 에노테이션을 사용해서 동시성을 제어 시 쓰기 충돌이 발생하면 `ObjectOptimisticLockingException` (스프링 예외 추상화) 이 발생한다.  
이 예외는 해당 트랜잭션이 자신의 업데이트 내용이 데이터베이스에 반영되지 않았음을 의미하므로, 이 경우 데이터베이스에 업데이트를 반영하고 싶다면 재시도가 필요하다.

재시도 시에는 최신 상태의 지갑으로 작업을 수행해야 하며, 바로 재시도할 경우 충돌이 다시 발생할 가능성이 있으므로, 충돌을 회피하기 위해 약간의 랜덤 딜레이를 추가하는 것이 좋다.