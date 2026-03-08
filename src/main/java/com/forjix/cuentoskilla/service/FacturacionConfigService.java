package com.forjix.cuentoskilla.service;

import com.forjix.cuentoskilla.model.Maestro;
import com.forjix.cuentoskilla.model.DTOs.FacturacionParametroRequest;
import com.forjix.cuentoskilla.model.DTOs.FacturacionParametroResponse;
import com.forjix.cuentoskilla.repository.MaestroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class FacturacionConfigService {

    public static final String GRUPO_FACTURACION = "FACTURACION";
    public static final String CODIGO_RUC = "EMPRESA_RUC";
    public static final String CODIGO_SERIE_ACTIVA = "BOLETA_SERIE_ACTIVA";
    public static final String CODIGO_CORRELATIVO = "BOLETA_CORRELATIVO_ACTUAL";

    private static final Pattern RUC_PATTERN = Pattern.compile("^\\d{11}$");
    private static final Pattern SERIE_PATTERN = Pattern.compile("^B\\d{3}$");

    private static final Set<String> ALLOWED_CODES = Set.of(
            "EMPRESA_RUC",
            "EMPRESA_RAZON_SOCIAL",
            "EMPRESA_DIRECCION_FISCAL",
            "EMPRESA_LOGO_URL",
            "SUNAT_CPE_03",
            "SUNAT_IGV_20",
            "SUNAT_DOC_1",
            "SUNAT_DOC_4",
            "SUNAT_DOC_7",
            CODIGO_SERIE_ACTIVA,
            CODIGO_CORRELATIVO
    );

    private final MaestroRepository maestroRepository;

    public FacturacionConfigService(MaestroRepository maestroRepository) {
        this.maestroRepository = maestroRepository;
    }

    @Transactional(readOnly = true)
    public List<FacturacionParametroResponse> listarParametros() {
        return maestroRepository.findByGrupoAndCodigoIn(GRUPO_FACTURACION, ALLOWED_CODES.stream().toList()).stream()
                .sorted(Comparator.comparing(Maestro::getCodigo))
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FacturacionParametroResponse obtenerParametro(String codigo) {
        validarCodigoSoportado(codigo);
        Maestro maestro = maestroRepository.findByCodigo(codigo)
                .orElseThrow(() -> new NoSuchElementException("Parámetro no encontrado: " + codigo));
        return mapToResponse(maestro);
    }

    @Transactional
    public FacturacionParametroResponse crearParametro(FacturacionParametroRequest request) {
        String codigo = normalizarCodigo(request.getCodigo());
        validarCodigoSoportado(codigo);

        if (maestroRepository.findByCodigo(codigo).isPresent()) {
            throw new IllegalArgumentException("El parámetro ya existe: " + codigo);
        }

        String valor = normalizarValor(request.getValor());
        validarValor(codigo, valor);

        Maestro maestro = new Maestro();
        maestro.setGrupo(GRUPO_FACTURACION);
        maestro.setCodigo(codigo);
        maestro.setValor(valor);
        maestro.setDescripcion(request.getDescripcion());
        maestro.setEstado(request.getEstado() == null || request.getEstado());

        return mapToResponse(maestroRepository.save(maestro));
    }

    @Transactional
    public FacturacionParametroResponse actualizarParametro(String codigo, FacturacionParametroRequest request) {
        String normalizedCode = normalizarCodigo(codigo);
        validarCodigoSoportado(normalizedCode);

        Maestro maestro = maestroRepository.findByCodigo(normalizedCode)
                .orElseThrow(() -> new NoSuchElementException("Parámetro no encontrado: " + normalizedCode));

        if (request.getValor() != null) {
            String valor = normalizarValor(request.getValor());
            validarValor(normalizedCode, valor);
            maestro.setValor(valor);
        }

        if (request.getDescripcion() != null) {
            maestro.setDescripcion(request.getDescripcion());
        }

        if (request.getEstado() != null) {
            maestro.setEstado(request.getEstado());
        }

        return mapToResponse(maestroRepository.save(maestro));
    }

    @Transactional
    public void eliminarParametro(String codigo) {
        String normalizedCode = normalizarCodigo(codigo);
        validarCodigoSoportado(normalizedCode);

        Maestro maestro = maestroRepository.findByCodigo(normalizedCode)
                .orElseThrow(() -> new NoSuchElementException("Parámetro no encontrado: " + normalizedCode));

        maestro.setEstado(false);
        maestroRepository.save(maestro);
    }

    @Transactional(readOnly = true)
    public String obtenerValorObligatorio(String codigo) {
        Maestro maestro = maestroRepository.findByCodigo(codigo)
                .orElseThrow(() -> new NoSuchElementException("Parámetro no encontrado: " + codigo));

        if (Boolean.FALSE.equals(maestro.getEstado())) {
            throw new IllegalStateException("Parámetro inactivo: " + codigo);
        }

        if (maestro.getValor() == null || maestro.getValor().trim().isEmpty()) {
            throw new IllegalStateException("Parámetro sin valor: " + codigo);
        }

        return maestro.getValor().trim();
    }

    @Transactional
    public int tomarSiguienteCorrelativoBoleta() {
        Maestro correlativo = maestroRepository.findByCodigoForUpdate(CODIGO_CORRELATIVO)
                .orElseThrow(() -> new NoSuchElementException("Parámetro no encontrado: " + CODIGO_CORRELATIVO));

        int actual;
        try {
            actual = Integer.parseInt(correlativo.getValor());
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Correlativo inválido");
        }

        int siguiente = actual + 1;
        correlativo.setValor(String.valueOf(siguiente));
        maestroRepository.save(correlativo);
        return siguiente;
    }

    private FacturacionParametroResponse mapToResponse(Maestro maestro) {
        return new FacturacionParametroResponse(
                maestro.getCodigo(),
                maestro.getValor(),
                maestro.getDescripcion(),
                maestro.getEstado()
        );
    }

    private void validarCodigoSoportado(String codigo) {
        if (!ALLOWED_CODES.contains(codigo)) {
            throw new IllegalArgumentException("Código de facturación no permitido: " + codigo);
        }
    }

    private void validarValor(String codigo, String valor) {
        switch (codigo) {
            case CODIGO_RUC -> {
                if (!RUC_PATTERN.matcher(valor).matches()) {
                    throw new IllegalArgumentException("RUC inválido. Debe tener 11 dígitos.");
                }
            }
            case CODIGO_SERIE_ACTIVA -> {
                if (!SERIE_PATTERN.matcher(valor).matches()) {
                    throw new IllegalArgumentException("Serie inválida. Debe cumplir patrón B###.");
                }
            }
            case CODIGO_CORRELATIVO -> {
                try {
                    int parsed = Integer.parseInt(valor);
                    if (parsed < 0) {
                        throw new IllegalArgumentException("Correlativo inválido. Debe ser >= 0.");
                    }
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Correlativo inválido. Debe ser numérico.");
                }
            }
            case "SUNAT_CPE_03" -> {
                if (!"03".equals(valor)) {
                    throw new IllegalArgumentException("SUNAT_CPE_03 debe ser 03.");
                }
            }
            case "SUNAT_IGV_20" -> {
                if (!"20".equals(valor)) {
                    throw new IllegalArgumentException("SUNAT_IGV_20 debe ser 20.");
                }
            }
            default -> {
                if (valor == null) {
                    throw new IllegalArgumentException("Valor no puede ser nulo.");
                }
            }
        }
    }

    private String normalizarCodigo(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            throw new IllegalArgumentException("Código requerido");
        }
        return codigo.trim().toUpperCase();
    }

    private String normalizarValor(String valor) {
        if (valor == null) {
            throw new IllegalArgumentException("Valor requerido");
        }
        return valor.trim();
    }
}

