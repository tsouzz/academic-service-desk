package br.edu.ifsp.chamados.exception;

import br.edu.ifsp.chamados.dto.ErroApiDTO;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestControllerAdvice(annotations = RestController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final HttpStatusCode STATUS_422 = HttpStatusCode.valueOf(422);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErroApiDTO> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatusCode status = ex.getStatusCode();
        String mensagem = ex.getReason() != null ? ex.getReason() : "Não foi possível processar a solicitação.";
        return resposta(status, mensagem, request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroApiDTO> handleValidacao(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> detalhes = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .toList();
        return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos. Verifique os campos informados.", request, detalhes);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroApiDTO> handleJsonInvalido(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST,
                "Corpo da requisição ausente ou JSON inválido (verifique também os valores de status e prioridade).",
                request, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroApiDTO> handleTipoInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST,
                "Valor inválido para o parâmetro '" + ex.getName() + "'.", request, List.of());
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroApiDTO> handleRegraNegocio(RegraNegocioException ex, HttpServletRequest request) {
        return resposta(STATUS_422, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler({OptimisticLockingFailureException.class, OptimisticLockException.class})
    public ResponseEntity<ErroApiDTO> handleConflitoVersao(Exception ex, HttpServletRequest request) {
        log.warn("Conflito de versão em {}: {}", request.getRequestURI(), ex.getMessage());
        return resposta(HttpStatus.CONFLICT,
                "O chamado foi alterado por outro usuário. Consulte-o novamente e reenvie com a versão atual.",
                request, List.of());
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErroApiDTO> handlePersistencia(DataAccessException ex, HttpServletRequest request) {
        log.error("Erro de persistência em {}", request.getRequestURI(), ex);
        return resposta(HttpStatus.CONFLICT, MensagemBancoExtrator.extrair(ex), request, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroApiDTO> handleAcessoNegado(AccessDeniedException ex, HttpServletRequest request) {
        return resposta(HttpStatus.FORBIDDEN, "Você não possui permissão para executar esta ação.", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroApiDTO> handleInesperado(Exception ex, HttpServletRequest request) {
        log.error("Erro interno inesperado em {}", request.getRequestURI(), ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro interno inesperado. Tente novamente em instantes.", request, List.of());
    }

    private ResponseEntity<ErroApiDTO> resposta(HttpStatusCode status, String mensagem,
                                                HttpServletRequest request, List<String> detalhes) {
        String descricao = status instanceof HttpStatus httpStatus ? httpStatus.getReasonPhrase() : "Erro";
        ErroApiDTO corpo = ErroApiDTO.of(status.value(), descricao, mensagem, request.getRequestURI(), detalhes);
        return ResponseEntity.status(status).body(corpo);
    }
}
