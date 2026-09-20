SET @ship_time_exists = (
    SELECT COUNT(*)
    FROM `information_schema`.`columns`
    WHERE `table_schema` = DATABASE()
      AND `table_name` = 'order_info'
      AND `column_name` = 'ship_time'
);

SET @ship_time_sql = IF(
    @ship_time_exists = 0,
    'ALTER TABLE `order_info` ADD COLUMN `ship_time` datetime DEFAULT NULL AFTER `payment_time`',
    'SELECT 1'
);

PREPARE ship_time_stmt FROM @ship_time_sql;
EXECUTE ship_time_stmt;
DEALLOCATE PREPARE ship_time_stmt;
