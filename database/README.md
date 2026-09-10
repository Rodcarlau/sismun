# PostgreSQL do SisMun

## Configuração

- Servidor: `localhost`
- Porta: `5432`
- Banco: `sismun`
- Schema: `public`
- Usuário da aplicação: `sismun_app`

Execute `setup-postgresql.sql` como administrador do PostgreSQL. Substitua o marcador de senha no momento da execução; nunca grave a senha real no script ou no repositório.

```powershell
& "C:\Program Files\PostgreSQL\16\bin\psql.exe" -h localhost -p 5432 -U postgres -d postgres -f database/setup-postgresql.sql
```

Teste a conexão com o usuário exclusivo da aplicação:

```powershell
& "C:\Program Files\PostgreSQL\16\bin\psql.exe" -h localhost -p 5432 -U sismun_app -d sismun
```

No `psql`, confirme com `SELECT current_database(), current_user, current_schema();`.

## Execução da aplicação

Defina as variáveis somente na sessão atual do PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
$env:SISMUN_DB_URL="jdbc:postgresql://localhost:5432/sismun"
$env:SISMUN_DB_USERNAME="sismun_app"
$env:SISMUN_DB_PASSWORD="SUBSTITUIR"
$env:SISMUN_SEED_ENABLED="true"
$env:SISMUN_ADMIN_USERNAME="admin"
$env:SISMUN_ADMIN_PASSWORD="SUBSTITUIR"
$env:SISMUN_ADMIN_EMAIL="admin@exemplo.local"
mvn spring-boot:run
```

O arquivo `.env.example` serve apenas de referência: o Spring Boot não carrega arquivos `.env` automaticamente. Não use `setx` para estes segredos.

O Flyway executa as migrations em `src/main/resources/db/migration`. No PostgreSQL, o Hibernate está configurado com `ddl-auto=validate`: ele valida a correspondência entre entidades e tabelas, mas não cria nem altera a estrutura.

Os testes automatizados usam exclusivamente H2 e o profile `test`:

```powershell
mvn clean test
```

## Backup e restauração

```powershell
& "C:\Program Files\PostgreSQL\16\bin\pg_dump.exe" -h localhost -p 5432 -U sismun_app -d sismun -F c -f sismun_backup.dump
```

Antes de restaurar, faça um backup atual e confirme o banco de destino:

```powershell
& "C:\Program Files\PostgreSQL\16\bin\pg_restore.exe" -h localhost -p 5432 -U sismun_app -d sismun --clean --if-exists sismun_backup.dump
```

Não execute a restauração sem planejamento. A opção `--clean` remove objetos existentes no banco de destino antes de recriá-los.
