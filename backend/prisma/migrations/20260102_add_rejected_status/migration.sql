-- Add REJECTED status to UserStatus enum
ALTER TYPE "UserStatus" ADD VALUE 'REJECTED';

-- Add rejectedAt column to users table
ALTER TABLE "users" ADD COLUMN "rejectedAt" TIMESTAMP(3);
