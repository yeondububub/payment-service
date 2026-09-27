DELIMITER $$

CREATE TRIGGER after_payment_order_update
    AFTER UPDATE ON payment_orders
    FOR EACH ROW
BEGIN
    IF NEW.ledger_updated = TRUE AND NEW.wallet_updated = TRUE THEN
        IF ( SELECT COUNT(*)
             FROM payment_orders
             WHERE payment_event_id = NEW.payment_event_id
               AND (ledger_updated = FALSE OR wallet_updated = FALSE)) = 0 THEN
    UPDATE payment_events
    SET is_payment_done = TRUE
    WHERE id = NEW.payment_event_id;
end if ;
end if ;
end $$

DELIMITER ;
