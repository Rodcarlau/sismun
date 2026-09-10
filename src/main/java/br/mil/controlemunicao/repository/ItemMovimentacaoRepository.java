package br.mil.controlemunicao.repository;

import br.mil.controlemunicao.entity.ItemMovimentacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface ItemMovimentacaoRepository extends JpaRepository<ItemMovimentacao, Long> {
    @EntityGraph(attributePaths={"loteMunicao","loteMunicao.municao","loteMunicao.municao.tipoMunicao","movimentacao"}) java.util.List<ItemMovimentacao> findByMovimentacaoId(Long movimentacaoId);
}
