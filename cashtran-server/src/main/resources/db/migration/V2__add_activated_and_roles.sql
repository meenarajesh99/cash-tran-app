-- Add activated column if not exists
ALTER TABLE cashtran_user ADD COLUMN IF NOT EXISTS activated BOOLEAN DEFAULT FALSE;

-- Create authority table
CREATE TABLE IF NOT EXISTS authority (
                                         authority_id SERIAL PRIMARY KEY,
                                         name VARCHAR(50) NOT NULL UNIQUE
);

-- Create user_authority junction table
CREATE TABLE IF NOT EXISTS user_authority (
                                              user_id INT NOT NULL REFERENCES cashtran_user(user_id) ON DELETE CASCADE,
                                              authority_id INT NOT NULL REFERENCES authority(authority_id) ON DELETE CASCADE,
                                              PRIMARY KEY (user_id, authority_id)
);

-- Insert default roles
INSERT INTO authority (name) VALUES ('ROLE_USER'), ('ROLE_ADMIN'), ('ROLE_MODERATOR')
ON CONFLICT DO NOTHING;

-- Migrate existing users (if any)
UPDATE cashtran_user SET activated = TRUE WHERE activated IS NULL;