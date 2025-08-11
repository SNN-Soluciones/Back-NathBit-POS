package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "unidades_medida",
		uniqueConstraints = {
				@UniqueConstraint(columnNames = {"codigo"}),
				@UniqueConstraint(columnNames = {"simbolo"})
		})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnidadMedida {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 10)
	private String codigo; // Según Hacienda

	@Column(nullable = false, length = 10)
	private String simbolo;

	@Column(nullable = false, length = 100)
	private String descripcion;

	@Column(nullable = false)
	@Builder.Default
	private Boolean activo = true;
}