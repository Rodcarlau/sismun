package br.mil.controlemunicao.controller;

import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/formularios")
public class FormularioController {

    public record Formulario(String titulo, String arquivo) {}

    private static final List<Formulario> FORMULARIOS = List.of(
        new Formulario("Apanho de Munição", "modelo_apanho_de_municao.docx"),
        new Formulario("Guarda de munição", "modelo_guarda_de_municao.docx"),
        new Formulario("Extensão da guarda de munição", "modelo_extensao_guarda_de_municao.docx"),
        new Formulario("Apoio de materiais para escolta armada", "modelo_apoio_materiais_escolta_armada.docx"),
        new Formulario("Devolução de cartuchos de munições", "modelo_devolucao_cartuchos_de_municoes.docx")
    );

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("formularios", FORMULARIOS);
        return "formularios/lista";
    }

    @GetMapping("/{arquivo:.+}")
    public ResponseEntity<Resource> baixarModelo(@PathVariable String arquivo) {
        if (FORMULARIOS.stream().noneMatch(formulario -> formulario.arquivo().equals(arquivo))) {
            return ResponseEntity.notFound().build();
        }
        Resource modelo = new ClassPathResource("documentos/" + arquivo);
        if (!modelo.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(arquivo).build().toString())
            .body(modelo);
    }
}