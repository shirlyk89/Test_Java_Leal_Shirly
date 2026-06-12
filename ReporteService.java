package testjava;

import controller.GestorVentas;
import controller.GestorCelulares;
import model.Celular;
import persistencia.ConexionDB;
import utils.ArchivoUtils;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio Singleton para la generación del Reporte Global de Gestión.
 * Agrega datos de ventas, inventario y créditos desde MySQL.
 */
public class ReporteService {

    // -----------------------------------------------------------------------
    // SINGLETON
    // -----------------------------------------------------------------------
    private static ReporteService instancia;

    private final GestorVentas    gestorVentas;
    private final GestorCelulares gestorCelulares;
    private final GestorCredito  gestorCreditos;

    private static final int    STOCK_MINIMO   = 5;
    private static final String ARCHIVO_SALIDA = "reporte_global.txt";
    private static final String SEP_DOBLE      = "=".repeat(72);
    private static final String SEP_SIMPLE     = "-".repeat(72);

    /** Constructor privado — patrón Singleton */
    private ReporteService() {
        this.gestorVentas    = new GestorVentas();
        this.gestorCelulares = new GestorCelulares();
        this.gestorCreditos  = new GestorCredito();
    }

    /** Punto de acceso global a la instancia única */
    public static synchronized ReporteService getInstancia() {
        if (instancia == null) {
            instancia = new ReporteService();
        }
        return instancia;
    }

    // -----------------------------------------------------------------------
    // MÉTODO PRINCIPAL — Orquestador
    // -----------------------------------------------------------------------
    public void generarReporteGlobal() {
        System.out.println("⏳ Conectando con la base de datos y recopilando datos...\n");

        StringBuilder reporte = new StringBuilder();
        reporte.append(construirEncabezado());
        reporte.append(seccionTotalVentas());
        reporte.append(seccionCelularesVendidosPorModelo());
        reporte.append(seccionClientesConCreditosPendientes());
        reporte.append(seccionStockActual());
        reporte.append(construirPie());

        String contenido = reporte.toString();

        // Imprimir en consola
        System.out.println(contenido);

        // Persistir en archivo
        try {
            boolean ok = ArchivoUtils.exportarReporteTxt(ARCHIVO_SALIDA, contenido);
            if (ok) {
                System.out.println("✅ Reporte guardado en: " + ARCHIVO_SALIDA);
            } else {
                System.out.println("⚠️  No se pudo escribir el archivo. Revisa permisos de escritura.");
            }
        } catch (Exception e) {
            System.out.println("❌ Error al escribir el archivo: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // SECCIÓN 1 — Total de ventas facturadas
    //   Query: SELECT SUM(total) FROM ventas
    // -----------------------------------------------------------------------
    private String seccionTotalVentas() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(SEP_DOBLE).append("\n");
        sb.append("  SECCIÓN 1 ▸ TOTAL DE VENTAS FACTURADAS\n");
        sb.append(SEP_SIMPLE).append("\n");

        try {
            double totalVentas = gestorVentas.obtenerTotalDineroVentas();

            if (totalVentas == 0.0) {
                sb.append("  ℹ️  No existen ventas registradas en el sistema.\n");
            } else {
                sb.append(String.format("  %-40s %s%n",
                    "Monto total acumulado de ventas:",
                    String.format("$%,.2f", totalVentas)));
            }

        } catch (SQLException e) {
            sb.append("  ❌ Error al consultar ventas en BD: ").append(e.getMessage()).append("\n");
        }

        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // SECCIÓN 2 — Celulares vendidos agrupados por modelo
    //   Query JDBC directo: SELECT modelo, SUM(dv.cantidad) ... GROUP BY modelo
    //   + Stream para ordenar el resultado en memoria
    // -----------------------------------------------------------------------
    private String seccionCelularesVendidosPorModelo() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(SEP_DOBLE).append("\n");
        sb.append("  SECCIÓN 2 ▸ UNIDADES VENDIDAS AGRUPADAS POR MODELO\n");
        sb.append(SEP_SIMPLE).append("\n");

        String sql = """
                SELECT c.marca, c.modelo,
                       COALESCE(SUM(dv.cantidad), 0) AS total_vendido
                FROM celulares c
                LEFT JOIN detalle_ventas dv ON c.id = dv.id_celular
                GROUP BY c.id, c.marca, c.modelo
                ORDER BY total_vendido DESC
                """;

        try (PreparedStatement ps = ConexionDB.getInstancia()
                                              .getConexion()
                                              .prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            // Cargar resultados en lista para aplicar Streams
            List<int[]> filas = new ArrayList<>();       // [0]=totalVendido
            List<String[]> etiquetas = new ArrayList<>(); // [0]=marca, [1]=modelo

            while (rs.next()) {
                etiquetas.add(new String[]{rs.getString("marca"), rs.getString("modelo")});
                filas.add(new int[]{rs.getInt("total_vendido")});
            }

            if (etiquetas.isEmpty()) {
                sb.append("  ℹ️  No hay celulares registrados en el sistema.\n");
                return sb.toString();
            }

            // Verificar si TODOS tienen 0 ventas
            boolean sinVentas = filas.stream()
                .allMatch(f -> f[0] == 0);

            if (sinVentas) {
                sb.append("  ℹ️  No se han registrado ventas de celulares aún.\n");
                return sb.toString();
            }

            sb.append(String.format("  %-5s %-15s %-20s %s%n",
                "N°", "MARCA", "MODELO", "UNIDADES VENDIDAS"));
            sb.append("  ").append(SEP_SIMPLE).append("\n");

            for (int i = 0; i < etiquetas.size(); i++) {
                sb.append(String.format("  %-5d %-15s %-20s %d unid.%n",
                    i + 1,
                    truncar(etiquetas.get(i)[0], 15),
                    truncar(etiquetas.get(i)[1], 20),
                    filas.get(i)[0]));
            }

            // Totalizador con Stream sobre la lista en memoria
            int totalUnidades = filas.stream()
                .mapToInt(f -> f[0])
                .sum();

            sb.append(SEP_SIMPLE).append("\n");
            sb.append(String.format("  %-40s %d unidades%n",
                "Total unidades vendidas:", totalUnidades));

        } catch (SQLException e) {
            sb.append("  ❌ Error al consultar detalle de ventas: ").append(e.getMessage()).append("\n");
        }

        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // SECCIÓN 3 — Clientes con créditos pendientes
    //   Stream: filtrar saldo > 0 y estado ACTIVO o VENCIDO
    // -----------------------------------------------------------------------
    private String seccionClientesConCreditosPendientes() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(SEP_DOBLE).append("\n");
        sb.append("  SECCIÓN 3 ▸ CLIENTES CON CRÉDITOS PENDIENTES\n");
        sb.append(SEP_SIMPLE).append("\n");

        try {
            List<Credito> todosLosCreditos = gestorCreditos.obtenerTodos();

            if (todosLosCreditos.isEmpty()) {
                sb.append("  ℹ️  No existen créditos registrados en el sistema.\n");
                return sb.toString();
            }

            // Stream: filtrar saldo > 0 Y estado no sea PAGADO
            List<Credito> pendientes = todosLosCreditos.stream()
                .filter(c -> c.getSaldoPendiente() > 0)
                .filter(c -> c.getEstado() != EstadoCredito.PAGADO)
                .sorted(Comparator.comparingDouble(Credito::getSaldoPendiente).reversed())
                .collect(Collectors.toList());

            if (pendientes.isEmpty()) {
                sb.append("  ✅ Todos los créditos han sido pagados. Sin pendientes.\n");
                return sb.toString();
            }

            sb.append(String.format("  %-22s %-14s %-13s %-10s %-10s%n",
                "CLIENTE", "IDENTIFICACIÓN", "MONTO TOTAL", "SALDO", "ESTADO"));
            sb.append("  ").append(SEP_SIMPLE).append("\n");

            pendientes.forEach(c ->
                sb.append(String.format("  %-22s %-14s $%-12,.2f $%-9,.2f %s%n",
                    truncar(c.getNombre(), 22),
                    c.getIdentificacion(),
                    c.getMontoTotal(),
                    c.getSaldoPendiente(),
                    iconoEstado(c.getEstado())))
            );

            // Totalizadores con Streams
            double carteraPendiente = pendientes.stream()
                .mapToDouble(Credito::getSaldoPendiente)
                .sum();

            long creditosVencidos = pendientes.stream()
                .filter(c -> c.getEstado() == EstadoCredito.VENCIDO)
                .count();

            sb.append(SEP_SIMPLE).append("\n");
            sb.append(String.format("  %-40s $%,.2f%n", "Cartera pendiente total:", carteraPendiente));
            sb.append(String.format("  %-40s %d crédito(s)%n", "Créditos vencidos:", creditosVencidos));

        } catch (SQLException e) {
            sb.append("  ❌ Error al consultar créditos: ").append(e.getMessage()).append("\n");
        }

        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // SECCIÓN 4 — Stock actual con alertas de mínimo
    //   Stream: detectar productos con stock < STOCK_MINIMO
    // -----------------------------------------------------------------------
    private String seccionStockActual() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(SEP_DOBLE).append("\n");
        sb.append("  SECCIÓN 4 ▸ STOCK ACTUAL DEL INVENTARIO\n");
        sb.append(String.format("  Umbral de alerta: productos con %d unidades o menos%n", STOCK_MINIMO));
        sb.append(SEP_SIMPLE).append("\n");

        try {
            List<Celular> celulares = gestorCelulares.obtenerTodosLosCelulares();

            if (celulares.isEmpty()) {
                sb.append("  ℹ️  No hay productos registrados en el inventario.\n");
                return sb.toString();
            }

            // Stream: separar en alerta vs normal, luego concatenar (alertas primero)
            List<Celular> enAlerta = celulares.stream()
                .filter(c -> c.getStock() <= STOCK_MINIMO)
                .sorted(Comparator.comparingInt(Celular::getStock))
                .collect(Collectors.toList());

            List<Celular> enNormal = celulares.stream()
                .filter(c -> c.getStock() > STOCK_MINIMO)
                .sorted(Comparator.comparingInt(Celular::getStock))
                .collect(Collectors.toList());

            List<Celular> ordenados = new ArrayList<>();
            ordenados.addAll(enAlerta);
            ordenados.addAll(enNormal);

            sb.append(String.format("  %-5s %-13s %-16s %-7s %-8s %s%n",
                "ID", "MARCA", "MODELO", "GAMA", "STOCK", "ESTADO"));
            sb.append("  ").append(SEP_SIMPLE).append("\n");

            ordenados.forEach(c -> {
                String estado = c.getStock() <= STOCK_MINIMO ? "🔴 BAJO STOCK" : "🟢 OK";
                sb.append(String.format("  %-5d %-13s %-16s %-7s %-8d %s%n",
                    c.getId(),
                    truncar(c.getMarca(), 13),
                    truncar(c.getModelo(), 16),
                    c.getGama(),
                    c.getStock(),
                    estado));
            });

            // Resumen con Streams
            int totalUnidades = celulares.stream().mapToInt(Celular::getStock).sum();

            sb.append(SEP_SIMPLE).append("\n");
            sb.append(String.format("  %-40s %d producto(s)%n",
                "Productos en alerta de stock:", enAlerta.size()));
            sb.append(String.format("  %-40s %d unidades%n",
                "Total unidades en inventario:", totalUnidades));

        } catch (SQLException e) {
            sb.append("  ❌ Error al consultar inventario: ").append(e.getMessage()).append("\n");
        }

        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // Encabezado y pie del reporte
    // -----------------------------------------------------------------------
    private String construirEncabezado() {
        String fecha = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm:ss"));

        return SEP_DOBLE + "\n" +
               "         REPORTE GLOBAL DE GESTIÓN — TECNOSTORE\n" +
               "         Fecha de generación: " + fecha + "\n" +
               SEP_DOBLE;
    }

    private String construirPie() {
        return "\n" + SEP_DOBLE + "\n" +
               "  FIN DEL REPORTE — TecnoStore  |  Sistema de Gestión v1.0\n" +
               SEP_DOBLE + "\n";
    }

    // -----------------------------------------------------------------------
    // Helpers privados
    // -----------------------------------------------------------------------
    private String truncar(String texto, int max) {
        if (texto == null) return "";
        return texto.length() <= max ? texto : texto.substring(0, max - 1) + "…";
    }

    private String iconoEstado(EstadoCredito estado) {
        return switch (estado) {
            case ACTIVO  -> "ACTIVO";
            case VENCIDO -> "VENCIDO ⚠";
            case PAGADO  -> "PAGADO";
        };
    }

    // -----------------------------------------------------------------------
    // MAIN — Ejecutable independiente desde consola
    // -----------------------------------------------------------------------
    public static void main(String[] args) {
        System.out.println(SEP_DOBLE);
        System.out.println("       TECNOSTORE — GENERADOR DE REPORTE GLOBAL DE GESTIÓN");
        System.out.println(SEP_DOBLE);

        // Acceso vía Singleton
        ReporteService.getInstancia().generarReporteGlobal();
    }
}