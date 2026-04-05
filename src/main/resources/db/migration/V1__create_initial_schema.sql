-- Customers table
CREATE TABLE customers (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    phone       VARCHAR(50),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Invoices table
CREATE TABLE invoices (
    id              BIGSERIAL PRIMARY KEY,
    customer_id     BIGINT NOT NULL REFERENCES customers(id),
    invoice_number  VARCHAR(50) NOT NULL UNIQUE,
    amount          NUMERIC(19,2) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    due_date        DATE NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_invoice_status CHECK (status IN ('PENDING','PAID','OVERDUE','CANCELLED')),
    CONSTRAINT chk_invoice_amount CHECK (amount > 0)
);

-- Payments table
CREATE TABLE payments (
    id              BIGSERIAL PRIMARY KEY,
    invoice_id      BIGINT NOT NULL REFERENCES invoices(id),
    amount          NUMERIC(19,2) NOT NULL,
    payment_method  VARCHAR(50) NOT NULL,
    paid_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_payment_amount CHECK (amount > 0)
);

-- Indexes
CREATE INDEX idx_invoices_customer_id ON invoices(customer_id);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_payments_invoice_id ON payments(invoice_id);
