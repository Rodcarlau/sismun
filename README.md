# Controle de Munição

Sistema web para controle de munições, lotes, paióis, estoques, movimentações e devoluções em ambiente militar.

## Tecnologias
- Java 17+
- Spring Boot 3.3.x
- Spring MVC
- Spring Data JPA
- Hibernate
- Thymeleaf
- Spring Security
- H2 Database
- Maven

## Requisitos
- JDK 17 ou superior
- Maven 3.8+

## Executar

### Linux/macOS
mvn clean test
mvn spring-boot:run

### Windows
mvn clean test
mvn spring-boot:run

## Acesso
- Aplicação: http://localhost:8080
- H2 Console: http://localhost:8080/h2-console

## Credenciais de desenvolvimento
- Usuário: admin
- Senha: admin123

## Estrutura
- `controller/` — páginas MVC e fluxo web
- `service/` — regras de negócio e transações
- `repository/` — acesso ao banco
- `entity/` — entidades JPA
- `config/` — configuração e inicialização
- `exception/` — tratamento de erros

## Banco
O projeto usa H2 em desenvolvimento e está preparado para evoluir para PostgreSQL sem grandes mudanças de código.
