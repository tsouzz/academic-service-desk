package br.edu.ifsp.chamados.dto;

import br.edu.ifsp.chamados.model.Chamado;

import java.util.List;

public record DashboardSolicitanteDTO(
        List<Chamado> emAberto,
        List<Chamado> encerrados,
        long aguardandoAcao
) {}
