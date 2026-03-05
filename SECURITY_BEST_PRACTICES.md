# 🔒 Guía de Mejores Prácticas de API - OWASP

## Resumen de Mejoras Implementadas

Este documento describes las mejoras de seguridad implementadas siguiendo **OWASP Top 10 para APIs**.

---

## 1. ✅ API Versionado

**Cambio**: Todas las rutas ahora usan `/api/v1/` en lugar de `/api/`

**Antes:**
```
GET /api/cuentos
POST /api/auth/login
PUT /api/orders/{id}
```

**Después:**
```
GET /api/v1/cuentos
POST /api/v1/auth/login
PUT /api/v1/orders/{id}
```

**Beneficios:**
- Facilita evolución de API
- Permite deprecación controlada
- Versiones múltiples en paralelo

---

## 2. ✅ Rate Limiting

**Componente:** `RateLimitingFilter.java`

**Límites implementados:**
- **Endpoints públicos**: 60 req/minuto
- **Endpoints autenticados**: 100 req/minuto
- **Login**: 5 intentos/minuto

**Respuesta cuando se excede:**
```json
HTTP/429 Too Many Requests
{
  "error": "Too many requests. Rate limit exceeded."
}
```

**Headers devueltos:**
```
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 42
```

---

## 3. ✅ Headers de Seguridad HTTP

**Componente:** `SecurityHeadersFilter.java`

**Headers implementados:**

| Header | Valor | Propósito |
|--------|-------|----------|
| X-Content-Type-Options | nosniff | Evita MIME type sniffing |
| X-Frame-Options | DENY | Previene clickjacking |
| X-XSS-Protection | 1; mode=block | Protección XSS |
| Content-Security-Policy | Restringido | Control de recursos |
| Referrer-Policy | strict-origin-when-cross-origin | Control de referrer |
| Permissions-Policy | Restringido | Controla APIs del browser |
| Cache-Control | no-store | Evita caché de datos sensibles |

---

## 4. ✅ CORS Mejorado

**Cambio:** Ahora especifica orígenes, métodos y headers explícitamente

**Antes:**
```java
config.setAllowedHeaders(List.of("*")); // ❌ Inseguro
config.setAllowedOrigins(List.of("*")); // ❌ Inseguro
```

**Después:**
```java
config.setAllowedOrigins(List.of(
    "http://localhost:4200",
    "https://cuentos-killa-fe.vercel.app"
));
config.setAllowedHeaders(List.of(
    "Content-Type",
    "Authorization",
    "X-Requested-With"
));
config.setMaxAge(3600L); // 1 hora
```

---

## 5. ✅ Manejo de Errores Standardizado

**Componente:** `ApiResponse<T>.java`

**Estructura de respuesta:**
```json
{
  "success": false,
  "code": "VALIDATION_ERROR",
  "message": "Error en validación de datos",
  "errors": {
    "email": "debe ser un email válido",
    "password": "debe tener al menos 8 caracteres"
  },
  "timestamp": "2026-03-04T14:30:00",
  "path": "/api/v1/auth/register"
}
```

**Códigos de error estandarizados:**
- `VALIDATION_ERROR` (400)
- `AUTHENTICATION_ERROR` (401)
- `AUTHORIZATION_ERROR` (403)
- `NOT_FOUND` (404)
- `FILE_TOO_LARGE` (413)
- `INTERNAL_SERVER_ERROR` (500)

**Beneficio:** NO se exponen detalles internos del servidor

---

## 6. ✅ Autorización Granular

**Cambio:** Usar `@PreAuthorize` en métodos en lugar de solo `@RequestMapping`

**Ejemplo:**
```java
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    
    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<User> createUser(@Valid @RequestBody CreateUserRequest request) {
        // Solo ADMIN puede crear usuarios
    }
    
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ApiResponse<StatsDTO> getStats() {
        // Solo ADMIN o MANAGER
    }
    
    @PutMapping("/orders/{id}")
    @PreAuthorize("hasRole('ADMIN') or #userId == principal.id")
    public ApiResponse<Order> updateOrder(@PathVariable Long id, @RequestParam Long userId) {
        // ADMIN o dueño del recurso
    }
}
```

---

## 7. ✅ Validación de Entrada

**Cambio:** Usar anotaciones de validación en DTOs

**Ejemplo:**
```java
public class LoginRequest {
    
    @NotBlank(message = "Email es requerido")
    @Email(message = "Email debe ser válido")
    private String email;
    
    @NotBlank(message = "Password es requerido")
    @Size(min = 8, max = 128, message = "Password debe tener 8-128 caracteres")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
        message = "Password debe contener mayúscula, minúscula, número y carácter especial"
    )
    private String password;
}
```

**En Controller:**
```java
@PostMapping("/login")
public ApiResponse<LoginResponse> login(
    @Valid @RequestBody LoginRequest request) {
    // Si hay error de validación, automáticamente retorna 400
}
```

---

## 8. ✅ Logging Seguro

**Cambio:** No loguear datos sensibles

**❌ MALO:**
```java
logger.info("Login attempt for email: {} - password: {}", email, password);
```

**✅ BUENO:**
```java
logger.info("Login attempt for email: {}", email);
logger.debug("Authentication successful for user: {}", userId);
```

---

## 9. ✅ Autenticación y Autorización

**Cambio:** SecurityConfig ahora especifica qué rutas requieren qué rol

```java
.authorizeHttpRequests(auth -> auth
    // Públicas
    .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login").permitAll()
    .requestMatchers("/api/v1/cuentos/**").permitAll()
    
    // Autenticadas
    .requestMatchers("/api/v1/users/**").authenticated()
    .requestMatchers("/api/v1/orders/**").authenticated()
    
    // Admin
    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
    .requestMatchers("/api/v1/maestro/**").hasRole("ADMIN")
    
    // Otra
    .anyRequest().authenticated()
)
```

---

## 10. ✅ BCrypt Strength Mejorado

**Cambio:** Aumentar fuerza de BCrypt de 10 a 12

```java
new BCryptPasswordEncoder(12) // Más seguro, un poco más lento
```

---

## 🔄 Próximos Pasos: Actualizar Controllers

Para aplicar estas mejoras en todos los controladores, sigue este patrón:

### Template de Controller Actualizado:

```java
package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.model.DTOs.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

/**
 * API Controller - v1
 * Rutas: /api/v1/resources
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/resources")
@RequiredArgsConstructor
public class ResourceController {
    
    private final ResourceService resourceService;
    
    /**
     * Obtener todos los recursos
     * GET /api/v1/resources
     * 
     * @return Lista de recursos
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ResourceDTO>>> getAll() {
        log.info("GET /api/v1/resources - Listando todos los recursos");
        List<ResourceDTO> resources = resourceService.findAll();
        return ResponseEntity.ok(
            ApiResponse.success(resources, "Recursos obtenidos exitosamente")
        );
    }
    
    /**
     * Obtener recurso por ID
     * GET /api/v1/resources/{id}
     * 
     * @param id ID del recurso
     * @return Recurso encontrado
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ResourceDTO>> getById(@PathVariable Long id) {
        log.info("GET /api/v1/resources/{} - Obteniendo recurso", id);
        ResourceDTO resource = resourceService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Recurso no encontrado"));
        return ResponseEntity.ok(
            ApiResponse.success(resource)
        );
    }
    
    /**
     * Crear nuevo recurso
     * POST /api/v1/resources
     * Solo para usuarios autenticados
     * 
     * @param request DTO de creación
     * @return Recurso creado
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ResourceDTO>> create(
            @Valid @RequestBody CreateResourceRequest request) {
        log.info("POST /api/v1/resources - Creando nuevo recurso");
        ResourceDTO created = resourceService.create(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(created, "Recurso creado exitosamente"));
    }
    
    /**
     * Actualizar recurso
     * PUT /api/v1/resources/{id}
     * Solo para dueño o ADMIN
     * 
     * @param id ID del recurso
     * @param request DTO de actualización
     * @return Recurso actualizado
     */
    @PutMapping("/{id}")
    @PreAuthorize("@resourceService.isOwner(#id, principal.id) or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ResourceDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateResourceRequest request) {
        log.info("PUT /api/v1/resources/{} - Actualizando recurso", id);
        ResourceDTO updated = resourceService.update(id, request);
        return ResponseEntity.ok(
            ApiResponse.success(updated, "Recurso actualizado exitosamente")
        );
    }
    
    /**
     * Eliminar recurso
     * DELETE /api/v1/resources/{id}
     * Solo para dueño o ADMIN
     * 
     * @param id ID del recurso
     * @return Confirmación de eliminación
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@resourceService.isOwner(#id, principal.id) or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        log.info("DELETE /api/v1/resources/{} - Eliminando recurso", id);
        resourceService.delete(id);
        return ResponseEntity.ok(
            ApiResponse.success(null, "Recurso eliminado exitosamente")
        );
    }
}
```

---

## 📋 Checklist de Implementación

- [ ] Actualizar todas las rutas a `/api/v1/`
- [ ] Remover `@CrossOrigin` de controladores (CORS manejado globalmente)
- [ ] Agregar `@PreAuthorize` a métodos que requieren autorización
- [ ] Usar `@Valid` en todos los `@RequestBody`
- [ ] Retornar `ApiResponse<T>` en todos los endpoints
- [ ] No loguear datos sensibles
- [ ] Usar DTOs para requests/responses
- [ ] Documentar endpoints con JavaDocs
- [ ] Agregar validaciones en DTOs
- [ ] Actualizar README con nueva estructura de API

---

## 📚 Referencias OWASP

- [OWASP API Security Top 10](https://owasp.org/www-project-api-security/)
- [OWASP Testing Guide](https://owasp.org/www-project-web-security-testing-guide/)
- [OWASP Cheat Sheets](https://cheatsheetseries.owasp.org/)

---

**Última actualización:** 4 de Marzo de 2026
**Responsable:** Equipo de Seguridad
