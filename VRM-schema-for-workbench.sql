-- Canonical Vehicle Repair Management schema
-- MySQL 8.x / InnoDB / utf8mb4
-- This file is a rebuild script. Back up the database before executing it.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS mechanic_allocations;
DROP TABLE IF EXISTS mechanics;
DROP TABLE IF EXISTS feedback;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS invoices;
DROP TABLE IF EXISTS bay_allocations;
DROP TABLE IF EXISTS garage_bays;
DROP TABLE IF EXISTS job_services;
DROP TABLE IF EXISTS jobs;
DROP TABLE IF EXISTS service_requests;
DROP TABLE IF EXISTS garage_services;
DROP TABLE IF EXISTS garage_variants;
DROP TABLE IF EXISTS services;
DROP TABLE IF EXISTS bikes;
DROP TABLE IF EXISTS addresses;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS variants;
DROP TABLE IF EXISTS models;
DROP TABLE IF EXISTS brands;
DROP TABLE IF EXISTS garages;

CREATE TABLE users (
    uid BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password VARCHAR(255) NOT NULL,
    ph_no VARCHAR(15) NULL,
    status ENUM('ACTIVE','INACTIVE','BLOCKED') NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (uid),
    UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE brands (
    bid BIGINT NOT NULL AUTO_INCREMENT,
    brand_name VARCHAR(100) NOT NULL,
    PRIMARY KEY (bid),
    UNIQUE KEY uq_brands_name (brand_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE models (
    model_id BIGINT NOT NULL AUTO_INCREMENT,
    bid BIGINT NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    PRIMARY KEY (model_id),
    UNIQUE KEY uq_models_brand_name (bid, model_name),
    CONSTRAINT fk_models_brand FOREIGN KEY (bid) REFERENCES brands (bid)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE variants (
    variant_id BIGINT NOT NULL AUTO_INCREMENT,
    model_id BIGINT NOT NULL,
    variant_name VARCHAR(100) NOT NULL,
    PRIMARY KEY (variant_id),
    UNIQUE KEY uq_variants_model_name (model_id, variant_name),
    CONSTRAINT fk_variants_model FOREIGN KEY (model_id) REFERENCES models (model_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE garages (
    gid BIGINT NOT NULL AUTO_INCREMENT,
    garage_name VARCHAR(150) NOT NULL,
    address_line VARCHAR(255) NULL,
    city VARCHAR(100) NULL,
    state VARCHAR(100) NULL,
    pincode VARCHAR(10) NULL,
    PRIMARY KEY (gid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE addresses (
    address_id BIGINT NOT NULL AUTO_INCREMENT,
    uid BIGINT NOT NULL,
    address_type ENUM('HOME','OFFICE','OTHER') NOT NULL,
    address_line VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    pincode VARCHAR(10) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (address_id),
    KEY idx_addresses_uid (uid),
    CONSTRAINT fk_addresses_user FOREIGN KEY (uid) REFERENCES users (uid)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE bikes (
    bike_id BIGINT NOT NULL AUTO_INCREMENT,
    uid BIGINT NOT NULL,
    variant_id BIGINT NOT NULL,
    registration_no VARCHAR(20) NOT NULL,
    manufacturing_year YEAR NULL,
    purchase_date DATE NULL,
    fuel_type ENUM('PETROL','ELECTRIC','OTHER') NOT NULL,
    odometer_reading INT UNSIGNED NOT NULL DEFAULT 0,
    color VARCHAR(30) NULL,
    image_url VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (bike_id),
    UNIQUE KEY uq_bikes_registration (registration_no),
    KEY idx_bikes_uid (uid),
    KEY idx_bikes_variant (variant_id),
    CONSTRAINT fk_bikes_user FOREIGN KEY (uid) REFERENCES users (uid)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_bikes_variant FOREIGN KEY (variant_id) REFERENCES variants (variant_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_bikes_odometer CHECK (odometer_reading >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE services (
    sid BIGINT NOT NULL AUTO_INCREMENT,
    service_name VARCHAR(150) NOT NULL,
    specialization VARCHAR(100) NULL,
    duration INT UNSIGNED NULL,
    price DECIMAL(10,2) NOT NULL,
    service_type ENUM('SERVICE','PART','MAINTENANCE') NOT NULL,
    PRIMARY KEY (sid),
    CONSTRAINT chk_services_duration CHECK (duration IS NULL OR duration > 0),
    CONSTRAINT chk_services_price CHECK (price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE garage_variants (
    gid BIGINT NOT NULL,
    variant_id BIGINT NOT NULL,
    PRIMARY KEY (gid, variant_id),
    KEY idx_garage_variants_variant (variant_id),
    CONSTRAINT fk_garage_variants_garage FOREIGN KEY (gid) REFERENCES garages (gid)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_garage_variants_variant FOREIGN KEY (variant_id) REFERENCES variants (variant_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE garage_services (
    gid BIGINT NOT NULL,
    sid BIGINT NOT NULL,
    duration INT UNSIGNED NULL,
    price DECIMAL(10,2) NULL,
    PRIMARY KEY (gid, sid),
    KEY idx_garage_services_sid (sid),
    CONSTRAINT fk_garage_services_garage FOREIGN KEY (gid) REFERENCES garages (gid)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_garage_services_service FOREIGN KEY (sid) REFERENCES services (sid)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_garage_services_duration CHECK (duration IS NULL OR duration > 0),
    CONSTRAINT chk_garage_services_price CHECK (price IS NULL OR price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE service_requests (
    sr_id BIGINT NOT NULL AUTO_INCREMENT,
    bike_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    issue TEXT NOT NULL,
    image_url VARCHAR(500) NULL,
    gid BIGINT NOT NULL,
    status ENUM('PENDING','REVIEWED','DATE_PROPOSED','DATE_REJECTED','ACCEPTED','REJECTED','CANCELLED','COMPLETED') NOT NULL DEFAULT 'PENDING',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    request_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    preferred_delivery_date DATE NULL,
    garage_proposed_delivery_date DATE NULL,
    customer_date_response ENUM('ACCEPTED','REJECTED') NULL,
    physical_review_date DATETIME NULL,
    vehicle_handling_option ENUM('DROP','PICKUP','TOW') NULL,
    PRIMARY KEY (sr_id),
    KEY idx_service_requests_bike (bike_id),
    KEY idx_service_requests_gid_status (gid, status),
    CONSTRAINT fk_service_requests_bike FOREIGN KEY (bike_id) REFERENCES bikes (bike_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_service_requests_garage FOREIGN KEY (gid) REFERENCES garages (gid)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE jobs (
    jid BIGINT NOT NULL AUTO_INCREMENT,
    sr_id BIGINT NOT NULL,
    status ENUM('CREATED','IN_PROGRESS','COMPLETED','CANCELLED') NOT NULL DEFAULT 'CREATED',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    job_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    service_start_date DATETIME NULL,
    service_end_date DATETIME NULL,
    expected_delivery_date DATE NULL,
    actual_delivery_date DATE NULL,
    pickup_status ENUM('PENDING','PICKED_UP','NOT_REQUIRED') NOT NULL DEFAULT 'PENDING',
    delivery_status ENUM('PENDING','DELIVERED','NOT_REQUIRED') NOT NULL DEFAULT 'PENDING',
    PRIMARY KEY (jid),
    UNIQUE KEY uq_jobs_service_request (sr_id),
    KEY idx_jobs_status (status),
    CONSTRAINT fk_jobs_service_request FOREIGN KEY (sr_id) REFERENCES service_requests (sr_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_jobs_service_dates CHECK (service_end_date IS NULL OR service_start_date IS NULL OR service_end_date >= service_start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE job_services (
    js_id BIGINT NOT NULL AUTO_INCREMENT,
    jid BIGINT NOT NULL,
    sid BIGINT NOT NULL,
    status ENUM('PENDING','IN_PROGRESS','APPROVED','REJECTED','COMPLETED') NOT NULL DEFAULT 'PENDING',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    selection_type ENUM('COMPULSORY','OPTIONAL') NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    service_part_type ENUM('SERVICE','PART') NOT NULL,
    actual_start_time DATETIME NULL,
    actual_end_time DATETIME NULL,
    time_taken INT UNSIGNED NULL,
    PRIMARY KEY (js_id),
    KEY idx_job_services_jid (jid),
    KEY idx_job_services_sid (sid),
    CONSTRAINT fk_job_services_job FOREIGN KEY (jid) REFERENCES jobs (jid)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_job_services_service FOREIGN KEY (sid) REFERENCES services (sid)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_job_services_price CHECK (price >= 0),
    CONSTRAINT chk_job_services_time CHECK (time_taken IS NULL OR time_taken >= 0),
    CONSTRAINT chk_job_services_times CHECK (actual_end_time IS NULL OR actual_start_time IS NULL OR actual_end_time >= actual_start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE invoices (
    iv_id BIGINT NOT NULL AUTO_INCREMENT,
    jid BIGINT NOT NULL,
    status ENUM('UNPAID','PENDING','GENERATED','PAID','CANCELLED') NOT NULL DEFAULT 'UNPAID',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    invoice_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    amount DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (iv_id),
    UNIQUE KEY uq_invoices_job (jid),
    CONSTRAINT fk_invoices_job FOREIGN KEY (jid) REFERENCES jobs (jid)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_invoices_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE payments (
    payment_id BIGINT NOT NULL AUTO_INCREMENT,
    iv_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    payment_reference VARCHAR(100) NULL,
    status ENUM('PENDING','PAID','SUCCESS','FAILED','REFUNDED') NOT NULL DEFAULT 'PENDING',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    payment_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (payment_id),
    UNIQUE KEY uq_payments_invoice (iv_id),
    CONSTRAINT fk_payments_invoice FOREIGN KEY (iv_id) REFERENCES invoices (iv_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_payments_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE feedback (
    feedback_id BIGINT NOT NULL AUTO_INCREMENT,
    jid BIGINT NOT NULL,
    uid BIGINT NOT NULL,
    gid BIGINT NOT NULL,
    rating TINYINT UNSIGNED NOT NULL,
    comments TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (feedback_id),
    UNIQUE KEY uq_feedback_job (jid),
    KEY idx_feedback_gid (gid),
    CONSTRAINT fk_feedback_job FOREIGN KEY (jid) REFERENCES jobs (jid)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_feedback_user FOREIGN KEY (uid) REFERENCES users (uid)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_feedback_garage FOREIGN KEY (gid) REFERENCES garages (gid)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_feedback_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE garage_bays (
    bay_id BIGINT NOT NULL AUTO_INCREMENT,
    gid BIGINT NOT NULL,
    bay_name VARCHAR(50) NOT NULL,
    PRIMARY KEY (bay_id),
    UNIQUE KEY uq_garage_bays_name (gid, bay_name),
    CONSTRAINT fk_garage_bays_garage FOREIGN KEY (gid) REFERENCES garages (gid)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE bay_allocations (
    allocation_id BIGINT NOT NULL AUTO_INCREMENT,
    bay_id BIGINT NOT NULL,
    jid BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status ENUM('ACTIVE','COMPLETED','CANCELLED') NOT NULL DEFAULT 'ACTIVE',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (allocation_id),
    KEY idx_bay_allocations_bay_dates (bay_id, start_date, end_date),
    KEY idx_bay_allocations_job (jid),
    CONSTRAINT fk_bay_allocations_bay FOREIGN KEY (bay_id) REFERENCES garage_bays (bay_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_bay_allocations_job FOREIGN KEY (jid) REFERENCES jobs (jid)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_bay_allocation_dates CHECK (end_date >= start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE mechanics (
    mechanic_id BIGINT NOT NULL AUTO_INCREMENT,
    gid BIGINT NOT NULL,
    mechanic_name VARCHAR(100) NOT NULL,
    specialization VARCHAR(100) NOT NULL,
    free_from DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (mechanic_id),
    KEY idx_mechanics_gid (gid),
    CONSTRAINT fk_mechanics_garage FOREIGN KEY (gid) REFERENCES garages (gid)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE mechanic_allocations (
    allocation_id BIGINT NOT NULL AUTO_INCREMENT,
    mechanic_id BIGINT NOT NULL,
    jid BIGINT NOT NULL,
    js_id BIGINT NOT NULL,
    start_datetime DATETIME NOT NULL,
    end_datetime DATETIME NOT NULL,
    status ENUM('ALLOCATED','IN_PROGRESS','COMPLETED','CANCELLED') NOT NULL DEFAULT 'ALLOCATED',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    notes VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (allocation_id),
    UNIQUE KEY uq_mechanic_job_service (js_id, mechanic_id),
    KEY idx_mechanic_allocations_mechanic_time (mechanic_id, start_datetime, end_datetime),
    CONSTRAINT fk_mechanic_allocations_mechanic FOREIGN KEY (mechanic_id) REFERENCES mechanics (mechanic_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_mechanic_allocations_job FOREIGN KEY (jid) REFERENCES jobs (jid)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_mechanic_allocations_job_service FOREIGN KEY (js_id) REFERENCES job_services (js_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT chk_mechanic_allocation_dates CHECK (end_datetime >= start_datetime)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
