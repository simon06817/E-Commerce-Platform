DROP TABLE IF EXISTS order_item;
DROP TABLE IF EXISTS order_info;
DROP TABLE IF EXISTS cart;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS product_category;
DROP TABLE IF EXISTS user_seller;
DROP TABLE IF EXISTS user_buyer;
DROP TABLE IF EXISTS user_admin;

CREATE TABLE user_admin (
  id bigint auto_increment primary key,
  username varchar(50) not null unique,
  password varchar(100) not null,
  phone varchar(20),
  email varchar(100),
  create_time timestamp default current_timestamp,
  update_time timestamp default current_timestamp,
  deleted tinyint default 0
);

CREATE TABLE user_buyer (
  id bigint auto_increment primary key,
  username varchar(50) not null unique,
  password varchar(100) not null,
  nickname varchar(50),
  phone varchar(20),
  email varchar(100),
  address varchar(255),
  create_time timestamp default current_timestamp,
  update_time timestamp default current_timestamp,
  deleted tinyint default 0
);

CREATE TABLE user_seller (
  id bigint auto_increment primary key,
  username varchar(50) not null unique,
  password varchar(100) not null,
  shop_name varchar(100),
  phone varchar(20),
  email varchar(100),
  create_time timestamp default current_timestamp,
  update_time timestamp default current_timestamp,
  deleted tinyint default 0
);

CREATE INDEX idx_user_seller_shop_name ON user_seller(shop_name);

CREATE TABLE product_category (
  id bigint auto_increment primary key,
  name varchar(50) not null,
  parent_id bigint default 0,
  sort_order int default 0,
  create_time timestamp default current_timestamp,
  update_time timestamp default current_timestamp,
  status int default 1
);

CREATE TABLE product (
  id bigint auto_increment primary key,
  seller_id bigint not null,
  name varchar(100) not null,
  description varchar(500),
  price decimal(10,2) not null,
  stock int default 0,
  category_id bigint,
  main_image varchar(255),
  status tinyint default 1,
  create_time timestamp default current_timestamp,
  update_time timestamp default current_timestamp
);

CREATE TABLE cart (
  id bigint auto_increment primary key,
  buyer_id bigint not null,
  product_id bigint not null,
  quantity int not null default 1,
  checked tinyint default 1,
  create_time timestamp default current_timestamp,
  update_time timestamp default current_timestamp,
  unique (buyer_id, product_id)
);

CREATE TABLE order_info (
  id bigint auto_increment primary key,
  order_no varchar(32) not null unique,
  idempotency_key varchar(64) unique,
  buyer_id bigint not null,
  seller_id bigint,
  checkout_group_id varchar(64),
  total_amount decimal(10,2) not null,
  status tinyint default 0,
  receiver_name varchar(50),
  receiver_phone varchar(20),
  receiver_address varchar(255),
  create_time timestamp default current_timestamp,
  update_time timestamp default current_timestamp,
  payment_time timestamp,
  ship_time timestamp,
  complete_time timestamp
);

CREATE TABLE order_item (
  id bigint auto_increment primary key,
  order_id bigint not null,
  product_id bigint not null,
  product_name varchar(100) not null,
  product_image varchar(255),
  price decimal(10,2) not null,
  quantity int not null,
  subtotal decimal(10,2) not null,
  create_time timestamp default current_timestamp
);

CREATE TABLE order_outbox (
  id bigint auto_increment primary key,
  order_id bigint not null,
  event_type varchar(50) not null,
  payload varchar(2000),
  status tinyint default 0,
  retry_count int default 0,
  create_time timestamp default current_timestamp,
  update_time timestamp default current_timestamp
);

CREATE TABLE order_event_record (
  id bigint auto_increment primary key,
  order_id bigint not null,
  event_type varchar(50) not null,
  payload varchar(2000),
  create_time timestamp default current_timestamp,
  unique (order_id, event_type)
);

CREATE TABLE product_review (
  id bigint auto_increment primary key,
  product_id bigint not null,
  buyer_id bigint not null,
  order_id bigint not null,
  order_item_id bigint not null unique,
  rating tinyint not null,
  content varchar(500),
  reply_content varchar(500),
  reply_time timestamp,
  create_time timestamp default current_timestamp,
  update_time timestamp default current_timestamp
);

CREATE TABLE order_return (
  id bigint auto_increment primary key,
  order_id bigint not null,
  order_item_id bigint not null unique,
  buyer_id bigint not null,
  seller_id bigint not null,
  product_id bigint not null,
  quantity int not null,
  reason varchar(500) not null,
  status tinyint default 0,
  refund_amount decimal(10,2) not null,
  handle_note varchar(500),
  apply_time timestamp default current_timestamp,
  handle_time timestamp
);

CREATE TABLE order_notification (
  id bigint auto_increment primary key,
  order_id bigint not null,
  event_type varchar(50) not null,
  recipient_id bigint not null,
  recipient_role varchar(20) not null,
  title varchar(100) not null,
  content varchar(500) not null,
  is_read tinyint default 0,
  create_time timestamp default current_timestamp,
  unique (order_id, event_type, recipient_role, recipient_id)
);
