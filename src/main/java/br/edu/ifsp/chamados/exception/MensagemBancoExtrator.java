package br.edu.ifsp.chamados.exception;

import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;

import java.sql.SQLException;

public final class MensagemBancoExtrator {

    private static final String MENSAGEM_PADRAO =
            "Não foi possível concluir a operação no banco de dados devido a uma restrição de integridade.";

    private MensagemBancoExtrator() {
    }

    public static String extrair(DataAccessException ex) {
        Throwable root = NestedExceptionUtils.getMostSpecificCause(ex);
        if (!(root instanceof SQLException sqlEx) || sqlEx.getMessage() == null) {
            return MENSAGEM_PADRAO;
        }

        String sqlState = sqlEx.getSQLState();
        if ("23505".equals(sqlState)) {
            return "Operação bloqueada: já existe um registro com os mesmos dados únicos.";
        }
        if ("23503".equals(sqlState)) {
            return "Operação bloqueada: este registro está vinculado a outros dados ativos no sistema.";
        }

        String mensagem = limparMensagem(sqlEx.getMessage());
        return mensagem.isBlank() ? MENSAGEM_PADRAO : mensagem;
    }

    private static String limparMensagem(String bruta) {
        String texto = bruta.trim();
        texto = cortarAntesDe(texto, "Where:");
        texto = cortarAntesDe(texto, "Detail:");
        return texto.replaceFirst("(?i)^(ERROR|ERRO):\\s*", "").trim();
    }

    private static String cortarAntesDe(String texto, String marcador) {
        int idx = texto.indexOf("\n  " + marcador);
        if (idx == -1) idx = texto.indexOf("\n" + marcador);
        return idx > 0 ? texto.substring(0, idx).trim() : texto;
    }
}
