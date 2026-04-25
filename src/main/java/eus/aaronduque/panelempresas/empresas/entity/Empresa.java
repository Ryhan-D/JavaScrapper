package eus.aaronduque.panelempresas.empresas.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "empresas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String nombre;

    @Column(unique = true, length = 12)
    private String cif;

    @Column(length = 100)
    private String sector;

    @Column(length = 50)
    private String provincia;

    @Column(length = 100)
    private String municipio;

    @Column(columnDefinition = "TEXT")
    private String direccion;

    // Categorización por tamaño
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TamanoEmpresa tamano;

    @Column(name = "tamano_confianza", precision = 3, scale = 2)
    private BigDecimal tamanoConfianza;

    @Column(name = "tamano_justificacion", columnDefinition = "TEXT")
    private String tamanoJustificacion;

    @Column(name = "empleados_estimados")
    private Integer empleadosEstimados;

    // Estado de procesamiento
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_enriquecimiento", length = 20)
    @Builder.Default
    private EstadoEnriquecimiento estadoEnriquecimiento = EstadoEnriquecimiento.pendiente;

    @Column(columnDefinition = "TEXT")
    private String notas;

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}