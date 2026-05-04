INSERT INTO customers (name, email, password, address) VALUES
('홍길동', 'hong@test.com', 'password123', '서울시 강남구'),
('김철수', 'kim@test.com',  'password123', '서울시 마포구'),
('이영희', 'lee@test.com',  'password123', '경기도 성남시');

INSERT INTO products (name, description, price, stock_quantity) VALUES
('상품A', '상품A 설명', 10000, 100),
('상품B', '상품B 설명', 20000,  50),
('상품C', '상품C 설명', 30000, 200);

INSERT INTO orders (customer_id, product_id, quantity, status) VALUES
(1, 1, 2, 'Order Received'),
(1, 2, 1, 'Shipped'),
(2, 3, 3, 'Order Received');
