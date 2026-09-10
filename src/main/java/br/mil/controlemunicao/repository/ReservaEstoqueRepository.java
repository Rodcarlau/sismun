package br.mil.controlemunicao.repository;

import br.mil.controlemunicao.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.*;

public interface ReservaEstoqueRepository extends JpaRepository<ReservaEstoque, Long> {
    @EntityGraph(attributePaths={"estoque","estoque.paiol"}) Optional<ReservaEstoque> findByItemMovimentacaoId(Long itemId);
    @EntityGraph(attributePaths={"estoque","estoque.paiol","itemMovimentacao"}) List<ReservaEstoque> findByItemMovimentacaoMovimentacaoId(Long movimentacaoId);
}
