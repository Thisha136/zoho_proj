-- Required by the scheduler: each service resolves to a mechanic specialization.
ALTER TABLE services
    ADD COLUMN IF NOT EXISTS specialization VARCHAR(100) NULL AFTER service_name;

CREATE INDEX IF NOT EXISTS idx_mechanics_gid_specialization
    ON mechanics (gid, specialization);

CREATE INDEX IF NOT EXISTS idx_mechanic_allocations_overlap
    ON mechanic_allocations (mechanic_id, start_datetime, end_datetime, status);
