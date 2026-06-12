package testjava;


import controller.GestorClientes;
import model.Cliente;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

public class GestorCredito {
     GestorClientes gestorClientes = new GestorClientes();

    private final CreditoDAO creditoDAO;

    public GestorCredito() {
        this.creditoDAO = new CreditoDAO();
    }

    // -----------------------------------------------------------------------
    // Registrar un crédito nuevo para un cliente existente
    // -----------------------------------------------------------------------
    public String registrarCredito(Cliente cliente, double monto, int cuotas, Date fechaVencimiento) {
        if (monto <= 0)  return " Error: El monto del crédito debe ser mayor a cero.";
        if (cuotas <= 0) return " Error: El número de cuotas debe ser mayor a cero.";
        if (fechaVencimiento == null || fechaVencimiento.before(new Date()))
            return " Error: La fecha de vencimiento debe ser futura.";

        Credito credito = new Credito(cliente, monto, cuotas, fechaVencimiento);

        try {
            creditoDAO.registrar(credito);
            return String.format(
                " Crédito #%d registrado. Cliente: %s | Cuotas: %d x $%,.2f",
                credito.getIdCredito(), cliente.getNombre(),
                credito.getNumeroCuotas(), credito.getValorCuota()
            );
        } catch (SQLException e) {
            return " Error en BD al registrar crédito: " + e.getMessage();
        }
    }

    // -----------------------------------------------------------------------
    // Registrar el pago de una cuota
    // -----------------------------------------------------------------------
    public String pagarCuota(Credito credito) {
        if (credito.getEstado() == EstadoCredito.PAGADO)
            return " Este crédito ya está completamente pagado.";

        credito.pagarCuota();

        try {
            creditoDAO.actualizarSaldoYEstado(credito);
            return String.format(
                " Pago registrado. Saldo pendiente: $%,.2f | Estado: %s",
                credito.getSaldoPendiente(), credito.getEstado()
            );
        } catch (SQLException e) {
            return " Error en BD al registrar pago: " + e.getMessage();
        }
    }

    // -----------------------------------------------------------------------
    // Consultas
    // -----------------------------------------------------------------------
    public List<Credito> obtenerTodos() throws SQLException {
        return creditoDAO.listarTodos();
    }

    public List<Credito> obtenerPorCliente(int idCliente) throws SQLException {
        return creditoDAO.buscarPorCliente(idCliente);
    }
    
    
   public String adquirirCreditoPorCedula(String cedulaCliente, double monto,
                                           int numeroCuotas, Date fechaVencimiento) {
        
        // 2. SEGUNDO CAMBIO CRÍTICO: Usar la variable en minúscula 'gestorClientes'
        // Además, como 'buscarClientePorCedula' está dentro de GestorClientes, lo llamas a través de él.
      
        Cliente cliente = gestorClientes.buscarClientePorCedula(cedulaCliente);

        if (cliente == null) {
            return " Error: No se encontró ningún cliente con la identificación: " + cedulaCliente;
        }

        // Delegar toda la lógica y persistencia a GestorClientes (¡Esto ya está perfecto en minúscula!)
        return gestorClientes.adquirirCredito(cliente, monto, numeroCuotas, fechaVencimiento);
    }

}
