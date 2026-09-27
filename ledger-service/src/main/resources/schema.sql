create table test.accounts
(
    id   bigint auto_increment
        primary key,
    name varchar(100) not null
);

create table test.ledger_transactions
(
    id              bigint auto_increment
        primary key,
    description     varchar(100)                       null,
    reference_id    bigint                             not null,
    reference_type  varchar(50)                        null,
    order_id        varchar(255)                       null,
    idempotency_key varchar(255) charset ucs2          not null,
    created_at      datetime default CURRENT_TIMESTAMP not null
);

create table test.ledger_entries
(
    id             bigint auto_increment
        primary key,
    amount         decimal(15, 2)                     not null,
    account_id     bigint                             not null,
    transaction_id bigint                             not null,
    type           enum ('CREDIT', 'DEBIT')           not null,
    created_at     datetime default CURRENT_TIMESTAMP null,
    constraint ledger_entries_ibfk_1
        foreign key (transaction_id) references test.ledger_transactions (id),
    constraint ledger_entries_ibfk_2
        foreign key (account_id) references test.accounts (id)
);

create index account_id
    on test.ledger_entries (account_id);

create index transaction_id
    on test.ledger_entries (transaction_id);
