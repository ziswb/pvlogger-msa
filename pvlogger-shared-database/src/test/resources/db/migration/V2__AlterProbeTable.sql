-- Like the data accessor's V10: ALTER TABLE needs ownership, so this fails
-- unless the migrating role owns the table.
ALTER TABLE probe SET (autovacuum_analyze_threshold = 5000);
