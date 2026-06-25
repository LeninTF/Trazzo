-- ==============================================================================
-- 1. TIPOS ENUMERADOS
-- ==============================================================================

CREATE TYPE currency_enum AS ENUM ('SOLES', 'DOLAR', 'EURO');
CREATE TYPE tipo_dato_enum AS ENUM ('INT', 'CHAR', 'STRING', 'DOUBLE', 'FLOAT', 'LONG', 'BOOLEAN');
CREATE TYPE type_enum AS ENUM ('TRIAL', 'INFO');
CREATE TYPE status_enum AS ENUM ('PENDING', 'IN_REVIEW', 'APPROVED', 'REJECTED');
CREATE TYPE action_enum AS ENUM ('CREATE', 'UPDATE', 'DELETE');
CREATE TYPE status_payment_enum AS ENUM ('PENDING', 'APPROVED', 'REJECTED');
CREATE TYPE estado_pago_comprobante_enum AS ENUM ('PENDIENTE', 'COMPLETADO', 'FALLIDO', 'REEMBOLSADO');
CREATE TYPE subscription_status_enum AS ENUM ('ACTIVE', 'SUSPENDED', 'CANCELED', 'TRIAL');
CREATE TYPE document_type_enum AS ENUM ('DNI', 'CARNET DE EXTRANJERÍA', 'PASAPORTE', 'OTRO');
CREATE TYPE metodo_recuperacion_type_enum AS ENUM ('EMAIL', 'PHONE');
CREATE TYPE status_login_enum AS ENUM ('SUCCESS', 'FAILED_WRONG_PASSWORD', 'FAILED_USER_NOT_FOUND', 'FAILED_INACTIVE_USER', 'LOCKED_OUT', 'LOGOUT_EXPLICIT');
CREATE TYPE tipo_comprobante_enum AS ENUM ('01_FACTURA', '03_BOLETA', '07_NOTA_CREDITO', '08_NOTA_DEBITO');

-- ==============================================================================
-- 2. CATÁLOGOS GLOBALES Y PLANES
-- ==============================================================================

CREATE TABLE "Plans" (
    "id" SERIAL PRIMARY KEY,
    "name" VARCHAR(100) UNIQUE NOT NULL,
    "price" DECIMAL(10,2) NOT NULL,
    "currency" currency_enum NOT NULL,
    "billingPeriod" VARCHAR(50),
    "isActive" BOOLEAN DEFAULT TRUE,
    "createdAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deletedAt" TIMESTAMP
);

CREATE TABLE "Features" (
    "id" SERIAL PRIMARY KEY,
    "name" VARCHAR(100) UNIQUE NOT NULL,
    "description" TEXT,
    "createdAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE "PlanFeatures" (
    "id" SERIAL PRIMARY KEY,
    "planId" INT REFERENCES "Plans"("id") ON DELETE CASCADE,
    "featureId" INT REFERENCES "Features"("id") ON DELETE CASCADE,
    "tipoDato" tipo_dato_enum NOT NULL,
    "value" JSONB NOT NULL,
    "dateStart" DATE,
    "dateEnd" DATE,
    "isActive" BOOLEAN DEFAULT TRUE,
    "createdAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_plan_features_value CHECK (
        ("tipoDato" IN ('INT', 'DOUBLE', 'FLOAT', 'LONG') AND jsonb_typeof("value") = 'number') OR
        ("tipoDato" IN ('CHAR', 'STRING') AND jsonb_typeof("value") = 'string') OR
        ("tipoDato" = 'BOOLEAN' AND jsonb_typeof("value") = 'boolean')
    ),
    CONSTRAINT chk_plan_features_date CHECK (
        ("isActive" = FALSE) OR ("isActive" = TRUE AND "dateStart" IS NOT NULL)
    )
);

-- ==============================================================================
-- 3. NÚCLEO MULTI-TENANT
-- ==============================================================================

CREATE TABLE "Holding" (
    "id" SERIAL PRIMARY KEY,
    "taxId" VARCHAR(20) UNIQUE NOT NULL,
    "reasonSocial" VARCHAR(255) NOT NULL,
    "state" BOOLEAN DEFAULT TRUE,
    "type" VARCHAR(50) CHECK ("type" IN ('PUBLICO', 'PRIVADO')),
    "createdAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deletedAt" TIMESTAMP
);

CREATE TABLE "Tenants" (
    "id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "holdingId" INT REFERENCES "Holding"("id") ON DELETE SET NULL,
    "subDomain" VARCHAR(100) UNIQUE NOT NULL,
    "planId" INT REFERENCES "Plans"("id"),
    "activatedAt" TIMESTAMP,
    "createdAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deletedAt" TIMESTAMP
);

CREATE TABLE "TenantBranding" (
    "tenantId" UUID PRIMARY KEY REFERENCES "Tenants"("id") ON DELETE CASCADE,
    "logoUrl" VARCHAR(255),
    "slogan" TEXT,
    "primaryColor" VARCHAR(20),
    "secondaryColor" VARCHAR(20),
    "createdAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE "TenantSettings" (
    "tenantId" UUID PRIMARY KEY REFERENCES "Tenants"("id") ON DELETE CASCADE,
    "dbName" VARCHAR(100) NOT NULL,
    "dbHost" VARCHAR(255) NOT NULL,
    "dbPort" VARCHAR(10) NOT NULL,
    "dbUser" VARCHAR(100) NOT NULL,
    "dbPassword" VARCHAR(255) NOT NULL,
    "createdAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE "Subscriptions" (
    "id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "planId" INT REFERENCES "Plans"("id"),
    "tenantId" UUID REFERENCES "Tenants"("id") ON DELETE RESTRICT,
    "dateStart" DATE NOT NULL,
    "dateEnd" DATE,
    "status" subscription_status_enum DEFAULT 'TRIAL',
    "purchasePrice" DECIMAL(10,2) NOT NULL,
    "createdAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- 4. PAGOS Y FACTURACIÓN
-- ==============================================================================

CREATE TABLE "PaymentTransactions" (
    "id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "tenantId" UUID NOT NULL REFERENCES "Tenants"("id") ON DELETE RESTRICT,
    "subscriptionId" UUID NOT NULL REFERENCES "Subscriptions"("id") ON DELETE RESTRICT,
    "mpPreferenceId" VARCHAR(255),
    "mpPaymentId" VARCHAR(255),
    "amount" DECIMAL(10,2) NOT NULL,
    "netAmount" DECIMAL(10,2) NOT NULL,
    "statusPayment" status_payment_enum DEFAULT 'PENDING',
    "createdAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE "PaymentWebhooksLog" (
    "id" VARCHAR(255) PRIMARY KEY,
    "mpEventId" VARCHAR(255),
    "action" VARCHAR(100),
    "rawPayload" JSONB,
    "processed" BOOLEAN DEFAULT FALSE,
    "receivedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE "Invoices" (
    "id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "pdfUrl" VARCHAR(255),
    "tenantId" UUID REFERENCES "Tenants"("id"),
    "paymentTransactionId" UUID REFERENCES "PaymentTransactions"("id"),
    "invoiceSeries" VARCHAR(50) UNIQUE NOT NULL,
    "consecutiveNumber" VARCHAR(50) UNIQUE NOT NULL,
    "type" tipo_comprobante_enum,
    "issuerTaxId" VARCHAR(20) NOT NULL CHECK ("issuerTaxId" ~ '^[0-9]{11}$'),
    "issuerName" VARCHAR(255) NOT NULL,
    "issuerTaxAddress" TEXT NOT NULL,
    "clientTaxId" VARCHAR(20) NOT NULL CHECK ("clientTaxId" ~ '^[0-9]{8,15}$'),
    "clientName" VARCHAR(255) NOT NULL,
    "clientDireccion" TEXT,
    "currencyCode" VARCHAR(10),
    "exchangeRate" DECIMAL(10,4),
    "subTotal" DECIMAL(10,2) NOT NULL,
    "igvAmount" DECIMAL(10,2) NOT NULL,
    "total" DECIMAL(10,2) NOT NULL,
    "descuentoTotal" DECIMAL(10,2),
    "estadoPago" estado_pago_comprobante_enum DEFAULT 'PENDIENTE',
    "observaciones" TEXT,
    "createdAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "expirationDate" DATE,
    CONSTRAINT chk_invoice_total CHECK ("total" = "subTotal" + "igvAmount"),
    CONSTRAINT chk_invoice_igv CHECK ("igvAmount" = ROUND("subTotal" * 0.18, 2))
);

CREATE TABLE "InvoiceDetails" (
    "id" SERIAL PRIMARY KEY,
    "invoiceId" UUID REFERENCES "Invoices"("id") ON DELETE CASCADE,
    "subscriptionId" UUID REFERENCES "Subscriptions"("id"),
    "description" TEXT NOT NULL,
    "cantidad" INT NOT NULL,
    "valorUnitario" DECIMAL(10,2) NOT NULL,
    "precioUnitario" DECIMAL(10,2) NOT NULL,
    "unitIgv" DECIMAL(10,2) NOT NULL,
    "subtotal" DECIMAL(10,2),
    "igv" DECIMAL(10,2) NOT NULL,
    "total" DECIMAL(10,2) NOT NULL,
    "createdAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- 5. IDENTIDAD, USUARIOS Y ROLES
-- ==============================================================================

CREATE TABLE "Persons" (
    "id" SERIAL PRIMARY KEY,
    "imgUrl" VARCHAR(255),
    "documentType" document_type_enum NOT NULL,
    "documentValue" VARCHAR(50) UNIQUE NOT NULL,
    "name" VARCHAR(100) NOT NULL,
    "fatherSurname" VARCHAR(100) NOT NULL,
    "motherSurname" VARCHAR(100) NOT NULL,
    "birthDate" DATE,
    "createdAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE "Users" (
    "id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "personId" INT REFERENCES "Persons"("id") ON DELETE CASCADE,
    "tenantId" UUID REFERENCES "Tenants"("id"),
    "email" VARCHAR(150) UNIQUE NOT NULL,
    "phone" VARCHAR(20),
    "password" VARCHAR(255) NOT NULL,
    "createdAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deletedAt" TIMESTAMP
);

CREATE TABLE "RolesMaster" (
    "id" SERIAL PRIMARY KEY,
    "name" VARCHAR(50) UNIQUE NOT NULL,
    "description" TEXT
);

CREATE TABLE "UserRolesMaster" (
    "userId" UUID REFERENCES "Users"("id") ON DELETE CASCADE,
    "rolesMasterId" INT REFERENCES "RolesMaster"("id") ON DELETE CASCADE,
    "createdAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("userId", "rolesMasterId")
);

CREATE TABLE "LogInHistory" (
    "id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "userId" UUID REFERENCES "Users"("id") ON DELETE SET NULL,
    "attemptedEmail" VARCHAR(150),
    "status" status_login_enum NOT NULL,
    "ipAddress" VARCHAR(45),
    "userAgent" TEXT,
    "createdAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE "MetodoRecuperacion" (
    "id" SERIAL PRIMARY KEY,
    "usersId" UUID REFERENCES "Users"("id") ON DELETE CASCADE,
    "methodType" metodo_recuperacion_type_enum NOT NULL,
    "value" VARCHAR(255) NOT NULL,
    "createdAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "deletedAt" TIMESTAMP
);

-- ==============================================================================
-- 6. SOLICITUDES Y LEADS
-- ==============================================================================

CREATE TABLE "Requests" (
    "id" SERIAL PRIMARY KEY,
    "type" type_enum NOT NULL,
    "title" VARCHAR(255) NOT NULL,
    "message" TEXT NOT NULL,
    "status" status_enum DEFAULT 'PENDING',
    "createdAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE "RequestContacts" (
    "requestId" INT PRIMARY KEY REFERENCES "Requests"("id") ON DELETE CASCADE,
    "name" VARCHAR(100) NOT NULL,
    "lastName" VARCHAR(100) NOT NULL,
    "email" VARCHAR(150) UNIQUE NOT NULL,
    "phoneNumber" VARCHAR(20) NOT NULL,
    "taxId" VARCHAR(20) NOT NULL,
    "companyName" VARCHAR(255) NOT NULL,
    "createdAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE "RequestComments" (
    "id" SERIAL PRIMARY KEY,
    "requestId" INT REFERENCES "Requests"("id") ON DELETE CASCADE,
    "requestContactId" INT REFERENCES "RequestContacts"("requestId"),
    "comment" TEXT NOT NULL,
    "createdAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE "UserRequestComment" (
    "id" SERIAL PRIMARY KEY,
    "userId" UUID REFERENCES "Users"("id") ON DELETE CASCADE,
    "requestCommentId" INT REFERENCES "RequestComments"("id") ON DELETE CASCADE,
    "createdAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE "RequestsRecord" (
    "id" SERIAL PRIMARY KEY,
    "requestId" INT REFERENCES "Requests"("id") ON DELETE CASCADE,
    "status" status_enum NOT NULL,
    "userId" UUID REFERENCES "Users"("id") ON DELETE SET NULL,
    "changeReason" TEXT,
    "createdAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- 7. AUDITORÍA
-- ==============================================================================

CREATE TABLE "TenantSettingsRecord" (
    "id" SERIAL PRIMARY KEY,
    "tenantSettingId" UUID REFERENCES "TenantSettings"("tenantId"),
    "dbName" VARCHAR(100),
    "dbHost" VARCHAR(255),
    "dbPort" VARCHAR(10),
    "dbUser" VARCHAR(100),
    "dbPassword" VARCHAR(255),
    "userId" UUID REFERENCES "Users"("id"),
    "changeReason" TEXT,
    "createdAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE "Audit" (
    "id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    "entity" VARCHAR(100) NOT NULL,
    "entityId" VARCHAR(255) NOT NULL,
    "action" action_enum NOT NULL,
    "userId" UUID REFERENCES "Users"("id") ON DELETE SET NULL,
    "endpoint" VARCHAR(255),
    "ipAddress" VARCHAR(45),
    "userAgent" TEXT,
    "oldValue" JSONB,
    "newValue" JSONB,
    "createdAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- 8. SPRING SESSION (gestionado por Spring Session JDBC)
-- ==============================================================================

CREATE TABLE SPRING_SESSION (
    PRIMARY_ID CHAR(36) NOT NULL,
    SESSION_ID CHAR(36) NOT NULL,
    CREATION_TIME BIGINT NOT NULL,
    LAST_ACCESS_TIME BIGINT NOT NULL,
    MAX_INACTIVE_INTERVAL INT NOT NULL,
    EXPIRY_TIME BIGINT NOT NULL,
    PRINCIPAL_NAME VARCHAR(100),
    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
);

CREATE UNIQUE INDEX SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);
CREATE INDEX SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
CREATE INDEX SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);

CREATE TABLE SPRING_SESSION_ATTRIBUTES (
    SESSION_PRIMARY_ID CHAR(36) NOT NULL,
    ATTRIBUTE_NAME VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES BYTEA NOT NULL,
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID)
        REFERENCES SPRING_SESSION (PRIMARY_ID) ON DELETE CASCADE
);
