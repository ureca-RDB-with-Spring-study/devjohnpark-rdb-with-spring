-- 고객 데이터 입력
INSERT INTO users(name, email, password, address, birth_date) VALUES
('션', 'sean@example.com', 'password123', '서울시 강남구', '1990-01-15'),
('네이트', 'nate@example.com', 'password123', '경기도 성남시', '1988-05-22'),
('세종대왕', 'sejong@example.com', 'password123', '서울시 종로구', '1397-05-15'),
('이순신', 'sunsin@example.com', 'password123', '전라남도 여수시', '1545-04-28'),
('마리 퀴리', 'marie@example.com', 'password123', '서울시 강남구', '1867-11-07'),
('레오나르도 다빈치', 'vinci@example.com', 'password123', '이탈리아 피렌체', '1452-04-15');

-- 상품 데이터 입력
INSERT INTO products(name, category, price, stock_quantity) VALUES
('프리미엄 게이밍 마우스', '전자기기', 75000, 50),
('기계식 키보드', '전자기기', 120000, 30),
('4K UHD 모니터', '전자기기', 350000, 20),
('관계형 데이터베이스 입문', '도서', 28000, 100),
('고급 가죽 지갑', '패션', 150000, 15),
('스마트 워치', '전자기기', 280000, 40);

-- 주문 데이터 입력
INSERT INTO orders(user_id, product_id, quantity, status, order_date) VALUES
(1, 1, 1, 'COMPLETED', '2025-06-10 10:00:00'),
(1, 4, 2, 'COMPLETED', '2025-06-10 10:05:00'),
(2, 2, 1, 'SHIPPED', '2025-06-11 14:20:00'),
(3, 4, 1, 'COMPLETED', '2025-06-12 09:00:00'),
(4, 3, 1, 'PENDING', '2025-06-15 11:30:00'),
(5, 1, 1, 'COMPLETED', '2025-06-16 18:00:00'),
(2, 1, 2, 'SHIPPED', '2025-06-17 12:00:00');

-- 직원 데이터 입력
INSERT INTO employees(employee_id, name, manager_id) VALUES
(1, '김회장', NULL),
(2, '박사장', 1),
(3, '이부장', 2),
(4, '최과장', 3),
(5, '정대리', 4),
(6, '홍사원', 4);

-- 사이즈 데이터 입력
INSERT INTO sizes(size) VALUES ('S'), ('M'), ('L'), ('XL');

-- 색상 데이터 입력
INSERT INTO colors(color) VALUES ('Red'), ('Blue'), ('Black');
