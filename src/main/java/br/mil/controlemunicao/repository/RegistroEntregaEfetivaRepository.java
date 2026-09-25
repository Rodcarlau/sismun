package br.mil.controlemunicao.repository;

import br.mil.controlemunicao.entity.RegistroEntregaEfetiva;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistroEntregaEfetivaRepository extends JpaRepository<RegistroEntregaEfetiva, Long> {
    List<RegistroEntregaEfetiva> findByItemMovimentacaoIdOrderById(Long itemMovimentacaoId);

    @EntityGraph(attributePaths = {"itemMovimentacao", "loteMunicao", "loteMunicao.municao", "loteMunicao.municao.tipoMunicao"})
    List<RegistroEntregaEfetiva> findByItemMovimentacaoMovimentacaoIdOrderById(Long movimentacaoId);
}
