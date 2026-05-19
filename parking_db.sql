-- phpMyAdmin SQL Dump Corrected
SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET AUTOCOMMIT = 0;
START TRANSACTION;
SET time_zone = "+00:00";
SET FOREIGN_KEY_CHECKS = 0; -- Disable checks to allow table creation and data insertion

-- --------------------------------------------------------
-- Database: `parking_db`
-- --------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `parking_db`;
USE `parking_db`;

-- --------------------------------------------------------
-- Table structure for table `user`
-- --------------------------------------------------------
CREATE TABLE `user` (
  `userId` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `email` varchar(100) NOT NULL,
  `password` varchar(255) NOT NULL,
  `isAdmin` tinyint(1) DEFAULT 0,
  PRIMARY KEY (`userId`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------
-- Table structure for table `vehicle`
-- --------------------------------------------------------
CREATE TABLE `vehicle` (
  `licensePlate` varchar(20) NOT NULL,
  `userId` int(11) NOT NULL,
  `vehicleType` varchar(20) NOT NULL,
  PRIMARY KEY (`licensePlate`),
  KEY `userId` (`userId`),
  CONSTRAINT `vehicle_ibfk_1` FOREIGN KEY (`userId`) REFERENCES `user` (`userId`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------
-- Table structure for table `parking_space`
-- --------------------------------------------------------
CREATE TABLE `parking_space` (
  `spaceId` int(11) NOT NULL AUTO_INCREMENT,
  `code` varchar(20) NOT NULL,
  `floor` int(11) NOT NULL,
  `vehicleType` varchar(20) NOT NULL,
  `isOccupied` tinyint(1) NOT NULL DEFAULT 0,
  `occupiedByPlate` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`spaceId`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------
-- Table structure for table `reservation`
-- --------------------------------------------------------
CREATE TABLE `reservation` (
  `reservationId` int(11) NOT NULL AUTO_INCREMENT,
  `spaceId` int(11) NOT NULL,
  `licensePlate` varchar(20) NOT NULL,
  `reservationDate` datetime NOT NULL,
  `cancelledByAdmin` tinyint(1) NOT NULL DEFAULT 0,
  `notified` tinyint(1) NOT NULL DEFAULT 0,
  `isActive` tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`reservationId`),
  KEY `spaceId` (`spaceId`),
  KEY `licensePlate` (`licensePlate`),
  CONSTRAINT `reservation_ibfk_1` FOREIGN KEY (`spaceId`) REFERENCES `parking_space` (`spaceId`) ON DELETE CASCADE,
  CONSTRAINT `reservation_ibfk_2` FOREIGN KEY (`licensePlate`) REFERENCES `vehicle` (`licensePlate`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------
-- Table structure for table `occupancy_log`
-- --------------------------------------------------------
CREATE TABLE `occupancy_log` (
  `timestamp` datetime NOT NULL,
  `occupiedCount` int(11) NOT NULL,
  PRIMARY KEY (`timestamp`) -- Fixed: reserved keyword timestamp
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------
-- Data Insertions (Ordered to satisfy constraints)
-- --------------------------------------------------------

-- 1. Insert Users first
INSERT INTO `user` (`userId`, `username`, `email`, `password`, `isAdmin`) VALUES
(8,  'admin',  'admin@parking.com',  'qqqqqqqqqqqqqqqqqqqqoA==:DxRuWOd/OnxvVZw6k+vuT7r1OWBqgTXXeDyCiac9buM=', 1),
(9,  'ali',    'ali@parking.com',    'u7u7u7u7u7u7u7u7u7u7AQ==:8zLr4d8ofPqoygP3HENrL5DwB63BeaLxaZpfQlD455k=', 0),
(10, 'Manolo', 'manolo@parking.com', 'zMzMzMzMzMzMzMzMzMzMAg==:xCCRpm1OZPE0nCbInoe6Gcm3ARP7eSOUg6LY5J88SB8=', 0),
(11, 'Marti',  'marti@parking.com',  '3d3d3d3d3d3d3d3d3d3dAw==:h8glgIj9+tPnrKAxQYOXKsDf4OOSPfZ2++SgSroaOqQ=', 0);

-- 2. Insert Vehicles next
INSERT INTO `vehicle` (`licensePlate`, `userId`, `vehicleType`) VALUES
('ABC001',  9, 'CAR'),
('ABC002',  9, 'MOTORCYCLE'),
('ABC003',  9, 'CAR'),
('ABC004',  9, 'LARGE_VEHICLE'),
('ABC005',  9, 'CAR'),
('ABC006',  9, 'CAR'),
('ABC007',  9, 'MOTORCYCLE'),
('ABC008', 10, 'CAR'),
('ABC009', 10, 'MOTORCYCLE'),
('ABC010', 10, 'LARGE_VEHICLE'),
('ABC011', 10, 'CAR'),
('ABC012', 10, 'CAR'),
('ABC013', 10, 'MOTORCYCLE'),
('ABC014', 11, 'CAR'),
('ABC015', 11, 'LARGE_VEHICLE'),
('ABC016', 11, 'CAR'),
('ABC017', 11, 'MOTORCYCLE'),
('ABC018', 11, 'CAR'),
('ABC019', 11, 'CAR'),
('ABC020', 11, 'CAR');

-- 3. Insert Parking Spaces
INSERT INTO `parking_space` (`code`, `floor`, `vehicleType`, `isOccupied`, `occupiedByPlate`) VALUES
('A-01', 1, 'CAR',           1, 'ABC001'),
('A-02', 1, 'CAR',           0,  NULL),
('A-03', 1, 'CAR',           1, 'ABC003'),
('A-04', 1, 'CAR',           0,  NULL),
('A-05', 1, 'MOTORCYCLE',    1, 'ABC002'),
('A-06', 1, 'MOTORCYCLE',    0,  NULL),
('A-07', 1, 'LARGE_VEHICLE', 1, 'ABC004'),
('A-08', 1, 'LARGE_VEHICLE', 0,  NULL),
('A-09', 1, 'CAR',           1, 'ABC005'),
('A-10', 1, 'CAR',           0,  NULL),
('B-01', 2, 'CAR',           1, 'ABC006'),
('B-02', 2, 'CAR',           0,  NULL),
('B-03', 2, 'MOTORCYCLE',    1, 'ABC007'),
('B-04', 2, 'MOTORCYCLE',    0,  NULL),
('B-05', 2, 'CAR',           1, 'ABC008'),
('B-06', 2, 'CAR',           0,  NULL),
('B-07', 2, 'LARGE_VEHICLE', 1, 'ABC010'),
('B-08', 2, 'LARGE_VEHICLE', 0,  NULL),
('B-09', 2, 'CAR',           1, 'ABC011'),
('B-10', 2, 'CAR',           0,  NULL);

-- 4. Insert Reservations
INSERT INTO `reservation` (`spaceId`, `licensePlate`, `reservationDate`, `cancelledByAdmin`, `notified`, `isActive`) VALUES
((SELECT `spaceId` FROM `parking_space` WHERE `code`='A-01'), 'ABC001', '2026-04-20 08:00:00', 0, 0, 1),
((SELECT `spaceId` FROM `parking_space` WHERE `code`='A-03'), 'ABC003', '2026-04-21 09:15:00', 0, 0, 1),
((SELECT `spaceId` FROM `parking_space` WHERE `code`='A-05'), 'ABC002', '2026-04-22 10:30:00', 0, 0, 1),
((SELECT `spaceId` FROM `parking_space` WHERE `code`='A-07'), 'ABC004', '2026-04-23 11:00:00', 0, 0, 1),
((SELECT `spaceId` FROM `parking_space` WHERE `code`='A-09'), 'ABC005', '2026-04-24 08:45:00', 0, 0, 1),
((SELECT `spaceId` FROM `parking_space` WHERE `code`='B-01'), 'ABC006', '2026-04-25 07:30:00', 0, 0, 1),
((SELECT `spaceId` FROM `parking_space` WHERE `code`='B-03'), 'ABC007', '2026-04-26 09:00:00', 0, 0, 1),
((SELECT `spaceId` FROM `parking_space` WHERE `code`='B-05'), 'ABC008', '2026-04-27 10:00:00', 0, 0, 1),
((SELECT `spaceId` FROM `parking_space` WHERE `code`='B-07'), 'ABC010', '2026-04-28 11:30:00', 0, 0, 1),
((SELECT `spaceId` FROM `parking_space` WHERE `code`='B-09'), 'ABC011', '2026-04-29 13:00:00', 0, 0, 1),
((SELECT `spaceId` FROM `parking_space` WHERE `code`='A-02'), 'ABC012', '2026-03-10 08:00:00', 0, 0, 0),
((SELECT `spaceId` FROM `parking_space` WHERE `code`='A-04'), 'ABC013', '2026-03-12 09:00:00', 1, 1, 0),
((SELECT `spaceId` FROM `parking_space` WHERE `code`='A-06'), 'ABC009', '2026-03-14 10:00:00', 0, 0, 0),
((SELECT `spaceId` FROM `parking_space` WHERE `code`='A-08'), 'ABC014', '2026-03-16 11:00:00', 0, 0, 0),
((SELECT `spaceId` FROM `parking_space` WHERE `code`='A-10'), 'ABC015', '2026-03-18 12:00:00', 1, 1, 0),
((SELECT `spaceId` FROM `parking_space` WHERE `code`='B-02'), 'ABC016', '2026-03-20 08:00:00', 0, 0, 0),
((SELECT `spaceId` FROM `parking_space` WHERE `code`='B-04'), 'ABC017', '2026-03-22 09:00:00', 0, 0, 0),
((SELECT `spaceId` FROM `parking_space` WHERE `code`='B-06'), 'ABC018', '2026-03-24 10:00:00', 1, 1, 0),
((SELECT `spaceId` FROM `parking_space` WHERE `code`='B-08'), 'ABC019', '2026-03-26 11:00:00', 0, 0, 0),
((SELECT `spaceId` FROM `parking_space` WHERE `code`='B-10'), 'ABC020', '2026-03-28 12:00:00', 0, 0, 0);

-- 5. Insert Occupancy Logs
INSERT INTO `occupancy_log` (`timestamp`, `occupiedCount`) VALUES
('2026-04-17 08:00:00',  3),
('2026-04-18 08:00:00',  5),
('2026-04-19 08:00:00',  7),
('2026-04-20 08:00:00',  4),
('2026-04-21 08:00:00',  6),
('2026-04-22 08:00:00',  9),
('2026-04-23 08:00:00', 11),
('2026-04-24 08:00:00', 10),
('2026-04-25 08:00:00', 13),
('2026-04-26 08:00:00', 12),
('2026-04-27 08:00:00', 15),
('2026-04-28 08:00:00', 14),
('2026-04-29 08:00:00', 16),
('2026-04-30 08:00:00', 10),
('2026-05-01 08:00:00',  8),
('2026-05-02 08:00:00',  7),
('2026-05-03 08:00:00',  9),
('2026-05-04 08:00:00', 11),
('2026-05-05 08:00:00', 13),
('2026-05-06 08:00:00', 10),
('2026-05-07 08:00:00', 8),
('2026-05-08 09:00:00', 6);

SET FOREIGN_KEY_CHECKS = 1; -- Re-enable checks
COMMIT;