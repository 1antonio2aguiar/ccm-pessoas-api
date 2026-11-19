/*
 * Unificação de bairros — estruturas novas
 * Oracle 11g R1
 *
 * Rodar depois dos scripts de cidades (01, 02) e distritos (03).
 * Blocos 1 e 3 como DBO_UNI_PESSOAS, bloco 2 como DBO_CORREIOS,
 * bloco 4 com o dono de cada objeto (ou DBA).
 *
 * IMPORTANTE: depois disso, NÃO rodar mais a SP_BAIRROS_GERA_UNIFICADO.
 * Ela apaga os bairros STATUS = 'N' dos Correios, reescreve NOME_TRANSF
 * de todos os bairros dos Correios e grava linhas sem código dos Correios.
 */

-------------------------------------------------------------------------------
-- 1. Controle de duplicados e "não vincular"  (como DBO_UNI_PESSOAS)
-------------------------------------------------------------------------------
--   STATUS = 'D' : código duplicado da origem; ID_UNIFICADO aponta para o
--                  bairro unificado que já tinha outro código desta origem.
--   STATUS = 'N' : decisão do usuário de não vincular o bairro.
--
-- As tabelas BAIRROS_UNIFICADO_RH e BAIRROS_UNIFICADO_SAUDE da procedure
-- não são usadas pela tela (podem ficar só para consulta).

CREATE TABLE DBO_UNI_PESSOAS.BAIRROS_UNIFICADO_CTRL
(
    ID               NUMBER(10)     NOT NULL,
    ORIGEM           VARCHAR2(10)   NOT NULL,
    CIDADE_ORIGEM    NUMBER(10)     NOT NULL,
    DISTRITO_ORIGEM  NUMBER(10)     NOT NULL,
    BAIRRO_ORIGEM    NUMBER(15)     NOT NULL,
    STATUS           CHAR(1)        NOT NULL,
    ID_UNIFICADO     NUMBER(10),
    MOTIVO           VARCHAR2(500),
    USUARIO          VARCHAR2(100),
    DATA_REGISTRO    DATE           DEFAULT SYSDATE NOT NULL,
    CONSTRAINT PK_BAIRROS_UNIFICADO_CTRL PRIMARY KEY (ID),
    CONSTRAINT UK_BAIRROS_UNIFICADO_CTRL UNIQUE (ORIGEM, CIDADE_ORIGEM, DISTRITO_ORIGEM, BAIRRO_ORIGEM),
    CONSTRAINT CK_BAIRROS_UNIFICADO_CTRL_ST CHECK (STATUS IN ('D', 'N'))
);

CREATE SEQUENCE DBO_UNI_PESSOAS.SEQ_BAIRROS_UNIFICADO_CTRL
    MINVALUE 1 MAXVALUE 9999999999 INCREMENT BY 1 START WITH 1 NOCACHE NOORDER NOCYCLE;

-- Índices para as consultas da tela (a tabela tem só os índices de Correios e PESSOAS)
CREATE INDEX DBO_UNI_PESSOAS.IDX_BAIRROS_UNIFICADO_RH    ON DBO_UNI_PESSOAS.BAIRROS_UNIFICADO (CIDADE_RH, DISTRITO_RH, BAIRRO_RH);
CREATE INDEX DBO_UNI_PESSOAS.IDX_BAIRROS_UNIFICADO_SAU   ON DBO_UNI_PESSOAS.BAIRROS_UNIFICADO (CIDADE_SAUDE, DISTRITO_SAUDE, BAIRRO_SAUDE);
CREATE INDEX DBO_UNI_PESSOAS.IDX_BAIRROS_UNIFICADO_SANE  ON DBO_UNI_PESSOAS.BAIRROS_UNIFICADO (CIDADE_SANE, DISTRITO_SANE, BAIRRO_SANE);
CREATE INDEX DBO_UNI_PESSOAS.IDX_BAIRROS_UNIFICADO_ESCO  ON DBO_UNI_PESSOAS.BAIRROS_UNIFICADO (CIDADE_ESCOLA, DISTRITO_ESCOLA, BAIRRO_ESCOLA);

-------------------------------------------------------------------------------
-- 2. Sequence dos bairros criados fora dos Correios  (como DBO_CORREIOS)
-------------------------------------------------------------------------------
-- Sequence global, começando no maior código atual + 10000.

DECLARE
    V_INICIO NUMBER;
BEGIN
    SELECT NVL(MAX(BAIRRO), 0) + 10000
      INTO V_INICIO
      FROM DBO_CORREIOS.BAIRROS;

    EXECUTE IMMEDIATE
        'CREATE SEQUENCE DBO_CORREIOS.SEQ_BAIRROS_FICTICIOS'
        || ' MINVALUE 1 MAXVALUE 9999999999 INCREMENT BY 1'
        || ' START WITH ' || V_INICIO
        || ' NOCACHE NOORDER NOCYCLE';
END;
/

-------------------------------------------------------------------------------
-- 3. Conferência da SEQ_BAIRROS_UNIFICADO  (como DBO_UNI_PESSOAS)
-------------------------------------------------------------------------------

SELECT (SELECT MAX(ID) FROM DBO_UNI_PESSOAS.BAIRROS_UNIFICADO) AS MAX_ID,
       S.LAST_NUMBER,
       CASE
           WHEN S.LAST_NUMBER > (SELECT NVL(MAX(ID), 0) FROM DBO_UNI_PESSOAS.BAIRROS_UNIFICADO)
           THEN 'OK'
           ELSE 'AJUSTAR'
       END AS SITUACAO
  FROM ALL_SEQUENCES S
 WHERE S.SEQUENCE_OWNER = 'DBO_UNI_PESSOAS'
   AND S.SEQUENCE_NAME  = 'SEQ_BAIRROS_UNIFICADO';

-------------------------------------------------------------------------------
-- 4. Permissões para o usuário da API (DBO_CCM_PESSOAS)
-------------------------------------------------------------------------------

-- Como DBO_UNI_PESSOAS:
GRANT SELECT, INSERT, UPDATE, DELETE ON DBO_UNI_PESSOAS.BAIRROS_UNIFICADO      TO DBO_CCM_PESSOAS;
GRANT SELECT, INSERT, UPDATE, DELETE ON DBO_UNI_PESSOAS.BAIRROS_UNIFICADO_CTRL TO DBO_CCM_PESSOAS;
GRANT SELECT ON DBO_UNI_PESSOAS.SEQ_BAIRROS_UNIFICADO      TO DBO_CCM_PESSOAS;
GRANT SELECT ON DBO_UNI_PESSOAS.SEQ_BAIRROS_UNIFICADO_CTRL TO DBO_CCM_PESSOAS;

-- Como DBO_CORREIOS:
GRANT SELECT, INSERT ON DBO_CORREIOS.BAIRROS               TO DBO_CCM_PESSOAS;
GRANT SELECT         ON DBO_CORREIOS.SEQ_BAIRROS_FICTICIOS TO DBO_CCM_PESSOAS;

-- Como dono de cada tabela de bairros de origem (conferir o objeto real):
SELECT SYNONYM_NAME, TABLE_OWNER, TABLE_NAME
  FROM ALL_SYNONYMS
 WHERE OWNER = 'DBO_UNI_PESSOAS'
   AND SYNONYM_NAME IN ('PES_BAIRROS', 'RH_BAIRROS', 'SANE_BAIRROS',
                        'SAU_BAIRROS', 'ESCO_BAIRROS', 'COR_BAIRROS');
-- GRANT SELECT ON <dono>.<tabela> TO DBO_CCM_PESSOAS;  -- para cada linha acima

-------------------------------------------------------------------------------
-- 5. Antes de usar a tela: situação da carga antiga (procedure)
-------------------------------------------------------------------------------
-- A procedure gravou linhas SEM código dos Correios (CIDADE_CORREIOS = 0).
-- Um bairro de origem preso numa dessas linhas conta como "já vinculado"
-- na tela, mas a migração de pessoas não acha o CODIGO_CCM dele.
-- Se a contagem abaixo for relevante, use "Recomeçar do zero" na tela.

SELECT STATUS, COUNT(*) QTD
  FROM DBO_UNI_PESSOAS.BAIRROS_UNIFICADO
 WHERE NVL(CIDADE_CORREIOS, 0) = 0
 GROUP BY STATUS
 ORDER BY STATUS;
