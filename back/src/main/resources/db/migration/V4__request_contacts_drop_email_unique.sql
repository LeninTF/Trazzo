-- The "max 2 requests per RUC" business rule allows the same person (same email) to submit
-- a second request under the same RUC. A hard UNIQUE on email would block that, so uniqueness
-- is enforced in the application layer (by tax_id) instead.
ALTER TABLE request_contacts DROP CONSTRAINT request_contacts_email_key;
