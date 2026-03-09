package com.forjix.cuentoskilla.service;

import com.forjix.cuentoskilla.model.Boleta;
import com.forjix.cuentoskilla.model.BoletaGeneracionEstado;
import com.forjix.cuentoskilla.model.Maestro;
import com.forjix.cuentoskilla.model.Order;
import com.forjix.cuentoskilla.model.OrderItem;
import com.forjix.cuentoskilla.model.OrderStatus;
import com.forjix.cuentoskilla.model.User;
import com.forjix.cuentoskilla.repository.BoletaRepository;
import com.forjix.cuentoskilla.repository.MaestroRepository;
import com.forjix.cuentoskilla.repository.OrderRepository;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class BoletaServiceTest {

    private BoletaRepository boletaRepository;
    private OrderRepository orderRepository;
    private MaestroRepository maestroRepository;
    private BoletaService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        boletaRepository = Mockito.mock(BoletaRepository.class);
        orderRepository = Mockito.mock(OrderRepository.class);
        maestroRepository = Mockito.mock(MaestroRepository.class);
        FacturacionConfigService facturacionConfigService = new FacturacionConfigService(maestroRepository);
        service = new BoletaService(boletaRepository, orderRepository, facturacionConfigService);
        ReflectionTestUtils.setField(service, "boletaUploadDir", tempDir.toString());
        service.init();
    }

    @Test
    void generarBoletaSiCorrespondeEsIdempotenteSiYaExiste() throws IOException {
        Boleta boleta = new Boleta();
        boleta.setId(99L);
        boleta.setEstadoGeneracion(BoletaGeneracionEstado.GENERADA);

        Path existingPdf = tempDir.resolve("boleta_existente.pdf");
        Files.writeString(existingPdf, "dummy");
        boleta.setFilePath(existingPdf.toString());

        Mockito.when(boletaRepository.findByOrder_Id(1L)).thenReturn(Optional.of(boleta));

        Boleta result = service.generarBoletaSiCorresponde(1L);

        assertEquals(99L, result.getId());
        Mockito.verify(boletaRepository).findByOrder_Id(1L);
        Mockito.verifyNoMoreInteractions(boletaRepository);
    }

    @Test
    void generarBoletaIncluyeTextosClaveDelNuevoDiseno() throws IOException {
        Order order = buildOrder();
        Mockito.when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        Mockito.when(boletaRepository.findByOrder_Id(1L)).thenReturn(Optional.empty());

        stubMaestro("BOLETA_SERIE_ACTIVA", "B001");
        stubMaestro("EMPRESA_RAZON_SOCIAL", "Cuentos de Killa");
        stubMaestro("EMPRESA_RUC", "00000000000");
        stubMaestro("EMPRESA_DIRECCION_FISCAL", "POR CONFIGURAR");

        Maestro correlativo = maestroActivo("BOLETA_CORRELATIVO_ACTUAL", "1");
        Mockito.when(maestroRepository.findByCodigoForUpdate(eq("BOLETA_CORRELATIVO_ACTUAL")))
                .thenReturn(Optional.of(correlativo));
        Mockito.when(maestroRepository.save(any(Maestro.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Mockito.when(boletaRepository.save(any(Boleta.class))).thenAnswer(invocation -> {
            Boleta value = invocation.getArgument(0);
            if (value.getId() == null) {
                value.setId(100L);
            }
            return value;
        });

        Boleta result = service.generarBoletaSiCorresponde(1L);

        assertEquals(BoletaGeneracionEstado.GENERADA, result.getEstadoGeneracion());
        assertEquals("B001-00000002", result.getNumeroComprobante());
        assertNotNull(result.getFilePath());
        assertTrue(Files.exists(Path.of(result.getFilePath())));

        String text = normalizeForAssert(extractPdfText(Path.of(result.getFilePath())));
        assertTrue(text.contains("BOLETA DE VENTA ELECTRONICA"));
        assertTrue(text.contains("FECHA EMISION:"));
        assertTrue(text.contains("DATOS DEL EMISOR Y CLIENTE"));
        assertTrue(text.contains("DESCRIPCION DEL ARTICULO"));
        assertTrue(text.contains("TOTAL EXONERADO IGV (LEY 31053):"));
        assertTrue(text.contains("CODIGO SUNAT CPE:"));
        assertTrue(text.contains("AFECTACION IGV:"));
        assertTrue(text.contains("DIRECCION: AV. LOS OLIVOS 123, DPTO 201, MIRAFLORES, LIMA, LIMA, 15074"));
        assertTrue(text.contains("VALIDAR CON SUNAT"));
        assertTrue(text.contains("QR"));
        assertTrue(text.contains("FOMENTANDO LA IMAGINACION."));
    }

    private void stubMaestro(String codigo, String valor) {
        Mockito.when(maestroRepository.findByCodigo(eq(codigo)))
                .thenReturn(Optional.of(maestroActivo(codigo, valor)));
    }

    private Maestro maestroActivo(String codigo, String valor) {
        Maestro maestro = new Maestro();
        maestro.setCodigo(codigo);
        maestro.setValor(valor);
        maestro.setEstado(true);
        return maestro;
    }

    private Order buildOrder() {
        User user = new User();
        user.setNombre("Daniel");
        user.setEmail("camusper@gmail.com");
        user.setDocumentoNumero("73883334");

        OrderItem item = new OrderItem();
        item.setNombre("Mis primeros pasos");
        item.setCantidad(1);
        item.setPrecio_unitario(60.0);
        item.setSubtotal(new BigDecimal("60.00"));

        Order order = new Order();
        order.setId(1L);
        order.setEstado(OrderStatus.PAGO_VERIFICADO);
        order.setCreatedAt(LocalDateTime.of(2026, 3, 9, 9, 30, 0));
        order.setTotal(new BigDecimal("60.00"));
        order.setUser(user);
        order.setDireccion("Av. Los Olivos 123, Dpto 201, Miraflores, Lima, Lima, 15074");
        order.setItems(List.of(item));
        return order;
    }

    private String extractPdfText(Path path) throws IOException {
        try (PdfReader reader = new PdfReader(path.toString())) {
            StringBuilder out = new StringBuilder();
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                out.append(extractor.getTextFromPage(page)).append('\n');
            }
            return out.toString();
        }
    }

    private String normalizeForAssert(String value) {
        String noAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return noAccents.toUpperCase().replaceAll("\\s+", " ").trim();
    }
}
