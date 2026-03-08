package com.forjix.cuentoskilla.service;

import com.forjix.cuentoskilla.model.Boleta;
import com.forjix.cuentoskilla.model.BoletaGeneracionEstado;
import com.forjix.cuentoskilla.model.Order;
import com.forjix.cuentoskilla.model.OrderItem;
import com.forjix.cuentoskilla.model.OrderStatus;
import com.forjix.cuentoskilla.repository.BoletaRepository;
import com.forjix.cuentoskilla.repository.OrderRepository;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.ExceptionConverter;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
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
import java.awt.Color;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class BoletaService {

    private static final Logger logger = LoggerFactory.getLogger(BoletaService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final Charset CP1252 = Charset.forName("windows-1252");
    private static final Color COLOR_PRIMARY = new Color(57, 27, 16);
    private static final Color COLOR_BORDER = new Color(151, 113, 83);
    private static final Color COLOR_SOFT_GREEN = new Color(239, 246, 240);
    private static final Color COLOR_SOFT_BEIGE = new Color(239, 222, 200);
    private static final Color COLOR_SOFT_IVORY = new Color(250, 246, 239);
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
        return generarBoleta(orderId, false);
    }

    @Transactional
    public BoletaRetryResult reintentarGeneracionBoleta(Long orderId) {
        Boleta boleta = generarBoleta(orderId, true);
        return new BoletaRetryResult(
                orderId,
                boleta.getNumeroComprobante(),
                boleta.getEstadoGeneracion().name(),
                boleta.getIntentos(),
                boleta.getUltimoError()
        );
    }

    private Boleta generarBoleta(Long orderId, boolean forzarRegeneracion) {
        Optional<Boleta> existente = boletaRepository.findByOrder_Id(orderId);
        if (existente.isPresent()) {
            Boleta boleta = existente.get();
            if (!forzarRegeneracion
                    && boleta.getEstadoGeneracion() == BoletaGeneracionEstado.GENERADA
                    && hasReadableFile(boleta.getFilePath())) {
                return boleta;
            }

            Order orderExistente = orderRepository.findById(orderId)
                    .orElseThrow(() -> new NoSuchElementException("Pedido no encontrado"));
            if (orderExistente.getEstado() != OrderStatus.PAGO_VERIFICADO) {
                throw new IllegalStateException("ORDER_NOT_READY_FOR_BOLETA");
            }

            if (forzarRegeneracion) {
                boleta.setEstadoGeneracion(BoletaGeneracionEstado.PENDIENTE);
                boleta.setUltimoError(null);
                boleta.setFilePath(null);
            }
            procesarGeneracion(boleta, orderExistente);
            return boleta;
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Pedido no encontrado"));

        if (order.getEstado() != OrderStatus.PAGO_VERIFICADO) {
            throw new IllegalStateException("ORDER_NOT_READY_FOR_BOLETA");
        }

        Boleta boleta = crearRegistroBoletaPendiente(order);
        procesarGeneracion(boleta, order);
        return boleta;
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
            boleta.setUltimoError(truncateError(extractErrorMessage(e)));
            boletaRepository.save(boleta);
            logger.error("Error generando PDF de boleta. orderId={}, numeroComprobante={}, intento={}",
                    order.getId(), boleta.getNumeroComprobante(), boleta.getIntentos(), e);
        }
    }

    private void generarPdf(Path destination, Order order, String numeroComprobante) {
        Document document = new Document(PageSize.A4, 36f, 36f, 28f, 28f);

        try (OutputStream outputStream = Files.newOutputStream(destination)) {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            String razonSocial = facturacionConfigService.obtenerValorObligatorio("EMPRESA_RAZON_SOCIAL");
            String ruc = facturacionConfigService.obtenerValorObligatorio("EMPRESA_RUC");
            String direccion = facturacionConfigService.obtenerValorObligatorio("EMPRESA_DIRECCION_FISCAL");

            String clienteNombre = order.getUser() != null ? order.getUser().getNombre() : null;
            String clienteCorreo = order.getUser() != null ? order.getUser().getEmail() : null;
            String clienteDocumento = order.getUser() != null ? order.getUser().getDocumento() : null;
            LocalDateTime fechaEmision = order.getCreatedAt() != null ? order.getCreatedAt() : LocalDateTime.now();
            BigDecimal total = order.getTotal() != null ? order.getTotal() : BigDecimal.ZERO;

            agregarEncabezado(document, numeroComprobante, fechaEmision);
            agregarBloqueDatos(document, razonSocial, ruc, direccion, clienteNombre, clienteCorreo, clienteDocumento);
            agregarTablaDetalle(document, order.getItems());
            agregarResumen(document, total);
            agregarPie(document);
            document.close();
        } catch (IOException | RuntimeException e) {
            if (document.isOpen()) {
                try {
                    document.close();
                } catch (RuntimeException closeException) {
                    e.addSuppressed(closeException);
                }
            }
            throw new IllegalStateException("No se pudo generar el PDF de boleta", e);
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

    private String safeText(String value) {
        return (value == null || value.isBlank()) ? "N/D" : value;
    }

    private boolean isNotEmpty(Collection<?> value) {
        return value != null && !value.isEmpty();
    }

    private void agregarEncabezado(Document document, String numeroComprobante, LocalDateTime fechaEmision) throws DocumentException {
        Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, COLOR_PRIMARY);
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 30, COLOR_PRIMARY);
        Font numberFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 26, COLOR_PRIMARY);
        Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 11, COLOR_PRIMARY);

        PdfPTable brandRow = new PdfPTable(new float[]{1f, 1f});
        brandRow.setWidthPercentage(100f);
        brandRow.getDefaultCell().setBorder(PdfPCell.NO_BORDER);

        PdfPCell brandCell = createNoBorderCell();
        Paragraph brand = new Paragraph();
        brand.add(new Chunk(sanitizeForPdf("Cuentos"), brandFont));
        brand.add(Chunk.NEWLINE);
        brand.add(new Chunk(sanitizeForPdf("de Killa"), brandFont));
        brandCell.addElement(brand);
        brandRow.addCell(brandCell);
        brandRow.addCell(createNoBorderCell());
        document.add(brandRow);

        PdfPTable titleRow = new PdfPTable(new float[]{62f, 38f});
        titleRow.setWidthPercentage(100f);
        titleRow.setSpacingBefore(6f);
        titleRow.setSpacingAfter(4f);

        PdfPCell titleCell = createNoBorderCell();
        Paragraph title = new Paragraph(sanitizeForPdf("BOLETA DE VENTA ELECTRONICA"), titleFont);
        title.setLeading(32f);
        titleCell.addElement(title);
        titleRow.addCell(titleCell);

        PdfPCell numberCell = createNoBorderCell();
        numberCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph number = new Paragraph(sanitizeForPdf("Nro " + safeText(numeroComprobante)), numberFont);
        number.setAlignment(Element.ALIGN_RIGHT);
        numberCell.addElement(number);
        Paragraph date = new Paragraph(sanitizeForPdf("Fecha emision: " + DATE_FORMATTER.format(fechaEmision)), dateFont);
        date.setAlignment(Element.ALIGN_RIGHT);
        numberCell.addElement(date);
        titleRow.addCell(numberCell);
        document.add(titleRow);
    }

    private void agregarBloqueDatos(Document document,
                                    String razonSocial,
                                    String ruc,
                                    String direccion,
                                    String clienteNombre,
                                    String clienteCorreo,
                                    String clienteDocumento) throws DocumentException {
        Font sectionTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, COLOR_PRIMARY);
        Paragraph section = new Paragraph(sanitizeForPdf("DATOS DEL EMISOR Y CLIENTE"), sectionTitle);
        section.setSpacingBefore(8f);
        section.setSpacingAfter(8f);
        document.add(section);

        PdfPTable container = new PdfPTable(new float[]{1f, 1f});
        container.setWidthPercentage(100f);

        PdfPCell emisor = createInfoCell();
        addLabelValueLine(emisor, "EMISOR:", razonSocial);
        addLabelValueLine(emisor, "RUC:", ruc);
        addLabelValueLine(emisor, "Direccion fiscal:", direccion);
        container.addCell(emisor);

        PdfPCell cliente = createInfoCell();
        addLabelValueLine(cliente, "CLIENTE:", safeText(clienteNombre));
        addLabelValueLine(cliente, "Correo:", safeText(clienteCorreo));
        addLabelValueLine(cliente, "Documento:", safeText(clienteDocumento));
        container.addCell(cliente);
        container.setSpacingAfter(14f);

        document.add(container);
    }

    private void agregarTablaDetalle(Document document, Collection<OrderItem> items) throws DocumentException {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COLOR_PRIMARY);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 12, COLOR_PRIMARY);

        PdfPTable table = new PdfPTable(new float[]{58f, 13f, 14f, 15f});
        table.setWidthPercentage(100f);
        table.setSpacingAfter(12f);

        table.addCell(createHeaderCell("Descripcion del Articulo", headerFont, Element.ALIGN_LEFT));
        table.addCell(createHeaderCell("Cant.", headerFont, Element.ALIGN_CENTER));
        table.addCell(createHeaderCell("P.U.", headerFont, Element.ALIGN_RIGHT));
        table.addCell(createHeaderCell("Subtotal", headerFont, Element.ALIGN_RIGHT));

        if (isNotEmpty(items)) {
            for (OrderItem item : items) {
                if (item == null) {
                    continue;
                }
                BigDecimal subtotal = item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO;
                BigDecimal unitPrice = BigDecimal.valueOf(item.getPrecio_unitario());

                table.addCell(createBodyCell("- " + safeText(item.getNombre()), valueFont, Element.ALIGN_LEFT));
                table.addCell(createBodyCell(String.valueOf(item.getCantidad()), valueFont, Element.ALIGN_CENTER));
                table.addCell(createBodyCell("S/ " + money(unitPrice), valueFont, Element.ALIGN_RIGHT));
                table.addCell(createBodyCell("S/ " + money(subtotal), valueFont, Element.ALIGN_RIGHT));
            }
        } else {
            PdfPCell emptyCell = createBodyCell("Sin detalle de articulos", valueFont, Element.ALIGN_LEFT);
            emptyCell.setColspan(4);
            table.addCell(emptyCell);
        }

        document.add(table);
    }

    private void agregarResumen(Document document, BigDecimal total) throws DocumentException {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, COLOR_PRIMARY);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 14, COLOR_PRIMARY);
        Font valueBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, COLOR_PRIMARY);

        PdfPTable resumen = new PdfPTable(new float[]{74f, 26f});
        resumen.setWidthPercentage(100f);
        resumen.addCell(createSummaryCell("Total Exonerado IGV (Ley 31053):", labelFont, Element.ALIGN_RIGHT));
        resumen.addCell(createSummaryCell("S/ " + money(total), valueBold, Element.ALIGN_RIGHT));
        resumen.addCell(createSummaryCell("Codigo SUNAT CPE:", labelFont, Element.ALIGN_RIGHT));
        resumen.addCell(createSummaryCell("03", valueFont, Element.ALIGN_RIGHT));
        resumen.addCell(createSummaryCell("Afectacion IGV:", labelFont, Element.ALIGN_RIGHT));
        resumen.addCell(createSummaryCell("20 (Exonerado)", valueFont, Element.ALIGN_RIGHT));

        PdfPTable wrapper = new PdfPTable(1);
        wrapper.setWidthPercentage(50f);
        wrapper.setHorizontalAlignment(Element.ALIGN_RIGHT);
        PdfPCell wrapCell = new PdfPCell(resumen);
        wrapCell.setBorderColor(COLOR_BORDER);
        wrapCell.setBorderWidth(1.5f);
        wrapCell.setPadding(10f);
        wrapCell.setBackgroundColor(COLOR_SOFT_IVORY);
        wrapper.addCell(wrapCell);
        wrapper.setSpacingAfter(16f);
        document.add(wrapper);
    }

    private void agregarPie(Document document) throws DocumentException {
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 13, COLOR_PRIMARY);
        Font strongFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, COLOR_PRIMARY);
        Font qrFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, COLOR_PRIMARY);

        PdfPTable footer = new PdfPTable(new float[]{86f, 14f});
        footer.setWidthPercentage(100f);

        PdfPCell textCell = createNoBorderCell();
        Paragraph line1 = new Paragraph();
        line1.setAlignment(Element.ALIGN_CENTER);
        line1.add(new Chunk(sanitizeForPdf("Gracias por tu compra en "), normalFont));
        line1.add(new Chunk(sanitizeForPdf("Cuentos de Killa!"), strongFont));
        textCell.addElement(line1);
        Paragraph line2 = new Paragraph(sanitizeForPdf("Fomentando la imaginacion."), normalFont);
        line2.setAlignment(Element.ALIGN_CENTER);
        textCell.addElement(line2);
        footer.addCell(textCell);

        PdfPCell qrCell = new PdfPCell(new Phrase(sanitizeForPdf("Validar SUNAT\nQR"), qrFont));
        qrCell.setBorderColor(COLOR_BORDER);
        qrCell.setBorderWidth(1.2f);
        qrCell.setMinimumHeight(64f);
        qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        qrCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        qrCell.setPadding(6f);
        footer.addCell(qrCell);

        document.add(footer);
    }

    private void addLabelValueLine(PdfPCell cell, String label, String value) {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COLOR_PRIMARY);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 12, COLOR_PRIMARY);
        Paragraph paragraph = new Paragraph();
        paragraph.setSpacingAfter(4f);
        paragraph.add(new Chunk(sanitizeForPdf(label + " "), labelFont));
        paragraph.add(new Chunk(sanitizeForPdf(safeText(value)), valueFont));
        cell.addElement(paragraph);
    }

    private PdfPCell createNoBorderCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setPadding(0f);
        return cell;
    }

    private PdfPCell createInfoCell() {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(12f);
        cell.setBackgroundColor(COLOR_SOFT_GREEN);
        cell.setBorderColor(COLOR_BORDER);
        cell.setBorderWidth(1f);
        return cell;
    }

    private PdfPCell createHeaderCell(String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(sanitizeForPdf(text), font));
        cell.setPadding(9f);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(COLOR_SOFT_BEIGE);
        cell.setBorderColor(COLOR_PRIMARY);
        cell.setBorderWidth(1f);
        return cell;
    }

    private PdfPCell createBodyCell(String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(sanitizeForPdf(text), font));
        cell.setPadding(9f);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorderColor(COLOR_PRIMARY);
        cell.setBorderWidth(1f);
        return cell;
    }

    private PdfPCell createSummaryCell(String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(sanitizeForPdf(text), font));
        cell.setPadding(4f);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setBackgroundColor(COLOR_SOFT_IVORY);
        return cell;
    }

    private String money(BigDecimal value) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value;
        return String.format(Locale.US, "%.2f", safe.doubleValue());
    }

    private String sanitizeForPdf(String input) {
        if (input == null) {
            return "";
        }

        CharsetEncoder encoder = CP1252.newEncoder();
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (encoder.canEncode(c)) {
                out.append(c);
            } else {
                out.append('?');
            }
        }
        return out.toString();
    }

    private String extractErrorMessage(Throwable throwable) {
        if (throwable == null) {
            return "Error sin detalle";
        }

        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && !message.isBlank()) {
                return message;
            }

            if (current instanceof ExceptionConverter exceptionConverter) {
                Exception wrapped = exceptionConverter.getException();
                if (wrapped != null && wrapped != current) {
                    current = wrapped;
                    continue;
                }
            }

            current = current.getCause();
        }

        return throwable.getClass().getSimpleName();
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


