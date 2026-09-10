package br.mil.controlemunicao.repository;

import br.mil.controlemunicao.entity.MovimentacaoEstoque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Long> {
    java.util.List<MovimentacaoEstoque> findByMovimentacaoId(Long movimentacaoId);
    @Modifying @Query("update MovimentacaoEstoque h set h.itemMovimentacao=null where h.itemMovimentacao.id=:itemId") void desvincularItem(Long itemId);
    @Modifying @Query("delete from MovimentacaoEstoque h where h.movimentacao.id=:movimentacaoId") void deleteByMovimentacaoId(Long movimentacaoId);
}
