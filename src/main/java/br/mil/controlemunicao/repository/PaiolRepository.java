package br.mil.controlemunicao.repository;

import br.mil.controlemunicao.entity.Paiol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;
import java.util.Optional;

public interface PaiolRepository extends JpaRepository<Paiol, Long> {
 @Override @EntityGraph(attributePaths={"organizacaoMilitar","omDetentoraMunicao"}) List<Paiol> findAll();
 @Override @EntityGraph(attributePaths={"organizacaoMilitar","omDetentoraMunicao"}) Optional<Paiol> findById(Long id);
 boolean existsByCodigo(String codigo);
 Optional<Paiol> findByPrincipalTrue();
 boolean existsByPrincipalTrueAndIdNot(Long id);
}
