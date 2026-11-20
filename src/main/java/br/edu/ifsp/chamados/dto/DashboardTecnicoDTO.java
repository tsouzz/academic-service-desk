package br.edu.ifsp.chamados.dto;

import br.edu.ifsp.chamados.model.Chamado;

import java.util.List;

public record DashboardTecnicoDTO(
        List<Chamado> meusAtribuidos,
        List<Chamado> filaSetor,
        long concluidosPorMim
) {}
