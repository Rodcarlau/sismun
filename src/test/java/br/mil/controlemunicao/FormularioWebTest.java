package br.mil.controlemunicao;

import java.io.ByteArrayInputStream;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class FormularioWebTest {
    private static final String DOWNLOAD = "/formularios/modelo_apanho_de_municao.docx";
    private static final String W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";

    @Autowired
    private MockMvc mvc;

    @Test
    void exigeLoginParaPaginaEDownload() throws Exception {
        for (String route : new String[] {"/formularios", DOWNLOAD}) {
            mvc.perform(get(route)).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        }
    }

    @Test
    void paginaUsaLayoutExistenteEOfereceModelo() throws Exception {
        mvc.perform(get("/formularios").with(user("operador").roles("OPERADOR")))
            .andExpect(status().isOk())
            .andExpect(view().name("layout/base"))
            .andExpect(content().string(containsString("Modelo DOCX em branco para preenchimento manual.")))
            .andExpect(content().string(containsString(DOWNLOAD)));
    }

    @Test
    void downloadEntregaSempreOArquivoEstaticoSemDadosDoUsuario() throws Exception {
        byte[] esperado = new ClassPathResource("documentos/modelo_apanho_de_municao.docx")
            .getContentAsByteArray();
        for (String role : new String[] {"ADMINISTRADOR", "GESTOR", "OPERADOR"}) {
            mvc.perform(get(DOWNLOAD).with(user("usuario-" + role).roles(role)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .andExpect(header().string("Content-Disposition",
                    "attachment; filename=\"modelo_apanho_de_municao.docx\""))
                .andExpect(content().bytes(esperado));
        }
    }

    @Test
    void modeloEditavelTemCincoLinhasVaziasEImagem() throws Exception {
        byte[] bytes = new ClassPathResource("documentos/modelo_apanho_de_municao.docx")
            .getContentAsByteArray();
        boolean documentoEncontrado = false;
        boolean imagemEncontrada = false;
        try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (entry.getName().startsWith("word/media/")) imagemEncontrada = true;
                if (!entry.getName().equals("word/document.xml")) continue;
                documentoEncontrado = true;
                var factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                var document = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(zip.readAllBytes()));
                var rows = document.getElementsByTagNameNS(W, "tr");
                assertEquals(6, rows.getLength());
                for (int i = 1; i < rows.getLength(); i++) {
                    assertTrue(rows.item(i).getTextContent().isBlank());
                }
                String text = document.getDocumentElement().getTextContent();
                assertTrue(text.contains("[DIEx]"));
                assertTrue(text.contains("[EB]"));
                assertFalse(text.contains("duas mil quatrocentas"));
                assertFalse(text.contains("7,62mm"));
                assertFalse(text.contains("FEV 26"));
                assertFalse(text.contains("2519"));
            }
        }
        assertTrue(documentoEncontrado);
        assertTrue(imagemEncontrada);
    }
    private static final String[] NOVOS_MODELOS = {
        "modelo_guarda_de_municao.docx",
        "modelo_extensao_guarda_de_municao.docx",
        "modelo_apoio_materiais_escolta_armada.docx",
        "modelo_devolucao_cartuchos_de_municoes.docx"
    };

    @Test
    void novosModelosEstaoNaPaginaEExigemLoginParaDownload() throws Exception {
        var pagina = mvc.perform(get("/formularios").with(user("operador").roles("OPERADOR")))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        for (String nome : NOVOS_MODELOS) {
            String rota = "/formularios/" + nome;
            assertTrue(pagina.contains(rota), nome);
            mvc.perform(get(rota)).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
            byte[] esperado = new ClassPathResource("documentos/" + nome).getContentAsByteArray();
            for (String role : new String[] {"ADMINISTRADOR", "GESTOR", "OPERADOR"}) {
                mvc.perform(get(rota).with(user("usuario-" + role).roles(role)))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", "attachment; filename=\"" + nome + "\""))
                    .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    .andExpect(content().bytes(esperado));
            }
        }
    }

    @Test
    void arquivosForaDoCatalogoNaoSaoDisponibilizados() throws Exception {
        for (String nome : new String[] {"inexistente.docx", "application.yml"}) {
            mvc.perform(get("/formularios/" + nome).with(user("operador").roles("OPERADOR")))
                .andExpect(status().isNotFound());
        }
    }

    @Test
    void novosModelosContemCamposManuaisSemDadosDoExemplo() throws Exception {
        for (String nome : NOVOS_MODELOS) {
            byte[] bytes = new ClassPathResource("documentos/" + nome).getContentAsByteArray();
            boolean documentoEncontrado = false;
            try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
                for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                    if (!entry.getName().equals("word/document.xml")) continue;
                    documentoEncontrado = true;
                    var factory = DocumentBuilderFactory.newInstance();
                    factory.setNamespaceAware(true);
                    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                    var document = factory.newDocumentBuilder()
                        .parse(new ByteArrayInputStream(zip.readAllBytes()));
                    String text = document.getDocumentElement().getTextContent();
                    assertTrue(text.contains("[DIEx]"), nome);
                    assertTrue(text.contains("[EB]"), nome);
                    for (String antigo : new String[] {
                            "64445.", "LEONARDO FAULHABER", "98528", "96966", "2519",
                            "CMRJ", "1º BG", "0800MAR26", "1000 MAR 26", "7,62", "9mm"}) {
                        assertFalse(text.contains(antigo), nome + ": " + antigo);
                    }
                    if (nome.equals("modelo_apoio_materiais_escolta_armada.docx")) {
                        var rows = document.getElementsByTagNameNS(W, "tr");
                        assertEquals(6, rows.getLength());
                        for (int i = 1; i < rows.getLength(); i++) {
                            assertTrue(rows.item(i).getTextContent().isBlank());
                        }
                    }
                }
            }
            assertTrue(documentoEncontrado, nome);
        }
    }}