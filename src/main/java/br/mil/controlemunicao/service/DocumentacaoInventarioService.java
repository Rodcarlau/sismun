package br.mil.controlemunicao.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.PluralAttribute;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.*;

@Service
public class DocumentacaoInventarioService {
    public record Rota(String verbo, String caminho, String controller, String metodoJava) {}
    public record Relacao(String origem, String campo, String destino, String tipo) {}

    private final RequestMappingHandlerMapping mapping;
    private final EntityManager entityManager;

    public DocumentacaoInventarioService(RequestMappingHandlerMapping mapping, EntityManager entityManager) {
        this.mapping = mapping;
        this.entityManager = entityManager;
    }

    public List<Rota> rotas() {
        List<Rota> resultado = new ArrayList<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : mapping.getHandlerMethods().entrySet()) {
            HandlerMethod handler = entry.getValue();
            if (!handler.getBeanType().getPackageName().equals("br.mil.controlemunicao.controller")) continue;
            if (entry.getKey().getPathPatternsCondition() == null) continue;
            Set<RequestMethod> verbos = entry.getKey().getMethodsCondition().getMethods();
            for (String caminho : entry.getKey().getPathPatternsCondition().getPatternValues()) {
                if (verbos.isEmpty()) resultado.add(new Rota("ANY", caminho, handler.getBeanType().getSimpleName(), handler.getMethod().getName()));
                else for (RequestMethod verbo : verbos) resultado.add(new Rota(verbo.name(), caminho, handler.getBeanType().getSimpleName(), handler.getMethod().getName()));
            }
        }
        resultado.sort(Comparator.comparing(Rota::caminho).thenComparing(Rota::verbo));
        return resultado;
    }

    public List<Relacao> relacoes() {
        List<Relacao> resultado = new ArrayList<>();
        for (EntityType<?> entidade : entityManager.getMetamodel().getEntities()) {
            if (!entidade.getJavaType().getPackageName().equals("br.mil.controlemunicao.entity")) continue;
            for (Attribute<?, ?> atributo : entidade.getAttributes()) {
                if (!atributo.isAssociation()) continue;
                Class<?> destino = atributo.isCollection() ? ((PluralAttribute<?, ?, ?>) atributo).getElementType().getJavaType() : atributo.getJavaType();
                resultado.add(new Relacao(entidade.getName(), atributo.getName(), destino.getSimpleName(), atributo.getPersistentAttributeType().name()));
            }
        }
        resultado.sort(Comparator.comparing(Relacao::origem).thenComparing(Relacao::campo));
        return resultado;
    }
}
