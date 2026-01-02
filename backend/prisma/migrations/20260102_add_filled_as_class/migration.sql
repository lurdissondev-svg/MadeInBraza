-- Add filledAsClass column to party_slots for storing the class chosen when filling a FREE slot
ALTER TABLE "party_slots" ADD COLUMN "filledAsClass" "PlayerClass";
