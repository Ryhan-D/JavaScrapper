package eus.aaronduque.panelempresas.empresas.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "empresas_contactos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpresaContacto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(length = 255)
    private String nombre;

    @Column(length = 255)
    private String cargo;

    @Enumerated(EnumType.STRING)
    @Column(name = "cargo_categoria", length = 50)
    private CategoriaCargo cargoCategoria;

    @Column(length = 255)
    private String email;

    @Column(length = 50)
    private String telefono;

    @Column(length = 50)
    private String fuente;

    @Column(precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal confianza = new BigDecimal("0.50");

    @CreationTimestamp
    @Column(name = "fecha_extraccion", updatable = false)
    private LocalDateTime fechaExtraccion;
}