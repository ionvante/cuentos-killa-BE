package com.forjix.cuentoskilla.service.storage;

import com.forjix.cuentoskilla.model.Order;
import com.forjix.cuentoskilla.model.Voucher;
import com.forjix.cuentoskilla.repository.OrderRepository;
import com.forjix.cuentoskilla.repository.VoucherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileSystemStorageServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private OrderRepository orderRepository;

    // @InjectMocks will not work here because we need to set uploadDir manually after construction
    private FileSystemStorageService storageService;

    @BeforeEach
    void setUp() {
        // Manually instantiate and set the uploadDir using reflection or by modifying the service
        // For simplicity, let's assume FileSystemStorageService can have uploadDir set post-construction
        // Or, we can use reflection to set the @Value field if needed.
        // A better way would be to make uploadDir configurable via a constructor or setter for tests.
        // For this subtask, we'll proceed as if we can set it.
        // The @PostConstruct init() method needs to be called after setting uploadDir.

        storageService = new FileSystemStorageService(voucherRepository, orderRepository);
        // Simulate Spring's @Value injection and @PostConstruct for testing
        try {
            java.lang.reflect.Field uploadDirField = FileSystemStorageService.class.getDeclaredField("uploadDir");
            uploadDirField.setAccessible(true);
            uploadDirField.set(storageService, tempDir.toString());

            storageService.init(); // Manually call init after setting the path
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set uploadDir for test", e);
        }
    }

    @Test
    void store_shouldSaveFileAndCreateVoucherRecord() throws IOException {
        Long orderId = 1L;
        String originalFilename = "test-voucher.pdf";
        String contentType = "application/pdf";
        String ip = "127.0.0.1";
        String dispositivo = "test-device";
        long fileSize = 1024;

        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                originalFilename,
                contentType,
                "test data".getBytes()
        );

        Order mockOrder = new Order();
        mockOrder.setId(orderId);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(invocation -> {
            Voucher v = invocation.getArgument(0);
            v.setId(100L); // Simulate saving and getting an ID
            return v;
        });

        Voucher result = storageService.store(multipartFile, orderId, originalFilename, contentType, ip, dispositivo, fileSize);

        assertThat(result).isNotNull();
        assertThat(result.getOrder()).isEqualTo(mockOrder);
        assertThat(result.getNombreArchivo()).contains(originalFilename); // Will have UUID prefix
        assertThat(result.getTipoArchivo()).isEqualTo(contentType);
        assertThat(result.getIp()).isEqualTo(ip);
        assertThat(result.getDispositivo()).isEqualTo(dispositivo);
        assertThat(result.getPeso()).isEqualTo(String.valueOf(fileSize));
        assertThat(result.getFecha()).isEqualTo(LocalDate.now());
        assertThat(result.getFilePath()).isNotNull();
        assertThat(Files.exists(Path.of(result.getFilePath()))).isTrue();

        ArgumentCaptor<Voucher> voucherArgumentCaptor = ArgumentCaptor.forClass(Voucher.class);
        verify(voucherRepository).save(voucherArgumentCaptor.capture());
        Voucher capturedVoucher = voucherArgumentCaptor.getValue();

        assertThat(capturedVoucher.getFilePath()).isEqualTo(tempDir.resolve(capturedVoucher.getNombreArchivo()).toString());
    }

    @Test
    void store_whenFileIsEmpty_shouldThrowStorageException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "", "text/plain", new byte[0]);
        Long orderId = 1L;

        assertThatThrownBy(() -> storageService.store(emptyFile, orderId, "", "text/plain", "127.0.0.1", "test", 0))
                .isInstanceOf(StorageException.class)
                .hasMessage("Failed to store empty file.");
        
        verify(voucherRepository, never()).save(any());
    }

    @Test
    void store_whenOrderNotFound_shouldThrowStorageException() {
        MockMultipartFile multipartFile = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());
        Long nonExistentOrderId = 99L;

        when(orderRepository.findById(nonExistentOrderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> storageService.store(multipartFile, nonExistentOrderId, "test.txt", "text/plain", "127.0.0.1", "test", 10))
                .isInstanceOf(StorageException.class)
                .hasMessage("Order not found with id: " + nonExistentOrderId);
        
        verify(voucherRepository, never()).save(any());
    }
    
    @Test
    void init_shouldCreateDirectoryIfNotExist() throws IOException {
         Path newUploadDir = tempDir.resolve("test-uploads");
         // For this test, we need a new instance or re-initialize with a different path
         FileSystemStorageService newService = new FileSystemStorageService(voucherRepository, orderRepository);
         try {
            java.lang.reflect.Field uploadDirField = FileSystemStorageService.class.getDeclaredField("uploadDir");
            uploadDirField.setAccessible(true);
            uploadDirField.set(newService, newUploadDir.toString());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
         newService.init();
         assertThat(Files.exists(newUploadDir)).isTrue();
         assertThat(Files.isDirectory(newUploadDir)).isTrue();
    }
}
