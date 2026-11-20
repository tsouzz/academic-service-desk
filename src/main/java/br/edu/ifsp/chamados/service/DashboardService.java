package br.edu.ifsp.chamados.service;

import br.edu.ifsp.chamados.dto.DashboardGestorDTO;
import br.edu.ifsp.chamados.dto.DashboardSolicitanteDTO;
import br.edu.ifsp.chamados.dto.DashboardTecnicoDTO;
import br.edu.ifsp.chamados.dto.IndicadorDepartamento;
import br.edu.ifsp.chamados.model.Chamado;
import br.edu.ifsp.chamados.model.Status;
import br.edu.ifsp.chamados.security.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.function.ToLongFunction;

@Service
public class DashboardService {

    private static final Set<Status> STATUS_FINAIS = Set.of(Status.CONCLUIDO, Status.CANCELADO);

    private final ChamadoService chamadoService;
    private final RotinasBancoService rotinas;

    public DashboardService(ChamadoService chamadoService, RotinasBancoService rotinas) {
        this.chamadoService = chamadoService;
        this.rotinas = rotinas;
    }

    @Transactional(readOnly = true)
    public DashboardGestorDTO montarDashboardGestor() {
        List<IndicadorDepartamento> indicadores = rotinas.indicadores();

        return new DashboardGestorDTO(
                indicadores,
                somar(indicadores, IndicadorDepartamento::total),
                somar(indicadores, IndicadorDepartamento::abertos),
                somar(indicadores, i -> i.emAnalise() + i.aguardando()),
                somar(indicadores, IndicadorDepartamento::concluidos),
                somar(indicadores, IndicadorDepartamento::cancelados));
    }

    @Transactional(readOnly = true)
    public DashboardTecnicoDTO montarDashboardTecnico(UsuarioAutenticado tecnico) {
        List<Chamado> visiveis = chamadoService.listar(null, null, tecnico);

        List<Chamado> meusAtribuidos = visiveis.stream()
                .filter(c -> estaAtribuidoA(c, tecnico))
                .filter(this::estaEmAberto)
                .toList();

        List<Chamado> filaSetor = visiveis.stream()
                .filter(c -> c.getResponsavel() == null)
                .filter(this::estaEmAberto)
                .filter(c -> pertenceAoSetorDe(c, tecnico))
                .toList();

        long concluidosPorMim = visiveis.stream()
                .filter(c -> estaAtribuidoA(c, tecnico))
                .filter(c -> c.getStatus() == Status.CONCLUIDO)
                .count();

        return new DashboardTecnicoDTO(meusAtribuidos, filaSetor, concluidosPorMim);
    }

    @Transactional(readOnly = true)
    public DashboardSolicitanteDTO montarDashboardSolicitante(UsuarioAutenticado solicitante) {
        List<Chamado> meusChamados = chamadoService.listar(null, null, solicitante);

        List<Chamado> emAberto = meusChamados.stream().filter(this::estaEmAberto).toList();
        List<Chamado> encerrados = meusChamados.stream().filter(c -> !estaEmAberto(c)).toList();

        long aguardandoAcao = emAberto.stream()
                .filter(c -> c.getStatus() == Status.AGUARDANDO_SOLICITANTE)
                .count();

        return new DashboardSolicitanteDTO(emAberto, encerrados, aguardandoAcao);
    }

    private long somar(List<IndicadorDepartamento> indicadores, ToLongFunction<IndicadorDepartamento> campo) {
        return indicadores.stream().mapToLong(campo).sum();
    }

    private boolean estaEmAberto(Chamado chamado) {
        return !STATUS_FINAIS.contains(chamado.getStatus());
    }

    private boolean estaAtribuidoA(Chamado chamado, UsuarioAutenticado usuario) {
        return chamado.getResponsavel() != null
                && chamado.getResponsavel().getId().equals(usuario.getId());
    }

    private boolean pertenceAoSetorDe(Chamado chamado, UsuarioAutenticado usuario) {
        return usuario.getDepartamentoId() != null
                && chamado.getCategoria().getDepartamento().getId().equals(usuario.getDepartamentoId());
    }
}
