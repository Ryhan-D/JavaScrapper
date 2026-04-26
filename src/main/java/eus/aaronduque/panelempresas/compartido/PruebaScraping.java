package eus.aaronduque.panelempresas.compartido;

import eus.aaronduque.panelempresas.extraccion.dto.DatosExtraidos;
import eus.aaronduque.panelempresas.extraccion.service.ExtractorLLM;
import eus.aaronduque.panelempresas.scraping.dto.ResultadoScraping;
import eus.aaronduque.panelempresas.scraping.service.DescubridorPaginas;
import eus.aaronduque.panelempresas.scraping.service.LimpiadorHtml;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class PruebaScraping implements CommandLineRunner {

    private final DescubridorPaginas descubridorPaginas;
    private final LimpiadorHtml limpiadorHtml;
    private final ExtractorLLM extractorLLM;

    @Override
    public void run(String... args) {
        String[] urlsPrueba = {
            "https://www.petronor.eus",
            "https://www.itpaero.com"
        };

        log.info("=== PRUEBA SCRAPING + GEMINI ===");
        for (String url : urlsPrueba) {
            log.info("");
            log.info(">>> Procesando {}", url);

            List<ResultadoScraping> paginas = descubridorPaginas.descubrir(url, 3);
            if (paginas.isEmpty()) {
                log.info("    NO se encontraron páginas accesibles");
                continue;
            }

            String textoCombinado = paginas.stream()
                .map(p -> "--- " + p.getUrl() + " ---\n" + limpiadorHtml.limpiar(p.getHtml()))
                .collect(Collectors.joining("\n\n"));

            log.info("    Texto combinado: {} caracteres", textoCombinado.length());
            log.info("    Llamando a Gemini...");

            long inicio = System.currentTimeMillis();
            DatosExtraidos datos = extractorLLM.extraer(textoCombinado);
            long duracion = System.currentTimeMillis() - inicio;

            log.info("    Gemini tardó {}ms", duracion);
            log.info("     Emails genéricos: {}", datos.getEmailsGenericos());
            log.info("     Teléfonos: {}", datos.getTelefonos());
            log.info("     Personas encontradas: {}", datos.getPersonas().size());
            datos.getPersonas().forEach(p -> 
                log.info("       - {} ({})", p.getNombre(), p.getCargo()));
            log.info("     Redes sociales: {}", datos.getRedesSociales());
            log.info("     Descripción: {}", datos.getDescripcionBreve());
        }
        log.info("");
        log.info("=== FIN PRUEBA ===");
    }
}