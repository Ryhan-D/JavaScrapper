package eus.aaronduque.panelempresas.enriquecimiento.controller;

import eus.aaronduque.panelempresas.enriquecimiento.dto.ResultadoEnriquecimiento;
import eus.aaronduque.panelempresas.enriquecimiento.service.EnriquecimientoService;
import eus.aaronduque.panelempresas.empresas.entity.Empresa;
import eus.aaronduque.panelempresas.empresas.entity.EstadoEnriquecimiento;
import eus.aaronduque.panelempresas.empresas.repository.EmpresaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/empresas")
@RequiredArgsConstructor
@Slf4j
public class EnriquecimientoController {

    private final EnriquecimientoService enriquecimientoService;
    private final EmpresaRepository empresaRepository;

    private static final int LIMITE_DEFAULT = 3;
    private static final int LIMITE_MAXIMO = 5;

    /**
     * Enriquece una sola empresa por su id (sincrono).
     */
    @PostMapping("/{id}/enriquecer")
    public ResponseEntity<ResultadoEnriquecimiento> enriquecerUna(@PathVariable Long id) {
        log.info("POST /empresas/{}/enriquecer recibido", id);

        ResultadoEnriquecimiento resultado = enriquecimientoService.enriquecer(id);

        // Mapeo de estado final → código HTTP
        HttpStatus status = switch (resultado.getEstadoFinal()) {
            case enriquecida -> HttpStatus.OK;
            case sin_web -> HttpStatus.BAD_REQUEST;
            case error -> HttpStatus.UNPROCESSABLE_ENTITY;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };

        return ResponseEntity.status(status).body(resultado);
    }

    /**
     * Enriquece hasta N empresas pendientes en bucle síncrono.
     */
    @PostMapping("/enriquecer-pendientes")
    public ResponseEntity<Map<String, Object>> enriquecerPendientes(
            @RequestParam(defaultValue = "3") int limite) {

        if (limite < 1) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "El parámetro 'limite' debe ser mayor o igual a 1"
            ));
        }
        int limiteEfectivo = Math.min(limite, LIMITE_MAXIMO);
        if (limite > LIMITE_MAXIMO) {
            log.warn("Limite solicitado {} excede el máximo {}, ajustando", limite, LIMITE_MAXIMO);
        }

        log.info("POST /empresas/enriquecer-pendientes (limite efectivo: {})", limiteEfectivo);

        // Buscar empresas pendientes
        List<Empresa> pendientes = empresaRepository
            .findByEstadoEnriquecimiento(EstadoEnriquecimiento.pendiente)
            .stream()
            .limit(limiteEfectivo)
            .toList();

        if (pendientes.isEmpty()) {
            log.info("No hay empresas pendientes de enriquecer");
            return ResponseEntity.ok(Map.of(
                "procesadas", 0,
                "mensaje", "No hay empresas pendientes",
                "resultados", List.of()
            ));
        }

        // Procesar una por una
        List<ResultadoEnriquecimiento> resultados = new ArrayList<>();
        long inicioTotal = System.currentTimeMillis();

        for (Empresa empresa : pendientes) {
            try {
                ResultadoEnriquecimiento r = enriquecimientoService.enriquecer(empresa.getId());
                resultados.add(r);
            } catch (Exception e) {
                log.error("Fallo procesando empresa {}: {}", empresa.getId(), e.getMessage(), e);
                // Si una empresa lanza algo inesperado, lo registramos y seguimos
                resultados.add(ResultadoEnriquecimiento.builder()
                    .empresaId(empresa.getId())
                    .nombreEmpresa(empresa.getNombre())
                    .estadoFinal(EstadoEnriquecimiento.error)
                    .error("Excepción no controlada: " + e.getMessage())
                    .build());
            }
        }

        long duracionTotal = System.currentTimeMillis() - inicioTotal;

        // Contadores para el resumen
        long exitosas = resultados.stream()
            .filter(r -> r.getEstadoFinal() == EstadoEnriquecimiento.enriquecida).count();
        long errores = resultados.stream()
            .filter(r -> r.getEstadoFinal() == EstadoEnriquecimiento.error).count();
        long sinWeb = resultados.stream()
            .filter(r -> r.getEstadoFinal() == EstadoEnriquecimiento.sin_web).count();

        log.info("Bucle completado en {}ms: {} exitosas, {} errores, {} sin web",
            duracionTotal, exitosas, errores, sinWeb);

        return ResponseEntity.ok(Map.of(
            "procesadas", resultados.size(),
            "exitosas", exitosas,
            "errores", errores,
            "sinWeb", sinWeb,
            "duracionTotalMs", duracionTotal,
            "resultados", resultados
        ));
    }
}