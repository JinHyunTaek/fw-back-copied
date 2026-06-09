 ALTER TABLE fighter
      ADD COLUMN significant_strikes_landed_per_min   DOUBLE NULL,
      ADD COLUMN significant_strikes_absorbed_per_min DOUBLE NULL,
      ADD COLUMN takedown_avg_per15min               DOUBLE NULL,
      ADD COLUMN submission_avg_per15min             DOUBLE NULL,
      ADD COLUMN striking_accuracy_pct                INT    NULL,
      ADD COLUMN striking_defence_pct                 INT    NULL,
      ADD COLUMN takedown_accuracy_pct                INT    NULL,
      ADD COLUMN takedown_defence_pct                 INT    NULL;