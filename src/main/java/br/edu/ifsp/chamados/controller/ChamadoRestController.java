package br.edu.ifsp.chamados.controller;

import br.edu.ifsp.chamados.dto.ChamadoAtualizacaoDTO;
import br.edu.ifsp.chamados.dto.ChamadoCadastroDTO;
import br.edu.ifsp.chamados.dto.ChamadoResponseDTO;
import br.edu.ifsp.chamados.model.Chamado;
import br.edu.ifsp.chamados.model.Status;
import br.edu.ifsp.chamados.security.UsuarioAutenticado;
import br.edu.ifsp.chamados.service.ChamadoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/chamados")
public class ChamadoRestController {

    private final ChamadoService service;

    public ChamadoRestController(ChamadoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ChamadoResponseDTO>> listar(@RequestParam(required = false) Status status,
                                                           @RequestParam(required = false) Long categoriaId,
                                                           @AuthenticationPrincipal UsuarioAutenticado usuarioLogado) {
        List<ChamadoResponseDTO> chamados = service.listar(status, categoriaId, usuarioLogado).stream()
                .map(ChamadoResponseDTO::from)
                .toList();
        return ResponseEntity.ok(chamados);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChamadoResponseDTO> buscarPorId(@PathVariable Long id,
                                                          @AuthenticationPrincipal UsuarioAutenticado usuarioLogado) {
        Chamado chamado = service.buscarComPermissao(id, usuarioLogado);
        return ResponseEntity.ok(ChamadoResponseDTO.from(chamado));
    }

    @PostMapping
    public ResponseEntity<ChamadoResponseDTO> cadastrar(@Valid @RequestBody ChamadoCadastroDTO dto,
                                                        @AuthenticationPrincipal UsuarioAutenticado usuarioLogado) {
        Chamado criado = service.cadastrar(dto, usuarioLogado);
        URI localizacao = URI.create("/api/chamados/" + criado.getId());
        return ResponseEntity.created(localizacao).body(ChamadoResponseDTO.from(criado));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChamadoResponseDTO> atualizar(@PathVariable Long id,
                                                        @Valid @RequestBody ChamadoAtualizacaoDTO dto,
                                                        @AuthenticationPrincipal UsuarioAutenticado usuarioLogado) {
        Chamado atualizado = service.atualizar(id, dto, usuarioLogado);
        return ResponseEntity.ok(ChamadoResponseDTO.from(atualizado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id,
                                        @AuthenticationPrincipal UsuarioAutenticado usuarioLogado) {
        service.excluir(id, usuarioLogado);
        return ResponseEntity.noContent().build();
    }
}
