ALTER TABLE plans ADD COLUMN algorithm VARCHAR(50) NOT NULL DEFAULT 'sliding_window';

UPDATE plans SET algorithm = 'sliding_window' WHERE plan_name = 'free';
UPDATE plans SET algorithm = 'token_bucket' WHERE plan_name = 'pro';
UPDATE plans SET algorithm = 'token_bucket' WHERE plan_name = 'pro_plus';