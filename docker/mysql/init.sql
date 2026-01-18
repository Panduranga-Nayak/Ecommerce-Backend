CREATE DATABASE IF NOT EXISTS ecommerce_user;
CREATE DATABASE IF NOT EXISTS ecommerce_product;
CREATE DATABASE IF NOT EXISTS ecommerce_cart;
CREATE DATABASE IF NOT EXISTS ecommerce_order;
CREATE DATABASE IF NOT EXISTS ecommerce_payment;

CREATE USER IF NOT EXISTS 'ecommerce_user'@'%' IDENTIFIED BY 'ecommerce_user_pass';
CREATE USER IF NOT EXISTS 'ecommerce_product'@'%' IDENTIFIED BY 'ecommerce_product_pass';
CREATE USER IF NOT EXISTS 'ecommerce_cart'@'%' IDENTIFIED BY 'ecommerce_cart_pass';
CREATE USER IF NOT EXISTS 'ecommerce_order'@'%' IDENTIFIED BY 'ecommerce_order_pass';
CREATE USER IF NOT EXISTS 'ecommerce_payment'@'%' IDENTIFIED BY 'ecommerce_payment_pass';

GRANT ALL PRIVILEGES ON ecommerce_user.* TO 'ecommerce_user'@'%';
GRANT ALL PRIVILEGES ON ecommerce_product.* TO 'ecommerce_product'@'%';
GRANT ALL PRIVILEGES ON ecommerce_cart.* TO 'ecommerce_cart'@'%';
GRANT ALL PRIVILEGES ON ecommerce_order.* TO 'ecommerce_order'@'%';
GRANT ALL PRIVILEGES ON ecommerce_payment.* TO 'ecommerce_payment'@'%';

FLUSH PRIVILEGES;
