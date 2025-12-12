-- Comprehensive HPMS Schema for MySQL
-- Hospital Patient Management System - Complete database structure
-- Created with full support for all system modules and relationships

-- ============================================================================
-- DATABASE INITIALIZATION
-- ============================================================================

-- Create database if not exists
CREATE DATABASE IF NOT EXISTS hpms2_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Use the database
USE hpms2_db;

-- ============================================================================
-- CORE USER MANAGEMENT TABLES
-- ============================================================================

-- Users (authentication and core identity)
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    profile_picture_url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    staff_number VARCHAR(50),
    linked_patient_id VARCHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_role (role),
    INDEX idx_status (status),
    INDEX idx_linked_patient (linked_patient_id)
);

-- ============================================================================
-- ORGANIZATIONAL STRUCTURE
-- ============================================================================

-- Departments
CREATE TABLE IF NOT EXISTS departments (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_name (name)
);

-- Rooms (wards/units)
CREATE TABLE IF NOT EXISTS rooms (
    id VARCHAR(36) PRIMARY KEY,
    ward_id VARCHAR(36),
    room_number VARCHAR(50) NOT NULL,
    capacity INT NOT NULL DEFAULT 1,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ward (ward_id),
    INDEX idx_room_number (room_number)
);

-- Beds (individual beds in rooms)
CREATE TABLE IF NOT EXISTS beds (
    id VARCHAR(36) PRIMARY KEY,
    room_id VARCHAR(36) NOT NULL,
    bed_number VARCHAR(50) NOT NULL,
    occupied BOOLEAN DEFAULT FALSE,
    current_admission_id VARCHAR(36),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    INDEX idx_room (room_id),
    INDEX idx_occupied (occupied),
    INDEX idx_current_admission (current_admission_id)
);

-- ============================================================================
-- STAFF AND PERSONNEL
-- ============================================================================

-- Doctors
CREATE TABLE IF NOT EXISTS doctors (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL UNIQUE,
    specialization VARCHAR(255),
    license_number VARCHAR(100),
    license_expiry DATE,
    years_of_experience INT DEFAULT 0,
    department_id VARCHAR(36),
    consultation_fee DECIMAL(10, 2),
    biography TEXT,
    contact_number VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (department_id) REFERENCES departments(id),
    INDEX idx_user (user_id),
    INDEX idx_department (department_id),
    INDEX idx_specialization (specialization),
    INDEX idx_status (status)
);

-- Doctor Education
CREATE TABLE IF NOT EXISTS doctor_education (
    id VARCHAR(36) PRIMARY KEY,
    doctor_id VARCHAR(36) NOT NULL,
    institution VARCHAR(255),
    degree VARCHAR(100),
    field_of_study VARCHAR(255),
    graduation_year INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE,
    INDEX idx_doctor (doctor_id)
);

-- Doctor Experience
CREATE TABLE IF NOT EXISTS doctor_experience (
    id VARCHAR(36) PRIMARY KEY,
    doctor_id VARCHAR(36) NOT NULL,
    institution VARCHAR(255),
    position VARCHAR(100),
    start_date DATE,
    end_date DATE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE,
    INDEX idx_doctor (doctor_id)
);

-- Doctor Schedules
CREATE TABLE IF NOT EXISTS doctor_schedules (
    id VARCHAR(36) PRIMARY KEY,
    doctor_id VARCHAR(36) NOT NULL,
    day_of_week INT NOT NULL,
    start_time TIME,
    end_time TIME,
    available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE,
    INDEX idx_doctor (doctor_id),
    UNIQUE KEY idx_doctor_day (doctor_id, day_of_week)
);

-- Staff
CREATE TABLE IF NOT EXISTS staff (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL UNIQUE,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role_type VARCHAR(100),
    department_id VARCHAR(36),
    contact_number VARCHAR(20),
    hire_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (department_id) REFERENCES departments(id),
    INDEX idx_user (user_id),
    INDEX idx_department (department_id),
    INDEX idx_role_type (role_type),
    INDEX idx_status (status)
);

-- Staff Schedules
CREATE TABLE IF NOT EXISTS staff_schedules (
    id VARCHAR(36) PRIMARY KEY,
    staff_id VARCHAR(36) NOT NULL,
    day_of_week INT NOT NULL,
    start_time TIME,
    end_time TIME,
    available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (staff_id) REFERENCES staff(id) ON DELETE CASCADE,
    INDEX idx_staff (staff_id),
    UNIQUE KEY idx_staff_day (staff_id, day_of_week)
);

-- Admin
CREATE TABLE IF NOT EXISTS admin (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL UNIQUE,
    position_or_title VARCHAR(255),
    contact_number VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user (user_id)
);

-- ============================================================================
-- PATIENT MANAGEMENT
-- ============================================================================

-- Patients
CREATE TABLE IF NOT EXISTS patients (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36),
    patient_number VARCHAR(50) NOT NULL UNIQUE,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    date_of_birth DATE NOT NULL,
    sex VARCHAR(20),
    gender VARCHAR(20),
    blood_type VARCHAR(5),
    civil_status VARCHAR(50),
    address TEXT,
    contact_number VARCHAR(20),
    emergency_contact_name VARCHAR(100),
    emergency_contact_number VARCHAR(20),
    age INT,
    allergies TEXT,
    current_medications TEXT,
    insurance_provider VARCHAR(255),
    insurance_number VARCHAR(100),
    phil_health_number VARCHAR(100),
    insurance_expiry DATE,
    occupation VARCHAR(100),
    employer_name VARCHAR(255),
    work_address TEXT,
    religion VARCHAR(100),
    preferred_language VARCHAR(100),
    preferred_contact_method VARCHAR(50),
    symptoms TEXT,
    height_cm DECIMAL(5, 2),
    weight_kg DECIMAL(5, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_user (user_id),
    INDEX idx_patient_number (patient_number),
    INDEX idx_dob (date_of_birth),
    INDEX idx_name (last_name, first_name)
);

-- Medical Profiles (patient health history overview)
CREATE TABLE IF NOT EXISTS medical_profiles (
    id VARCHAR(36) PRIMARY KEY,
    patient_id VARCHAR(36) NOT NULL,
    chronic_conditions TEXT,
    surgical_history TEXT,
    family_medical_history TEXT,
    allergies TEXT,
    current_medications TEXT,
    immunizations TEXT,
    last_checkup_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    INDEX idx_patient (patient_id)
);

-- Medical Records (detailed visit/consultation records)
CREATE TABLE IF NOT EXISTS medical_records (
    id VARCHAR(36) PRIMARY KEY,
    patient_id VARCHAR(36) NOT NULL,
    doctor_id VARCHAR(36),
    visit_date DATETIME NOT NULL,
    chief_complaint TEXT,
    diagnosis TEXT,
    treatment_plan TEXT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id),
    INDEX idx_patient (patient_id),
    INDEX idx_doctor (doctor_id),
    INDEX idx_visit_date (visit_date)
);

-- Insurance Policies
CREATE TABLE IF NOT EXISTS insurance_policies (
    id VARCHAR(36) PRIMARY KEY,
    patient_id VARCHAR(36) NOT NULL,
    provider_name VARCHAR(255),
    policy_number VARCHAR(100),
    coverage_type VARCHAR(100),
    coverage_percentage DECIMAL(5, 2),
    max_coverage_amount DECIMAL(12, 2),
    expiry_date DATE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    INDEX idx_patient (patient_id),
    INDEX idx_provider (provider_name),
    INDEX idx_active (is_active)
);

-- ============================================================================
-- APPOINTMENTS AND SCHEDULING
-- ============================================================================

-- Appointments
CREATE TABLE IF NOT EXISTS appointments (
    id VARCHAR(36) PRIMARY KEY,
    patient_id VARCHAR(36) NOT NULL,
    doctor_id VARCHAR(36) NOT NULL,
    schedule_date DATE NOT NULL,
    schedule_time TIME NOT NULL,
    reason TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id),
    INDEX idx_patient (patient_id),
    INDEX idx_doctor (doctor_id),
    INDEX idx_schedule_date (schedule_date),
    INDEX idx_status (status),
    UNIQUE KEY idx_unique_appointment (patient_id, doctor_id, schedule_date, schedule_time)
);

-- Visits (completed appointments/consultations)
CREATE TABLE IF NOT EXISTS visits (
    id VARCHAR(36) PRIMARY KEY,
    appointment_id VARCHAR(36),
    patient_id VARCHAR(36) NOT NULL,
    doctor_id VARCHAR(36),
    visit_date DATETIME NOT NULL,
    chief_complaint TEXT,
    diagnosis TEXT,
    treatment_plan TEXT,
    vital_signs_bp VARCHAR(20),
    vital_signs_pulse INT,
    vital_signs_temperature DECIMAL(5, 2),
    vital_signs_respiratory_rate INT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (appointment_id) REFERENCES appointments(id),
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id),
    INDEX idx_appointment (appointment_id),
    INDEX idx_patient (patient_id),
    INDEX idx_doctor (doctor_id),
    INDEX idx_visit_date (visit_date)
);

-- ============================================================================
-- ADMISSIONS AND INPATIENT CARE
-- ============================================================================

-- Admissions (patient hospital admissions)
CREATE TABLE IF NOT EXISTS admissions (
    id VARCHAR(36) PRIMARY KEY,
    patient_id VARCHAR(36) NOT NULL,
    admitted_at DATETIME NOT NULL,
    admitted_by VARCHAR(36),
    ward_id VARCHAR(36),
    room_id VARCHAR(36),
    bed_id VARCHAR(36),
    admission_reason TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    discharged_at DATETIME,
    discharge_summary_id VARCHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    FOREIGN KEY (admitted_by) REFERENCES users(id),
    FOREIGN KEY (room_id) REFERENCES rooms(id),
    FOREIGN KEY (bed_id) REFERENCES beds(id),
    INDEX idx_patient (patient_id),
    INDEX idx_status (status),
    INDEX idx_admitted_at (admitted_at),
    INDEX idx_room (room_id)
);

-- ============================================================================
-- LABORATORY AND DIAGNOSTICS
-- ============================================================================

-- Lab Orders (test requests)
CREATE TABLE IF NOT EXISTS lab_orders (
    id VARCHAR(36) PRIMARY KEY,
    patient_id VARCHAR(36) NOT NULL,
    doctor_id VARCHAR(36),
    order_date DATETIME NOT NULL,
    test_type VARCHAR(255) NOT NULL,
    test_description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    priority VARCHAR(20),
    urgency BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id),
    INDEX idx_patient (patient_id),
    INDEX idx_doctor (doctor_id),
    INDEX idx_status (status),
    INDEX idx_order_date (order_date)
);

-- Lab Results (test results)
CREATE TABLE IF NOT EXISTS lab_results (
    id VARCHAR(36) PRIMARY KEY,
    lab_order_id VARCHAR(36) NOT NULL,
    patient_id VARCHAR(36) NOT NULL,
    result_date DATETIME NOT NULL,
    test_name VARCHAR(255),
    result_value VARCHAR(500),
    unit_of_measurement VARCHAR(50),
    reference_range VARCHAR(100),
    abnormal_flag BOOLEAN DEFAULT FALSE,
    interpretation TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (lab_order_id) REFERENCES lab_orders(id) ON DELETE CASCADE,
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    INDEX idx_lab_order (lab_order_id),
    INDEX idx_patient (patient_id),
    INDEX idx_result_date (result_date)
);

-- ============================================================================
-- BILLING AND FINANCE
-- ============================================================================

-- Billings (invoices and charges)
CREATE TABLE IF NOT EXISTS billings (
    id VARCHAR(36) PRIMARY KEY,
    patient_id VARCHAR(36) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    service_date DATE,
    due_date DATE,
    paid_date DATE,
    payment_method VARCHAR(50),
    reference_number VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    INDEX idx_patient (patient_id),
    INDEX idx_status (status),
    INDEX idx_service_date (service_date),
    INDEX idx_due_date (due_date)
);

-- ============================================================================
-- NOTIFICATIONS
-- ============================================================================

-- Notifications (user alerts and messages)
CREATE TABLE IF NOT EXISTS notifications (
    id VARCHAR(36) PRIMARY KEY,
    recipient_user_id VARCHAR(36) NOT NULL,
    message TEXT NOT NULL,
    message_type VARCHAR(50),
    seen BOOLEAN DEFAULT FALSE,
    seen_at DATETIME,
    action_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (recipient_user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_recipient (recipient_user_id),
    INDEX idx_seen (seen),
    INDEX idx_created_at (created_at)
);

-- ============================================================================
-- AUDIT AND HISTORY TABLES
-- ============================================================================

-- User Activity Log (for audit trail)
CREATE TABLE IF NOT EXISTS user_activity_log (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    action VARCHAR(100),
    entity_type VARCHAR(100),
    entity_id VARCHAR(36),
    details TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user (user_id),
    INDEX idx_created_at (created_at),
    INDEX idx_action (action)
);

-- Contact Info (for flexible contact information)
CREATE TABLE IF NOT EXISTS contact_info (
    id VARCHAR(36) PRIMARY KEY,
    entity_type VARCHAR(50),
    entity_id VARCHAR(36),
    contact_type VARCHAR(50),
    value VARCHAR(255),
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_contact_type (contact_type)
);

-- Address Info (for flexible address storage)
CREATE TABLE IF NOT EXISTS address_info (
    id VARCHAR(36) PRIMARY KEY,
    entity_type VARCHAR(50),
    entity_id VARCHAR(36),
    street_address VARCHAR(255),
    city VARCHAR(100),
    state_province VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_entity (entity_type, entity_id)
);

-- ============================================================================
-- INDEXES FOR PERFORMANCE
-- ============================================================================

-- Additional composite indexes for common queries
CREATE INDEX IF NOT EXISTS idx_appointments_date_status ON appointments(schedule_date, status);
CREATE INDEX IF NOT EXISTS idx_medical_records_patient_date ON medical_records(patient_id, visit_date DESC);
CREATE INDEX IF NOT EXISTS idx_admissions_patient_status ON admissions(patient_id, status);
CREATE INDEX IF NOT EXISTS idx_lab_orders_patient_status ON lab_orders(patient_id, status);
CREATE INDEX IF NOT EXISTS idx_billings_patient_status ON billings(patient_id, status);
CREATE INDEX IF NOT EXISTS idx_notifications_recipient_seen ON notifications(recipient_user_id, seen, created_at DESC);
