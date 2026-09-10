package br.mil.controlemunicao.repository;

import br.mil.controlemunicao.entity.OrganizacaoMilitar;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizacaoMilitarRepository extends JpaRepository<OrganizacaoMilitar, Long> {
    boolean existsByCodigo(String codigo);
}
