# Tutorial 3 — De JPA para MongoDB: mapeamento objeto-documento

## Missão e ponto de partida

Migrar a API da [etapa JPA](02-jpa-persistencia.md) para MongoDB, preservando regras de propostas e curtidas. Esta etapa vem **antes de [Swagger](04-openapi-swagger.md) e [Spring Security](05-seguranca.md)** para que ambos trabalhem com o contrato definitivo de IDs em string.

A branch de origem é `feature/02-jpa-persistence-impl`; o resultado está em `feature/03-mongodb-persistence`. O H2 era descartável: não vamos importar registros SQL. Removemos Flyway e suas migrations, iniciando um banco novo com os mesmos catálogos demonstrativos.

Ao concluir, você deverá explicar como um documento difere de uma linha relacional, justificar referências versus documentos embutidos e demonstrar persistência e unicidade com testes reais.

## 1. Entenda a mudança de modelo

| Antes: JPA/ORM | Agora: Spring Data MongoDB/ODM |
|---|---|
| Tabelas e linhas | Coleções e documentos BSON |
| `@Entity`, `@Table` | `@Document` |
| `@GeneratedValue`, `Long` | `@Id`, ObjectId exposto como `String` |
| `@ManyToOne`, `@ManyToMany`, FKs | IDs e conjuntos de IDs com validação no serviço |
| Tabelas de junção | Arrays dentro de `Proposta` |
| Chave composta `CurtidaId` | ID próprio e índice composto único |
| `JpaRepository`, Specification | `MongoRepository`, `MongoTemplate` e Criteria |
| Dirty checking, `flush`, transações JPA | `save`/`insert` explícitos e atomicidade por documento |
| `@PrePersist`, `@PreUpdate` | Auditoria Spring Data |
| Flyway SQL | Inicialização idempotente e índices MongoDB |

MongoDB não é um ORM relacional. Chamamos o novo mapeamento de **ODM**, objeto-documento. A API continua MVC, com DTOs e serviços. Repository não é um padrão GoF e a migração não exige novos padrões de projeto.

Leia o [modelo atual](00-modelo-dados.md). As classes permanecem em `domain.entity` para facilitar a comparação, mas agora representam documentos. `Usuario`, `Categoria`, `Curso` e `PublicoAlvo` são coleções independentes. Propostas guardam suas referências por ID; curtidas ficam separadas para evitar um array de crescimento ilimitado. Não usamos `DBRef`, cascata ou carregamento LAZY.

## 2. Prepare as dependências

Use JDK 17 e Docker com Compose. A partir de uma cópia limpa da etapa JPA, crie uma branch para seu exercício. Não altere a branch que preserva a aula anterior.

No `build.gradle`, remova os starters JPA, Flyway e H2 Console, `flyway-mysql`, os drivers H2/MySQL e os starters de testes JPA/Flyway. Mantenha as dependências Web MVC, Validation e Lombok. Acrescente:

```gradle
implementation 'org.springframework.boot:spring-boot-starter-data-mongodb'
testImplementation 'org.testcontainers:testcontainers-mongodb'
testImplementation 'org.testcontainers:testcontainers-junit-jupiter'
```

As versões vêm do gerenciamento de dependências do Spring Boot do projeto. Mantenha `useJUnitPlatform()`. Não instale Gradle globalmente.

Remova `application-mysql.yaml` e `src/main/resources/db/migration/*.sql`. Essa remoção se aplica a este exercício com banco novo; em uma aplicação com dados reais, seria necessário planejar importação e validação antes do corte.

## 3. Suba o banco com Docker Compose

Crie `compose.yaml` na raiz:

```yaml
services:
  mongodb:
    image: mongo:7.0
    ports:
      - "127.0.0.1:27017:27017"
    volumes:
      - mongodb_data:/data/db
    healthcheck:
      test: ["CMD", "mongosh", "--quiet", "--eval", "db.adminCommand('ping').ok"]
      interval: 5s
      timeout: 5s
      retries: 20
      start_period: 10s
volumes:
  mongodb_data:
```

Usamos MongoDB 7.0 tanto no Compose quanto nos testes. Na validação, a imagem 8.0 não iniciou com o kernel do Docker local, devido à incompatibilidade descrita nas [notas oficiais do MongoDB 8.0](https://www.mongodb.com/docs/manual/release-notes/8.0/). Uma atualização futura deve validar o kernel e os testes antes de trocar a versão; não reaproveite diretamente volumes de uma versão mais nova em uma versão anterior.

O volume conserva os dados após recriar o container. O healthcheck verifica se o banco aceita comandos. A porta fica limitada ao localhost; este ambiente é didático, sem autenticação, e não é uma configuração de produção. A aplicação Java roda no host, fora do Compose.

```bash
docker compose config
docker compose up -d --wait
docker compose ps
docker compose exec mongodb mongosh piapi --eval 'db.runCommand({ping: 1})'
```

Se a porta estiver ocupada, ajuste a porta do host no Compose e também `MONGODB_URI`. Use `docker compose logs mongodb` para diagnosticar falhas.

## 4. Configure conexão, índices e auditoria

Substitua `application.yaml` por:

```yaml
spring:
  application:
    name: PIApi
  mongodb:
    uri: ${MONGODB_URI:mongodb://localhost:27017/piapi}
  data:
    mongodb:
      auto-index-creation: true
app:
  dados-demonstrativos: ${DADOS_DEMONSTRATIVOS:true}
```

Neste Spring Boot 4, a conexão usa **`spring.mongodb.uri`**, enquanto a criação de índices continua em `spring.data.mongodb.auto-index-creation`. Inclua o nome do banco na URI. Não mantenha `spring.datasource`, `spring.jpa`, H2 Console ou Flyway.

Na classe `Main`, acrescente `@EnableMongoAuditing`, importada de `org.springframework.data.mongodb.config`. Isso habilita `@CreatedDate` e `@LastModifiedDate`. MongoDB armazena datas com precisão de milissegundos; não espere recuperar nanossegundos.

Os índices declarados nas classes precisam existir no banco para garantir unicidade. A opção de configuração permite que Spring Data os crie na inicialização. Mudanças futuras de índices em bancos populados precisam de planejamento próprio; a anotação não resolve duplicações antigas automaticamente.

## 5. Converta os documentos e repositórios

Remova os imports e anotações `jakarta.persistence` de todas as entidades e remova `CurtidaId`. Não basta trocar `@Entity` por `@Document`: remodele as referências e substitua os callbacks JPA.

Abaixo estão os dois documentos com relações. As classes completas, inclusive catálogos e usuário, estão em [`domain/entity`](../src/main/java/br/com/fatecararas/piapi/domain/entity).

### Proposta

```java
package br.com.fatecararas.piapi.domain.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import br.com.fatecararas.piapi.domain.StatusProposta;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;

// Uma proposta é um documento; não existem tabelas de junção nem carregamento LAZY.
@Document("propostas")
@CompoundIndex(name = "status_data", def = "{'status': 1, 'criadaEm': -1, '_id': -1}")
@Getter
@Setter
@NoArgsConstructor
public class Proposta {
    @Id
    private String id;
    private String titulo;
    private String resumo;
    private String descricao;
    private StatusProposta status = StatusProposta.PUBLICADA;
    // Referências manuais armazenadas como strings, sem FK ou cascata automática.
    // O serviço valida os IDs e consulta o autor para construir o DTO.
    private String autorId;
    private String categoriaId;
    // Arrays de IDs substituem as tabelas associativas. Set evita repetições.
    private Set<String> cursoIds = new HashSet<>();
    private Set<String> publicoAlvoIds = new HashSet<>();
    // Auditoria substitui @PrePersist/@PreUpdate; alterações exigem save explícito.
    @CreatedDate
    private Instant criadaEm;
    @LastModifiedDate
    private Instant atualizadaEm;
}
```

### Curtida

```java
package br.com.fatecararas.piapi.domain.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;

// Coleção separada evita um array de curtidas sem limite dentro da proposta.
@Document("curtidas")
@CompoundIndex(name = "usuario_proposta_unicos", def = "{'usuarioId': 1, 'propostaId': 1}", unique = true)
@Getter
@Setter
@NoArgsConstructor
public class Curtida {
    @Id
    private String id;
    // O índice composto garante a regra mesmo sob requisições concorrentes.
    private String usuarioId;
    @Indexed
    private String propostaId;
    @CreatedDate
    private Instant criadaEm;

    public Curtida(String usuarioId, String propostaId) {
        this.usuarioId = usuarioId;
        this.propostaId = propostaId;
    }
}
```

### Usuário e catálogos

Para `Usuario`, mantenha nome, email, senhaHash, perfil e ativo; use `@Document("usuarios")`, `@Id String id`, `@Indexed(unique = true)` no email e `@CreatedDate` em `criadoEm`. A futura etapa de cadastro deve normalizar email para minúsculas e produzir um hash real. Nunca exponha `senhaHash` na resposta.

Para `Categoria`, `Curso` e `PublicoAlvo`, use as coleções `categorias`, `cursos` e `publicos_alvo`; mantenha os campos de nome, descrição/sigla e situação ativa. Todos usam ID string e nome com índice único. No campo `sigla` de `Curso`, use:

```java
@Indexed(unique = true, partialFilter = "{ 'sigla': { '$type': 'string' } }")
private String sigla;
```

O índice parcial permite vários documentos sem sigla, mas impede repetir uma sigla string. Não use apenas um índice único simples em um campo opcional. Remover `@Column` também remove restrições de tamanho/nulabilidade daquela modelagem: os DTOs e serviços precisam continuar validando entradas. As futuras rotas administrativas devem acrescentar seus próprios DTOs de validação.

### Repositórios

Troque `JpaRepository<T, Long>` por `MongoRepository<T, String>`, importando `org.springframework.data.mongodb.repository.MongoRepository`. Mantenha as consultas derivadas por nome e situação ativa dos catálogos, com parâmetros string. Remova `JpaSpecificationExecutor` de `PropostaRepository`.

`CurtidaRepository` passa a ser:

```java
public interface CurtidaRepository extends MongoRepository<Curtida, String> {
    long countByPropostaId(String propostaId);
    boolean existsByUsuarioIdAndPropostaId(String usuarioId, String propostaId);
    void deleteByUsuarioIdAndPropostaId(String usuarioId, String propostaId);
}
```

Os nomes referem-se aos campos diretos do documento, não a caminhos da antiga chave embutida.

## 6. Adapte serviços e contrato HTTP

Consulte os arquivos finais [`PropostaService`](../src/main/java/br/com/fatecararas/piapi/domain/PropostaService.java), [`CatalogoService`](../src/main/java/br/com/fatecararas/piapi/domain/CatalogoService.java) e [`Ids`](../src/main/java/br/com/fatecararas/piapi/domain/Ids.java) ao reproduzir esta etapa.

1. Troque todos os IDs `Long` por `String` nos DTOs, parâmetros de rota, filtros e serviços. Arrays passam a `List<String>` nos DTOs; o documento usa `Set<String>` para evitar duplicações. Mantenha as validações de texto, campos obrigatórios e coleções não vazias.
2. Antes de consultar IDs externos, use `ObjectId.isValid` e normalize com `new ObjectId(id).toHexString()`. `Ids.normalizar` centraliza esse comportamento e lança `RequisicaoInvalidaException` (`400`). IDs com formato correto e proposta ausente retornam `404`.
3. Valide catálogos ativos no serviço. Grave os IDs validados em `categoriaId`, `cursoIds` e `publicoAlvoIds`; grave o ID do autor demonstrativo no backend. Não aceite autor no corpo.
4. Remova `@Transactional` JPA e `flush`. Na atualização e no arquivamento, execute `propostas.save(proposta)`; sem isso, mudar um objeto Java não altera o banco.
5. Use `MongoTemplate` para filtros dinâmicos: monte `Criteria`, conte antes de aplicar paginação e use `PageRequest.of(page, 10, Sort.by(DESC, "criadaEm", "id"))`. Combine filtros com AND e mantenha a restrição pública a `PUBLICADA`.
6. Para `q`, use `Pattern.quote(q.trim())` com busca sem distinção de caixa em título, resumo e descrição. Isso faz `.*` ser texto literal, em vez de uma expressão regular fornecida pelo cliente.
7. Monte o DTO a partir dos IDs e consulte `UsuarioRepository` para obter `autorNome`. Continue calculando curtidas no servidor. O hash nunca aparece no DTO.

Na curtida, execute `insert(new Curtida(usuarioId, propostaId))`. Se houver `DuplicateKeyException`, confira se o par já existe: nesse caso a operação repetida é sucesso; caso contrário, propague a falha. Um `exists` seguido de `save` sozinho não garante unicidade em requisições simultâneas. Na remoção, exclua pelo par, sem apagar usuário ou proposta.

O ambiente standalone não oferece transações entre esses documentos. A validação de referência seguida da escrita não é uma transação distribuída; a etapa administrativa futura deverá considerar concorrência com desativação/remoção de catálogos. Contagem e listagem também são leituras separadas.

## 7. Recrie somente os dados demonstrativos ausentes

Implemente [`DadosIniciais`](../src/main/java/br/com/fatecararas/piapi/config/DadosIniciais.java) como `ApplicationRunner` condicional à propriedade `app.dados-demonstrativos`. Ele substitui o seed SQL, não um sistema completo de migrations.

Use IDs fixos para que os exemplos sejam reproduzíveis:

| Registro | ID |
|---|---|
| Usuário de demonstração | `000000000000000000000001` |
| Tecnologia e inovação | `000000000001000000000001` |
| Desenvolvimento de Software Multiplataforma | `000000000002000000000001` |
| Comunidade local | `000000000003000000000001` |

Os demais itens mantêm os catálogos da etapa JPA; o sufixo identifica cada item. Converta a entidade para `Document` usando o conversor Spring Data, retire `_id` e faça `upsert` por esse ID usando **`setOnInsert`** para os demais campos. Assim, reiniciar não duplica registros nem reativa/renomeia os que foram editados. Forneça explicitamente `criadoEm` no seed, pois esse update não passa pelos callbacks de `save`.

Mantenha o marcador `PENDENTE-ETAPA-SEGURANCA` no usuário demonstrativo, sem tratá-lo como senha. `DADOS_DEMONSTRATIVOS=false` desativa o runner, mas a API ainda precisa de um usuário com o ID demonstrativo para criar/curtir até a autenticação ser implementada.

## 8. Execute e teste como cliente

```bash
./gradlew bootRun
```

Em outro terminal, consulte `GET /api/categorias`, `GET /api/cursos` e `GET /api/publicos-alvo`. Importe a coleção atualizada de [`insomnia/propostas-insomnia.json`](../insomnia/propostas-insomnia.json). Crie uma proposta com o corpo do [modelo de dados](00-modelo-dados.md) e copie o `id` retornado para a variável `proposta_id` no Insomnia. Não use mais IDs numéricos como `1`.

Verifique: criação `201` com `Location`; detalhe e atualização `200`; curtidas repetidas permanecem em uma; arquivamento `204`; consulta pública posterior `404`. IDs malformados, página negativa e referências inválidas/inativas retornam `400`.

Inspecione o banco:

```bash
docker compose exec mongodb mongosh piapi --eval 'db.propostas.find().toArray()'
docker compose exec mongodb mongosh piapi --eval 'db.curtidas.getIndexes()'
```

Pare a API, reinicie o banco e execute a API novamente. A proposta e seu estado devem permanecer:

```bash
docker compose restart mongodb
./gradlew bootRun
```

Para encerrar sem perder os dados, use `docker compose down`. **Somente para apagar o banco local e reiniciar o exercício**, use `docker compose down -v`; esse comando remove o volume e todos os dados. Na próxima inicialização da API, os catálogos serão recriados.

## 9. Verifique a integração com MongoDB real

Substitua `PersistenciaJpaTests` e o teste de contexto sem banco por [`PersistenciaMongoTests`](../src/test/java/br/com/fatecararas/piapi/PersistenciaMongoTests.java). O teste usa `@SpringBootTest`, `@AutoConfigureMockMvc` e um `MongoDBContainer("mongo:7.0")`, sem depender do MongoDB do Compose.

Registre `spring.mongodb.uri` com `MONGO::getConnectionString` e `spring.mongodb.database` com `piapi_test` usando `@DynamicPropertySource`. Essa URI do Testcontainers não inclui o banco; por isso o nome é definido separadamente. As portas são dinâmicas e os containers são descartados ao final. Não habilite execução silenciosamente sem Docker: os testes devem falhar se a infraestrutura necessária estiver ausente.

```bash
./gradlew test
```

A suíte verifica documentos e referências após nova leitura, auditoria, atualização/arquivamento, filtros combinados, paginação, busca literal, catálogos inválidos/inativos, IDs HTTP, índices únicos, siglas opcionais, seed repetido e curtidas concorrentes. A limpeza entre casos usa `deleteAll`, conservando índices. Não use rollback de `@DataJpaTest` como estratégia de isolamento MongoDB.

## Desafio

Desative um curso usado por uma proposta. Demonstre que ele desaparece do catálogo de seleção, que a proposta antiga preserva sua referência e que uma nova proposta não pode selecioná-lo. Explique por que o banco não faz essa validação sozinho. Depois justifique por que curtidas não foram embutidas na proposta.

## Pronto quando

- A API usa somente MongoDB e não depende de JPA, SQL, H2 ou Flyway.
- Documentos comentados explicam IDs, referências, índices e auditoria.
- DTOs e coleção Insomnia usam strings para todos os IDs.
- Reinicialização preserva dados; o seed não sobrescreve alterações.
- Os testes reais passam, inclusive unicidade de curtidas concorrentes.
- Você consegue seguir para [Swagger](04-openapi-swagger.md) e depois [Security](05-seguranca.md) com o novo contrato.

## Referências

- [Configuração MongoDB no Spring Boot](https://docs.spring.io/spring-boot/reference/data/nosql.html)
- [Mapeamento no Spring Data MongoDB](https://docs.spring.io/spring-data/mongodb/reference/mongodb/mapping/mapping.html)
- [Módulo MongoDB do Testcontainers](https://java.testcontainers.org/modules/databases/mongodb/)
