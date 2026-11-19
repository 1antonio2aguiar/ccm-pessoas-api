/*
 * Unificação de cidades — estruturas novas
 * Oracle 11g R1
 *
 * Rodar na ordem. Os blocos 1 e 3 como DBO_UNI_PESSOAS,
 * o bloco 2 como DBO_CORREIOS e o bloco 4 com cada dono dos objetos (ou DBA).
 *
 * IMPORTANTE: depois disso, NÃO rodar mais a SP_CIDADES_GERA_UNIFICADO.
 * Ela apaga CIDADES_UNIFICADO e as cidades/distritos/bairros/logradouros
 * com STATUS = 'N' dos Correios, o que desfaz o trabalho feito pela tela.
 */

-------------------------------------------------------------------------------
-- 1. Controle de duplicados e "não vincular"  (como DBO_UNI_PESSOAS)
-------------------------------------------------------------------------------
--   STATUS = 'D' : código duplicado da origem; ID_UNIFICADO aponta para a
--                  cidade unificada que já tinha outro código desta origem.
--   STATUS = 'N' : decisão do usuário de não vincular a cidade.

CREATE TABLE DBO_UNI_PESSOAS.CIDADES_UNIFICADO_CTRL
(
    ID             NUMBER(10)     NOT NULL,
    ORIGEM         VARCHAR2(10)   NOT NULL,
    CIDADE_ORIGEM  NUMBER(10)     NOT NULL,
    STATUS         CHAR(1)        NOT NULL,
    ID_UNIFICADO   NUMBER(10),
    MOTIVO         VARCHAR2(500),
    USUARIO        VARCHAR2(100),
    DATA_REGISTRO  DATE           DEFAULT SYSDATE NOT NULL,
    CONSTRAINT PK_CIDADES_UNIFICADO_CTRL PRIMARY KEY (ID),
    CONSTRAINT UK_CIDADES_UNIFICADO_CTRL UNIQUE (ORIGEM, CIDADE_ORIGEM),
    CONSTRAINT CK_CIDADES_UNIFICADO_CTRL_ST CHECK (STATUS IN ('D', 'N'))
);

CREATE SEQUENCE DBO_UNI_PESSOAS.SEQ_CIDADES_UNIFICADO_CTRL
    MINVALUE 1 MAXVALUE 9999999999 INCREMENT BY 1 START WITH 1 NOCACHE NOORDER NOCYCLE;

-------------------------------------------------------------------------------
-- 2. Sequence das cidades criadas fora da base dos Correios  (como DBO_CORREIOS)
-------------------------------------------------------------------------------
-- Substitui o "SELECT MAX(CIDADE + 1)" da procedure.
-- Começa no maior código atual + 1.

DECLARE
    V_INICIO NUMBER;
BEGIN
    SELECT NVL(MAX(CIDADE), 0) + 1
      INTO V_INICIO
      FROM DBO_CORREIOS.CIDADES;

    EXECUTE IMMEDIATE
        'CREATE SEQUENCE DBO_CORREIOS.SEQ_CIDADES_FICTICIAS'
        || ' MINVALUE 1 MAXVALUE 9999999999 INCREMENT BY 1'
        || ' START WITH ' || V_INICIO
        || ' NOCACHE NOORDER NOCYCLE';
END;
/

-------------------------------------------------------------------------------
-- 3. Conferência da SEQ_CIDADES_UNIFICADO  (como DBO_UNI_PESSOAS)
-------------------------------------------------------------------------------
-- O próximo valor precisa ser maior que o MAX(ID) atual.
-- Se a consulta abaixo retornar 'AJUSTAR', recriar a sequence começando
-- em MAX(ID) + 1 (NÃO usar o DROP/CREATE comentado na procedure com START WITH 1).

SELECT (SELECT MAX(ID) FROM DBO_UNI_PESSOAS.CIDADES_UNIFICADO) AS MAX_ID,
       S.LAST_NUMBER,
       CASE
           WHEN S.LAST_NUMBER > (SELECT NVL(MAX(ID), 0) FROM DBO_UNI_PESSOAS.CIDADES_UNIFICADO)
           THEN 'OK'
           ELSE 'AJUSTAR'
       END AS SITUACAO
  FROM ALL_SEQUENCES S
 WHERE S.SEQUENCE_OWNER = 'DBO_UNI_PESSOAS'
   AND S.SEQUENCE_NAME  = 'SEQ_CIDADES_UNIFICADO';

-------------------------------------------------------------------------------
-- 4. Permissões para o usuário da API (DBO_CCM_PESSOAS)
-------------------------------------------------------------------------------
-- A API acessa os sinônimos através de DBO_UNI_PESSOAS.<SINONIMO>.
-- Para isso, DBO_CCM_PESSOAS precisa de SELECT nas TABELAS de origem
-- (o grant é sempre no objeto real, não no sinônimo).
-- Ajustar os nomes reais dos objetos conforme o ambiente.

-- Como DBO_UNI_PESSOAS:
GRANT SELECT, INSERT, UPDATE ON DBO_UNI_PESSOAS.CIDADES_UNIFICADO          TO DBO_CCM_PESSOAS;
GRANT SELECT, INSERT, UPDATE ON DBO_UNI_PESSOAS.CIDADES_UNIFICADO_CTRL     TO DBO_CCM_PESSOAS;
GRANT SELECT                 ON DBO_UNI_PESSOAS.SEQ_CIDADES_UNIFICADO      TO DBO_CCM_PESSOAS;
GRANT SELECT                 ON DBO_UNI_PESSOAS.SEQ_CIDADES_UNIFICADO_CTRL TO DBO_CCM_PESSOAS;
GRANT EXECUTE                ON DBO_UNI_PESSOAS.FN_NORMALIZA_CIDADE        TO DBO_CCM_PESSOAS;

-- Como DBO_CORREIOS:
GRANT SELECT, INSERT ON DBO_CORREIOS.CIDADES               TO DBO_CCM_PESSOAS;
GRANT SELECT         ON DBO_CORREIOS.SEQ_CIDADES_FICTICIAS TO DBO_CCM_PESSOAS;

-- Como dono de cada tabela de origem (exemplos — conferir o objeto real de cada sinônimo):
-- GRANT SELECT ON DBO_PESSOAS.CIDADES           TO DBO_CCM_PESSOAS;
-- GRANT SELECT ON DBO_RH.CIDADES                TO DBO_CCM_PESSOAS;
-- GRANT SELECT ON DBO_CODAU_SANEAMENTO.CIDADES  TO DBO_CCM_PESSOAS;
-- GRANT SELECT ON <SCHEMA_SAUDE>.CIDADES        TO DBO_CCM_PESSOAS;
-- GRANT SELECT ON <SCHEMA_ESCOLA>.CIDADES       TO DBO_CCM_PESSOAS;

-- Para descobrir o objeto real de cada sinônimo:
SELECT SYNONYM_NAME, TABLE_OWNER, TABLE_NAME
  FROM ALL_SYNONYMS
 WHERE OWNER = 'DBO_UNI_PESSOAS'
   AND SYNONYM_NAME IN ('PES_CIDADES', 'RH_CIDADES', 'SANE_CIDADES',
                        'SAU_CIDADES', 'ESCO_CIDADES', 'COR_CIDADES');
