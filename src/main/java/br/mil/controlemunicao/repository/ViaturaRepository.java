package br.mil.controlemunicao.repository;

import br.mil.controlemunicao.entity.Viatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;

public interface ViaturaRepository extends JpaRepository<Viatura, Long> {
 @Override @EntityGraph(attributePaths="organizacaoMilitar") List<Viatura> findAll();
}
