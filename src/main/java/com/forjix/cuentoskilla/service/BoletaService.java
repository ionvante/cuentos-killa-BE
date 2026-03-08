package com.forjix.cuentoskilla.service;

import com.forjix.cuentoskilla.model.Boleta;
import com.forjix.cuentoskilla.model.BoletaGeneracionEstado;
import com.forjix.cuentoskilla.model.Order;
import com.forjix.cuentoskilla.model.OrderItem;
import com.forjix.cuentoskilla.model.OrderStatus;
import com.forjix.cuentoskilla.repository.BoletaRepository;
import com.forjix.cuentoskilla.repository.OrderRepository;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class BoletaService {

    private static final Logger logger = LoggerFactory.getLogger(BoletaService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final CharsetEncoder CP1252_ENCODER = Charset.forName("windows-1252").newEncoder();
    private static final int MAX_ERROR_LENGTH = 500;

    private final BoletaRepository boletaRepository;
    private final OrderRepository orderRepository;
    private final FacturacionConfigService facturacionConfigService;

    @Value("${boleta.upload-dir:uploads/boletas}")
    private String boletaUploadDir;

    private Path boletaRoot;

    public BoletaService(BoletaRepository boletaRepository,
                         OrderRepository orderRepository,
                         FacturacionConfigService facturacionConfigService) {
        this.boletaRepository = boletaRepository;
        this.orderRepository = orderRepository;
        this.facturacionConfigService = facturacionConfigService;
    }

    @PostConstruct
    public void init() {
        try {
            boletaRoot = Paths.get(boletaUploadDir).toAbsolutePath().normalize();
            Files.createDirectories(boletaRoot);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo inicializar el directorio de boletas", e);
        }
    }

    @Transactional
    public Boleta generarBoletaSiCorresponde(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Pedido no encontrado"));

        if (order.getEstado() != OrderStatus.PAGO_VERIFICADO) {
            throw new IllegalStateException("ORDER_NOT_READY_FOR_BOLETA");
        }

        Boleta boleta = boletaRepository.findByOrder_Id(orderId)
                .orElseGet(() -> crearRegistroBoletaPendiente(order));

        if (boleta.getEstadoGeneracion() == BoletaGeneracionEstado.GENERADA && hasReadableFile(boleta.getFilePath())) {
            return boleta;
        }

        procesarGeneracion(boleta, order);
        return boleta;
    }

    @Transactional
    public BoletaRetryResult reintentarGeneracionBoleta(Long orderId) {
        Boleta boleta = generarBoletaSiCorresponde(orderId);
        return new BoletaRetryResult(
                orderId,
                boleta.getNumeroComprobante(),
                boleta.getEstadoGeneracion().name(),
                boleta.getIntentos(),
                boleta.getUltimoError()
        );
    }

    @Transactional(readOnly = true)
    public BoletaArchivo obtenerBoletaParaDescarga(Long orderId, Long requesterId, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Pedido no encontrado"));

        if (!isAdmin && !order.getUser().getId().equals(requesterId)) {
            throw new SecurityException("ACCESS_DENIED");
        }

        Boleta boleta = boletaRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> {
                    if (order.getEstado() != OrderStatus.PAGO_VERIFICADO) {
                        return new IllegalStateException("ORDER_NOT_READY_FOR_BOLETA");
                    }
                    return new IllegalStateException("BOLETA_NOT_READY");
                });

        if (boleta.getEstadoGeneracion() != BoletaGeneracionEstado.GENERADA || !hasReadableFile(boleta.getFilePath())) {
            throw new IllegalStateException("BOLETA_NOT_READY");
        }

        Path filePath = Paths.get(boleta.getFilePath()).normalize().toAbsolutePath();
        return new BoletaArchivo(filePath, boleta.getNumeroComprobante());
    }

    private Boleta crearRegistroBoletaPendiente(Order order) {
        String serie = facturacionConfigService.obtenerValorObligatorio(FacturacionConfigService.CODIGO_SERIE_ACTIVA);
        int correlativo = facturacionConfigService.tomarSiguienteCorrelativoBoleta();
        String numeroComprobante = String.format("%s-%08d", serie, correlativo);

        Boleta boleta = new Boleta();
        boleta.setOrder(order);
        boleta.setSerie(serie);
        boleta.setCorrelativo(correlativo);
        boleta.setNumeroComprobante(numeroComprobante);
        boleta.setEstadoGeneracion(BoletaGeneracionEstado.PENDIENTE);
        boleta.setIntentos(0);
        boleta.setUltimoError(null);
        boleta.setFilePath(null);

        try {
            return boletaRepository.save(boleta);
        } catch (DataIntegrityViolationException ex) {
            return boletaRepository.findByOrder_Id(order.getId())
                    .orElseThrow(() -> ex);
        }
    }

    private void procesarGeneracion(Boleta boleta, Order order) {
        String filename = "boleta_" + boleta.getNumeroComprobante().replace("-", "_") + ".pdf";
        Path destination = boletaRoot.resolve(filename).normalize().toAbsolutePath();

        Integer actualIntentos = boleta.getIntentos() == null ? 0 : boleta.getIntentos();
        boleta.setIntentos(actualIntentos + 1);

        try {
            generarPdf(destination, order, boleta.getNumeroComprobante());
            boleta.setFilePath(destination.toString());
            boleta.setEstadoGeneracion(BoletaGeneracionEstado.GENERADA);
            boleta.setUltimoError(null);
            boletaRepository.save(boleta);
        } catch (RuntimeException e) {
            boleta.setEstadoGeneracion(BoletaGeneracionEstado.ERROR);
            boleta.setUltimoError(truncateError(e.getMessage()));
            boletaRepository.save(boleta);
            logger.error("Error generando PDF de boleta. orderId={}, numeroComprobante={}, intento={}",
                    order.getId(), boleta.getNumeroComprobante(), boleta.getIntentos(), e);
        }
    }

    private void generarPdf(Path destination, Order order, String numeroComprobante) {
        Document document = new Document(PageSize.A4);

        try (OutputStream outputStream = Files.newOutputStream(destination)) {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

            addParagraphSafe(document, titleFont, "BOLETA DE VENTA ELECTRONICA");
            addParagraphSafe(document, regularFont, "N° " + safeText(numeroComprobante));

            LocalDateTime fechaEmision = order.getCreatedAt() != null ? order.getCreatedAt() : LocalDateTime.now();
            addParagraphSafe(document, regularFont, "Fecha emision: " + DATE_FORMATTER.format(fechaEmision));
            addParagraphSafe(document, regularFont, " ");

            String razonSocial = facturacionConfigService.obtenerValorObligatorio("EMPRESA_RAZON_SOCIAL");
            String ruc = facturacionConfigService.obtenerValorObligatorio("EMPRESA_RUC");
            String direccion = facturacionConfigService.obtenerValorObligatorio("EMPRESA_DIRECCION_FISCAL");

            addParagraphSafe(document, regularFont, "Emisor: " + razonSocial);
            addParagraphSafe(document, regularFont, "RUC: " + ruc);
            addParagraphSafe(document, regularFont, "Direccion fiscal: " + direccion);
            addParagraphSafe(document, regularFont, " ");

            String clienteNombre = order.getUser() != null ? order.getUser().getNombre() : null;
            String clienteCorreo = order.getUser() != null ? order.getUser().getEmail() : null;
            String clienteDocumento = order.getUser() != null ? order.getUser().getDocumento() : null;

            addParagraphSafe(document, regularFont, "Cliente: " + safeText(clienteNombre));
            addParagraphSafe(document, regularFont, "Correo: " + safeText(clienteCorreo));
            if (clienteDocumento != null && !clienteDocumento.isBlank()) {
                addParagraphSafe(document, regularFont, "Documento: " + clienteDocumento);
            }
            addParagraphSafe(document, regularFont, " ");

            addParagraphSafe(document, regularFont, "Detalle:");
            if (isNotEmpty(order.getItems())) {
                for (OrderItem item : order.getItems()) {
                    if (item == null) {
                        continue;
                    }
                    BigDecimal subtotal = item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO;
                    String linea = String.format("- %s | Cant: %d | P.U.: %.2f | Subtotal: %.2f",
                            safeText(item.getNombre()),
                            item.getCantidad(),
                            item.getPrecio_unitario(),
                            subtotal.doubleValue());
                    addParagraphSafe(document, regularFont, linea);
                }
            }

            addParagraphSafe(document, regularFont, " ");
            BigDecimal total = order.getTotal() != null ? order.getTotal() : BigDecimal.ZERO;
            addParagraphSafe(document, regularFont, "Total exonerado IGV (Ley 31053): S/ " + total);
            addParagraphSafe(document, regularFont, "Codigo SUNAT CPE: 03");
            addParagraphSafe(document, regularFont, "Afectacion IGV: 20 (Exonerado)");
        } catch (IOException | DocumentException | RuntimeException e) {
            throw new IllegalStateException("No se pudo generar el PDF de boleta", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    private String truncateError(String message) {
        String value = message == null ? "Error sin detalle" : message;
        if (value.length() <= MAX_ERROR_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_ERROR_LENGTH);
    }

    private boolean hasReadableFile(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        try {
            Path filePath = Paths.get(path).normalize().toAbsolutePath();
            return Files.exists(filePath) && Files.isReadable(filePath);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private void addParagraphSafe(Document document, Font font, String content) throws DocumentException {
        document.add(new Paragraph(sanitizeForPdf(content), font));
    }

    private String safeText(String value) {
        return (value == null || value.isBlank()) ? "N/D" : value;
    }

    private boolean isNotEmpty(Collection<?> value) {
        return value != null && !value.isEmpty();
    }

    private String sanitizeForPdf(String input) {
        if (input == null) {
            return "";
        }

        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (CP1252_ENCODER.canEncode(c)) {
                out.append(c);
            } else {
                out.append('?');
            }
        }
        return out.toString();
    }

    public record BoletaArchivo(Path filePath, String numeroComprobante) {
    }

    public record BoletaRetryResult(Long orderId,
                                    String numeroComprobante,
                                    String estadoGeneracion,
                                    Integer intentos,
                                    String ultimoError) {
    }
}
