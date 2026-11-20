package br.edu.ifsp.chamados.controller;

import br.edu.ifsp.chamados.dto.ChamadoAtualizacaoDTO;
import br.edu.ifsp.chamados.dto.ChamadoCadastroDTO;
import br.edu.ifsp.chamados.dto.ChamadoResponseDTO;
import br.edu.ifsp.chamados.model.*;
import br.edu.ifsp.chamados.security.UsuarioAutenticado;
import br.edu.ifsp.chamados.service.ChamadoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChamadoRestControllerTest {

    @Mock
    private ChamadoService service;

    private ChamadoRestController controller;
    private UsuarioAutenticado gestor;
    private Chamado chamado;

    @BeforeEach
    void setUp() {
        controller = new ChamadoRestController(service);

        Departamento ti = new Departamento();
        ti.setId(1L);
        ti.setSigla("TI");
        ti.setNome("Tecnologia da Informação");

        Categoria categoria = new Categoria();
        categoria.setId(10L);
        categoria.setNome("Suporte técnico");
        categoria.setDepartamento(ti);

        Usuario solicitante = usuario(1L, "Aluno", Papel.ALUNO);
        gestor = new UsuarioAutenticado(usuario(3L, "Gestor", Papel.GESTOR));

        chamado = new Chamado();
        chamado.setId(100L);
        chamado.setTitulo("Monitor não liga");
        chamado.setDescricao("Sem vídeo");
        chamado.setStatus(Status.ABERTO);
        chamado.setPrioridade(Prioridade.ALTA);
        chamado.setCategoria(categoria);
        chamado.setSolicitante(solicitante);
        chamado.setDataAbertura(LocalDate.now());
        chamado.setDataAtualizacao(LocalDate.now());
        chamado.setVersao(0L);
    }

    @Test
    @DisplayName("GET /api/chamados deve devolver 200 com a lista de DTOs")
    void deveListarChamados() {
        when(service.listar(null, null, gestor)).thenReturn(List.of(chamado));

        ResponseEntity<List<ChamadoResponseDTO>> resposta = controller.listar(null, null, gestor);

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertEquals(1, resposta.getBody().size());
        assertEquals("TI", resposta.getBody().get(0).categoria().setor());
        assertNull(resposta.getBody().get(0).responsavel());
    }

    @Test
    @DisplayName("GET /api/chamados/{id} deve devolver 200 com o chamado")
    void deveBuscarPorId() {
        when(service.buscarComPermissao(100L, gestor)).thenReturn(chamado);

        ResponseEntity<ChamadoResponseDTO> resposta = controller.buscarPorId(100L, gestor);

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertEquals(100L, resposta.getBody().id());
    }

    @Test
    @DisplayName("GET /api/chamados/{id} deve propagar 404 quando o chamado não existe")
    void devePropagar404QuandoNaoEncontrado() {
        when(service.buscarComPermissao(999L, gestor))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Chamado #999 não encontrado."));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.buscarPorId(999L, gestor));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("POST /api/chamados deve devolver 201 com o header Location")
    void deveCadastrarComStatus201() {
        ChamadoCadastroDTO dto = new ChamadoCadastroDTO();
        dto.setTitulo("Monitor não liga");
        dto.setDescricao("Sem vídeo");
        dto.setCategoriaId(10L);
        when(service.cadastrar(dto, gestor)).thenReturn(chamado);

        ResponseEntity<ChamadoResponseDTO> resposta = controller.cadastrar(dto, gestor);

        assertEquals(HttpStatus.CREATED, resposta.getStatusCode());
        assertEquals("/api/chamados/100", resposta.getHeaders().getLocation().toString());
        assertEquals(100L, resposta.getBody().id());
    }

    @Test
    @DisplayName("PUT /api/chamados/{id} deve devolver 200 com o chamado atualizado")
    void deveAtualizar() {
        ChamadoAtualizacaoDTO dto = new ChamadoAtualizacaoDTO();
        dto.setVersao(0L);
        dto.setStatus(Status.EM_ANALISE);
        dto.setPrioridade(Prioridade.ALTA);
        chamado.setStatus(Status.EM_ANALISE);
        when(service.atualizar(100L, dto, gestor)).thenReturn(chamado);

        ResponseEntity<ChamadoResponseDTO> resposta = controller.atualizar(100L, dto, gestor);

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertEquals(Status.EM_ANALISE, resposta.getBody().status());
    }

    @Test
    @DisplayName("DELETE /api/chamados/{id} deve devolver 204 sem corpo")
    void deveExcluirComStatus204() {
        ResponseEntity<Void> resposta = controller.excluir(100L, gestor);

        assertEquals(HttpStatus.NO_CONTENT, resposta.getStatusCode());
        assertNull(resposta.getBody());
        verify(service).excluir(100L, gestor);
    }

    private Usuario usuario(Long id, String nome, Papel papel) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNome(nome);
        u.setEmail(nome.toLowerCase() + "@ifsp.edu.br");
        u.setSenhaHash("hash");
        u.setPapel(papel);
        return u;
    }
}
