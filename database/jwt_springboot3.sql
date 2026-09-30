IF DB_ID(N'jwt_springboot3') IS NULL
    CREATE DATABASE jwt_springboot3;
GO

USE jwt_springboot3;
GO

IF OBJECT_ID(N'dbo.users', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.users (
        id          INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        full_name   NVARCHAR(50)      NOT NULL,
        email       VARCHAR(100)      NOT NULL,
        images      NVARCHAR(500)     NULL,
        password    VARCHAR(255)      NOT NULL,
        created_at  DATETIME2(6)      NULL,
        updated_at  DATETIME2(6)      NULL,
        CONSTRAINT UQ_users_email UNIQUE (email)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM dbo.users WHERE email = 'trungnh@hcmute.edu.vn')
BEGIN
    INSERT INTO dbo.users (full_name, email, images, password, created_at, updated_at)
    VALUES (N'Nguyen Huu Trung',
            'trungnh@hcmute.edu.vn',
            '/images/avatar.svg',
            '$2a$10$XyAIzvuMqVD0IGJrHk9EeOo8WgvgjYs4RzQfEo1W4Z2Yw0ELziTCW',
            SYSDATETIME(), SYSDATETIME());
END
GO

SELECT id, full_name, email, images, created_at FROM dbo.users;
GO