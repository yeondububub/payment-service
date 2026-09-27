CREATE TABLE payment_events
(                                                                    -- 사용자가 구매하기 버튼을 클랙했을때 생성되는 이벤트
    id              BIGINT AUTO_INCREAMENT PRIMARY KEY,              -- 식별자
    buyer_id        BIGINT       NOT NULL,                           -- 구매자 식별자
    is_payment_done BOOLEAN      NOT NULL DEFAULT FALSE,             -- 결제완료여부
    payment_key     varchar(255) UNIQUE,                             --PSP에서 생성한 결재 ID
    order_id        varchar(255) UNIQUE,                             --결제를 구분하는 주문 식별자
    type            ENUM('NORMAL') NOT NULL,                         -- 결재 유형 ex)일반결제, 자동결제
    order_name      VARCHAR(255) NOT NULL,                           --결재 주문 이름
    method          ENUM('CARD'),                                    -- 결재방법 ex) 카드결재, 간편결재
    psp_raw_data    JSON,                                            --PSP 결재 승인 후에 받는 응답데이터 추후 분석,환불 관리용
    approved_at     DATETIME,                                        --결재 승인 시각
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성시각
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 업데이트 된 시각
);

create table payment_orders
(                                                                                                                          --결재해야할 대상
    id                   BIGINT AUTO_INCREAMENT PRIMARY KEY,                                                               -- 식별자
    payment_event_id     BIGINT         NOT NULL,                                                                          -- 페이먼트 이벤트 참조식별자
    seller_id            BIGINT         NOT NULL,                                                                          -- 거래가 이뤄지는 판매자 아이디
    product_id           BIGINT         NOT NULL,                                                                          -- 구매되는 상품의 ID
    order_id             varchar(255)   NOT NULL,                                                                          -- 주문식별자 왜 상위 테이블의 event에서 조회할 수 있는데 여기도 있느냐? -  변경 불가능하기 때문에 조회 성능관점에서 추가의 이점이 있음.
    amount               DECIMAL(12, 2) NOT NULL,                                                                          -- 거래금액
    payment_order_status ENUM('NOT_STARTED', 'EXECUTING', 'SUCCESS', 'FAILURE', 'UNKNOWN') NOT NULL DEFAULT 'NOT_STARTED', -- 결제주문 상태
    ledger_updated       BOOLEAN        NOT NULL DEFAULT FALSE,                                                            -- 장부 업데이트 여부
    wallet_updated       BOOLEAN        NOT NULL DEFAULT FALSE,                                                            -- 지갑 업데이트 여부
    failed_count         TINYINT        NOT NULL DEFAULT 0,                                                                -- 결제실패 횟수
    threshold            TINYINT        NOT NULL DEFAULT 5,                                                                -- 결제실패 허용 횟수
    created_at           DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,                                                -- 결제주문 생성시각
    updated_at           DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,                                                -- 업데이트 시각

    foreign key (payment_event_id) references payment_events (id)
);

CREATE TABLE payment_order_histories
(                                                                                       --결제주문의 상태 변화를 기록하는데 사용된다. 문제해결위주
    id               BIGINT AUTO_INCREAMENT PRIMARY KEY,                                --식별자
    payment_order_id BIGINT   NOT NULL,                                                 -- 주문결재번호 참조식별자
    previous_status  ENUM('NOT_STARTED', 'EXECUTING', 'FAILURE', 'UNKNOWN'),            -- 변경전 주문상태
    new_status       ENUM('NOT_STARTED', 'EXECUTING', 'FAILURE', 'SUCCESS', 'UNKNOWN'), -- 변경 후 주문상태
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,                       -- 생성시각
    changed_by       VARCHAR(255),                                                      -- 변경을 수행한 사용자
    reason           VARCHAR(255),                                                      -- 상태변화의 이유

    FOREIGN KEY (payment_order_id) references payment_order (id)
);



CREATE TABLE outboxes
(                                                                                                --아웃박스이벤트 발행 데이터
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,                                           --식별자
    idempotency_key varchar(255) NOT NULL,                                                       -- 멱등성 키
    status          ENUM('INIT', 'FAILURE', 'SUCCESS') DEFAULT 'INIT',                           -- 메시지 상태
    type            VARCHAR(40),
    partition_key   INT                   DEFAULT 0,
    payload         JSON,
    metadata        JSON,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,                             -- 생성시각
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 변경시간
);
