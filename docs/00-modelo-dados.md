# Modelo de dados — propostas interdisciplinares

O domínio da API da Fatec Antonio Brambilla usa **MongoDB com Spring Data MongoDB** a partir do [tutorial 03](03-mongodb-persistencia.md). A [etapa JPA](02-jpa-persistencia.md) e sua branch preservam o modelo relacional anterior para comparação. O mapeamento atual é objeto-documento (ODM).

## Regras do domínio

- Uma proposta representa uma ideia de trabalho interdisciplinar, com um autor definido pelo backend.
- Categoria, cursos e públicos-alvo são catálogos controlados. Uma proposta exige uma categoria e ao menos um curso e um público-alvo ativos.
- Os endpoints de catálogo retornam todos os itens ativos. Itens desativados continuam referenciados nas propostas antigas.
- A listagem retorna dez propostas por página; `page` começa em zero. A consulta pública só mostra propostas publicadas, mesmo quando solicitado `status=ARQUIVADA`.
- `DELETE` arquiva a proposta, preservando histórico e curtidas.
- Cada usuário pode curtir uma proposta uma única vez. A quantidade é calculada pelo servidor.
- A etapa 05 introduzirá autenticação, autorização por autoria e perfis `USER` e `ADMIN`. Até lá, o backend usa o usuário demonstrativo; o cliente não envia `autorId`.
- Rascunhos, comentários e anexos pertencem ao backlog após o primeiro ciclo.

## Documentos e tipos

Todos os documentos têm `id: String` em Java e na API, representando um ObjectId de 24 caracteres hexadecimais. No MongoDB, o campo `_id` é BSON ObjectId. Referências manuais são strings canônicas em minúsculas, não objetos `DBRef` nem chaves estrangeiras.

| Classe / coleção | Campos além do ID | Regras |
|---|---|---|
| `Proposta` / `propostas` | `titulo`, `resumo`, `descricao`, `status`, `autorId`, `categoriaId`, `cursoIds`, `publicoAlvoIds`, `criadaEm`, `atualizadaEm` | Título de 5–120 caracteres; resumo até 300; descrição até 10.000; textos obrigatórios. Status `PUBLICADA` ou `ARQUIVADA`. Conjuntos de IDs não vazios. |
| `Usuario` / `usuarios` | `nome`, `email`, `senhaHash`, `perfil`, `ativo`, `criadoEm` | Email único, normalizado para minúsculas antes de gravar. Perfil padrão `USER`. Nunca retornar hash nos DTOs. |
| `Categoria` / `categorias` | `nome`, `descricao`, `ativa` | Nome único; ativa por padrão. |
| `Curso` / `cursos` | `nome`, `sigla`, `ativo` | Nome único; sigla opcional e única quando informada; ativo por padrão. |
| `PublicoAlvo` / `publicos_alvo` | `nome`, `descricao`, `ativo` | Nome único; ativo por padrão. |
| `Curtida` / `curtidas` | `usuarioId`, `propostaId`, `criadaEm` | Par usuário/proposta único; ID próprio substitui a chave composta JPA. |

Datas usam `Instant` em Java e BSON Date no banco (precisão de milissegundos). `@CreatedDate` e `@LastModifiedDate`, com `@EnableMongoAuditing`, preenchem as datas nas gravações pelos repositórios. A carga inicial usa `setOnInsert` e fornece a data do usuário explicitamente.

Na futura manutenção de usuários/catálogos, conservar os limites de domínio anteriores: nome de usuário/curso até 120, email até 180, nome de categoria até 80, nome de público até 100, descrições de catálogo até 300 e sigla até 20 caracteres. Esses endpoints ainda não existem; os limites não são restrições de coluna MongoDB. Hoje os DTOs de proposta validam o contrato HTTP e os serviços validam as referências.

## Referências e agregados

Uma proposta armazena `autorId`, `categoriaId` e arrays `cursoIds`/`publicoAlvoIds`. Não há tabelas de ligação. Nomes dos catálogos não são copiados para cada proposta, evitando sincronização de cópias; o nome do autor é consultado ao montar a resposta.

Curtidas ficam em coleção separada porque podem crescer sem limite definido. Embutir todas em uma proposta aumentaria o documento e a contenção de escrita. O índice único composto garante a unicidade sob concorrência; apenas consultar antes de inserir não seria suficiente.

MongoDB não verifica existência de referências nem faz cascatas. Os serviços validam catálogos ao criar/editar. A gravação de cada documento é atômica; não há transação entre coleções neste ambiente standalone. As futuras operações administrativas precisarão respeitar essa fronteira — preferir desativar registros a apagar referências usadas.

Exemplo de documento, em representação JSON estendida:

```json
{
  "_id": {"$oid": "000000000004000000000001"},
  "titulo": "Horta inteligente para a comunidade",
  "resumo": "Monitoramento de uma horta comunitária.",
  "descricao": "Integração de tecnologia e sustentabilidade.",
  "status": "PUBLICADA",
  "autorId": "000000000000000000000001",
  "categoriaId": "000000000001000000000001",
  "cursoIds": ["000000000002000000000001"],
  "publicoAlvoIds": ["000000000003000000000001"],
  "criadaEm": {"$date": "2026-10-24T12:00:00Z"},
  "atualizadaEm": {"$date": "2026-10-24T12:00:00Z"}
}
```

O campo interno `_class` também pode ser gravado pelo conversor Spring Data; ele não é parte do contrato HTTP.

## Índices e consultas

- `_id` é único em todas as coleções.
- `usuarios.email` e os nomes de categorias, cursos e públicos-alvo são únicos.
- `cursos.sigla` tem índice único parcial para valores do tipo string; vários cursos podem omitir a sigla.
- `curtidas(usuarioId, propostaId)` é único; `curtidas.propostaId` tem índice para contagem.
- `propostas(status, criadaEm desc, _id desc)` apoia a listagem pública ordenada. ID desempata datas iguais.

A aplicação cria esses índices na inicialização. A busca `q` procura texto literal, ignorando caixa, em título, resumo e descrição. Ela não oferece busca sem acentos nem um mecanismo de busca textual indexado. Os filtros opcionais são combinados por AND. Índices adicionais dependem de medir consultas reais.

## Contrato HTTP

| Método e rota | Operação | Acesso previsto na etapa 05 |
|---|---|---|
| `GET /api/propostas` | Lista com `q`, `categoriaId`, `publicoAlvoId`, `cursoId`, `status`, `page` | Público, publicadas |
| `GET /api/propostas/{id}` | Consulta proposta publicada | Público |
| `POST /api/propostas` | Cria proposta | USER/ADMIN |
| `PUT /api/propostas/{id}` | Atualiza campos editáveis | Autor/ADMIN |
| `DELETE /api/propostas/{id}` | Arquiva | Autor/ADMIN |
| `PUT /api/propostas/{id}/curtida` | Curtida idempotente | USER/ADMIN |
| `DELETE /api/propostas/{id}/curtida` | Remove curtida idempotentemente | USER/ADMIN |
| `GET /api/categorias` | Categorias ativas | Público |
| `GET /api/cursos` | Cursos ativos | Público |
| `GET /api/publicos-alvo` | Públicos ativos | Público |

Criação e atualização recebem:

```json
{
  "titulo": "Horta inteligente para a comunidade",
  "resumo": "Sistema acessível para monitorar uma horta comunitária.",
  "descricao": "Proposta interdisciplinar para apoiar o cultivo local.",
  "publicoAlvoIds": ["000000000003000000000001"],
  "categoriaId": "000000000001000000000001",
  "cursoIds": ["000000000002000000000001"]
}
```

Criação retorna `201` com `Location`; consultas e atualizações retornam `200`; arquivamento, `204`. IDs malformados e dados inválidos retornam `400`; proposta ausente ou indisponível retorna `404`. A etapa de segurança acrescentará `401`/`403`.

A resposta de proposta inclui IDs como strings, `autorNome`, `quantidadeCurtidas`, `curtidaPeloUsuarioAtual` e datas; nunca senha. A página mantém `conteudo`, `pagina`, `tamanho`, `totalElementos` e `totalPaginas`.

## Arquitetura

O fluxo continua Controller → Service → Repository. `MongoTemplate` implementa os filtros dinâmicos que antes usavam Specification JPA. Não há necessidade de introduzir um padrão GoF nesta troca de persistência.
