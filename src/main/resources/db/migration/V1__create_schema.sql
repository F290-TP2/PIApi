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
