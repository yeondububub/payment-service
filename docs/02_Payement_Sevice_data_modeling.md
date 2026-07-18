# Payment Service 데이터 모델링

## 어떤 데이터들이 필요한가?

- **Payment Event**: 결제를 요구하며 식별하는 데이터
- **Payment Order**: 실제 결제 대상

이외에도 결제 승인에 문제가 발생했을 때 해결하기 위한 Audit Log 와 같은 데이터가 필요해보인다. 
결제 주문의 상태를 추적할 수 있도록 하는 Payment Order History 라는 데이터도 만들자.

## 각 데이터들은 어떤 필드들이 필요한가?

### Payment Event

| name | type | description |
| --- | --- | --- |
| id | PK, BIG INT, AUTO INCREMENT | 결제 이벤트 고유 식별자 |
| buyer_id | BIG INT | 구매자 식별자  |
| is_payment_done | BOOLEAN | 결제가 완료되었는지 여부 |
| payment_key | VARCHAR, UNIQUE | PSP 애서 생성한 결제 식별자 |
| order_id | VARCHAR, UNIQUE | 결제를 구분해주는 주문 식별자 |
| type | ENUM | 결제 유형. (e.g 일반 결제, 자동 결제 등) |
| order_name | VARCHAR | 결제 주문 이름 |
| method | ENUM | 결제 방법 (e.g 카드 결제, 간편 결제, 휴대폰 등)  |
| psp_raw_data | JSON | PSP 로 부터 받은 원시 데이터 |
| created_at | DATETIME | 생성된 시각 |
| updated_at | DATETIME | 업데이트 된 시각 |
| approved_at | DATETIME | 결제 승인된 시각 |

### Payment Order

| name | type | description |
| --- | --- | --- |
| id | PK, BIG INT, AUTO INCREMENT | 결제 주문 고유 식별자 |
| payment_event_id | FK, BIG INT | Payment Event 를 참조하는 식별자 |
| seller_id | BIG INT | 판매자 식별자 |
| product_id | BIG INT | 제품 식별자 |
| order_id | VARCHAR | 결제를 구분해주는 주문 식별자 |
| amount | DECIMAL | 결제 금액 |
| payment_order_status | ENUM | 결제 주문 상태 (e.g NOT_STARTED, EXECUTING, SUCCESS 등)  |
| ledger_updated | BOOLEAN | 장부 업데이트 여부 |
| wallet_updated | BOOLEAN | 지갑 업데이트 여부 |
| failed_count | TINYINT | 결제 실패 카운트 |
| threshold | TINYINT | 결제 실패 허용 임계값 |
| created_at | DATETIME | 생성된 시각 |
| updated_at | DATETIME | 업데이트 된 시각 |


### Payment Order History

| name | type | description |
| --- | --- | --- |
| id | PK, BIG INT, AUTO INCREMENT | 결제 주문 변경 이력 고유 식별자 |
| payment_order_id | FK, BIG INT | Payment Order 를 참조하는 식별자 |
| previous_status | ENUM | 변경 전 결제 상태 |
| new_status | ENUM | 변경 후 결제 상태 |
| created_at | DATETIME | 생성된 시각 |
| changed_by | VARCHAR | 변경을 수행한 사용자 또는 시스템 식별자 |
| reason | VARCHAR | 상태 변경의 이유 |