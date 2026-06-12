-- ============================================================
--  TecnoStore — Tabla: creditos
--  FK hacia clientes(id)
-- ============================================================

CREATE TABLE IF NOT EXISTS creditos (
    id                INT          NOT NULL AUTO_INCREMENT,
    id_cliente        INT          NOT NULL,
    monto_total       DECIMAL(12,2) NOT NULL,
    numero_cuotas     INT          NOT NULL,
    valor_cuota       DECIMAL(12,2) NOT NULL,
    saldo_pendiente   DECIMAL(12,2) NOT NULL,
    fecha_apertura    DATE         NOT NULL,
    fecha_vencimiento DATE         NOT NULL,
    estado            ENUM('ACTIVO','PAGADO','VENCIDO') NOT NULL DEFAULT 'ACTIVO',

    PRIMARY KEY (id),
    CONSTRAINT fk_credito_cliente
        FOREIGN KEY (id_cliente) REFERENCES clientes(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);
