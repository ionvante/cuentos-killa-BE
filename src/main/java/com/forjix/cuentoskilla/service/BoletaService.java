package com.forjix.cuentoskilla.service;

import com.forjix.cuentoskilla.model.Boleta;
import com.forjix.cuentoskilla.model.BoletaGeneracionEstado;
import com.forjix.cuentoskilla.model.Order;
import com.forjix.cuentoskilla.model.OrderItem;
import com.forjix.cuentoskilla.model.OrderStatus;
import com.forjix.cuentoskilla.repository.BoletaRepository;
import com.forjix.cuentoskilla.repository.OrderRepository;
import com.forjix.cuentoskilla.service.storage.FirebaseStorageService;
import com.forjix.cuentoskilla.service.storage.StorageException;
import com.forjix.cuentoskilla.repository.MaestroRepository;
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
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.annotation.PostConstruct;
import java.awt.Color;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.io.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoletaService {

    private static final Logger logger = LoggerFactory.getLogger(BoletaService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final Charset CP1252 = Charset.forName("windows-1252");
    private static final Color COLOR_PRIMARY = new Color(74, 43, 35);
    private static final Color COLOR_BORDER = new Color(74, 43, 35);
    private static final Color COLOR_INFO_BLOCK = new Color(242, 245, 234);
    private static final Color COLOR_HEADER_ROW = new Color(207, 164, 118);
    private static final Color COLOR_SUMMARY_BLOCK = new Color(235, 218, 200);
    private static final Color COLOR_PAGE_BG = new Color(250, 247, 240);
    private static final Color COLOR_QR_BANNER = new Color(0, 82, 204);
    private static final int MAX_ERROR_LENGTH = 500;
    private static final String FIREBASE_PREFIX = "firebase://";
    private static final String FIREBASE_BOLETAS_DIR = "boletas/";

    private final BoletaRepository boletaRepository;
    private final OrderRepository orderRepository;
    private final FacturacionConfigService facturacionConfigService;
    private final FirebaseStorageService firebaseStorageService;
    private final MaestroRepository maestroRepository;

    @Value("${boleta.upload-dir:uploads/boletas}")
    private String boletaUploadDir;

    @Value("${storage.provider:local}")
    private String storageProvider;

    private Path boletaRoot;

    public BoletaService(BoletaRepository boletaRepository,
            OrderRepository orderRepository,
            FacturacionConfigService facturacionConfigService) {
        this(boletaRepository, orderRepository, facturacionConfigService, null, null);
    }

    @Autowired
    public BoletaService(BoletaRepository boletaRepository,
            OrderRepository orderRepository,
            FacturacionConfigService facturacionConfigService,
            FirebaseStorageService firebaseStorageService,
            MaestroRepository maestroRepository) {
        this.boletaRepository = boletaRepository;
        this.orderRepository = orderRepository;
        this.facturacionConfigService = facturacionConfigService;
        this.firebaseStorageService = firebaseStorageService;
        this.maestroRepository = maestroRepository;
    }

    @PostConstruct
    public void init() {
        if (isFirebaseProvider()) {
            return;
        }
        try {
            boletaRoot = Paths.get(boletaUploadDir).toAbsolutePath().normalize();
            Files.createDirectories(boletaRoot);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo inicializar el directorio de boletas", e);
        }
    }

    private boolean isGeneracionAlVueloEnabled() {
        if (maestroRepository == null) return false;
        return maestroRepository.findByCodigo("BOLETA_AL_VUELO")
                .map(m -> m.getEstado() != null && m.getEstado() && "true".equalsIgnoreCase(m.getValor()))
                .orElse(false);
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
                boleta.getUltimoError());
    }

    private Boleta generarBoleta(Long orderId, boolean forzarRegeneracion) {
        Optional<Boleta> existente = boletaRepository.findByOrder_Id(orderId);
        boolean alVuelo = isGeneracionAlVueloEnabled();

        if (existente.isPresent()) {
            Boleta boleta = existente.get();
            if (!forzarRegeneracion
                    && boleta.getEstadoGeneracion() == BoletaGeneracionEstado.GENERADA
                    && (alVuelo || hasReadableFile(boleta.getFilePath()))) {
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

        boolean alVuelo = isGeneracionAlVueloEnabled();

        if (boleta.getEstadoGeneracion() != BoletaGeneracionEstado.GENERADA || (!alVuelo && !hasReadableFile(boleta.getFilePath()))) {
            throw new IllegalStateException("BOLETA_NOT_READY");
        }

        if (alVuelo) {
            byte[] pdfBytes = generarPdfEnMemoria(order, boleta.getNumeroComprobante());
            return new BoletaArchivo(null, pdfBytes, boleta.getNumeroComprobante());
        }

        String storedPath = boleta.getFilePath();
        if (isFirebaseStoragePath(storedPath)) {
            try {
                ensureFirebaseConfigured();
                byte[] fileBytes = firebaseStorageService.download(stripFirebasePrefix(storedPath));
                return new BoletaArchivo(null, fileBytes, boleta.getNumeroComprobante());
            } catch (Exception e) {
                throw new IllegalStateException("BOLETA_NOT_READY", e);
            }
        }

        Path filePath = Paths.get(storedPath).normalize().toAbsolutePath();
        return new BoletaArchivo(filePath, null, boleta.getNumeroComprobante());
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
        Integer actualIntentos = boleta.getIntentos() == null ? 0 : boleta.getIntentos();
        boleta.setIntentos(actualIntentos + 1);
        String filename = boleta.getNumeroComprobante() + ".pdf";
        Path destination = null;

        try {
            destination = isFirebaseProvider()
                    ? Files.createTempFile("boleta-", ".pdf")
                    : boletaRoot.resolve(filename).normalize().toAbsolutePath();
            generarPdf(destination, order, boleta.getNumeroComprobante());
            if (isFirebaseProvider()) {
                ensureFirebaseConfigured();
                String firebasePath = FIREBASE_BOLETAS_DIR + filename;
                firebaseStorageService.upload(destination, firebasePath, "application/pdf");
                boleta.setFilePath(FIREBASE_PREFIX + firebasePath);
            } else {
                boleta.setFilePath(destination.toString());
            }
            boleta.setEstadoGeneracion(BoletaGeneracionEstado.GENERADA);
            boleta.setUltimoError(null);
            boletaRepository.save(boleta);
        } catch (IOException | RuntimeException e) {
            boleta.setEstadoGeneracion(BoletaGeneracionEstado.ERROR);
            boleta.setUltimoError(truncateError(extractErrorMessage(e)));
            boletaRepository.save(boleta);
            logger.error("Error generando PDF de boleta. orderId={}, numeroComprobante={}, intento={}",
                    order.getId(), boleta.getNumeroComprobante(), boleta.getIntentos(), e);
        } finally {
            if (isFirebaseProvider() && destination != null) {
                try {
                    Files.deleteIfExists(destination);
                } catch (IOException ex) {
                    logger.warn("No se pudo eliminar boleta temporal {}", destination, ex);
                }
            }
        }
    }

    private byte[] generarPdfEnMemoria(Order order, String numeroComprobante) {
        Document document = new Document(PageSize.A4, 36f, 36f, 28f, 28f);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            document.open();
            applyPageBackground(writer, document);

            String razonSocial = facturacionConfigService.obtenerValorObligatorio("EMPRESA_RAZON_SOCIAL");
            String ruc = facturacionConfigService.obtenerValorObligatorio("EMPRESA_RUC");
            String direccion = facturacionConfigService.obtenerValorObligatorio("EMPRESA_DIRECCION_FISCAL");

            String clienteNombre = order.getUser() != null ? order.getUser().getNombre() : null;
            String clienteCorreo = order.getUser() != null ? order.getUser().getEmail() : null;
            String clienteDocumento = order.getUser() != null ? order.getUser().getDocumentoNumero() : null;
            String clienteDireccion = resolverDireccionCliente(order);
            LocalDateTime fechaEmision = order.getCreatedAt() != null ? order.getCreatedAt() : LocalDateTime.now();
            BigDecimal total = order.getTotal() != null ? order.getTotal() : BigDecimal.ZERO;

            agregarEncabezado(document, razonSocial, numeroComprobante, fechaEmision);
            agregarBloqueDatos(document, razonSocial, ruc, direccion, clienteNombre, clienteCorreo, clienteDocumento,
                    clienteDireccion);
            agregarTablaDetalle(document, order.getItems());
            agregarResumen(document, total);
            agregarPie(document);
            document.close();
            
            return outputStream.toByteArray();
        } catch (RuntimeException e) {
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

    private void generarPdf(Path destination, Order order, String numeroComprobante) {
        Document document = new Document(PageSize.A4, 36f, 36f, 28f, 28f);

        try (OutputStream outputStream = Files.newOutputStream(destination)) {
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            document.open();
            applyPageBackground(writer, document);

            String razonSocial = facturacionConfigService.obtenerValorObligatorio("EMPRESA_RAZON_SOCIAL");
            String ruc = facturacionConfigService.obtenerValorObligatorio("EMPRESA_RUC");
            String direccion = facturacionConfigService.obtenerValorObligatorio("EMPRESA_DIRECCION_FISCAL");

            String clienteNombre = order.getUser() != null ? order.getUser().getNombre() : null;
            String clienteCorreo = order.getUser() != null ? order.getUser().getEmail() : null;
            String clienteDocumento = order.getUser() != null ? order.getUser().getDocumentoNumero() : null;
            String clienteDireccion = resolverDireccionCliente(order);
            LocalDateTime fechaEmision = order.getCreatedAt() != null ? order.getCreatedAt() : LocalDateTime.now();
            BigDecimal total = order.getTotal() != null ? order.getTotal() : BigDecimal.ZERO;

            agregarEncabezado(document, razonSocial, numeroComprobante, fechaEmision);
            agregarBloqueDatos(document, razonSocial, ruc, direccion, clienteNombre, clienteCorreo, clienteDocumento,
                    clienteDireccion);
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
        if (isFirebaseStoragePath(path)) {
            try {
                ensureFirebaseConfigured();
                return firebaseStorageService.exists(stripFirebasePrefix(path));
            } catch (Exception ex) {
                logger.warn("No se pudo verificar boleta en Firebase: {}", ex.getMessage());
                return false;
            }
        }
        try {
            Path filePath = Paths.get(path).normalize().toAbsolutePath();
            return Files.exists(filePath) && Files.isReadable(filePath);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private boolean isFirebaseProvider() {
        return storageProvider != null && storageProvider.equalsIgnoreCase("firebase");
    }

    private boolean isFirebaseStoragePath(String path) {
        return path != null && path.startsWith(FIREBASE_PREFIX);
    }

    private String stripFirebasePrefix(String path) {
        return path.substring(FIREBASE_PREFIX.length());
    }

    private void ensureFirebaseConfigured() {
        if (firebaseStorageService == null) {
            throw new StorageException("FIREBASE_NOT_CONFIGURED");
        }
    }

    private String safeText(String value) {
        return (value == null || value.isBlank()) ? "N/D" : value;
    }

    private String resolverDireccionCliente(Order order) {
        if (order == null) {
            return null;
        }
        if (order.getDireccion() != null && !order.getDireccion().isBlank()) {
            return order.getDireccion();
        }

        List<String> parts = new ArrayList<>();
        addAddressPart(parts, order.getCalle());
        addAddressPart(parts, order.getReferencia());
        addAddressPart(parts, order.getDistrito());
        addAddressPart(parts, order.getProvincia());
        addAddressPart(parts, order.getDepartamento());
        addAddressPart(parts, order.getCodigoPostal());
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private void addAddressPart(List<String> parts, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(value.trim());
        }
    }

    private boolean isNotEmpty(Collection<?> value) {
        return value != null && !value.isEmpty();
    }

    private void agregarEncabezado(Document document,
            String razonSocial,
            String numeroComprobante,
            LocalDateTime fechaEmision) throws DocumentException {
        Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 19, COLOR_PRIMARY);
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 27, COLOR_PRIMARY);
        Font numberFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, COLOR_PRIMARY);
        Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 11, COLOR_PRIMARY);

        PdfPTable brandRow = new PdfPTable(new float[] { 100f });
        brandRow.setWidthPercentage(100f);
        brandRow.setSpacingAfter(5f);

        PdfPCell brandCell = createNoBorderCell();
        brandCell.setPaddingBottom(2f);
        Paragraph brand = new Paragraph(sanitizeForPdf(safeText(razonSocial)), brandFont);
        brand.setLeading(21f);
        brandCell.addElement(brand);
        brandRow.addCell(brandCell);
        document.add(brandRow);

        PdfPTable titleRow = new PdfPTable(new float[] { 66f, 34f });
        titleRow.setWidthPercentage(100f);
        titleRow.setSpacingAfter(8f);

        PdfPCell titleCell = createNoBorderCell();
        Paragraph title = new Paragraph(sanitizeForPdf("BOLETA DE VENTA ELECTRONICA"), titleFont);
        title.setLeading(28f);
        titleCell.addElement(title);
        titleRow.addCell(titleCell);

        PdfPCell numberCell = createNoBorderCell();
        numberCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph number = new Paragraph(sanitizeForPdf("Nro " + safeText(numeroComprobante)), numberFont);
        number.setAlignment(Element.ALIGN_RIGHT);
        numberCell.addElement(number);
        Paragraph date = new Paragraph(sanitizeForPdf("Fecha emision: " + DATE_FORMATTER.format(fechaEmision)),
                dateFont);
        date.setAlignment(Element.ALIGN_RIGHT);
        numberCell.addElement(date);
        titleRow.addCell(numberCell);
        document.add(titleRow);

        PdfPTable divider = new PdfPTable(1);
        divider.setWidthPercentage(100f);
        PdfPCell dividerCell = new PdfPCell();
        dividerCell.setBorder(Rectangle.BOTTOM);
        dividerCell.setBorderColor(COLOR_BORDER);
        dividerCell.setBorderWidthBottom(0.9f);
        dividerCell.setFixedHeight(4f);
        dividerCell.setPadding(0f);
        divider.addCell(dividerCell);
        divider.setSpacingAfter(8f);
        document.add(divider);
    }

    private void agregarBloqueDatos(Document document,
            String razonSocial,
            String ruc,
            String direccion,
            String clienteNombre,
            String clienteCorreo,
            String clienteDocumento,
            String clienteDireccion) throws DocumentException {
        Font sectionTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, COLOR_PRIMARY);
        Paragraph section = new Paragraph(sanitizeForPdf("DATOS DEL EMISOR Y CLIENTE"), sectionTitle);
        section.setSpacingBefore(3f);
        section.setSpacingAfter(7f);
        document.add(section);

        PdfPTable container = new PdfPTable(new float[] { 1f, 1f });
        container.setWidthPercentage(100f);
        container.setSpacingAfter(14f);

        PdfPCell emisor = createInfoCell();
        addLabelValueLine(emisor, "EMISOR:", razonSocial);
        addLabelValueLine(emisor, "RUC:", ruc);
        addLabelValueLine(emisor, "Direccion fiscal:", direccion);
        container.addCell(emisor);

        PdfPCell cliente = createInfoCell();
        addLabelValueLine(cliente, "CLIENTE:", safeText(clienteNombre));
        addLabelValueLine(cliente, "Correo:", safeText(clienteCorreo));
        addLabelValueLine(cliente, "Documento:", safeText(clienteDocumento));
        addLabelValueLine(cliente, "Direccion:", safeText(clienteDireccion));
        container.addCell(cliente);

        document.add(container);
    }

    private void agregarTablaDetalle(Document document, Collection<OrderItem> items) throws DocumentException {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COLOR_PRIMARY);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 12, COLOR_PRIMARY);

        PdfPTable table = new PdfPTable(new float[] { 58f, 13f, 14f, 15f });
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

                table.addCell(createBodyCell(safeText(item.getNombre()), valueFont, Element.ALIGN_LEFT));
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
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, COLOR_PRIMARY);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 13, COLOR_PRIMARY);
        Font valueBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, COLOR_PRIMARY);

        PdfPTable resumen = new PdfPTable(new float[] { 74f, 26f });
        resumen.setWidthPercentage(100f);
        resumen.addCell(createSummaryCell("Total Exonerado IGV (Ley 31053):", labelFont, Element.ALIGN_RIGHT));
        resumen.addCell(createSummaryCell("S/ " + money(total), valueBold, Element.ALIGN_RIGHT));
        resumen.addCell(createSummaryCell("Codigo SUNAT CPE:", labelFont, Element.ALIGN_RIGHT));
        resumen.addCell(createSummaryCell("03", valueFont, Element.ALIGN_RIGHT));
        resumen.addCell(createSummaryCell("Afectacion IGV:", labelFont, Element.ALIGN_RIGHT));
        resumen.addCell(createSummaryCell("20 (Exonerado)", valueFont, Element.ALIGN_RIGHT));

        PdfPTable wrapper = new PdfPTable(1);
        wrapper.setWidthPercentage(52f);
        wrapper.setHorizontalAlignment(Element.ALIGN_RIGHT);
        PdfPCell wrapCell = new PdfPCell(resumen);
        wrapCell.setBorderColor(COLOR_BORDER);
        wrapCell.setBorderWidth(1.5f);
        wrapCell.setPadding(11f);
        wrapCell.setBackgroundColor(COLOR_SUMMARY_BLOCK);
        wrapper.addCell(wrapCell);
        wrapper.setSpacingAfter(19f);
        document.add(wrapper);
    }

    private void agregarPie(Document document) throws DocumentException {
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12, COLOR_PRIMARY);
        Font strongFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COLOR_PRIMARY);
        Font qrLabelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        Font qrBodyFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COLOR_PRIMARY);

        PdfPTable footer = new PdfPTable(new float[] { 84f, 16f });
        footer.setWidthPercentage(100f);
        footer.setSpacingBefore(8f);

        PdfPCell textCell = createNoBorderCell();
        textCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        textCell.setVerticalAlignment(Element.ALIGN_BOTTOM);
        Paragraph line1 = new Paragraph();
        line1.setAlignment(Element.ALIGN_RIGHT);
        line1.add(new Chunk(sanitizeForPdf("Gracias por tu compra en "), normalFont));
        line1.add(new Chunk(sanitizeForPdf("Cuentos de Killa!"), strongFont));
        textCell.addElement(line1);
        Paragraph line2 = new Paragraph(sanitizeForPdf("Fomentando la imaginacion."), normalFont);
        line2.setAlignment(Element.ALIGN_RIGHT);
        textCell.addElement(line2);
        footer.addCell(textCell);

        PdfPTable qrTable = new PdfPTable(1);
        qrTable.setWidthPercentage(100f);

        PdfPCell bannerCell = new PdfPCell(new Phrase(sanitizeForPdf("Validar con SUNAT"), qrLabelFont));
        bannerCell.setBackgroundColor(COLOR_QR_BANNER);
        bannerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        bannerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        bannerCell.setPaddingTop(3f);
        bannerCell.setPaddingBottom(3f);
        bannerCell.setBorder(Rectangle.NO_BORDER);
        qrTable.addCell(bannerCell);

        PdfPCell qrBodyCell = new PdfPCell(new Phrase(sanitizeForPdf("QR"), qrBodyFont));
        qrBodyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        qrBodyCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        qrBodyCell.setMinimumHeight(52f);
        qrBodyCell.setPadding(4f);
        qrBodyCell.setBorderColor(COLOR_BORDER);
        qrBodyCell.setBorderWidth(1f);
        qrTable.addCell(qrBodyCell);

        PdfPCell qrCell = new PdfPCell(qrTable);
        qrCell.setBorderColor(COLOR_BORDER);
        qrCell.setBorderWidth(1f);
        qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        qrCell.setVerticalAlignment(Element.ALIGN_BOTTOM);
        qrCell.setPadding(2f);
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
        cell.setPadding(10f);
        cell.setBackgroundColor(COLOR_INFO_BLOCK);
        cell.setBorderColor(COLOR_BORDER);
        cell.setBorderWidth(1f);
        return cell;
    }

    private PdfPCell createHeaderCell(String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(sanitizeForPdf(text), font));
        cell.setPadding(9f);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(COLOR_HEADER_ROW);
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
        cell.setBackgroundColor(COLOR_SUMMARY_BLOCK);
        return cell;
    }

    private void applyPageBackground(PdfWriter writer, Document document) {
        Rectangle page = document.getPageSize();
        writer.getDirectContentUnder().saveState();
        writer.getDirectContentUnder().setColorFill(COLOR_PAGE_BG);
        writer.getDirectContentUnder().rectangle(page.getLeft(), page.getBottom(), page.getWidth(), page.getHeight());
        writer.getDirectContentUnder().fill();
        writer.getDirectContentUnder().restoreState();
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

    public record BoletaArchivo(Path filePath, byte[] pdfContent, String numeroComprobante) {
    }

    public record BoletaRetryResult(Long orderId,
            String numeroComprobante,
            String estadoGeneracion,
            Integer intentos,
            String ultimoError) {
    }
}
