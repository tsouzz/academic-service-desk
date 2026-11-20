# Sistema de Gestão de Chamados Acadêmicos

Projeto final da disciplina **Linguagem de Programação 2** — Bacharelado em Sistemas de Informação, Instituto Federal de Educação, Ciência e Tecnologia de São Paulo (IFSP), Câmpus São Paulo.

**Professor:** Anderson Peixoto

## Integrantes

| Nome |
|---|
| Thúlio Souza |
| Ewerton Monteiro |

## Descrição

Aplicação Spring Boot para registrar e acompanhar chamados acadêmicos (suporte técnico, documentos, matrícula, infraestrutura etc.). O sistema oferece:

- **Interface web** (Thymeleaf) com cadastro, listagem, detalhes, edição, atendimento e exclusão de chamados.
- **API REST** em JSON para o recurso de chamados.
- **Relatório com filtros e exportação** em CSV e PDF.
- **Controle de acesso por perfil** (aluno, professor, técnico e gestor).

## Funcionalidades

### Interface web

- Login por e-mail e senha, com dashboard diferente para cada perfil.
- Cadastro de chamado (título, descrição, categoria e anexos opcionais).
- Listagem de chamados com filtro por status e categoria.
- Consulta dos detalhes, com histórico de alterações e anexos.
- Edição de status, prioridade, responsável e observação de atendimento (técnico e gestor).
- Exclusão de chamado (somente gestor).
- Mensagens de sucesso e erro nas principais operações.
- Painel de indicadores por departamento e encerramento de chamados inativos (somente gestor).
- Controle de concorrência otimista: se dois atendentes editarem o mesmo chamado, o segundo é avisado do conflito.

### Permissões por perfil

| Ação | Aluno / Professor | Técnico | Gestor |
|---|:---:|:---:|:---:|
| Abrir chamado | ✔ | ✔ | ✔ |
| Ver chamados | apenas os próprios | todos | todos |
| Editar / atender chamado | ✘ | ✔ | ✔ |
| Excluir chamado | ✘ | ✘ | ✔ |
| Indicadores | ✘ | ✘ | ✔ |

## Tecnologias

- Java 21
- Spring Boot 4.1.2 (Web MVC, Data JPA/Hibernate, Validation, Security, Actuator)
- Thymeleaf
- PostgreSQL com Flyway (migrations, triggers e rotinas PL/pgSQL)
- OpenPDF (geração de PDF)
- Lombok
- JUnit 5, Mockito e Testcontainers

## Arquitetura

Pacote base: `br.edu.ifsp.chamados`

| Pacote | Responsabilidade |
|---|---|
| `controller` | `ChamadoController`, `DashboardController`, `IndicadorController` e `AuthController` (interface web); `ChamadoRestController` (API REST) |
| `service` | Regras da aplicação: `ChamadoService`, `DashboardService`, `RelatorioService`, `AnexoService`, `RotinasBancoService` |
| `repository` | Acesso a dados com Spring Data JPA |
| `model` | Entidades JPA (`Chamado`, `Usuario`, `Categoria`, `Departamento` etc.) e enums |
| `dto` | Objetos de transferência de dados de entrada e saída |
| `factory` | `ChamadoFactory`: centraliza a criação inicial de um chamado (status, datas e prioridade padrão da categoria) |
| `exception` | Exceções de domínio e tratadores de erro (`GlobalExceptionHandler` para a web e `ApiExceptionHandler` para a API) |
| `security` | Configuração do Spring Security e usuário autenticado |

A interface web e a API REST compartilham o mesmo `ChamadoService`, os mesmos repositórios, entidades e DTOs de entrada, de modo que as regras de negócio não são duplicadas.

## Como executar

### Pré-requisitos

- JDK 21
- PostgreSQL 16 (ou compatível) em execução em `localhost:5432`
- Maven (opcional; o projeto inclui o Maven Wrapper `mvnw`)

### 1. Criar o banco de dados

```sql
CREATE DATABASE chamados;
```

As tabelas, triggers e rotinas são criadas automaticamente pelo Flyway na primeira execução.

### 2. Definir as credenciais do banco

A aplicação lê o usuário e a senha do PostgreSQL das variáveis de ambiente `DB_USERNAME` e `DB_PASSWORD`. O Spring Boot **não** lê automaticamente um arquivo `.env`, então defina as variáveis antes de executar.

Linux / macOS:

```bash
export DB_USERNAME=postgres
export DB_PASSWORD=sua_senha
```

Windows (PowerShell):

```powershell
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "sua_senha"
```

Na IDE, configure as mesmas variáveis na configuração de execução da aplicação.

### 3. Executar com dados de exemplo (recomendado para demonstração)

O perfil `dev` carrega usuários, categorias e chamados de exemplo, em todos os status.

Linux / macOS:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Windows:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

> Sem o perfil `dev` o banco é criado vazio e não existe nenhum usuário para fazer login.

A aplicação sobe em **http://localhost:8080**.

### Usuários de exemplo

A senha de todos os usuários é `senha123`.

| Perfil | E-mail |
|---|---|
| Gestor | `thulio.alves@ifsp.edu.br` |
| Aluno | `ewerton.monteiro@aluno.ifsp.edu.br` |
| Professor | `professor.exemplo@ifsp.edu.br` |
| Técnico (TI) | `dennis.olher@ifsp.edu.br` |
| Técnico (Secretaria) | `lucas.silva@ifsp.edu.br` |
| Técnico (Infraestrutura) | `tecnico.infra@ifsp.edu.br` |

### Anexos

Os arquivos enviados são gravados em `${user.home}/chamados-uploads` (limite de 10 MB por arquivo e 25 MB por requisição).

## API REST

URL base: `http://localhost:8080/api/chamados`

### Autenticação

A API usa **HTTP Basic** com o e-mail e a senha de um usuário cadastrado (por exemplo, `thulio.alves@ifsp.edu.br` / `senha123`). As permissões são as mesmas da interface web.

### Endpoints

| Método | Rota | Descrição | Respostas |
|---|---|---|---|
| GET | `/api/chamados` | Lista os chamados. Aceita `?status=` e `?categoriaId=` | 200, 401 |
| GET | `/api/chamados/{id}` | Consulta um chamado | 200, 401, 403, 404 |
| POST | `/api/chamados` | Cadastra um chamado | 201 (com `Location`), 400, 401, 422 |
| PUT | `/api/chamados/{id}` | Atualiza um chamado | 200, 400, 401, 403, 404, 409, 422 |
| DELETE | `/api/chamados/{id}` | Exclui um chamado (somente gestor) | 204, 401, 403, 404 |

Valores aceitos:

- `status`: `ABERTO`, `EM_ANALISE`, `AGUARDANDO_SOLICITANTE`, `CONCLUIDO`, `CANCELADO`
- `prioridade`: os valores do enum `Prioridade` (por exemplo, `BAIXA`, `MEDIA`, `ALTA`, `URGENTE`)

### Exemplos

**POST** `/api/chamados`

```json
{
  "titulo": "Projetor da sala 3 não liga",
  "descricao": "O projetor não responde ao controle remoto.",
  "categoriaId": 1
}
```

**PUT** `/api/chamados/{id}`

```json
{
  "versao": 0,
  "status": "EM_ANALISE",
  "prioridade": "ALTA",
  "responsavelId": null,
  "observacaoAtendimento": "Chamado em análise pela equipe de TI."
}
```

> O campo `versao` é obrigatório no PUT. Consulte o chamado com GET e envie o valor atual; se ele estiver desatualizado, a API responde **409 Conflict**. Ao concluir ou cancelar, a `observacaoAtendimento` é obrigatória (erro **422**).

**Resposta de erro** (formato padrão):

```json
{
  "timestamp": "2026-11-20T20:15:00",
  "status": 404,
  "erro": "Not Found",
  "mensagem": "Chamado #999 não encontrado.",
  "caminho": "/api/chamados/999",
  "detalhes": []
}
```

### Como testar

O arquivo [`docs/api-chamados.http`](docs/api-chamados.http) contém requisições prontas para GET, POST, PUT e DELETE (incluindo casos de erro). Ele funciona no cliente HTTP do IntelliJ e na extensão REST Client do VS Code. Para usar Postman, Insomnia ou Thunder Client, configure a autenticação **Basic Auth** com um dos usuários de exemplo e envie o corpo como `application/json`.

Exemplo com `curl`:

```bash
curl -u thulio.alves@ifsp.edu.br:senha123 http://localhost:8080/api/chamados
```

## Relatório e exportação de dados

O relatório consulta os chamados com filtros e gera arquivos organizados.

- **Filtros:** status e categoria.
- **Formatos:** CSV (separado por `;`, com BOM UTF-8 para abrir corretamente no Excel) e PDF (tabela em A4 paisagem, com quem emitiu o relatório, data e total de registros).
- **Escopo:** técnicos e gestores exportam todos os chamados; alunos e professores exportam apenas os próprios.

### Como testar

1. Faça login como gestor (`thulio.alves@ifsp.edu.br` / `senha123`).
2. Em **Chamados**, use os filtros de status e categoria para conferir a listagem.
3. No dashboard do gestor, clique em **Exportar CSV** ou **Exportar PDF** para exportar todos os chamados.
4. Para exportar já filtrado, acesse as URLs diretamente (com o usuário logado no navegador):

```
http://localhost:8080/chamados/exportar/csv?status=ABERTO
http://localhost:8080/chamados/exportar/pdf?status=CONCLUIDO&categoriaId=1
```

### Como os dados fluem

1. O `ChamadoController` recebe os filtros (`status`, `categoriaId`) e o usuário logado.
2. O `ChamadoService.listar(...)` monta a consulta com `Specification` do Spring Data JPA, respeitando o perfil do usuário.
3. O `RelatorioService` transforma a lista de chamados em bytes (`gerarCsv` ou `gerarPdf` com OpenPDF).
4. O controller devolve os bytes em um `ResponseEntity` com `Content-Type` e `Content-Disposition: attachment`, e o navegador baixa o arquivo.

## Rotas principais da interface web

| Método | Rota | Descrição |
|---|---|---|
| GET | `/login` | Tela de login |
| GET | `/dashboard` | Dashboard conforme o perfil |
| GET | `/chamados` | Listagem com filtros |
| GET | `/chamados/novo` | Formulário de cadastro |
| POST | `/chamados` | Salva um novo chamado |
| GET | `/chamados/{id}` | Detalhes do chamado |
| GET / POST | `/chamados/{id}/editar` | Edição e atendimento |
| POST | `/chamados/{id}/excluir` | Exclusão (gestor) |
| GET | `/chamados/exportar/csv` | Exportação em CSV |
| GET | `/chamados/exportar/pdf` | Exportação em PDF |
| GET | `/indicadores` | Indicadores por departamento (gestor) |

## Testes automatizados

```bash
./mvnw test
```

Os testes unitários (serviços, `DashboardService`, `ChamadoRestController` e `ApiExceptionHandler`) usam Mockito. O teste de integração do banco usa Testcontainers com `postgres:16-alpine` e **requer o Docker em execução**.

## Banco de dados

- As migrations ficam em `src/main/resources/db/migration` (`V1__schema.sql` e `V2__objetos_plpgsql.sql`).
- Os dados de exemplo ficam em `src/main/resources/db/dev` e só são carregados com o perfil `dev`.
- O banco possui triggers de auditoria (histórico de alterações de status, imutável), preenchimento automático da data de conclusão e funções para indicadores e tempo de resolução.
- O Hibernate é configurado com `ddl-auto: validate`: o esquema é controlado apenas pelo Flyway.

## Refatoração e Clean Code

Exemplo para a apresentação: a lógica de montagem dos dashboards estava dentro do `DashboardController`. Ela foi movida para o `DashboardService`, que devolve DTOs (`DashboardGestorDTO`, `DashboardTecnicoDTO` e `DashboardSolicitanteDTO`). O controller ficou responsável apenas por escolher a view e preencher o `Model`, e a regra passou a ter testes unitários.

## Estrutura do repositório

```
chamados/
├── docs/                 # Requisições de teste da API
├── src/main/java/        # Código-fonte
├── src/main/resources/   # Templates, estáticos, configuração e migrations
├── src/test/java/        # Testes automatizados
└── pom.xml
```
