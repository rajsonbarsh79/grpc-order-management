CREATE USER order_app WITH PASSWORD 'order_pwd';
CREATE USER inventory_app WITH PASSWORD 'inventory_pwd';
CREATE USER payment_app WITH PASSWORD 'payment_pwd';

CREATE DATABASE order_db OWNER order_app;
CREATE DATABASE inventory_db OWNER inventory_app;
CREATE DATABASE payment_db OWNER payment_app;
