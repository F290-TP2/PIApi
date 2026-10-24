# Tutorial 2 — ORM com JPA: entidades e repositórios

> Material histórico da etapa JPA. Para reproduzir este código, use a branch `feature/02-jpa-persistence-impl`. A branch MongoDB substitui esta persistência no [tutorial 03](03-mongodb-persistencia.md); os trechos SQL/JPA abaixo são mantidos para comparação.

## Missão e escopo do tutorial

Este tutorial complementa a [primeira API REST](01-api-rest-insomnia.md) e aplica o [modelo de dados](00-modelo-dados.md). O objetivo é reproduzir a **camada de dados** da branch `feature/02-jpa-persistence-impl`: seis entidades, uma chave composta e seis repositórios. Controllers, DTOs, services, autenticação e testes HTTP pertencem a outros momentos do percurso; não serão implementados aqui.

O desenvolvimento desta branch substituiu a representação em memória por entidades JPA, introduziu repositories Spring Data, criou as tabelas e os catálogos com Flyway e configurou H2 e um perfil MySQL. O commit `e853098` contém a implementação de persistência e `c1c913e` acrescenta o gabarito anterior. Os exemplos abaixo acompanham os pacotes atuais da árvore de trabalho, incluindo os enums em `domain.model`. O [gabarito anterior](jpa.md) contém também integração com outras camadas e caminhos antigos; para reproduzir a implementação demonstrada ao vivo, siga este documento.

### Reprodução assíncrona

A sequência preserva o caráter incremental da disciplina: uma API já conhecida fornece o problema, e cada recurso do ORM aparece para resolver uma necessidade observável. JPA é a especificação do mapeamento; Hibernate é o provedor utilizado; Spring Data JPA fornece a implementação dos repositórios; Flyway versiona a estrutura SQL. Repository não é um padrão GoF. Não precisamos introduzir novos padrões nesta etapa.

A implementação foi demonstrada ao vivo. Agora, reproduza-a de forma assíncrona, seguindo os passos deste tutorial no seu ambiente. Execute os exemplos, confira os resultados e use o laboratório e os critérios de conclusão para verificar sua compreensão.

Ao final, o aluno deve conseguir escolher a cardinalidade, localizar a FK, distinguir tabela associativa de entidade associativa, implementar consultas derivadas e demonstrar persistência por teste sem depender da camada HTTP.

## 1. Prepare o ambiente

Use o projeto da etapa anterior, JDK 17, o Gradle Wrapper do repositório e acesso às dependências. Trabalhe na raiz do projeto. Confira `java -version` e `./gradlew --version`; não é necessário instalar Gradle globalmente. Mantenha as versões registradas no projeto.

No `build.gradle`, mantenha Web MVC e Validation e confira as dependências abaixo. Lombok gera métodos e construtores; habilite o processamento de anotações na IDE, se ela não reconhecer os métodos gerados.

```gradle
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
implementation 'org.springframework.boot:spring-boot-starter-flyway'
implementation 'org.flywaydb:flyway-mysql'
implementation 'org.springframework.boot:spring-boot-h2console'
runtimeOnly 'com.h2database:h2'
runtimeOnly 'com.mysql:mysql-connector-j'
compileOnly 'org.projectlombok:lombok'
annotationProcessor 'org.projectlombok:lombok'
testImplementation 'org.springframework.boot:spring-boot-starter-data-jpa-test'
testImplementation 'org.springframework.boot:spring-boot-starter-flyway-test'
```

O projeto já configura JUnit no bloco `test { useJUnitPlatform() }` por meio de `tasks.named('test')`. Sincronize o Gradle. Não duplique dependências existentes.

## 2. Configure o banco e crie o esquema

A configuração local desta branch é:

Arquivo `src/main/resources/application.yaml`:

```yaml
spring:
  application:
    name: PIApi
  datasource:
    url: jdbc:h2:mem:piapi;DB_CLOSE_DELAY=-1
    username: aluno
    password: fatec
    driver-class-name: org.h2.Driver
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: true
  flyway:
    enabled: true
  h2:
    console:
      enabled: true
      path: /h2-console
```

H2 usa memória: os dados desaparecem quando a JVM termina. `DB_CLOSE_DELAY=-1` mantém o banco enquanto a JVM estiver viva, mesmo sem conexões abertas; isso não conserva dados após reiniciar. `aluno` e `fatec` são credenciais locais de exercício.

Flyway cria as tabelas; `ddl-auto: validate` manda Hibernate conferir o mapeamento, sem criar tabelas. Não troque por `create` ou `update` neste roteiro. `format_sql` apenas formata SQL; para exibi-lo, acrescente `show-sql: true` dentro de `spring.jpa`. `open-in-view: false` exige que associações necessárias sejam acessadas dentro de uma transação.

Crie `src/main/resources/db/migration` e os dois arquivos seguintes **com estes nomes**. Se já existem, confira o conteúdo sem duplicá-los. Em banco compartilhado, não edite uma migration já aplicada: crie a próxima versão.

Arquivo `src/main/resources/db/migration/V1__create_schema.sql`:

```sql
CREATE TABLE usuario
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome       VARCHAR(120) NOT NULL,
    email      VARCHAR(180) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    perfil     VARCHAR(20)  NOT NULL,
    ativo      BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em  TIMESTAMP    NOT NULL
);

CREATE TABLE categoria
(
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome      VARCHAR(80) NOT NULL UNIQUE,
    descricao VARCHAR(300),
    ativa     BOOLEAN     NOT NULL DEFAULT TRUE
);

CREATE TABLE curso
(
    id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome  VARCHAR(120) NOT NULL UNIQUE,
    sigla VARCHAR(20) UNIQUE,
    ativo BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE publico_alvo
(
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome      VARCHAR(100) NOT NULL UNIQUE,
    descricao VARCHAR(300),
    ativo     BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE proposta
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    titulo        VARCHAR(120)   NOT NULL,
    resumo        VARCHAR(300)   NOT NULL,
    descricao     VARCHAR(10000) NOT NULL,
    status        VARCHAR(20)    NOT NULL,
    autor_id      BIGINT         NOT NULL,
    categoria_id  BIGINT         NOT NULL,
    criada_em     TIMESTAMP      NOT NULL,
    atualizada_em TIMESTAMP      NOT NULL,
    CONSTRAINT fk_proposta_autor FOREIGN KEY (autor_id) REFERENCES usuario (id),
    CONSTRAINT fk_proposta_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id)
);

CREATE TABLE proposta_curso
(
    proposta_id BIGINT NOT NULL,
    curso_id    BIGINT NOT NULL,
    PRIMARY KEY (proposta_id, curso_id),
    FOREIGN KEY (proposta_id) REFERENCES proposta (id),
    FOREIGN KEY (curso_id) REFERENCES curso (id)
);

CREATE TABLE proposta_publico_alvo
(
    proposta_id     BIGINT NOT NULL,
    publico_alvo_id BIGINT NOT NULL,
    PRIMARY KEY (proposta_id, publico_alvo_id),
    FOREIGN KEY (proposta_id) REFERENCES proposta (id),
    FOREIGN KEY (publico_alvo_id) REFERENCES publico_alvo (id)
);

CREATE TABLE curtida
(
    usuario_id  BIGINT    NOT NULL,
    proposta_id BIGINT    NOT NULL,
    criada_em   TIMESTAMP NOT NULL,
    PRIMARY KEY (usuario_id, proposta_id),
    FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    FOREIGN KEY (proposta_id) REFERENCES proposta (id)
);

CREATE INDEX idx_proposta_status_data ON proposta (status, criada_em);
CREATE INDEX idx_proposta_categoria ON proposta (categoria_id);
```

Arquivo `src/main/resources/db/migration/V2__dados_iniciais.sql`:

```sql
INSERT INTO usuario (id, nome, email, senha_hash, perfil, ativo, criado_em)
VALUES (1, 'Usuário de demonstração', 'demo@fatec.local',
        'PENDENTE-ETAPA-SEGURANCA', 'USER', TRUE, CURRENT_TIMESTAMP);

INSERT INTO categoria (id, nome, descricao)
VALUES (1, 'Tecnologia e inovação', 'Soluções digitais e novos produtos.'),
       (2, 'Sustentabilidade', 'Soluções ambientais e uso responsável de recursos.'),
       (3, 'Empreendedorismo', 'Soluções para negócios e desenvolvimento local.'),
       (4, 'Inclusão e cidadania', 'Soluções para participação social e acessibilidade.');

INSERT INTO curso (id, nome, sigla)
VALUES (1, 'Desenvolvimento de Software Multiplataforma', 'DSM'),
       (2, 'Gestão Empresarial', 'GE'),
       (3, 'Sistemas para Internet', 'SI');

INSERT INTO publico_alvo (id, nome, descricao)
VALUES (1, 'Comunidade local', 'Moradores e organizações da região.'),
       (2, 'Pequenos produtores', 'Pequenos produtores e cooperativas locais.'),
       (3, 'Pequenos negócios', 'Microempreendedores e empresas de pequeno porte.'),
       (4, 'Estudantes', 'Estudantes de escolas e instituições de ensino.');
```

A V1 define PKs, FKs, unicidade e índices. A V2 carrega um usuário e catálogos; não cria propostas. A senha demonstrativa é um marcador para a futura etapa de segurança, não uma implementação de login.

## 3. Leia as cardinalidades antes de escrever Java

| Relação | Regra | Representação atual |
|---|---|---|
| Usuario → Proposta | Um autor pode ter várias propostas; cada proposta tem um autor | `Proposta.autor`: `@ManyToOne`, FK `autor_id` |
| Categoria → Proposta | Uma categoria classifica várias propostas | `Proposta.categoria`: `@ManyToOne`, FK `categoria_id` |
| Proposta ↔ Curso | Várias propostas compartilham vários cursos | `@ManyToMany` e `proposta_curso` |
| Proposta ↔ PublicoAlvo | Várias propostas compartilham vários públicos | `@ManyToMany` e `proposta_publico_alvo` |
| Usuario → Curtida ← Proposta | Cada curtida liga um usuário a uma proposta | Duas relações `@ManyToOne` e PK composta |

**Atenção ao 1:1 discutido em sala:** o estado atual desta branch não contém `@OneToOne` nem `@OneToMany`. Ter uma referência Java a um usuário não torna a relação 1:1: várias propostas podem apontar para o mesmo usuário. O 1:N é representado pelo lado N com `@ManyToOne`; não é obrigatório criar uma coleção no outro lado.

Para comparar, imagine um exercício separado em que cada usuário tenha no máximo um registro de preferências. Na entidade hipotética `PreferenciasUsuario`, o trecho seria:

```java
@OneToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "usuario_id", nullable = false, unique = true)
private Usuario usuario;
```

A tabela hipotética precisaria de `usuario_id` com FK, `NOT NULL` e `UNIQUE`: a unicidade impede duas preferências para o mesmo usuário. Esse exemplo não integra o modelo atual e não deve ser colado em Curtida ou Proposta. Ele não obriga todo usuário a ter preferências.

Também seria possível navegar de Usuario para suas propostas com `@OneToMany(mappedBy = "autor")`. `mappedBy` apontaria para o **atributo Java** `Proposta.autor`, que já controla a FK. Essa navegação inversa não é necessária nesta implementação; numa relação bidirecional, mantenha ambos os lados coerentes na memória.

## 4. Crie os enums e as entidades

Use `src/main/java/br/com/fatecararas/piapi/domain/model` para enums e `domain/entity` para entidades. Esses pacotes ficam abaixo do pacote da classe `Main`, permitindo descoberta automática. Importe `jakarta.persistence`, não `javax.persistence`.

Nos exemplos, `@Entity` registra a entidade; `@Table` escolhe a tabela; `@Id` identifica a PK; `@GeneratedValue(IDENTITY)` usa a identidade gerada pelo banco. `@Column` descreve a coluna. Com `validate`, as restrições precisam estar nas migrations: anotações não substituem SQL nem validação de entrada. `nullable=false` não rejeita texto vazio, `length` não verifica o mínimo de caracteres e relações N:M não exigem ao menos um item por si só.

`@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor` e `@EqualsAndHashCode` são do Lombok, não do JPA. Evite `@Data` nas entidades relacionadas para não gerar comparações e `toString` que percorram relações. Como `@Id` fica no campo, JPA usa acesso aos campos e não exige setters para persistir Curtida.

### 4.1. Valores controlados

Arquivo `src/main/java/br/com/fatecararas/piapi/domain/model/PerfilUsuario.java`:

```java
package br.com.fatecararas.piapi.domain.model;

public enum PerfilUsuario {
    USER,
    ADMIN
}
```

Arquivo `src/main/java/br/com/fatecararas/piapi/domain/model/StatusProposta.java`:

```java
package br.com.fatecararas.piapi.domain.model;

public enum StatusProposta {
    PUBLICADA,
    ARQUIVADA
}
```

### 4.2. Catálogos e usuário

Comece por entidades sem associações. Observe as diferenças entre `ativa` de Categoria e `ativo` das demais; os nomes serão usados nas consultas derivadas.

Arquivo `src/main/java/br/com/fatecararas/piapi/domain/entity/Categoria.java`:

```java
package br.com.fatecararas.piapi.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// @Entity torna a classe uma entidade que o JPA pode persistir.
@Entity
// @Table informa o nome da tabela no banco.
@Table(name = "categoria")
@Getter
@Setter
// Lombok gera o construtor sem argumentos exigido pelo JPA.
@NoArgsConstructor
public class Categoria {

    // @Id identifica a chave primária; IDENTITY deixa o banco gerar seu valor.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @Column: nullable=false impede NULL; unique=true impede valores repetidos; length define o tamanho da coluna de texto.
    @Column(nullable = false, unique = true, length = 80)
    private String nome;

    // @Column: length define o tamanho da coluna de texto.
    @Column(length = 300)
    private String descricao;

    // @Column: nullable=false impede NULL.
    @Column(nullable = false)
    private boolean ativa = true;
}
```

Arquivo `src/main/java/br/com/fatecararas/piapi/domain/entity/Curso.java`:

```java
package br.com.fatecararas.piapi.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// @Entity torna a classe uma entidade que o JPA pode persistir.
@Entity
// @Table informa o nome da tabela no banco.
@Table(name = "curso")
@Getter
@Setter
// Lombok gera o construtor sem argumentos exigido pelo JPA.
@NoArgsConstructor
public class Curso {

    // @Id identifica a chave primária; IDENTITY deixa o banco gerar seu valor.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @Column: nullable=false impede NULL; unique=true impede valores repetidos; length define o tamanho da coluna de texto.
    @Column(nullable = false, unique = true, length = 120)
    private String nome;

    // @Column: unique=true impede valores repetidos; length define o tamanho da coluna de texto.
    @Column(unique = true, length = 20)
    private String sigla;

    // @Column: nullable=false impede NULL.
    @Column(nullable = false)
    private boolean ativo = true;
}
```

Arquivo `src/main/java/br/com/fatecararas/piapi/domain/entity/PublicoAlvo.java`:

```java
package br.com.fatecararas.piapi.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// @Entity torna a classe uma entidade que o JPA pode persistir.
@Entity
// @Table informa o nome da tabela no banco.
@Table(name = "publico_alvo")
@Getter
@Setter
// Lombok gera o construtor sem argumentos exigido pelo JPA.
@NoArgsConstructor
public class PublicoAlvo {

    // @Id identifica a chave primária; IDENTITY deixa o banco gerar seu valor.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @Column: nullable=false impede NULL; unique=true impede valores repetidos; length define o tamanho da coluna de texto.
    @Column(nullable = false, unique = true, length = 100)
    private String nome;

    // @Column: length define o tamanho da coluna de texto.
    @Column(length = 300)
    private String descricao;

    // @Column: nullable=false impede NULL.
    @Column(nullable = false)
    private boolean ativo = true;
}
```

Arquivo `src/main/java/br/com/fatecararas/piapi/domain/entity/Usuario.java`:

```java
package br.com.fatecararas.piapi.domain.entity;

import br.com.fatecararas.piapi.domain.model.PerfilUsuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

// @Entity torna a classe uma entidade que o JPA pode persistir.
@Entity
// @Table informa o nome da tabela no banco.
@Table(name = "usuario")
@Getter
@Setter
// Lombok gera o construtor sem argumentos exigido pelo JPA.
@NoArgsConstructor
public class Usuario {

    // @Id identifica a chave primária; IDENTITY deixa o banco gerar seu valor.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @Column: nullable=false impede NULL; length define o tamanho da coluna de texto.
    @Column(nullable = false, length = 120)
    private String nome;

    // @Column: nullable=false impede NULL; unique=true impede valores repetidos; length define o tamanho da coluna de texto.
    @Column(nullable = false, unique = true, length = 180)
    private String email;

    // @Column: name define o nome da coluna; nullable=false impede NULL.
    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    // Grava o nome do enum (texto), em vez de sua posição numérica.
    @Enumerated(EnumType.STRING)
    // @Column: nullable=false impede NULL; length define o tamanho da coluna de texto.
    @Column(nullable = false, length = 20)
    private PerfilUsuario perfil = PerfilUsuario.USER;

    // @Column: nullable=false impede NULL.
    @Column(nullable = false)
    private boolean ativo = true;

    // @Column: name define o nome da coluna; nullable=false impede NULL.
    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    // O JPA chama este método antes de inserir a entidade.
    @PrePersist
    void antesDeInserir() {
        if (criadoEm == null) {
            criadoEm = Instant.now();
        }
        if (perfil == null) {
            perfil = PerfilUsuario.USER;
        }
    }
}
```

`@Enumerated(STRING)` grava `USER` ou `ADMIN`. `@PrePersist` completa data e perfil antes da inserção via JPA. Esses callbacks não executam em INSERTs SQL manuais; por isso a V2 fornece os valores explicitamente. Normalização de email e codificação de senha não são feitas por essas anotações.

### 4.3. Proposta: N:1 e N:M

Crie primeiro os campos simples e depois as associações. `optional=false` declara a referência obrigatória no modelo; `nullable=false` descreve a coluna obrigatória. `LAZY` solicita carregamento sob demanda e não garante uma quantidade fixa de consultas.

`@JoinTable` nomeia a tabela intermediária. `joinColumns` é a FK da entidade que declara a associação; `inverseJoinColumns` é a FK da entidade de destino. Proposta controla essas duas relações. `Set` evita repetir a mesma instância no conjunto; a PK composta da tabela intermediária garante unicidade no banco. Não confunda o comportamento do conjunto com uma comparação automática de entidades por ID.

Arquivo `src/main/java/br/com/fatecararas/piapi/domain/entity/Proposta.java`:

```java
package br.com.fatecararas.piapi.domain.entity;

import br.com.fatecararas.piapi.domain.model.StatusProposta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

// @Entity torna a classe uma entidade que o JPA pode persistir.
@Entity
// @Table informa o nome da tabela no banco.
@Table(name = "proposta")
@Getter
@Setter
// Lombok gera o construtor sem argumentos exigido pelo JPA.
@NoArgsConstructor
public class Proposta {

    // @Id identifica a chave primária; IDENTITY deixa o banco gerar seu valor.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @Column: nullable=false impede NULL; length define o tamanho da coluna de texto.
    @Column(nullable = false, length = 120)
    private String titulo;

    // @Column: nullable=false impede NULL; length define o tamanho da coluna de texto.
    @Column(nullable = false, length = 300)
    private String resumo;

    // @Column: nullable=false impede NULL; length define o tamanho da coluna de texto.
    @Column(nullable = false, length = 10000)
    private String descricao;

    // Grava o nome do enum (texto), em vez de sua posição numérica.
    @Enumerated(EnumType.STRING)
    // @Column: nullable=false impede NULL; length define o tamanho da coluna de texto.
    @Column(nullable = false, length = 20)
    private StatusProposta status = StatusProposta.PUBLICADA;

    // N:1: várias linhas podem apontar para a mesma entidade. A referência é obrigatória.
    // LAZY solicita carregar a entidade relacionada apenas quando ela for acessada.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    // @JoinColumn indica a coluna de chave estrangeira nesta tabela.
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    // N:1: várias linhas podem apontar para a mesma entidade. A referência é obrigatória.
    // LAZY solicita carregar a entidade relacionada apenas quando ela for acessada.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    // @JoinColumn indica a coluna de chave estrangeira nesta tabela.
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    // N:M: propostas compartilham itens do catálogo por uma tabela de ligação.
    // joinColumns aponta para Proposta; inverseJoinColumns, para o item do catálogo.
    @ManyToMany
    @JoinTable(name = "proposta_curso",
            joinColumns = @JoinColumn(name = "proposta_id"),
            inverseJoinColumns = @JoinColumn(name = "curso_id"))
    private Set<Curso> cursos = new HashSet<>();

    // N:M: propostas compartilham itens do catálogo por uma tabela de ligação.
    // joinColumns aponta para Proposta; inverseJoinColumns, para o item do catálogo.
    @ManyToMany
    @JoinTable(name = "proposta_publico_alvo",
            joinColumns = @JoinColumn(name = "proposta_id"),
            inverseJoinColumns = @JoinColumn(name = "publico_alvo_id"))
    private Set<PublicoAlvo> publicosAlvo = new HashSet<>();

    // @Column: name define o nome da coluna; nullable=false impede NULL; updatable=false exclui a coluna dos UPDATEs do JPA.
    @Column(name = "criada_em", nullable = false, updatable = false)
    private Instant criadaEm;

    // @Column: name define o nome da coluna; nullable=false impede NULL.
    @Column(name = "atualizada_em", nullable = false)
    private Instant atualizadaEm;

    // O JPA chama este método antes de inserir a entidade.
    @PrePersist
    void antesDeInserir() {
        Instant agora = Instant.now();
        criadaEm = agora;
        atualizadaEm = agora;
        if (status == null) {
            status = StatusProposta.PUBLICADA;
        }
    }

    // O JPA chama este método antes de atualizar uma entidade alterada.
    @PreUpdate
    void antesDeAtualizar() {
        atualizadaEm = Instant.now();
    }
}
```

`@ManyToMany` usa LAZY por padrão. Não configuramos cascata: salvar uma proposta não cria automaticamente autor, categoria, cursos ou públicos. Busque registros existentes pelos repositórios antes de associá-los. Não use `CascadeType.REMOVE` para apagar catálogos compartilhados.

`@PreUpdate` atualiza a data quando o JPA atualiza a entidade; alterações apenas em coleções e atualizações SQL/JPQL em massa não devem ser usadas para presumir a execução desse callback. `updatable=false` retira a coluna de criação dos UPDATEs do JPA, sem torná-la imutável no banco.

### 4.4. Curtida: uma associação com dados próprios

O N:M conceitual entre quem curte e o que é curtido vira uma entidade, pois a ligação tem `criadaEm`. Não acrescente outro `@ManyToMany` entre Usuario e Proposta para representar a mesma curtida.

Crie a chave antes da entidade. `CurtidaId` é um valor composto, não uma sétima entidade: não tem tabela nem repository próprio.

Arquivo `src/main/java/br/com/fatecararas/piapi/domain/entity/CurtidaId.java`:

```java
package br.com.fatecararas.piapi.domain.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

// Compõe outra entidade: esta classe representa a chave, sem ter tabela própria.
@Embeddable
@Getter
@Setter
// Lombok gera o construtor sem argumentos exigido pelo JPA.
@NoArgsConstructor
@AllArgsConstructor
// Compara os dois IDs: a mesma dupla representa a mesma chave.
@EqualsAndHashCode
public class CurtidaId implements Serializable {

    private Long usuarioId;
    private Long propostaId;
}
```

Arquivo `src/main/java/br/com/fatecararas/piapi/domain/entity/Curtida.java`:

```java
package br.com.fatecararas.piapi.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

// @Entity torna a classe uma entidade que o JPA pode persistir.
@Entity
// @Table informa o nome da tabela no banco.
@Table(name = "curtida")
@Getter
// Lombok gera o construtor sem argumentos exigido pelo JPA.
@NoArgsConstructor
public class Curtida {

    // A chave primária é o par (usuarioId, propostaId), definido em CurtidaId.
    @EmbeddedId
    private CurtidaId id;

    // Usa o ID da entidade relacionada também no campo usuarioId da chave composta.
    @MapsId("usuarioId")
    // N:1: várias linhas podem apontar para a mesma entidade. A referência é obrigatória.
    // LAZY solicita carregar a entidade relacionada apenas quando ela for acessada.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    // @JoinColumn indica a coluna de chave estrangeira nesta tabela.
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    // Usa o ID da entidade relacionada também no campo propostaId da chave composta.
    @MapsId("propostaId")
    // N:1: várias linhas podem apontar para a mesma entidade. A referência é obrigatória.
    // LAZY solicita carregar a entidade relacionada apenas quando ela for acessada.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    // @JoinColumn indica a coluna de chave estrangeira nesta tabela.
    @JoinColumn(name = "proposta_id")
    private Proposta proposta;

    // @Column: name define o nome da coluna; nullable=false impede NULL.
    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    // Recebe usuário e proposta já persistidos, com IDs definidos.
    public Curtida(Usuario usuario, Proposta proposta) {
        this.id = new CurtidaId(usuario.getId(), proposta.getId());
        this.usuario = usuario;
        this.proposta = proposta;
        this.criadaEm = Instant.now();
    }
}
```

`@EmbeddedId` incorpora a dupla como PK. `@MapsId("usuarioId")` e `@MapsId("propostaId")` conectam cada associação ao atributo correspondente de CurtidaId; esses nomes são atributos Java. As mesmas colunas são PK e FK, sem IDs adicionais. Veja a definição oficial de [MapsId](https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/mapsid).

`Serializable` e o construtor sem argumentos compõem a chave desta implementação. `equals` e `hashCode` gerados comparam ambos os IDs. O construtor de Curtida recebe entidades já persistidas e preenche chave, referências e data. A PK permite `(1,10)` e `(1,11)`, mas não duas linhas `(1,10)`.

## 5. Crie os seis repositórios

Crie as interfaces em `domain/repository`. `JpaRepository<Entidade, TipoDaChave>` já oferece `save`, `findById`, `findAll`, `existsById`, `deleteById` e outras operações. Spring cria a implementação; não escreva uma classe com SQL para cada interface e não é necessário acrescentar `@Repository` aqui.

`save` pode persistir ou mesclar estado conforme a detecção de entidade nova; use o retorno quando precisar da entidade gerenciada. `saveAndFlush` antecipa a sincronização com o banco, sem confirmar a transação. O tipo de ID de Curtida é `CurtidaId`, não `Long`.

Arquivo `src/main/java/br/com/fatecararas/piapi/domain/repository/UsuarioRepository.java`:

```java
package br.com.fatecararas.piapi.domain.repository;

import br.com.fatecararas.piapi.domain.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
}
```

Arquivo `src/main/java/br/com/fatecararas/piapi/domain/repository/CategoriaRepository.java`:

```java
package br.com.fatecararas.piapi.domain.repository;

import br.com.fatecararas.piapi.domain.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByAtivaTrueOrderByNomeAsc();

    Optional<Categoria> findByIdAndAtivaTrue(Long id);
}
```

Arquivo `src/main/java/br/com/fatecararas/piapi/domain/repository/CursoRepository.java`:

```java
package br.com.fatecararas.piapi.domain.repository;

import br.com.fatecararas.piapi.domain.entity.Curso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CursoRepository extends JpaRepository<Curso, Long> {

    List<Curso> findByAtivoTrueOrderByNomeAsc();
}
```

Arquivo `src/main/java/br/com/fatecararas/piapi/domain/repository/PublicoAlvoRepository.java`:

```java
package br.com.fatecararas.piapi.domain.repository;

import br.com.fatecararas.piapi.domain.entity.PublicoAlvo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PublicoAlvoRepository extends JpaRepository<PublicoAlvo, Long> {

    List<PublicoAlvo> findByAtivoTrueOrderByNomeAsc();

    Optional<PublicoAlvo> findByIdAndAtivoTrue(Long id);
}
```

Arquivo `src/main/java/br/com/fatecararas/piapi/domain/repository/PropostaRepository.java`:

```java
package br.com.fatecararas.piapi.domain.repository;

import br.com.fatecararas.piapi.domain.entity.Proposta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PropostaRepository extends JpaRepository<Proposta, Long>,
        JpaSpecificationExecutor<Proposta> {
}
```

Arquivo `src/main/java/br/com/fatecararas/piapi/domain/repository/CurtidaRepository.java`:

```java
package br.com.fatecararas.piapi.domain.repository;

import br.com.fatecararas.piapi.domain.entity.Curtida;
import br.com.fatecararas.piapi.domain.entity.CurtidaId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurtidaRepository extends JpaRepository<Curtida, CurtidaId> {

    long countByIdPropostaId(Long propostaId);
}
```

Leia as consultas pelo nome: `findByAtivaTrueOrderByNomeAsc` busca categorias ativas e ordena pelo nome; `findByIdAndAtivoTrue` busca por ID apenas se o registro estiver ativo. `Optional` representa possível ausência. `countByIdPropostaId` percorre `Curtida.id.propostaId` e conta curtidas da proposta. São **propriedades Java**, não nomes SQL. Consulte [consultas derivadas do Spring Data](https://www.springframework.org/spring-data/data-jpa/reference/repositories/query-methods-details.html).

`JpaSpecificationExecutor<Proposta>` permite critérios compostos de consulta. A interface já está presente; implementar a lógica de filtros fica fora deste laboratório. A paginação é oferecida pelo repository com `PageRequest.of(0, 10)`, em que zero é a primeira página. O repository não impõe sozinho o tamanho de página definido no contrato HTTP.

## 6. Laboratório: provar o ORM sem controller

Crie o teste abaixo em `src/test/java/br/com/fatecararas/piapi/PersistenciaJpaTests.java`. Ele usa apenas a camada de dados, aplica as migrations em um H2 de teste e desfaz as alterações ao fim de cada teste. O import de `DataJpaTest` corresponde ao Spring Boot 4 do projeto.

O ponto principal é `flush` seguido de `clear`: primeiro enviamos as alterações ao banco; depois removemos as entidades do contexto para que a consulta não apenas devolva o mesmo objeto em memória.

```java
package br.com.fatecararas.piapi;

import br.com.fatecararas.piapi.domain.entity.*;
import br.com.fatecararas.piapi.domain.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class PersistenciaJpaTests {

    @Autowired UsuarioRepository usuarios;
    @Autowired CategoriaRepository categorias;
    @Autowired CursoRepository cursos;
    @Autowired PublicoAlvoRepository publicos;
    @Autowired PropostaRepository propostas;
    @Autowired CurtidaRepository curtidas;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbc;

    private Proposta novaProposta() {
        Proposta proposta = new Proposta();
        proposta.setTitulo("Horta comunitária conectada");
        proposta.setResumo("Monitoramento de uma horta local.");
        proposta.setDescricao("Integração de tecnologia e sustentabilidade.");
        proposta.setAutor(usuarios.findById(1L).orElseThrow());
        proposta.setCategoria(categorias.findById(1L).orElseThrow());
        proposta.getCursos().add(cursos.findById(1L).orElseThrow());
        proposta.getCursos().add(cursos.findById(2L).orElseThrow());
        proposta.getPublicosAlvo().add(publicos.findById(1L).orElseThrow());
        return propostas.saveAndFlush(proposta);
    }

    @Test
    void recuperaRelacionamentosDoBanco() {
        Proposta proposta = novaProposta();
        Long propostaId = proposta.getId();
        CurtidaId chave = new CurtidaId(1L, propostaId);
        curtidas.saveAndFlush(new Curtida(proposta.getAutor(), proposta));

        // Esvazia o contexto para verificar uma nova leitura do banco.
        entityManager.clear();

        Proposta lida = propostas.findById(propostaId).orElseThrow();
        assertThat(lida.getAutor().getEmail()).isEqualTo("demo@fatec.local");
        assertThat(lida.getCategoria().getId()).isEqualTo(1L);
        assertThat(lida.getCursos()).extracting(Curso::getId)
                .containsExactlyInAnyOrder(1L, 2L);
        assertThat(lida.getPublicosAlvo()).extracting(PublicoAlvo::getId)
                .containsExactly(1L);
        assertThat(lida.getCriadaEm()).isNotNull();
        assertThat(lida.getAtualizadaEm()).isNotNull();
        Curtida curtida = curtidas.findById(chave).orElseThrow();
        assertThat(curtida.getUsuario().getId()).isEqualTo(1L);
        assertThat(curtida.getProposta().getId()).isEqualTo(propostaId);
        assertThat(curtida.getCriadaEm()).isNotNull();
        assertThat(curtidas.countByIdPropostaId(propostaId)).isEqualTo(1L);

        curtidas.deleteById(chave);
        curtidas.flush();
        entityManager.clear();
        assertThat(curtidas.existsById(chave)).isFalse();
        assertThat(propostas.existsById(propostaId)).isTrue();
        assertThat(usuarios.existsById(1L)).isTrue();
    }

    @Test
    void bancoRejeitaDuasLinhasComAMesmaChaveDeCurtida() {
        Proposta proposta = novaProposta();
        curtidas.saveAndFlush(new Curtida(proposta.getAutor(), proposta));

        // INSERT direto força uma segunda linha, sem a semântica de merge do save.
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO curtida (usuario_id, proposta_id, criada_em) VALUES (?, ?, CURRENT_TIMESTAMP)",
                1L, proposta.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
```

Execute:

```bash
./gradlew test --tests '*PersistenciaJpaTests'
./gradlew test
```

O primeiro teste deve comprovar autor, categoria, dois cursos, um público, datas e curtida após nova consulta. O segundo usa INSERT direto apenas como contraprova da restrição física: salvar duas entidades com a mesma chave via `save` pode ser interpretado como atualização, não como tentativa de duplicação.

## 7. Observe as tabelas e a duração dos dados

Execute `./gradlew bootRun`. Abra `http://localhost:8080/h2-console` e conecte com JDBC URL `jdbc:h2:mem:piapi`, usuário `aluno` e senha `fatec`. Use o console da mesma aplicação em execução; um cliente externo com a mesma URL em memória terá outro banco.

```sql
SELECT * FROM usuario;
SELECT * FROM categoria;
SELECT * FROM curso;
SELECT * FROM publico_alvo;
SELECT * FROM proposta;
SELECT * FROM proposta_curso;
SELECT * FROM proposta_publico_alvo;
SELECT * FROM curtida;
```

Após iniciar, espere um usuário, quatro categorias, três cursos e quatro públicos na carga inicial. As tabelas de propostas e relações estarão vazias: os testes usam outro contexto e rollback. Para observar gravações do laboratório, coloque um breakpoint após o `flush` e inspecione as consultas e entidades no teste, ou confira o SQL no relatório de execução; não espere que esses dados apareçam no banco de `bootRun`.

Para experimentar conservação entre reinicializações, pare a aplicação e execute, sempre na mesma pasta:

```bash
./gradlew bootRun --args='--spring.datasource.url=jdbc:h2:file:./data/piapi'
```

No console, use `jdbc:h2:file:./data/piapi`. Registros gravados e confirmados nesse banco permanecem ao reiniciar com a mesma URL. Não versione os arquivos locais do banco. Isso é uma extensão de configuração; o padrão desta branch continua em memória.

O perfil MySQL existente é uma alternativa opcional:

Arquivo `src/main/resources/application-mysql.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/piapi
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
```

Crie previamente o banco `piapi` em um servidor MySQL e um usuário com acesso a ele. Defina `DB_USER` e `DB_PASSWORD` no ambiente local e execute `./gradlew bootRun --args='--spring.profiles.active=mysql'`. As migrations serão aplicadas nesse banco; não é necessário usar MySQL para concluir o tutorial.

## 8. Investigue erros comuns

| Sintoma | Verificação |
|---|---|
| Tabela ausente ou erro de validação do esquema | Confira nomes das migrations, Flyway ativo e correspondência entre colunas e entidades |
| Método derivado não reconhecido | Use `ativa` para Categoria, `ativo` para os demais e `id.propostaId` na chave de Curtida |
| Referência transiente ao salvar proposta | Busque e associe autor e catálogos já persistidos; não crie objetos vazios com nomes |
| Falha ao acessar relação lazy | Acesse a relação dentro da transação; o laboratório já usa a transação do `@DataJpaTest` |
| Chave de curtida inválida | Confira entidades com ID, campos de CurtidaId e valores de `@MapsId` |
| Dados desaparecem após reiniciar | H2 `mem` dura só durante a execução da JVM |
| Getter ou construtor não reconhecido | Confira dependências Lombok e processamento de anotações |
| Flyway acusa checksum diferente | Não altere migration aplicada em banco compartilhado; crie nova versão |

## 9. Desafio e critérios de conclusão

Acrescente um teste que desative uma categoria e demonstre a diferença entre `findById` e `findByIdAndAtivaTrue`. Depois associe o mesmo curso a duas propostas, consulte ambas após `clear` e explique por que não foi necessário duplicar o curso. Use os catálogos da V2 e mantenha o rollback dos testes.

Entregue o código e uma explicação curta com as evidências:

- As seis entidades e os seis repositories compilam; as migrations e os testes executam.
- A leitura após `clear` recupera campos e associações do banco.
- O aluno identifica PKs, FKs e as duas tabelas de ligação N:M.
- A dupla de IDs impede duas linhas de curtida iguais; usuários diferentes podem curtir a mesma proposta.
- O aluno explica por que Curtida usa duas relações N:1 e por que não há 1:1 neste domínio implementado.
- O aluno distingue restrições do banco, anotações de mapeamento e regras que ainda dependem de outras camadas.
- O aluno explica a diferença entre persistir durante a execução e conservar dados após reiniciar.

Ao concluir, confira tanto o funcionamento do código quanto sua capacidade de justificar o modelo. A integração com services e HTTP usará esta camada pronta como ponto de partida para a próxima atividade.
