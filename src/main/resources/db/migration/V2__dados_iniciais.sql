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
