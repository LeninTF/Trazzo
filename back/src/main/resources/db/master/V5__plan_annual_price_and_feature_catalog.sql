-- Plans currently store a single price for a single billing_period. The admin UI lets an
-- operator set an independent monthly AND annual price for the same plan (annual pricing is
-- commonly discounted, not just monthly * 12), so a second column is required to persist both
-- simultaneously.
ALTER TABLE plans ADD COLUMN price_annual DECIMAL(10,2);

-- Canonical feature catalog matching the admin "Gestor de Planes" UI's technical limits, modules
-- and business rules. Names match the frontend's module/regla ids exactly so the API can pass
-- feature values through by name without a translation table.
INSERT INTO features (name, description) VALUES
    ('max_trabajadores',      'Máximo de trabajadores permitidos'),
    ('max_sedes',              'Máximo de sedes permitidas'),
    ('almacenamiento_gb',      'Almacenamiento en GB'),
    ('reportes',               'Reportes Avanzados'),
    ('api-externa',            'API Externa'),
    ('facturacion-auto',       'Facturación Automática'),
    ('control-huella',         'Control por Huella'),
    ('escaneo-codigo',         'Escaneo de Código'),
    ('soporte-24-7',           'Soporte 24/7'),
    ('multi-sede',             'Multi-sede'),
    ('trial-gratuito',         'Trial Gratuito'),
    ('facturacion-publica',    'Facturación Pública')
ON CONFLICT (name) DO NOTHING;
