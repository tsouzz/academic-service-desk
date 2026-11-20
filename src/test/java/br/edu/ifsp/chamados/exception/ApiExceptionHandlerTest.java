package br.edu.ifsp.chamados.exception;

import br.edu.ifsp.chamados.dto.ErroApiDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/chamados/9");

    @Test
    @DisplayName("ResponseStatusException 404 deve virar JSON com status 404")
    void deveTraduzir404() {
        ResponseEntity<ErroApiDTO> resposta = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Chamado #9 não encontrado."), request);

        assertEquals(HttpStatus.NOT_FOUND, resposta.getStatusCode());
        assertEquals(404, resposta.getBody().status());
        assertEquals("Chamado #9 não encontrado.", resposta.getBody().mensagem());
        assertEquals("/api/chamados/9", resposta.getBody().caminho());
    }

    @Test
    @DisplayName("RegraNegocioException deve virar 422")
    void deveTraduzirRegraDeNegocio() {
        ResponseEntity<ErroApiDTO> resposta = handler.handleRegraNegocio(
                new RegraNegocioException("A categoria selecionada não é válida."), request);

        assertEquals(422, resposta.getStatusCode().value());
    }

    @Test
    @DisplayName("Conflito de versão deve virar 409")
    void deveTraduzirConflitoDeVersao() {
        ResponseEntity<ErroApiDTO> resposta = handler.handleConflitoVersao(
                new OptimisticLockingFailureException("versão divergente"), request);

        assertEquals(HttpStatus.CONFLICT, resposta.getStatusCode());
    }
}
