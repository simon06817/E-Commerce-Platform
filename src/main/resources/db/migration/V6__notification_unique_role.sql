ALTER TABLE `order_notification`
    DROP INDEX `uk_order_event_recipient`,
    ADD UNIQUE KEY `uk_order_event_recipient`
        (`order_id`, `event_type`, `recipient_role`, `recipient_id`);
