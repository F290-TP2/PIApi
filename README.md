# API de propostas interdisciplinares

Repositório didático da disciplina **Técnicas de Programação 2**. A turma vai evoluir uma API REST para cadastrar propostas de trabalhos interdisciplinares da **Fatec Antonio Brambilla**, consultar e filtrar propostas e registrar curtidas. O percurso usa Spring Boot com MVC e introduz padrões GoF quando eles resolvem um problema concreto.

## Roteiro da disciplina

0. [Modelo de dados e contrato do domínio](docs/00-modelo-dados.md) — entidades, atributos, relações e regras propostas.
1. [Primeira API REST e coleção Insomnia](docs/01-api-rest-insomnia.md) — contrato HTTP das propostas e coleção de requisições.
2. [Persistência com JPA e ORM](docs/02-jpa-persistencia.md) — entidades, repositório, serviço, validação e banco.
3. [Documentação com OpenAPI e Swagger UI](docs/03-openapi-swagger.md) — contrato navegável e testes pelo navegador.
4. [Autenticação e autorização](docs/04-seguranca.md) — perfis `ADMIN` e `USER`, rotas públicas e protegidas.
5. [Aplicação Web para propostas](docs/05-aplicacao-web.md) — login e publicação de propostas consumindo a API.

Após o primeiro ciclo, o backlog aprovado inclui rascunhos de propostas, comentários e anexos. Esses itens serão planejados em etapas próprias depois que os cinco tutoriais iniciais estiverem concluídos.

Cada etapa é um tutorial incremental: leia a missão, execute os passos, confira o resultado e conclua o desafio. Os tutoriais registram decisões para que a implementação possa ser feita em sala, sem exigir que todo o conteúdo seja conhecido de antemão.

## Domínio inicial

Uma proposta representa uma ideia de trabalho interdisciplinar da Fatec Antonio Brambilla. O modelo prevê título, resumo, descrição, categoria, públicos-alvo selecionados de uma lista controlada, cursos envolvidos, autor e curtidas. O catálogo apresenta todos os cursos ativos; a listagem retorna 10 propostas por página. Rascunhos, comentários e anexos estão planejados para melhorias futuras, depois do primeiro ciclo.

## Arquitetura e padrões

O Spring MVC organiza a entrada HTTP em controllers, que delegam regras a serviços e acesso a dados a repositórios. A persistência usará JPA. Padrões GoF serão apresentados com motivação e consequência, por exemplo **Strategy** para critérios de busca que variem, **Builder** para objetos de consulta mais complexos e **Observer** apenas se surgir uma necessidade real de notificação. Não se deve criar classes de padrão sem um problema que as justifique.

MVC é a organização usada pela API. MVP e MVVM serão discutidos como alternativas de apresentação: MVP concentra a coordenação de tela em um Presenter; MVVM expõe estado e comandos em um ViewModel. Não são substitutos automáticos para as camadas Controller/Service/Repository do backend.

## Executar

Requisitos: JDK 17 e acesso às dependências Gradle. Na raiz:

```bash
./gradlew bootRun
```

O controller que já existe responde em `http://localhost:8080/api/projetos` e ainda é um esqueleto demonstrativo. A coleção Insomnia registra o contrato futuro em `/api/propostas`; as rotas serão implementadas na etapa de persistência.

## Materiais

- `docs/`: tutoriais por etapa e conceitos de arquitetura.
- `insomnia/`: coleção de requisições para acompanhar o contrato da API.
- `src/`: aplicação Spring Boot em evolução.

As instruções de cada tutorial devem ser atualizadas junto com a implementação correspondente, especialmente quando mudarem rotas, exemplos ou configuração.
