-- Script to create database Realtime_RoomChat2
-- Run this in SQL Server Management Studio or Azure Data Studio

USE master;
GO

-- Check if database exists, if not create it
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'Realtime_RoomChat2')
BEGIN
    CREATE DATABASE Realtime_RoomChat2;
    PRINT 'Database Realtime_RoomChat2 created successfully!';
END
ELSE
BEGIN
    PRINT 'Database Realtime_RoomChat2 already exists!';
END
GO

-- Switch to the new database
USE Realtime_RoomChat2;
GO

PRINT 'Ready to use Realtime_RoomChat2 database!';
