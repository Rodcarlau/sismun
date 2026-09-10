package br.mil.controlemunicao.config;

import br.mil.controlemunicao.entity.*;
import br.mil.controlemunicao.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    @ConditionalOnProperty(name="app.seed.enabled", havingValue="true")
    CommandLineRunner initData(
            OrganizacaoMilitarRepository organizacaoMilitarRepository,
            PerfilRepository perfilRepository,
            UsuarioRepository usuarioRepository,
            MunicaoRepository municaoRepository,
            TipoMunicaoRepository tipoMunicaoRepository,
            LoteMunicaoRepository loteMunicaoRepository,
            PaiolRepository paiolRepository,
            EstoquePaiolRepository estoquePaiolRepository,
            MilitarRepository militarRepository,
            ViaturaRepository viaturaRepository,
            MovimentacaoRepository movimentacaoRepository,
            ItemMovimentacaoRepository itemMovimentacaoRepository,
            PasswordEncoder passwordEncoder,
            @Value("${SISMUN_ADMIN_USERNAME:admin}") String adminUsername,
            @Value("${SISMUN_ADMIN_PASSWORD:admin123}") String adminPassword,
            @Value("${SISMUN_ADMIN_EMAIL:admin@exemplo.local}") String adminEmail
    ) {
        return args -> {
            if (perfilRepository.count() == 0) {
                perfilRepository.saveAll(List.of(
                    new Perfil("ADMINISTRADOR", "Acesso completo"),
                    new Perfil("GESTOR", "Cadastros e movimentações"),
                    new Perfil("OPERADOR", "Movimentações permitidas"),
                    new Perfil("CONSULTA", "Somente leitura")
                ));
            }

            if (organizacaoMilitarRepository.count() == 0) {
                OrganizacaoMilitar om = new OrganizacaoMilitar();
                om.setNome("1º Batalhão de Infantaria");
                om.setSigla("1º BI");
                om.setCodigo("001");
                om.setCidade("Rio de Janeiro");
                om.setUf("RJ");
                om.setAtiva(true);
                organizacaoMilitarRepository.save(om);
            }

            if (usuarioRepository.count() == 0) {
                Perfil adminPerfil = perfilRepository.findByNome("ADMINISTRADOR").orElseThrow();
                Usuario admin = new Usuario();
                admin.setNome("Administrador");
                admin.setUsername(adminUsername);
                admin.setEmail(adminEmail);
                admin.setSenha(passwordEncoder.encode(adminPassword));
                admin.setAtivo(true);
                admin.setPerfis(List.of(adminPerfil));
                usuarioRepository.save(admin);
            }

            if (tipoMunicaoRepository.count() == 0) {
                tipoMunicaoRepository.saveAll(List.of(
                    new TipoMunicao("Pistola", "Arma curta"),
                    new TipoMunicao("Fuzil", "Arma longa"),
                    new TipoMunicao("Espingarda", "Arma de uso geral")
                ));
            }

            if (municaoRepository.count() == 0) {
                TipoMunicao pistola = tipoMunicaoRepository.findByNome("Pistola").orElseThrow();
                Municao m1 = new Municao();
                m1.setTipoMunicao(pistola);
                m1.setCalibre("9mm");
                m1.setDescricao("Cartucho 9mm para pistola");
                m1.setFabricante("Fabricante A");
                m1.setAtivo(true);
                municaoRepository.save(m1);

                Municao m2 = new Municao();
                m2.setTipoMunicao(tipoMunicaoRepository.findByNome("Fuzil").orElseThrow());
                m2.setCalibre("7,62x51");
                m2.setDescricao("Cartucho para fuzil");
                m2.setFabricante("Fabricante B");
                m2.setAtivo(true);
                municaoRepository.save(m2);
            }

            if (paiolRepository.count() == 0) {
                OrganizacaoMilitar om = organizacaoMilitarRepository.findAll().get(0);
                Paiol paiol = new Paiol();
                paiol.setNome("Paiol Central");
                paiol.setCodigo("P-01");
                paiol.setOrganizacaoMilitar(om);
                paiol.setEndereco("Galpão 1");
                paiol.setObservacao("Armazena munição de uso operacional");
                paiol.setAtivo(true);
                paiolRepository.save(paiol);
            }

            if (loteMunicaoRepository.count() == 0) {
                Municao municao = municaoRepository.findAll().get(0);
                OrganizacaoMilitar om = organizacaoMilitarRepository.findAll().get(0);
                LoteMunicao lote = new LoteMunicao();
                lote.setMunicao(municao);
                lote.setLote("LOT-2025-001");
                lote.setVirola("VR-0101");
                lote.setDataValidade(LocalDate.now().plusMonths(18));
                lote.setOrganizacaoProprietaria(om);
                lote.setAtivo(true);
                loteMunicaoRepository.save(lote);
            }

            if (estoquePaiolRepository.count() == 0) {
                Paiol paiol = paiolRepository.findAll().get(0);
                LoteMunicao lote = loteMunicaoRepository.findAll().get(0);
                EstoquePaiol estoque = new EstoquePaiol();
                estoque.setPaiol(paiol);
                estoque.setLoteMunicao(lote);
                estoque.setQuantidadeAtual(100);
                estoque.setQuantidadeReservada(0);
                estoque.setDataEntrada(LocalDateTime.now());
                estoque.setUltimaMovimentacao(LocalDateTime.now());
                estoquePaiolRepository.save(estoque);
            }

            if (militarRepository.count() == 0) {
                OrganizacaoMilitar om = organizacaoMilitarRepository.findAll().get(0);
                Militar militar = new Militar();
                militar.setNomeCompleto("Capitão Silva");
                militar.setIdentidade("1234567");
                militar.setPostoGraduacao(PostoGraduacao.CAPITAO);
                militar.setOrganizacaoMilitar(om);
                militar.setTelefone("(21) 99999-1111");
                militar.setEmail("capitao@mil.br");
                militar.setAtivo(true);
                militarRepository.save(militar);
            }

            if (viaturaRepository.count() == 0) {
                OrganizacaoMilitar om = organizacaoMilitarRepository.findAll().get(0);
                Viatura viatura = new Viatura();
                viatura.setTipo(TipoViatura.CAMINHAO);
                viatura.setModelo("Mercedes 2024");
                viatura.setPlaca("ABC-1234");
                viatura.setPrefixo("PV-01");
                viatura.setOrganizacaoMilitar(om);
                viatura.setAtivo(true);
                viaturaRepository.save(viatura);
            }

            if (movimentacaoRepository.count() == 0) {
                OrganizacaoMilitar om = organizacaoMilitarRepository.findAll().get(0);
                Paiol paiol = paiolRepository.findAll().get(0);
                Usuario usuario = usuarioRepository.findByUsername("admin").orElseThrow();
                Militar oficial = militarRepository.findAll().get(0);
                Movimentacao movimentacao = new Movimentacao();
                movimentacao.setDiex("DIEX-2025-001");
                movimentacao.setDataSolicitacao(LocalDate.now());
                movimentacao.setDataApanha(LocalDate.now().plusDays(1));
                movimentacao.setDataExecucaoAtividade(LocalDate.now().plusDays(2));
                movimentacao.setOmLocalApanha(om);
                movimentacao.setOmSolicitante(om);
                movimentacao.setOmDestino(om);
                movimentacao.setPaiolOrigem(paiol);
                movimentacao.setPaiolDestino(paiol);
                movimentacao.setOficialResponsavelPaiol(oficial);
                movimentacao.setOficialResponsavelEntrega(oficial);
                movimentacao.setOficialResponsavelRecebimento(oficial);
                movimentacao.setMotivoRetirada("Movimentação de teste");
                movimentacao.setObservacao("Dados iniciais para navegação");
                movimentacao.setStatus(StatusMovimentacao.RASCUNHO);
                movimentacao.setUsuarioCriador(usuario);
                movimentacao.setUsuarioUltimaAlteracao(usuario);
                movimentacaoRepository.save(movimentacao);

                LoteMunicao lote = loteMunicaoRepository.findAll().get(0);
                ItemMovimentacao item = new ItemMovimentacao();
                item.setMovimentacao(movimentacao);
                item.setLoteMunicao(lote);
                item.setQuantidadeSolicitada(20);
                item.setQuantidadeAutorizada(20);
                item.setQuantidadeEntregue(0);
                item.setQuantidadeRecebida(0);
                item.setObservacao("Item inicial");
                itemMovimentacaoRepository.save(item);
            }
        };
    }
}
