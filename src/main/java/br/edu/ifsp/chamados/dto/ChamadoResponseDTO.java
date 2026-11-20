package br.edu.ifsp.chamados.dto;

import br.edu.ifsp.chamados.model.Chamado;
import br.edu.ifsp.chamados.model.Prioridade;
import br.edu.ifsp.chamados.model.Status;
import br.edu.ifsp.chamados.model.Usuario;

import java.time.LocalDate;

public record ChamadoResponseDTO(
        Long id,
        String titulo,
        String descricao,
        Status status,
        Prioridade prioridade,
        CategoriaResumo categoria,
        UsuarioResumo solicitante,
        UsuarioResumo responsavel,
        String observacaoAtendimento,
        LocalDate dataAbertura,
        LocalDate dataAtualizacao,
        Long versao
) {

    public record CategoriaResumo(Long id, String nome, String setor) {}

    public record UsuarioResumo(Long id, String nome) {}

    public static ChamadoResponseDTO from(Chamado chamado) {
        return new ChamadoResponseDTO(
                chamado.getId(),
                chamado.getTitulo(),
                chamado.getDescricao(),
                chamado.getStatus(),
                chamado.getPrioridade(),
                new CategoriaResumo(
                        chamado.getCategoria().getId(),
                        chamado.getCategoria().getNome(),
                        chamado.getCategoria().getDepartamento().getSigla()),
                resumir(chamado.getSolicitante()),
                resumir(chamado.getResponsavel()),
                chamado.getObservacaoAtendimento(),
                chamado.getDataAbertura(),
                chamado.getDataAtualizacao(),
                chamado.getVersao());
    }

    private static UsuarioResumo resumir(Usuario usuario) {
        return usuario == null ? null : new UsuarioResumo(usuario.getId(), usuario.getNome());
    }
}
