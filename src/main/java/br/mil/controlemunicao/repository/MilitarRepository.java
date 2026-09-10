package br.mil.controlemunicao.repository;

import br.mil.controlemunicao.entity.Militar;
import br.mil.controlemunicao.entity.FuncaoMilitar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface MilitarRepository extends JpaRepository<Militar, Long> {
 boolean existsByIdentidadeAndIdNot(String identidade, Long id);
 @Override @EntityGraph(attributePaths="organizacaoMilitar") List<Militar> findAll();
 @Override @EntityGraph(attributePaths="organizacaoMilitar") Optional<Militar> findById(Long id);
 @EntityGraph(attributePaths="organizacaoMilitar") List<Militar> findByAtivoTrueAndFuncaoOrderByNomeCompleto(FuncaoMilitar funcao);
 @EntityGraph(attributePaths="organizacaoMilitar") List<Militar> findByAtivoTrueAndFuncaoInOrderByNomeCompleto(Collection<FuncaoMilitar> funcoes);
 @EntityGraph(attributePaths="organizacaoMilitar") List<Militar> findByAtivoTrueAndOrganizacaoMilitarIdOrderByNomeCompleto(Long organizacaoId);
 @EntityGraph(attributePaths="organizacaoMilitar") List<Militar> findByAtivoTrueAndOrganizacaoMilitarIdAndFuncaoInOrderByNomeCompleto(Long organizacaoId, Collection<FuncaoMilitar> funcoes);
}
