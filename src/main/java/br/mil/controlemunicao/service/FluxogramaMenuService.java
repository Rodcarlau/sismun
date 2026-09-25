package br.mil.controlemunicao.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FluxogramaMenuService {
    public record Menu(String id, String nome, String rota) {}
    public record Passo(String id, String raia, int coluna, String texto, String detalhe, String forma) {}
    public record Ligacao(String origem, String destino, String rotulo) {}
    public record Fluxo(Menu menu, List<String> raias, List<Passo> passos, List<Ligacao> ligacoes, String fonte) {}

    private static final Pattern LINK = Pattern.compile("<a\\b[^>]*th:href=\"@\\{(/[^}]*)}\"[^>]*>(.*?)</a>", Pattern.DOTALL);

    public List<Menu> menus() {
        try {
            String html = new String(new ClassPathResource("templates/fragments/sidebar.html").getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            Matcher matcher = LINK.matcher(html);
            List<Menu> menus = new ArrayList<>();
            Set<String> vistos = new HashSet<>();
            while (matcher.find()) {
                String rota = matcher.group(1);
                if (!vistos.add(rota)) continue;
                String nome = matcher.group(2).replaceAll("<[^>]+>", "").trim();
                if (nome.isBlank()) continue;
                String id = rota.substring(1);
                menus.add(new Menu(id, nome, rota));
            }
            return List.copyOf(menus);
        } catch (IOException e) { throw new IllegalStateException("Não foi possível ler o menu atual", e); }
    }

    public Fluxo fluxo(String id) {
        Menu menu = menus().stream().filter(m -> m.id().equals(id)).findFirst().orElseThrow(() -> new NoSuchElementException("Menu não encontrado"));
        Criador b = new Criador(menu);
        switch (id) {
            case "dashboard" -> b
                .p("u", "Usuário", 0, "Abrir Dashboard", "GET /dashboard", "usuario")
                .p("c", "Controller", 1, "dashboard()", "DashboardController", "processo")
                .p("r", "Repository", 2, "Consultar totais", "Municao, Paiol, EstoquePaiol e MovimentacaoRepository", "processo")
                .p("v", "Interface", 3, "Exibir indicadores", "Estoque físico, reservado, disponível e status", "processo")
                .sequencia("u", "c", "r", "v");
            case "organizacoes" -> cadastro(b, "OrganizacaoMilitarController", "OrganizacaoMilitarRepository", "organização", "@Valid; código numérico gerado no cadastro", "organizacao/form", "POST /organizacoes");
            case "municoes" -> b
                .p("u", "Usuário", 0, "Cadastrar ou editar", "Munições", "usuario")
                .p("i", "Interface", 1, "municao/form", "Tipo, calibre e demais campos", "processo")
                .p("c", "Controller", 2, "POST /municoes", "MunicaoController.salvar", "processo")
                .p("d", "Regra de negócio", 3, "@Valid aprovou?", "BindingResult", "decisao")
                .p("e", "Interface", 4, "Retornar formulário", "Erros de validação", "processo")
                .p("r", "Repository", 4, "Salvar munição", "MunicaoRepository.save", "processo")
                .p("v", "Interface", 5, "Listar munições", "POST /municoes/inativar/{id} alterna ativo", "processo")
                .sequencia("u", "i", "c", "d").l("d", "e", "Não").l("d", "r", "Sim").l("r", "v", "");
            case "lotes" -> cadastro(b, "LoteController", "LoteMunicaoRepository", "lote e virola", "Municao e OM proprietária selecionadas; sem validação explícita no controller", "lote/form", "POST /lotes");
            case "paiols" -> b
                .p("u", "Usuário", 0, "Cadastrar ou editar", "Paiol", "usuario")
                .p("i", "Interface", 1, "paiol/form", "OM, principal e OM detentora", "processo")
                .p("c", "Controller", 2, "POST /paiols", "PaiolController.salvar", "processo")
                .p("d", "Regra de negócio", 3, "Principal válido?", "Exige OM detentora e unicidade; @Valid", "decisao")
                .p("e", "Interface", 4, "Exibir erro", "Retorna paiol/form", "processo")
                .p("r", "Repository", 4, "Salvar paiol", "PaiolRepository.save; gera código P-XX no cadastro", "processo")
                .p("v", "Interface", 5, "Listar paióis", "redirect:/paiols", "processo")
                .sequencia("u", "i", "c", "d").l("d", "e", "Não").l("d", "r", "Sim").l("r", "v", "");
            case "militares" -> b
                .p("u", "Usuário", 0, "Cadastrar ou editar", "Militar", "usuario")
                .p("i", "Interface", 1, "militar/form", "Nome, identidade, posto, função e OM", "processo")
                .p("c", "Controller", 2, "POST /militares", "MilitarController.salvar @Transactional", "processo")
                .p("d", "Regra de negócio", 3, "Dados válidos?", "Obrigatórios, identidade única e OM existente", "decisao")
                .p("e", "Interface", 4, "Exibir erro", "Retorna militar/form", "processo")
                .p("r", "Repository", 4, "Salvar militar", "MilitarRepository.save", "processo")
                .p("v", "Interface", 5, "Listar militares", "Agrupados por OM", "processo")
                .sequencia("u", "i", "c", "d").l("d", "e", "Não").l("d", "r", "Sim").l("r", "v", "");
            case "viaturas" -> cadastro(b, "ViaturaController", "ViaturaRepository", "viatura", "Tipo e OM selecionados; sem validação explícita no controller", "viatura/form", "POST /viaturas");
            case "estoque" -> estoque(b);
            case "movimentacoes" -> movimentacoes(b);
            case "devolucoes" -> devolucoes(b);
            case "formularios" -> b
                .p("u", "Usuário", 0, "Escolher modelo", "Tela formularios/lista", "usuario")
                .p("c", "Controller", 1, "GET /formularios/{arquivo}", "FormularioController.baixarModelo", "processo")
                .p("d", "Regra de negócio", 2, "Modelo permitido?", "Lista fixa de arquivos DOCX", "decisao")
                .p("x", "Interface", 3, "404", "Arquivo não listado ou ausente", "processo")
                .p("f", "Documento", 3, "Ler DOCX", "classpath:documentos", "documento")
                .p("v", "Interface", 4, "Baixar modelo", "Content-Disposition: attachment", "processo")
                .sequencia("u", "c", "d").l("d", "x", "Não").l("d", "f", "Sim").l("f", "v", "");
            case "usuarios" -> b
                .p("u", "Usuário", 0, "Cadastrar / editar", "usuario/form", "usuario")
                .p("c", "Controller", 1, "POST /usuarios", "UsuarioController.salvar", "processo")
                .p("d", "Regra de negócio", 2, "Senha informada?", "Nova conta exige senha; edição preserva senha vazia", "decisao")
                .p("e", "Interface", 3, "Erro", "Senha ausente em novo usuário", "processo")
                .p("p", "Processamento interno", 3, "Codificar senha", "BCrypt quando informada", "automatico")
                .p("r", "Repository", 4, "Salvar usuário", "UsuarioRepository.save; status alternável em /{id}/status", "processo")
                .p("v", "Interface", 5, "Listar usuários", "redirect:/usuarios", "processo")
                .sequencia("u", "c", "d").l("d", "e", "Não / novo").l("d", "p", "Sim ou edição").sequencia("p", "r", "v");
            case "auditoria" -> b
                .p("u", "Usuário", 0, "Abrir Auditoria", "GET /auditoria", "usuario")
                .p("c", "Controller", 1, "listar()", "AuditoriaController", "processo")
                .p("r", "Repository", 2, "Consultar registros", "RegistroAuditoriaRepository; dataHora desc", "processo")
                .p("v", "Interface", 3, "Exibir lista", "auditoria/lista", "processo")
                .sequencia("u", "c", "r", "v");
            case "documentacao" -> b
                .p("u", "Usuário", 0, "Abrir documentação", "GET /documentacao", "usuario")
                .p("c", "Controller", 1, "Montar portal", "DocumentacaoController", "processo")
                .p("a", "Processamento interno", 2, "Ler inventários", "Rotas Spring MVC e metamodelo JPA", "automatico")
                .p("v", "Interface", 3, "Exibir portal", "documentacao/portal", "processo")
                .sequencia("u", "c", "a", "v");
            default -> b.p("u", "Usuário", 0, "Abrir menu", menu.rota(), "usuario")
                .p("x", "Processamento interno", 1, "Fluxo não identificado automaticamente", "Sem controller MVC correspondente ou ferramenta externa", "processo")
                .l("u", "x", "");
        }
        return b.build();
    }

    private void cadastro(Criador b, String controller, String repository, String objeto, String validacao, String tela, String endpoint) {
        b.p("u", "Usuário", 0, "Cadastrar ou editar", objeto, "usuario")
            .p("i", "Interface", 1, tela, "Formulário Thymeleaf", "processo")
            .p("c", "Controller", 2, endpoint, controller + ".salvar", "processo")
            .p("d", "Regra de negócio", 3, "Validar dados", validacao, "processo")
            .p("r", "Repository", 4, "Persistir", repository + ".save", "processo")
            .p("v", "Interface", 5, "Atualizar lista", "Redirecionamento ao menu", "processo")
            .sequencia("u", "i", "c", "d", "r", "v");
    }

    private void estoque(Criador b) {
        b.p("u", "Usuário", 0, "Cadastrar / ajustar", "estoque/form", "usuario")
            .p("c", "Controller", 1, "POST /estoque", "EstoqueController.salvar", "processo")
            .p("d", "Regra de negócio", 2, "Quantidade válida?", "Não negativa; ajuste não abaixo da reserva", "decisao")
            .p("x", "Interface", 3, "Bloquear ajuste", "Exceção; sem gravação", "processo")
            .p("s", "Estoque", 3, "Gravar saldo", "EstoqueService.salvarCadastro: novo ou ajuste", "processo")
            .p("h", "Repository", 4, "Persistir estoque", "EstoquePaiolRepository; histórico AJUSTE na edição", "processo")
            .p("r", "Interface", 5, "Exibir estoque", "Atual / reservado / disponível", "processo")
            .p("a", "Processamento interno", 6, "Reserva", "ReservaEstoqueService.reservar: reservado aumenta", "automatico")
            .p("b", "Estoque", 7, "Entrega", "Físico diminui; reserva liberada", "processo")
            .p("t", "Estoque", 8, "Transferência", "Destino aumenta quando paiol destino existe", "processo")
            .p("f", "Estoque", 9, "Consumo / devolução", "Baixa no destino; devolução credita paiol informado", "processo")
            .sequencia("u", "c", "d").l("d", "x", "Não").l("d", "s", "Sim").sequencia("s", "h", "r")
            .l("a", "b", "após solicitação").sequencia("b", "t", "f");
    }

    private void movimentacoes(Criador b) {
        b.p("u", "Usuário", 0, "Criar solicitação", "movimentacao/form; POST /movimentacoes", "usuario")
            .p("c", "Controller", 1, "salvar()", "MovimentacaoController", "processo")
            .p("d", "Regra de negócio", 2, "Dados e estoque válidos?", "DIEx, origem, responsáveis, lote ativo e disponível", "decisao")
            .p("x", "Interface", 3, "Exibir erro", "Solicitação não gravada", "processo")
            .p("r", "Estoque", 3, "Reservar", "ReservaEstoqueService: reservado aumenta", "processo")
            .p("s", "Repository", 4, "Salvar solicitação", "Movimentacao e itens; SOLICITADA", "processo")
            .p("a", "Service", 5, "Autorizar / separar", "AUTORIZADA → SEPARADA", "processo")
            .p("t", "Service", 6, "Despachar", "EM_TRANSPORTE", "processo")
            .p("e", "Usuário", 7, "Informar entrega efetiva", "Lote/virola e quantidade; POST /{id}/acao", "usuario")
            .p("q", "Regra de negócio", 8, "Divergência?", "Lote ou quantidade diferente da separada", "decisao")
            .p("j", "Processamento interno", 9, "Exigir justificativa", "RegistroEntregaEfetiva guarda valores informados", "automatico")
            .p("o", "Estoque", 10, "Baixar lote original", "Entrega usa quantidade original; libera diferença da reserva", "processo")
            .p("p", "Estoque", 11, "Transferir se destino", "Crédito do lote original no paiol destino", "processo")
            .p("v", "Repository", 12, "Criar devolução", "ENTREGUE → DEVOLUCAO_PENDENTE", "processo")
            .p("f", "Interface", 13, "Próxima etapa", "Consumo / devolução; ou cancelamento antes da entrega", "processo")
            .sequencia("u", "c", "d").l("d", "x", "Não").l("d", "r", "Sim")
            .sequencia("r", "s", "a", "t", "e", "q").l("q", "j", "Sim").l("q", "o", "Não")
            .l("j", "o", "justificada").sequencia("o", "p", "v", "f");
    }

    private void devolucoes(Criador b) {
        b.p("u", "Usuário", 0, "Registrar consumo", "devolucao/lista-abas", "usuario")
            .p("c", "Controller", 1, "POST /{id}/consumo", "DevolucaoController", "processo")
            .p("d", "Service", 2, "Saldo válido?", "DevolucaoService; item ainda não processado", "decisao")
            .p("x", "Interface", 3, "Bloquear", "Quantidade inválida ou repetição", "processo")
            .p("s", "Estoque", 3, "Baixar consumo", "No destino, se houver; histórico sem destino apenas documental", "processo")
            .p("r", "Repository", 4, "Gravar consumo", "ItemDevolucao.consumoProcessado", "processo")
            .p("u2", "Usuário", 5, "Registrar devolução", "Paiol destino, quantidade e anexo opcional", "usuario")
            .p("c2", "Controller", 6, "POST /{id}/devolucao", "DevolucaoController", "processo")
            .p("d2", "Service", 7, "Consumo registrado?", "Saldo rastreável e paiol válidos", "decisao")
            .p("s2", "Estoque", 8, "Devolver", "Baixa no destino e crédito no paiol escolhido", "processo")
            .p("r2", "Repository", 9, "Conciliar / persistir", "Devolucao e ItemDevolucao; anexo se enviado", "processo")
            .p("f", "Service", 10, "Todos processados?", "Saldo documental de cada item deve zerar", "decisao")
            .p("z", "Repository", 11, "Finalizar", "Devolucao e Movimentacao FINALIZADA", "processo")
            .p("p", "Interface", 11, "Pendente", "Aguardar itens restantes", "processo")
            .sequencia("u", "c", "d").l("d", "x", "Não").l("d", "s", "Sim")
            .sequencia("s", "r", "u2", "c2", "d2").l("d2", "x", "Não").l("d2", "s2", "Sim")
            .sequencia("s2", "r2", "f").l("f", "z", "Sim").l("f", "p", "Não");
    }

    private static class Criador {
        private final Menu menu;
        private final List<Passo> passos = new ArrayList<>();
        private final List<Ligacao> ligacoes = new ArrayList<>();
        Criador(Menu menu) { this.menu = menu; }
        Criador p(String id, String raia, int coluna, String texto, String detalhe, String forma) {
            passos.add(new Passo(id, raia, coluna, texto, detalhe, forma)); return this;
        }
        Criador l(String origem, String destino, String rotulo) { ligacoes.add(new Ligacao(origem, destino, rotulo)); return this; }
        Criador sequencia(String... ids) { for (int i = 1; i < ids.length; i++) l(ids[i-1], ids[i], ""); return this; }
        Fluxo build() {
            List<String> raias = passos.stream().map(Passo::raia).distinct().toList();
            return new Fluxo(menu, raias, List.copyOf(passos), List.copyOf(ligacoes), "Controllers, Services, Repositories e templates atuais");
        }
    }
}
