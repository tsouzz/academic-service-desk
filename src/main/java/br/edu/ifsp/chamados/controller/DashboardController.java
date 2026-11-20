package br.edu.ifsp.chamados.controller;

import br.edu.ifsp.chamados.dto.DashboardGestorDTO;
import br.edu.ifsp.chamados.dto.DashboardSolicitanteDTO;
import br.edu.ifsp.chamados.dto.DashboardTecnicoDTO;
import br.edu.ifsp.chamados.model.Papel;
import br.edu.ifsp.chamados.security.UsuarioAutenticado;
import br.edu.ifsp.chamados.service.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UsuarioAutenticado usuarioLogado, Model model) {
        if (usuarioLogado.getPapel() == Papel.GESTOR) {
            return dashboardGestor(model);
        }
        if (usuarioLogado.getPapel() == Papel.TECNICO) {
            return dashboardTecnico(usuarioLogado, model);
        }
        return dashboardSolicitante(usuarioLogado, model);
    }

    private String dashboardGestor(Model model) {
        DashboardGestorDTO dados = dashboardService.montarDashboardGestor();
        model.addAttribute("indicadores", dados.indicadores());
        model.addAttribute("totalGeral", dados.totalGeral());
        model.addAttribute("abertosGeral", dados.abertosGeral());
        model.addAttribute("emAndamentoGeral", dados.emAndamentoGeral());
        model.addAttribute("concluidosGeral", dados.concluidosGeral());
        model.addAttribute("canceladosGeral", dados.canceladosGeral());
        return "dashboard/gestor";
    }

    private String dashboardTecnico(UsuarioAutenticado tecnico, Model model) {
        DashboardTecnicoDTO dados = dashboardService.montarDashboardTecnico(tecnico);
        model.addAttribute("meusAtribuidos", dados.meusAtribuidos());
        model.addAttribute("filaSetor", dados.filaSetor());
        model.addAttribute("concluidosPorMim", dados.concluidosPorMim());
        return "dashboard/tecnico";
    }

    private String dashboardSolicitante(UsuarioAutenticado solicitante, Model model) {
        DashboardSolicitanteDTO dados = dashboardService.montarDashboardSolicitante(solicitante);
        model.addAttribute("emAberto", dados.emAberto());
        model.addAttribute("encerrados", dados.encerrados());
        model.addAttribute("aguardandoAcao", dados.aguardandoAcao());
        return "dashboard/solicitante";
    }
}
