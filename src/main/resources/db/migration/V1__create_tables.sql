DROP TABLE IF EXISTS orders; -- 제약조건 때문에 먼저 DROP
DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS sizes;
DROP TABLE IF EXISTS colors;

CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255)  NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    address     VARCHAR(500),
    birth_date  DATE,
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS products (
    product_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(255) NOT NULL,
    category       VARCHAR(100),
    description    TEXT,
    price          INT NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS orders (
    order_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id  BIGINT NOT NULL,
    quantity    INT NOT NULL,
    order_date  DATETIME DEFAULT CURRENT_TIMESTAMP,
    status      VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, SHIPPED, COMPLETED, CANCELLED
    CONSTRAINT fk_orders_users FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_orders_products  FOREIGN KEY (product_id)  REFERENCES products  (product_id)
);

CREATE TABLE IF NOT EXISTS employees (
      employee_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
      name         VARCHAR(255) NOT NULL,
      manager_id    BIGINT NULL, -- 관리자는 NULL 값을 가질수있음
      CONSTRAINT fk_employees_manager FOREIGN KEY (manager_id) REFERENCES employees (employee_id) -- SELF JOIN
);

-- CROSS JOIN 실습용
CREATE TABLE IF NOT EXISTS sizes (
       size VARCHAR(10) PRIMARY KEY
);

-- CROSS JOIN 실습용
CREATE TABLE IF NOT EXISTS colors (
        color VARCHAR(20) PRIMARY KEY
);