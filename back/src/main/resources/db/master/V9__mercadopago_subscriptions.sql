-- Mercado Pago Suscripciones (Preapproval API): links a Subscription to the Mercado Pago
-- preapproval that will confirm/renew its payment. Populated when the preapproval is created
-- (before redirecting the payer), so the webhook can find the Subscription with a direct query
-- instead of relying only on external_reference.
ALTER TABLE subscriptions ADD COLUMN mp_preapproval_id VARCHAR(255);
CREATE INDEX idx_subscriptions_mp_preapproval_id ON subscriptions (mp_preapproval_id) WHERE mp_preapproval_id IS NOT NULL;
