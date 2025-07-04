package com.forjix.cuentoskilla.service.storage;

import com.forjix.cuentoskilla.model.Order;
import com.forjix.cuentoskilla.model.OrderStatus;
import com.forjix.cuentoskilla.model.Voucher;
import com.forjix.cuentoskilla.repository.OrderRepository;
import com.forjix.cuentoskilla.repository.VoucherRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

public class FileSystemStorageServiceTest {

    private VoucherRepository voucherRepository;
    private OrderRepository orderRepository;
    private MeterRegistry meterRegistry;
    private Counter counter;
    private FileSystemStorageService service;

    @BeforeEach
    public void setUp() throws Exception {
        voucherRepository = Mockito.mock(VoucherRepository.class);
        orderRepository = Mockito.mock(OrderRepository.class);
        meterRegistry = Mockito.mock(MeterRegistry.class);
        counter = Mockito.mock(Counter.class);
        Mockito.when(meterRegistry.counter(anyString())).thenReturn(counter);

        service = new FileSystemStorageService(voucherRepository, orderRepository, meterRegistry);
        // set uploadDir manually
        Path temp = Files.createTempDirectory("vtest");
        java.lang.reflect.Field field = FileSystemStorageService.class.getDeclaredField("uploadDir");
        field.setAccessible(true);
        field.set(service, temp.toString());
        service.init();
    }

    @Test
    public void testStoreValidFile() {
        Order order = new Order();
        order.setId(1L);
        order.setEstado(OrderStatus.PAGO_PENDIENTE);
        Mockito.when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        Mockito.when(voucherRepository.save(any(Voucher.class))).thenAnswer(inv -> inv.getArgument(0));
        Mockito.when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[]{1,2,3});
        Voucher v = service.store(file, 1L, file.getOriginalFilename(), file.getContentType(), "127.0.0.1", "test", file.getSize());
        assertNotNull(v.getFilePath());
        assertEquals(OrderStatus.PAGO_ENVIADO, order.getEstado());
    }

    @Test
    public void testInvalidExtension() {
        Order order = new Order();
        order.setId(1L);
        order.setEstado(OrderStatus.PAGO_PENDIENTE);
        Mockito.when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", new byte[]{1});
        StorageException ex = assertThrows(StorageException.class, () ->
                service.store(file, 1L, file.getOriginalFilename(), file.getContentType(), "ip", "dev", file.getSize()));
        assertEquals("INVALID_FILE", ex.getMessage());
    }

    @Test
    public void testMaxSizeExceeded() {
        Order order = new Order();
        order.setId(1L);
        order.setEstado(OrderStatus.PAGO_PENDIENTE);
        Mockito.when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        byte[] large = new byte[6 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile("file", "big.jpg", "image/jpeg", large);
        StorageException ex = assertThrows(StorageException.class, () ->
                service.store(file, 1L, file.getOriginalFilename(), file.getContentType(), "ip", "dev", file.getSize()));
        assertEquals("MAX_UPLOAD_SIZE_EXCEEDED", ex.getMessage());
    }
}
