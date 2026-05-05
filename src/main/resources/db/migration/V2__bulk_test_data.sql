-- 재귀 CTE 최대 깊이 설정
SET SESSION cte_max_recursion_depth = 100000;

-- customers: 10,000건
INSERT INTO customers (name, email, password, address)
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 10000
)
SELECT
  CONCAT('고객_', LPAD(n, 5, '0')),
  CONCAT('user', n, '@test.com'),
  'password123',
  CONCAT('서울시 ', ELT((n % 5) + 1, '강남구', '마포구', '종로구', '송파구', '영등포구'), ' ', n, '로')
FROM seq;

-- products: 1,000건
INSERT INTO products (name, description, price, stock_quantity)
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 1000
)
SELECT
  CONCAT('상품_', LPAD(n, 4, '0')),
  CONCAT('상품_', LPAD(n, 4, '0'), ' 상세 설명'),
  (n % 10 + 1) * 10000,
  (n % 100) + 10
FROM seq;

-- orders: 100,000건
INSERT INTO orders (customer_id, product_id, quantity, status)
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 100000
)
SELECT
  (n % 10000) + 1,
  (n % 1000) + 1,
  (n % 5) + 1,
  ELT((n % 3) + 1, 'Order Received', 'Shipped', 'Delivered')
FROM seq;

SELECT
  (SELECT COUNT(*) FROM customers) AS customers,
  (SELECT COUNT(*) FROM products)  AS products,
  (SELECT COUNT(*) FROM orders)    AS orders;
