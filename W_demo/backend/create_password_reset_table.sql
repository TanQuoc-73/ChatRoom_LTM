-- SQL Script để tạo bảng password_reset_tokens
-- Chạy script này nếu Hibernate không tự động tạo bảng

USE Realtime_RoomChat2;
GO

-- Tạo bảng password_reset_tokens
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'password_reset_tokens')
BEGIN
    CREATE TABLE password_reset_tokens (
        id BIGINT PRIMARY KEY IDENTITY(1,1),
        user_id BIGINT NOT NULL,
        otp VARCHAR(6) NOT NULL,
        expires_at DATETIME2 NOT NULL,
        created_at DATETIME2 NOT NULL,
        used BIT NOT NULL DEFAULT 0,
        used_at DATETIME2 NULL,
        
        CONSTRAINT FK_password_reset_tokens_user 
            FOREIGN KEY (user_id) 
            REFERENCES app_users(id)
            ON DELETE CASCADE
    );
    
    PRINT 'Bảng password_reset_tokens đã được tạo thành công!';
END
ELSE
BEGIN
    PRINT 'Bảng password_reset_tokens đã tồn tại!';
END
GO

-- Tạo index để tăng tốc độ query
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_password_reset_tokens_user_id')
BEGIN
    CREATE INDEX IX_password_reset_tokens_user_id 
    ON password_reset_tokens(user_id);
    
    PRINT 'Index IX_password_reset_tokens_user_id đã được tạo!';
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_password_reset_tokens_otp')
BEGIN
    CREATE INDEX IX_password_reset_tokens_otp 
    ON password_reset_tokens(otp);
    
    PRINT 'Index IX_password_reset_tokens_otp đã được tạo!';
END
GO

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_password_reset_tokens_expires_at')
BEGIN
    CREATE INDEX IX_password_reset_tokens_expires_at 
    ON password_reset_tokens(expires_at);
    
    PRINT 'Index IX_password_reset_tokens_expires_at đã được tạo!';
END
GO

-- Query để kiểm tra cấu trúc bảng
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    CHARACTER_MAXIMUM_LENGTH,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'password_reset_tokens'
ORDER BY ORDINAL_POSITION;
GO

PRINT 'Script hoàn tất!';
