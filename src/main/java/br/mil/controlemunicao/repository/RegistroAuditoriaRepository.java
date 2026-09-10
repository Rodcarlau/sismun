package br.mil.controlemunicao.repository;

import br.mil.controlemunicao.entity.RegistroAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {
}
