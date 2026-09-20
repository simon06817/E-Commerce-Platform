ALTER TABLE `order_outbox`
    ADD COLUMN `trace_id` varchar(64) DEFAULT NULL AFTER `payload`,
    ADD KEY `idx_outbox_trace_id` (`trace_id`);

ALTER TABLE `order_event_record`
    ADD COLUMN `trace_id` varchar(64) DEFAULT NULL AFTER `payload`,
    ADD KEY `idx_event_record_trace_id` (`trace_id`);
