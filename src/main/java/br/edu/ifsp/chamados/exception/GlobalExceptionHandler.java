package br.edu.ifsp.chamados.exception;

import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({OptimisticLockingFailureException.class, OptimisticLockException.class})
    public String handleOptimisticLock(Exception ex,
                                       HttpServletRequest request,
                                       RedirectAttributes ra) {
        log.warn("Conflito de concorrência detectado em {}: {}", request.getRequestURI(), ex.getMessage());
        ra.addFlashAttribute("erro",
                "Este chamado foi atualizado por outro atendente enquanto você o editava. " +
                        "Os dados foram recarregados para evitar sobrescrita; revise antes de salvar novamente.");
        return "redirect:" + resolverDestinoSeguro(request);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public String handleRegraNegocio(RegraNegocioException ex,
                                     HttpServletRequest request,
                                     RedirectAttributes ra) {
        log.warn("Violação de regra de negócio em {}: {}", request.getRequestURI(), ex.getMessage());
        ra.addFlashAttribute("erro", ex.getMessage());
        return "redirect:" + resolverDestinoSeguro(request);
    }

    @ExceptionHandler(DataAccessException.class)
    public String handleDataAccess(DataAccessException ex,
                                   HttpServletRequest request,
                                   RedirectAttributes ra) {
        String mensagemLimpa = MensagemBancoExtrator.extrair(ex);
        log.error("Erro de persistência em {}: {}", request.getRequestURI(), mensagemLimpa, ex);
        ra.addFlashAttribute("erro", mensagemLimpa);
        return "redirect:" + resolverDestinoSeguro(request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException ex,
                                     HttpServletResponse response,
                                     Model model) {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        model.addAttribute("status", 403);
        model.addAttribute("mensagem", "Você não possui permissão para acessar este recurso ou executar esta ação.");
        return "error/403";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public String handleNoResourceFound(NoResourceFoundException ex,
                                        HttpServletResponse response,
                                        Model model) {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        model.addAttribute("status", 404);
        model.addAttribute("mensagem", "A página ou recurso solicitado não foi encontrado.");
        return "error/404";
    }

    @ExceptionHandler(ResponseStatusException.class)
    public String handleResponseStatus(ResponseStatusException ex,
                                       HttpServletRequest request,
                                       HttpServletResponse response,
                                       RedirectAttributes ra,
                                       Model model) {
        int code = ex.getStatusCode().value();
        String reason = ex.getReason() != null ? ex.getReason() : "Não foi possível processar a solicitação.";

        if (code == HttpStatus.BAD_REQUEST.value() && "POST".equalsIgnoreCase(request.getMethod())) {
            ra.addFlashAttribute("erro", reason);
            return "redirect:" + resolverDestinoSeguro(request);
        }

        response.setStatus(code);
        model.addAttribute("status", code);
        model.addAttribute("mensagem", reason);

        if (code == HttpStatus.FORBIDDEN.value()) {
            return "error/403";
        }
        if (code == HttpStatus.NOT_FOUND.value()) {
            return "error/404";
        }
        return "error/500";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneric(Exception ex,
                                HttpServletRequest request,
                                HttpServletResponse response,
                                Model model) {
        log.error("Erro interno inesperado em {}", request.getRequestURI(), ex);
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        model.addAttribute("status", 500);
        model.addAttribute("mensagem", "Ocorreu um erro interno inesperado no sistema. Tente novamente em instantes.");
        return "error/500";
    }

    private String resolverDestinoSeguro(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri != null && uri.matches("^/chamados/\\d+/editar$")) {
            return uri;
        }
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            try {
                URI refUri = URI.create(referer);
                String path = refUri.getPath();
                if (path != null && path.startsWith("/") && !path.equals(uri)) {
                    return path;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return "/chamados";
    }
}