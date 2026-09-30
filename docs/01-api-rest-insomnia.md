# Tutorial 1 — Contrato REST e Insomnia

## Missão

Definir como clientes vão conversar com a API de propostas da Fatec Antonio Brambilla. Ao terminar, você terá importado e explorado uma coleção Insomnia que registra as operações previstas. Nesta etapa, a coleção é o contrato de trabalho; os endpoints serão implementados na etapa JPA.

## Antes de começar

- JDK 17 instalado.
- Insomnia instalado.
- Repositório clonado e terminal aberto na raiz.

## 1. Conheça o contrato atual do código

Abra `src/main/java/br/com/fatecararas/piapi/resources/ProjetosResource.java`. Ele demonstra quatro mapeamentos vazios em `/api/projetos`. Esse esqueleto é um ponto de partida para estudar verbos e controllers; o domínio definido para a API é proposta interdisciplinar, por isso o contrato planejado usa `/api/propostas`.

## 2. Importe a coleção

No Insomnia, use **Import** e selecione `insomnia/propostas-insomnia.json`. A coleção inclui listagem e filtros, detalhe, criação, atualização, remoção lógica e curtidas. As chamadas são o contrato a implementar nas próximas etapas e podem retornar 404 enquanto os endpoints ainda não existirem.

## 3. Leia o controller

`@RestController` registra um controller que escreve respostas HTTP; `@RequestMapping` define o prefixo da rota; cada anotação `@GetMapping`, `@PostMapping`, `@PutMapping` ou `@DeleteMapping` associa um verbo à operação. Na implementação, uma proposta será tratada como recurso próprio, identificado por `{id}`.

| Operação | Verbo | Rota | Uso esperado |
|---|---|---|---|
| Listar | GET | `/api/propostas` | Obter página de propostas; aceita filtros |
| Consultar uma | GET | `/api/propostas/{id}` | Obter uma proposta |
| Criar | POST | `/api/propostas` | Enviar uma nova proposta |
| Atualizar | PUT | `/api/propostas/{id}` | Atualizar os campos editáveis |
| Arquivar | DELETE | `/api/propostas/{id}` | Retirar da listagem pública sem apagar o histórico |
| Curtir | PUT | `/api/propostas/{id}/curtida` | Registrar uma curtida do usuário autenticado |
| Remover curtida | DELETE | `/api/propostas/{id}/curtida` | Desfazer a própria curtida |

Os filtros iniciais são `q`, `categoriaId`, `publicoAlvoId`, `cursoId`, `status` e `page`. `q` busca título, resumo e descrição. Cada página contém 10 propostas. Públicos-alvo são selecionados de uma lista controlada. A definição de atributos, relações e regras está em [00-modelo-dados.md](00-modelo-dados.md). O conteúdo de criação e atualização usa esse modelo e a coleção como referência.

## O caminho de uma requisição

```mermaid
sequenceDiagram
    actor Pessoa
    participant I as Insomnia
    participant S as Spring MVC
    participant C as ProjetosResource
    Pessoa->>I: Executa método e rota
    I->>S: Requisição HTTP
    S->>C: Seleciona o mapeamento
    C-->>S: ResponseEntity
    S-->>I: Status e corpo HTTP
    I-->>Pessoa: Exibe a resposta
```

## 4. Experimente e registre

Escolha três chamadas da coleção e, antes de implementá-las, anote método, caminho, parâmetros, JSON de entrada e resposta de sucesso esperada. Observe que públicos-alvo são escolhidos por ID no catálogo e que cada página da consulta contém 10 propostas. Discuta por que curtida usa um recurso próprio e por que excluir uma proposta significa arquivá-la.

## Desafio

Revise o contrato com outra dupla. Confirme se os nomes dos campos são claros, se é possível descobrir as propostas por categoria, conteúdo, público-alvo e curso, e se os status HTTP esperados estão registrados. Leve as decisões para a etapa de implementação.

## Pronto quando

- A coleção importa no Insomnia.
- O grupo consegue explicar verbo HTTP, caminho, identificador e filtros.
- O modelo e os exemplos de JSON refletem a Fatec Antonio Brambilla.
- A turma registra dúvidas sobre validações e códigos de resposta para resolver na implementação.
