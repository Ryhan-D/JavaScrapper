package eus.aaronduque.panelempresas.compartido;

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

    @Override
    public void run(String... args) {
        String[] urlsPrueba = {
            "https://www.petronor.eus",
            "https://www.tubacex.com",
            "https://www.itpaero.com"
        };

        log.info("=== PRUEBA DESCUBRIMIENTO + LIMPIEZA ===");
        for (String url : urlsPrueba) {
            log.info("");
            log.info(">>> Procesando {}", url);

            List<ResultadoScraping> paginas = descubridorPaginas.descubrir(url, 4);

            if (paginas.isEmpty()) {
                log.info("    NO se encontraron páginas accesibles");
                continue;
            }

            // Concatenar el texto limpio de todas las páginas
            String textoCombinado = paginas.stream()
                .map(p -> "--- " + p.getUrl() + " ---\n" + limpiadorHtml.limpiar(p.getHtml()))
                .collect(Collectors.joining("\n\n"));

            log.info("    Páginas descargadas: {}", paginas.size());
            paginas.forEach(p -> log.info("       • {}", p.getUrl()));
            log.info("    Texto combinado: {} caracteres", textoCombinado.length());
            
            String muestra = textoCombinado.length() > 800 
                ? textoCombinado.substring(0, 800) + "..." 
                : textoCombinado;
            log.info("    Muestra:\n{}", muestra);
        }
        log.info("");
        log.info("=== FIN PRUEBA ===");
    }
}