-- Remove status columns that are no longer part of the mechanics and garage_bays tables.
-- bay_allocations.status is intentionally retained because it tracks allocation lifecycle.
ALTER TABLE garage_bays
    DROP COLUMN status;

ALTER TABLE mechanics
    DROP INDEX idx_mechanics_gid_status,
    ADD KEY idx_mechanics_gid (gid),
    DROP COLUMN status;

