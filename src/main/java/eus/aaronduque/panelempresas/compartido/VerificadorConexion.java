package eus.aaronduque.panelempresas.compartido;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Verifica al arrancar que la conexión a Supabase funciona.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VerificadorConexion implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            Integer numEmpresas = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM empresas",
                Integer.class
            );
            log.info("✓ Conexión a Supabase OK. Empresas en BD: {}", numEmpresas);
        } catch (Exception e) {
            log.error("✗ Error conectando a Supabase: {}", e.getMessage());
        }
    }
}