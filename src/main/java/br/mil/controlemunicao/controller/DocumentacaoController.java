package br.mil.controlemunicao.controller;

import br.mil.controlemunicao.service.DocumentacaoPdfService;
import br.mil.controlemunicao.service.DocumentacaoInventarioService;
import br.mil.controlemunicao.service.FluxogramaMenuService;
import br.mil.controlemunicao.service.FluxogramaRenderService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/documentacao")
public class DocumentacaoController {
    private final DocumentacaoPdfService pdfService;
    private final DocumentacaoInventarioService inventario;
    private final FluxogramaMenuService fluxos;
    private final FluxogramaRenderService render;

    public DocumentacaoController(DocumentacaoPdfService pdfService, DocumentacaoInventarioService inventario,
                                  FluxogramaMenuService fluxos, FluxogramaRenderService render) {
        this.pdfService = pdfService;
        this.inventario = inventario;
        this.fluxos = fluxos;
        this.render = render;
    }

    @GetMapping
    public String portal(Model model) {
        model.addAttribute("pageTitle", "Documentação do Sistema | SisMun");
        Map<String, String> menus = new LinkedHashMap<>();
        menus.put("Dashboard", "/dashboard"); menus.put("Organizações Militares", "/organizacoes");
        menus.put("Munições", "/municoes"); menus.put("Lotes", "/lotes"); menus.put("Paiôis", "/paiols");
        menus.put("Militares", "/militares"); menus.put("Viaturas", "/viaturas"); menus.put("Estoque", "/estoque");
        menus.put("Movimentações", "/movimentacoes"); menus.put("Devoluções", "/devolucoes");
        menus.put("Formulários", "/formularios"); menus.put("Usuários", "/usuarios"); menus.put("Auditoria", "/auditoria");
        model.addAttribute("menusDocumentacao", menus);
        model.addAttribute("rotasDocumentacao", inventario.rotas());
        model.addAttribute("relacoesDocumentacao", inventario.relacoes());
        model.addAttribute("menusFluxograma", fluxos.menus());
        return "documentacao/portal";
    }

    @GetMapping("/fluxogramas/{menu}")
    public String visualizarFluxograma(@org.springframework.web.bind.annotation.PathVariable String menu, Model model) {
        model.addAttribute("pageTitle", "Fluxograma | SisMun");
        model.addAttribute("fluxo", fluxos.fluxo(menu));
        return "documentacao/fluxograma";
    }

    @GetMapping(value = "/fluxogramas/{menu}.svg", produces = "image/svg+xml")
    public ResponseEntity<String> svg(@org.springframework.web.bind.annotation.PathVariable String menu) {
        return ResponseEntity.ok().contentType(MediaType.valueOf("image/svg+xml"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=fluxograma_" + menu + ".svg")
            .body(render.svg(menu));
    }

    @GetMapping(value = "/fluxogramas/{menu}.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdfFluxograma(@org.springframework.web.bind.annotation.PathVariable String menu) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=fluxograma_" + menu + ".pdf")
            .body(render.pdf(menu));
    }

    @GetMapping(value = "/fluxogramas.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> todosFluxogramas() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=fluxogramas_sismun.pdf")
            .body(render.pdfTodos());
    }

    @GetMapping(value = "/baixar/{tipo}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> baixar(@org.springframework.web.bind.annotation.PathVariable String tipo) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=SisMun-" + tipo + ".pdf")
            .body(pdfService.gerar(tipo));
    }
}
