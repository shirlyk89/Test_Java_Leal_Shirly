/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package testjava;
import java.util.Date;
import model.Cliente;
import testjava.EstadoCredito;

/**
 *
 * @author camper
 */
public class Credito extends Cliente {
     private int idCredito;
    private double montoTotal;
    private int numeroCuotas;
    private double valorCuota;        // Se calcula automáticamente: montoTotal / numeroCuotas
    private double saldoPendiente;    // Se reduce con cada cuota pagada
    private Date fechaApertura;
    private Date fechaVencimiento;
    private EstadoCredito estado;

    public Credito() {
        super();
        this.fechaApertura = new Date();
        this.estado = EstadoCredito.ACTIVO;
    }

    /**
     * Constructor completo para reconstruir desde la base de datos.
     */
    public Credito(int idCredito, int idCliente, String nombre, String identificacion,
                   String correo, String telefono,
                   double montoTotal, int numeroCuotas,
                   double saldoPendiente, Date fechaApertura,
                   Date fechaVencimiento, EstadoCredito estado) {
        super(idCliente, nombre, identificacion, correo, telefono);
        this.idCredito       = idCredito;
        this.montoTotal      = montoTotal;
        this.numeroCuotas    = numeroCuotas;
        this.valorCuota      = calcularValorCuota();
        this.saldoPendiente  = saldoPendiente;
        this.fechaApertura   = fechaApertura;
        this.fechaVencimiento = fechaVencimiento;
        this.estado          = estado;
    }

    /**
     * Constructor de negocio: registrar un crédito nuevo para un cliente existente.
     * El saldo inicial es igual al monto total.
     */
    public Credito(Cliente cliente, double montoTotal, int numeroCuotas, Date fechaVencimiento) {
        super(cliente.getId(), cliente.getNombre(), cliente.getIdentificacion(),
              cliente.getCorreo(), cliente.getTelefono());
        this.montoTotal      = montoTotal;
        this.numeroCuotas    = numeroCuotas;
        this.valorCuota      = calcularValorCuota();
        this.saldoPendiente  = montoTotal;
        this.fechaApertura   = new Date();
        this.fechaVencimiento = fechaVencimiento;
        this.estado          = EstadoCredito.ACTIVO;
    }

    // -----------------------------------------------------------------------
    // Lógica de negocio
    // -----------------------------------------------------------------------

    private double calcularValorCuota() {
        return (numeroCuotas > 0) ? montoTotal / numeroCuotas : 0.0;
    }

    /**
     * Registra el pago de una cuota y actualiza el saldo y el estado.
     * @return true si el pago fue aplicado, false si el crédito ya estaba saldado.
     */
    public boolean pagarCuota() {
        if (estado == EstadoCredito.PAGADO) return false;

        saldoPendiente = Math.max(0, saldoPendiente - valorCuota);

        if (saldoPendiente == 0) {
            estado = EstadoCredito.PAGADO;
        }
        return true;
    }

    // -----------------------------------------------------------------------
    // Getters y Setters
    // -----------------------------------------------------------------------

    public int getIdCredito() { return idCredito; }
    public void setIdCredito(int idCredito) { this.idCredito = idCredito; }

    public double getMontoTotal() { return montoTotal; }
    public void setMontoTotal(double montoTotal) {
        this.montoTotal = montoTotal;
        this.valorCuota = calcularValorCuota();
    }

    public int getNumeroCuotas() { return numeroCuotas; }
    public void setNumeroCuotas(int numeroCuotas) {
        this.numeroCuotas = numeroCuotas;
        this.valorCuota   = calcularValorCuota();
    }

    public double getValorCuota() { return valorCuota; }

    public double getSaldoPendiente() { return saldoPendiente; }
    public void setSaldoPendiente(double saldoPendiente) { this.saldoPendiente = saldoPendiente; }

    public Date getFechaApertura() { return fechaApertura; }
    public void setFechaApertura(Date fechaApertura) { this.fechaApertura = fechaApertura; }

    public Date getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(Date fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }

    public EstadoCredito getEstado() { return estado; }
    public void setEstado(EstadoCredito estado) { this.estado = estado; }

    @Override
    public String toString() {
        return String.format(
            "Credito #%d | Cliente: %s [%s] | Monto: $%,.2f | Cuotas: %d x $%,.2f | Saldo: $%,.2f | Estado: %s",
            idCredito, getNombre(), getIdentificacion(),
            montoTotal, numeroCuotas, valorCuota, saldoPendiente, estado
        );
    }
    
}
