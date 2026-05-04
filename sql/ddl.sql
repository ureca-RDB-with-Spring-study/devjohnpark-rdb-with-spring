show databases;
use appdb;
show tables;

create table customers (
	customer_id int auto_increment primary key,
    name varchar(50) not null, -- name requirement
    email varchar(100) not null unique, -- email not duplication 
    password varchar(255) not null,
    address varchar(500) not null,
    join_date datetime default current_timestamp -- 가입 시간을 따로 지정하지 않으면 default 설정에 따라 현재시각인 current_timestamp로 자동으로 저장
);

create table products (
	product_id int auto_increment primary key, 
    name varchar(50) not null,
    description text, -- 65535 byte 
    price int not null, 
    stock_quantity int not null default 0 -- 입력값이 없으면 0 
);

create table orders (
	order_id int auto_increment primary key,
    customer_id int not null,
    product_id int not null,
    quantity int not null,
    order_date datetime default current_timestamp,
    status varchar(20) not null default 'Order Received',
    
    constraint fk_orders_customers foreign key (customer_id) references customers(customer_id), -- fk 제약조건 설정 (customers table pk 참조)
    constraint fk_orders_products foreign key (product_id) references products(product_id) -- fk 제약조건 설정 (products table pk 참조)
);


