package com.forjix.cuentoskilla.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Configuration
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.credentials:firebase-adminsdk.json}")
    private String credentialsPath;

    @Value("${firebase.bucket:cuentoskillabe.firebasestorage.app}")
    private String bucketName;

    @PostConstruct
    public void init() {
        if (!FirebaseApp.getApps().isEmpty()) {
            logger.info("Firebase ya está inicializado");
            return;
        }
        
        try {
            // Verificar si el archivo existe
            if (!Files.exists(Paths.get(credentialsPath))) {
                logger.warn("Firebase credentials file not found at: {}. Firebase será inicializado sin credenciales (funcionalidad limitada).", credentialsPath);
                return;
            }
            
            try (FileInputStream serviceAccount = new FileInputStream(credentialsPath)) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setStorageBucket(bucketName)
                        .build();
                FirebaseApp.initializeApp(options);
                logger.info("Firebase inicializado exitosamente con credentials desde: {}", credentialsPath);
            }
        } catch (IOException e) {
            logger.error("Error al inicializar Firebase: {}", e.getMessage());
            // No lanzar excepción, permitir que la aplicación continúe sin Firebase
        } catch (Exception e) {
            logger.error("Error inesperado al inicializar Firebase: {}", e.getMessage());
        }
    }
}
