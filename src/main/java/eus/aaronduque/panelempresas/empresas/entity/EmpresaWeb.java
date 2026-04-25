package eus.aaronduque.panelempresas.empresas.entity;


import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "empresas_web")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpresaWeb {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", unique = true, nullable = false)
    private Empresa empresa;

    @Column(length = 255)
    private String dominio;

    @Column(name = "url_completa", length = 500)
    private String urlCompleta;

    // Mapeo nativo de Hibernate 6 para arrays de Postgres
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "emails_genericos", columnDefinition = "text[]")
    private List<String> emailsGenericos;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> telefonos;

    // Mapeo de JSONB a Map (esto sí seguimos usándolo de hypersistence-utils)
    @Type(JsonType.class)
    @Column(name = "redes_sociales", columnDefinition = "jsonb")
    private Map<String, String> redesSociales;

    @Column(name = "descripcion_extraida", columnDefinition = "TEXT")
    private String descripcionExtraida;

    @Column(name = "ultimo_scraping")
    private LocalDateTime ultimoScraping;

    @Column(name = "scraping_exitoso")
    @Builder.Default
    private Boolean scrapingExitoso = false;

    @Column(name = "scraping_error", columnDefinition = "TEXT")
    private String scrapingError;

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;
}