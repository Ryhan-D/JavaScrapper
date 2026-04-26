package eus.aaronduque.panelempresas.descubrimiento.controller;

import eus.aaronduque.panelempresas.descubrimiento.dto.ResultadoDescubrimiento;
import eus.aaronduque.panelempresas.descubrimiento.service.DescubridorDominios;
import eus.aaronduque.panelempresas.empresas.entity.Empresa;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/empresas")
@RequiredArgsConstructor
@Slf4j
public class DescubrimientoController {

    private final DescubridorDominios descubridorDominios;

    // Límites del endpoint masivo (mismos valores que el de enriquecimiento
    // para mantener consistencia entre fases)
    private static final int LIMITE_DEFAULT = 3;
    private static final int LIMITE_MAXIMO = 5;

    /**
     * Descubre el dominio oficial de una empresa concreta (modo individual).
     */
    @PostMapping("/{id}/descubrir-dominio")
    public ResponseEntity<ResultadoDescubrimiento> descubrirUna(@PathVariable Long id) {
        log.info("POST /empresas/{}/descubrir-dominio recibido", id);

        ResultadoDescubrimiento resultado = descubridorDominios.descubrir(id);

        HttpStatus status = switch (resultado.getEstado()) {
            case encontrado, sin_resultado -> HttpStatus.OK;
            case error -> HttpStatus.SERVICE_UNAVAILABLE;
        };

        return ResponseEntity.status(status).body(resultado);
    }

    /**
     * Descubre dominios para hasta N empresas pendientes que no tengan URL
     */
    @PostMapping("/descubrir-dominios-pendientes")
    public ResponseEntity<Map<String, Object>> descubrirPendientes(
            @RequestParam(defaultValue = "3") int limite) {

        // Validación del parámetro
        if (limite < 1) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "El parámetro 'limite' debe ser mayor o igual a 1"
            ));
        }
        int limiteEfectivo = Math.min(limite, LIMITE_MAXIMO);
        if (limite > LIMITE_MAXIMO) {
            log.warn("Limite solicitado {} excede el máximo {}, ajustando a {}",
                limite, LIMITE_MAXIMO, LIMITE_MAXIMO);
        }

        log.info("POST /empresas/descubrir-dominios-pendientes (limite efectivo: {})",
            limiteEfectivo);

        // Buscar empresas candidatas
        List<Empresa> candidatas = descubridorDominios.obtenerCandidatasPendientes(limiteEfectivo);

        if (candidatas.isEmpty()) {
            log.info("No hay empresas pendientes sin URL");
            return ResponseEntity.ok(Map.of(
                "procesadas", 0,
                "mensaje", "No hay empresas pendientes sin URL",
                "resultados", List.of()
            ));
        }

        // Procesar una por una
        List<ResultadoDescubrimiento> resultados = new ArrayList<>();
        long inicioTotal = System.currentTimeMillis();

        for (Empresa empresa : candidatas) {
            try {
                ResultadoDescubrimiento r = descubridorDominios.descubrir(empresa.getId());
                resultados.add(r);
            } catch (Exception e) {
                // Si una empresa lanza algo inesperado (ej: IllegalArgumentException
                // por desaparición concurrente), lo registramos y seguimos.
                log.error("Fallo procesando empresa {}: {}",
                    empresa.getId(), e.getMessage(), e);
                resultados.add(ResultadoDescubrimiento.builder()
                    .empresaId(empresa.getId())
                    .nombreEmpresa(empresa.getNombre())
                    .estado(ResultadoDescubrimiento.Estado.error)
                    .error("Excepción no controlada: " + e.getMessage())
                    .build());
            }
        }

        long duracionTotal = System.currentTimeMillis() - inicioTotal;

        // Contadores para el resumen
        long encontradas = resultados.stream()
            .filter(r -> r.getEstado() == ResultadoDescubrimiento.Estado.encontrado)
            .count();
        long sinResultado = resultados.stream()
            .filter(r -> r.getEstado() == ResultadoDescubrimiento.Estado.sin_resultado)
            .count();
        long errores = resultados.stream()
            .filter(r -> r.getEstado() == ResultadoDescubrimiento.Estado.error)
            .count();

        log.info("Descubrimiento masivo terminado: {} procesadas en {}ms "
            + "(encontradas={}, sin_resultado={}, errores={})",
            resultados.size(), duracionTotal, encontradas, sinResultado, errores);

        return ResponseEntity.ok(Map.of(
            "procesadas", resultados.size(),
            "encontradas", encontradas,
            "sin_resultado", sinResultado,
            "errores", errores,
            "duracionTotalMs", duracionTotal,
            "resultados", resultados
        ));
    }
}