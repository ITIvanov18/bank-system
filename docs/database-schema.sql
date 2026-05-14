-- MySQL dump 10.13  Distrib 8.0.45, for Linux (x86_64)
--
-- Host: localhost    Database: bank_db
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `bank_accounts`
--

DROP TABLE IF EXISTS `bank_accounts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bank_accounts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `iban` varchar(34) NOT NULL,
  `balance` decimal(19,2) NOT NULL,
  `status` enum('ACTIVE','CLOSED') NOT NULL,
  `owner_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `bg_bank_account_iban` (`iban`),
  KEY `fk_bank_account_owner` (`owner_id`),
  CONSTRAINT `fk_bank_account_owner` FOREIGN KEY (`owner_id`) REFERENCES `customers` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `corporate_customers`
--

DROP TABLE IF EXISTS `corporate_customers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `corporate_customers` (
  `id` bigint NOT NULL,
  `company_name` varchar(200) NOT NULL,
  `eik` varchar(13) NOT NULL,
  `representative_first_name` varchar(100) NOT NULL,
  `representative_last_name` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_corporate_customer_eik` (`eik`),
  CONSTRAINT `fk_corporate_customer_base` FOREIGN KEY (`id`) REFERENCES `customers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `customers`
--

DROP TABLE IF EXISTS `customers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `customers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `customer_discriminator` varchar(31) NOT NULL,
  `customer_type` enum('INDIVIDUAL','CORPORATE') NOT NULL,
  `email` varchar(255) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `is_first_login` bit(1) NOT NULL,
  `user_role` enum('CUSTOMER','EMPLOYEE') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_customers_email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flyway_schema_history`
--

DROP TABLE IF EXISTS `flyway_schema_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) DEFAULT NULL,
  `description` varchar(200) NOT NULL,
  `type` varchar(20) NOT NULL,
  `script` varchar(1000) NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `individual_customers`
--

DROP TABLE IF EXISTS `individual_customers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `individual_customers` (
  `id` bigint NOT NULL,
  `first_name` varchar(100) NOT NULL,
  `last_name` varchar(100) NOT NULL,
  `egn` varchar(10) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_individual_customer_egn` (`egn`),
  CONSTRAINT `fk_individual_customer_base` FOREIGN KEY (`id`) REFERENCES `customers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `installment_payments_log`
--

DROP TABLE IF EXISTS `installment_payments_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `installment_payments_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `loan_id` bigint NOT NULL,
  `installment_id` bigint NOT NULL,
  `amount_paid` decimal(19,2) NOT NULL,
  `paid_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_installment_payments_log_loan` (`loan_id`),
  KEY `fk_installment_payments_log_installment` (`installment_id`),
  CONSTRAINT `fk_installment_payments_log_installment` FOREIGN KEY (`installment_id`) REFERENCES `installments` (`id`),
  CONSTRAINT `fk_installment_payments_log_loan` FOREIGN KEY (`loan_id`) REFERENCES `loans` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `installments`
--

DROP TABLE IF EXISTS `installments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `installments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `loan_id` bigint NOT NULL,
  `installment_number` int NOT NULL,
  `due_date` date NOT NULL,
  `monthly_installment_amount` decimal(19,2) NOT NULL,
  `principal_part` decimal(19,2) NOT NULL,
  `interest_part` decimal(19,2) NOT NULL,
  `remaining_balance` decimal(19,2) NOT NULL,
  `status` enum('PENDING','PAID','OVERDUE') NOT NULL,
  `paid_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_installment_loan_installment_number` (`loan_id`,`installment_number`),
  CONSTRAINT `fk_installment_loan` FOREIGN KEY (`loan_id`) REFERENCES `loans` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `loan_review_logs`
--

DROP TABLE IF EXISTS `loan_review_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `loan_review_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `loan_id` bigint NOT NULL,
  `customer_id` bigint NOT NULL,
  `decision` enum('APPROVED','REJECTED') NOT NULL,
  `loan_type` enum('CONSUMER','MORTGAGE') NOT NULL,
  `principal_amount` decimal(19,2) NOT NULL,
  `annual_interest_rate` decimal(9,4) NOT NULL,
  `repayment_term_months` int NOT NULL,
  `customer_email` varchar(255) NOT NULL,
  `employee_email` varchar(255) NOT NULL,
  `decision_note` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_loan_review_log_loan` (`loan_id`),
  KEY `fk_loan_review_log_customer` (`customer_id`),
  CONSTRAINT `fk_loan_review_log_customer` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`),
  CONSTRAINT `fk_loan_review_log_loan` FOREIGN KEY (`loan_id`) REFERENCES `loans` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `loans`
--

DROP TABLE IF EXISTS `loans`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `loans` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `customer_id` bigint NOT NULL,
  `loan_type` enum('CONSUMER','MORTGAGE') NOT NULL,
  `principal_amount` decimal(19,2) NOT NULL,
  `annual_interest_rate` decimal(9,4) NOT NULL,
  `repayment_term_months` int NOT NULL,
  `status` enum('PENDING','ACTIVE','REJECTED','CLOSED') NOT NULL,
  `start_date` date DEFAULT NULL,
  `reviewed_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_loan_customer` (`customer_id`),
  CONSTRAINT `fk_loan_customer` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`),
  CONSTRAINT `chk_loans_product_terms` CHECK ((((`loan_type` = _utf8mb4'CONSUMER') and (`principal_amount` between 1000.00 and 40000.00) and (`repayment_term_months` between 12 and 120) and (((`principal_amount` - 1000.00) % 5.00) = 0)) or ((`loan_type` = _utf8mb4'MORTGAGE') and (`principal_amount` between 3000.00 and 500000.00) and (`repayment_term_months` between 1 and 360) and (((`principal_amount` - 3000.00) % 500.00) = 0))))
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `password_reset_tokens`
--

DROP TABLE IF EXISTS `password_reset_tokens`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `password_reset_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `customer_id` bigint NOT NULL,
  `token_hash` char(64) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `used_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_password_reset_tokens_token_hash` (`token_hash`),
  KEY `idx_password_reset_tokens_customer_active` (`customer_id`,`used_at`),
  CONSTRAINT `fk_password_reset_tokens_customer` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping routines for database 'bank_db'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-14 10:24:22
