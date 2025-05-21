package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.model.Order;
import com.forjix.cuentoskilla.model.User;
import com.forjix.cuentoskilla.model.Voucher;
import com.forjix.cuentoskilla.service.StorageService;
import com.forjix.cuentoskilla.service.OrderService; // Assuming OrderService might be needed for context or user setup
import com.forjix.cuentoskilla.repository.UserRepository; // For creating test users for authentication
import com.forjix.cuentoskilla.service.storage.StorageException; // Import for the exception

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser; // For testing secured endpoints
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.is;


@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean // Use MockBean for services StorageService
    private StorageService storageService;
    
    // We might not need to mock OrderService if we're not deeply testing order creation logic here
    // but focusing on the voucher upload part. If OrderService is essential for OrderController's setup,
    // it might need to be a @MockBean too. For now, let's assume it's not strictly needed for /voucher.

    @Autowired
    private WebApplicationContext context;
    
    @BeforeEach
    public void setup() {
        // Setup MockMvc with Spring Security context
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity()) // Apply Spring Security configuration
                .build();
    }


    @Test
    @WithMockUser(username = "testuser", roles = {"USER"}) // Simulate an authenticated user
    void uploadVoucher_whenAuthenticatedAndValidData_shouldReturnSuccess() throws Exception {
        Long orderId = 1L;
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "voucher.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "testPDFcontent".getBytes()
        );

        Voucher mockVoucher = new Voucher();
        mockVoucher.setId(1L);
        mockVoucher.setNombreArchivo("mock_voucher.pdf");
        // Set other necessary fields for the mockVoucher if they are returned in JSON

        when(storageService.store(any(MockMultipartFile.class), anyLong(), anyString(), anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(mockVoucher);

        mockMvc.perform(multipart("/api/orders/voucher")
                        .file(mockFile)
                        .param("idpedido", String.valueOf(orderId))
                        .param("dispositivo", "test-device")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Voucher uploaded successfully!")))
                .andExpect(jsonPath("$.voucherId", is(1)))
                .andExpect(jsonPath("$.fileName", is("mock_voucher.pdf")));
    }
    
    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void uploadVoucher_whenStorageServiceThrowsException_shouldReturnError() throws Exception {
        Long orderId = 2L;
        MockMultipartFile mockFile = new MockMultipartFile(
            "file", "error.txt", MediaType.TEXT_PLAIN_VALUE, "error content".getBytes()
        );

        when(storageService.store(any(), anyLong(), anyString(), anyString(), anyString(), anyString(), anyLong()))
            .thenThrow(new StorageException("Test storage failure"));

        mockMvc.perform(multipart("/api/orders/voucher")
                        .file(mockFile)
                        .param("idpedido", String.valueOf(orderId))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest()) // As per current OrderController error handling for StorageException
                .andExpect(jsonPath("$.error", is("Storage error: Test storage failure")));
    }

    @Test
    void uploadVoucher_whenNotAuthenticated_shouldReturnUnauthorized() throws Exception {
        Long orderId = 3L;
        MockMultipartFile mockFile = new MockMultipartFile(
            "file", "secret.txt", MediaType.TEXT_PLAIN_VALUE, "secret".getBytes()
        );
        
        // No @WithMockUser here

        mockMvc.perform(multipart("/api/orders/voucher")
                        .file(mockFile)
                        .param("idpedido", String.valueOf(orderId))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnauthorized()); // Or isForbidden() depending on exact security setup for unauthenticated
    }
    
    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void uploadVoucher_whenFileIsEmpty_shouldReturnBadRequest() throws Exception {
        Long orderId = 4L;
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file", "", MediaType.TEXT_PLAIN_VALUE, new byte[0] // Empty file
        );

        mockMvc.perform(multipart("/api/orders/voucher")
                        .file(emptyFile)
                        .param("idpedido", String.valueOf(orderId))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Cannot upload an empty file.")));
    }
    
    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void uploadVoucher_whenMissingOrderId_shouldReturnBadRequest() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile(
            "file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "content".getBytes()
        );

        // Missing "idpedido" parameter
        mockMvc.perform(multipart("/api/orders/voucher")
                        .file(mockFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest()); // Spring typically returns 400 for missing required params
    }

}
