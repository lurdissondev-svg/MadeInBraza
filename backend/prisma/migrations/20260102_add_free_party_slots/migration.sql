-- AlterTable: Make playerClass nullable in party_slots to allow "free" slots
ALTER TABLE "party_slots" ALTER COLUMN "playerClass" DROP NOT NULL;
