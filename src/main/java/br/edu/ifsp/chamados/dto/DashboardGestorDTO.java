package br.edu.ifsp.chamados.dto;

import java.util.List;

public record DashboardGestorDTO(
        List<IndicadorDepartamento> indicadores,
        long totalGeral,
        long abertosGeral,
        long emAndamentoGeral,
        long concluidosGeral,
        long canceladosGeral
) {}
