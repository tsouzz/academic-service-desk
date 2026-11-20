package br.edu.ifsp.chamados.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ErroApiDTO(
        LocalDateTime timestamp,
        int status,
        String erro,
        String mensagem,
        String caminho,
        List<String> detalhes
) {

    public static ErroApiDTO of(int status, String erro, String mensagem, String caminho) {
        return new ErroApiDTO(LocalDateTime.now(), status, erro, mensagem, caminho, List.of());
    }

    public static ErroApiDTO of(int status, String erro, String mensagem, String caminho, List<String> detalhes) {
        return new ErroApiDTO(LocalDateTime.now(), status, erro, mensagem, caminho, detalhes);
    }
}
