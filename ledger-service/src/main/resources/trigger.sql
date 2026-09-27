DELIMITER $$  ## 장부가 등록될때마다 차대변 금액확인하는 트리거

CREATE TRIGGER check_balance_after_insert
    AFTER INSERT ON ledger_entries
    FOR EACH ROW
BEGIN
    DECLARE credit_sum DECIMAL(15,2);
    DECLARE debit_sum DECIMAL(15,2);

    select SUM(amount) INTO credit_sum
    FROM ledger_entries
    WHERE transaction_id = NEW.transaction_id AND type = 'CREDIT';

    select SUM(amount) INTO debit_sum
    FROM ledger_entries
    WHERE transaction_id = NEW.transaction_id AND type = 'DEBIT';

    IF NOT (credit_sum - debit_sum = 0) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'The sum of CREDIT and DEBIT amounts does not balance to zero';
END IF ;
END $$

#### ==


DELIMITER $$ ## 업데이트 불가능 트리거

CREATE TRIGGER prevent_update_before
    BEFORE UPDATE ON ledger_entries
    FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Update are not allowed on this table';
end $$

DELIMITER ;


DELIMITER $$ ## 삭제 불가 트리거

CREATE TRIGGER prevent_delete_before
    BEFORE DELETE ON ledger_entries
    FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Delete are not allowed on this table';
end $$

DELIMITER ;
