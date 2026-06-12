package testjava;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import persistencia.ConexionDB;

public class CreditoDAO {

    private Connection getConexion() {
        return ConexionDB.getInstancia().getConexion();
    }

    // -----------------------------------------------------------------------
    // C — Registrar nuevo crédito
    // -----------------------------------------------------------------------
    public void registrar(Credito credito) throws SQLException {
        String sql = """
                INSERT INTO creditos
                    (id_cliente, monto_total, numero_cuotas, valor_cuota,
                     saldo_pendiente, fecha_apertura, fecha_vencimiento, estado)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = getConexion().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt   (1, credito.getId());
            ps.setDouble(2, credito.getMontoTotal());
            ps.setInt   (3, credito.getNumeroCuotas());
            ps.setDouble(4, credito.getValorCuota());
            ps.setDouble(5, credito.getSaldoPendiente());
            ps.setDate  (6, new java.sql.Date(credito.getFechaApertura().getTime()));
            ps.setDate  (7, new java.sql.Date(credito.getFechaVencimiento().getTime()));
            ps.setString(8, credito.getEstado().name());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) credito.setIdCredito(keys.getInt(1));
            }
        }
    }

    // -----------------------------------------------------------------------
    // R — Listar todos los créditos (JOIN con clientes)
    // -----------------------------------------------------------------------
    public List<Credito> listarTodos() throws SQLException {
        String sql = """
                SELECT cr.*, cl.nombre, cl.identificacion, cl.correo, cl.telefono
                FROM creditos cr
                INNER JOIN clientes cl ON cr.id_cliente = cl.id
                """;

        List<Credito> lista = new ArrayList<>();

        try (PreparedStatement ps = getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    // -----------------------------------------------------------------------
    // R — Buscar créditos por cliente
    // -----------------------------------------------------------------------
    public List<Credito> buscarPorCliente(int idCliente) throws SQLException {
        String sql = """
                SELECT cr.*, cl.nombre, cl.identificacion, cl.correo, cl.telefono
                FROM creditos cr
                INNER JOIN clientes cl ON cr.id_cliente = cl.id
                WHERE cr.id_cliente = ?
                """;

        List<Credito> lista = new ArrayList<>();

        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    // -----------------------------------------------------------------------
    // U — Actualizar saldo y estado (usado al pagar cuota)
    // -----------------------------------------------------------------------
    public void actualizarSaldoYEstado(Credito credito) throws SQLException {
        String sql = "UPDATE creditos SET saldo_pendiente = ?, estado = ? WHERE id = ?";

        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setDouble(1, credito.getSaldoPendiente());
            ps.setString(2, credito.getEstado().name());
            ps.setInt   (3, credito.getIdCredito());
            ps.executeUpdate();
        }
    }

    // -----------------------------------------------------------------------
    // Mapper: ResultSet → Credito
    // -----------------------------------------------------------------------
    private Credito mapear(ResultSet rs) throws SQLException {
        return new Credito(
            rs.getInt   ("id"),
            rs.getInt   ("id_cliente"),
            rs.getString("nombre"),
            rs.getString("identificacion"),
            rs.getString("correo"),
            rs.getString("telefono"),
            rs.getDouble("monto_total"),
            rs.getInt   ("numero_cuotas"),
            rs.getDouble("saldo_pendiente"),
            rs.getDate  ("fecha_apertura"),
            rs.getDate  ("fecha_vencimiento"),
            EstadoCredito.valueOf(rs.getString("estado"))
        );
    }
}