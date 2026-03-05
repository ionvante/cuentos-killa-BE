package com.forjix.cuentoskilla.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Inicialización de Firebase Admin SDK.
 *
 * Estrategia de credenciales (en orden de prioridad):
 * 1. Variable de entorno FIREBASE_CREDENTIALS_JSON → JSON inlineado (ideal para
 * Railway/Docker)
 * 2. Ruta de archivo configurada en firebase.credentials → archivo local (dev)
 */
@Configuration
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.credentials:firebase-adminsdk.json}")
    private String credentialsPath;

    @Value("${firebase.bucket:}")
    private String bucketName;

    /** Variable de entorno con el contenido JSON completo del service-account */
    @Value("${FIREBASE_CREDENTIALS_JSON:}")
    private String credentialsJson;

    @PostConstruct
    public void init() {
        if (!FirebaseApp.getApps().isEmpty()) {
            logger.info("Firebase ya está inicializado");
            return;
        }

        if (bucketName == null || bucketName.isBlank()) {
            logger.warn("FIREBASE_BUCKET no configurado — Firebase Storage no estará disponible");
            return;
        }

        try {
            InputStream credentialsStream = resolveCredentials();
            if (credentialsStream == null) {
                logger.warn("No se encontraron credenciales de Firebase. Storage no disponible.");
                return;
            }

            try (credentialsStream) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(credentialsStream))
                        .setStorageBucket(bucketName)
                        .build();
                FirebaseApp.initializeApp(options);
                logger.info("Firebase inicializado correctamente (bucket: {})", bucketName);
            }
        } catch (IOException e) {
            logger.error("Error al inicializar Firebase: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Error inesperado al inicializar Firebase: {}", e.getMessage());
        }
    }

    /**
     * Resuelve el stream de credenciales:
     * - Primero intenta la variable de entorno FIREBASE_CREDENTIALS_JSON
     * (Railway/prod)
     * - Luego intenta leer desde el archivo (desarrollo local)
     */
    private InputStream resolveCredentials() {
        // ① Variable de entorno con JSON inline (Railway, Docker, CI)
        if (credentialsJson != null && !credentialsJson.isBlank()) {
            logger.info("Firebase: usando credenciales desde FIREBASE_CREDENTIALS_JSON");
            return new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8));
        }

        // ② Archivo local (desarrollo)
        if (credentialsPath != null && Files.exists(Paths.get(credentialsPath))) {
            logger.info("Firebase: usando credenciales desde archivo: {}", credentialsPath);
            try {
                return new FileInputStream(credentialsPath);
            } catch (IOException e) {
                logger.error("No se pudo leer el archivo de credenciales: {}", e.getMessage());
            }
        }

        return null;
    }
}
