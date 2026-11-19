/*
 * Unificação de distritos — estruturas novas
 * Oracle 11g R1
 *
 * Rodar depois dos scripts de cidades (01 e 02).
 * Os blocos 1 e 3 como DBO_UNI_PESSOAS, o bloco 2 como DBO_CORREIOS,
 * o bloco 4 com o dono de cada objeto (ou DBA).
 *
 * IMPORTANTE: depois disso, NÃO rodar mais a SP_DISTRITOS_GERA_UNIFICADO.
 * Ela faz TRUNCATE em DISTRITOS_UNIFICADO e muda o STATUS de todos os
 * distritos dos Correios para 'I'/'L', desfazendo o que a tela gravou.
 */

-------------------------------------------------------------------------------
-- 1. Controle de duplicados e "não vincular"  (como DBO_UNI_PESSOAS)
-------------------------------------------------------------------------------
--   STATUS = 'D' : código duplicado da origem; ID_UNIFICADO aponta para o
--                  distrito unificado que já tinha outro código desta origem.
--   STATUS = 'N' : decisão do usuário de não vincular o distrito.
--
-- Substitui a DISTRITOS_UNIFICADO_DPL da procedure (que pode ser mantida
-- só para consulta do histórico).

CREATE TABLE DBO_UNI_PESSOAS.DISTRITOS_UNIFICADO_CTRL
(
    ID               NUMBER(10)     NOT NULL,
    ORIGEM           VARCHAR2(10)   NOT NULL,
    CIDADE_ORIGEM    NUMBER(10)     NOT NULL,
    DISTRITO_ORIGEM  NUMBER(10)     NOT NULL,
    STATUS           CHAR(1)        NOT NULL,
    ID_UNIFICADO     NUMBER(10),
    MOTIVO           VARCHAR2(500),
    USUARIO          VARCHAR2(100),
    DATA_REGISTRO    DATE           DEFAULT SYSDATE NOT NULL,
    CONSTRAINT PK_DISTRITOS_UNIFICADO_CTRL PRIMARY KEY (ID),
    CONSTRAINT UK_DISTRITOS_UNIFICADO_CTRL UNIQUE (ORIGEM, CIDADE_ORIGEM, DISTRITO_ORIGEM),
    CONSTRAINT CK_DISTRITOS_UNIFICADO_CTRL_ST CHECK (STATUS IN ('D', 'N'))
);

CREATE SEQUENCE DBO_UNI_PESSOAS.SEQ_DISTRITOS_UNIFICADO_CTRL
    MINVALUE 1 MAXVALUE 9999999999 INCREMENT BY 1 START WITH 1 NOCACHE NOORDER NOCYCLE;

-------------------------------------------------------------------------------
-- 2. Sequence dos distritos criados fora dos Correios  (como DBO_CORREIOS)
-------------------------------------------------------------------------------
-- Substitui o "MAX(DISTRITO) + 10000" por cidade da procedure.
-- Sequence global, começando acima do maior código atual + 10000,
-- assim o código nunca se repete em nenhuma cidade.

DECLARE
    V_INICIO NUMBER;
BEGIN
    SELECT NVL(MAX(DISTRITO), 0) + 10000
      INTO V_INICIO
      FROM DBO_CORREIOS.DISTRITOS;

    EXECUTE IMMEDIATE
        'CREATE SEQUENCE DBO_CORREIOS.SEQ_DISTRITOS_FICTICIOS'
        || ' MINVALUE 1 MAXVALUE 9999999999 INCREMENT BY 1'
        || ' START WITH ' || V_INICIO
        || ' NOCACHE NOORDER NOCYCLE';
END;
/

-------------------------------------------------------------------------------
-- 3. Conferência da SEQ_DISTRITOS_UNIFICADO  (como DBO_UNI_PESSOAS)
-------------------------------------------------------------------------------
-- A sequence tem MAXVALUE 999999 na procedure. Conferir se há folga.

SELECT (SELECT MAX(ID) FROM DBO_UNI_PESSOAS.DISTRITOS_UNIFICADO) AS MAX_ID,
       S.LAST_NUMBER,
       S.MAX_VALUE,
       CASE
           WHEN S.LAST_NUMBER > (SELECT NVL(MAX(ID), 0) FROM DBO_UNI_PESSOAS.DISTRITOS_UNIFICADO)
           THEN 'OK'
           ELSE 'AJUSTAR'
       END AS SITUACAO
  FROM ALL_SEQUENCES S
 WHERE S.SEQUENCE_OWNER = 'DBO_UNI_PESSOAS'
   AND S.SEQUENCE_NAME  = 'SEQ_DISTRITOS_UNIFICADO';

-- Se precisar de mais folga (cada "recomeçar do zero" consome um número por distrito):
-- ALTER SEQUENCE DBO_UNI_PESSOAS.SEQ_DISTRITOS_UNIFICADO MAXVALUE 9999999999;

-------------------------------------------------------------------------------
-- 4. Permissões para o usuário da API (DBO_CCM_PESSOAS)
-------------------------------------------------------------------------------

-- Como DBO_UNI_PESSOAS:
GRANT SELECT, INSERT, UPDATE, DELETE ON DBO_UNI_PESSOAS.DISTRITOS_UNIFICADO      TO DBO_CCM_PESSOAS;
GRANT SELECT, INSERT, UPDATE, DELETE ON DBO_UNI_PESSOAS.DISTRITOS_UNIFICADO_CTRL TO DBO_CCM_PESSOAS;
GRANT SELECT ON DBO_UNI_PESSOAS.SEQ_DISTRITOS_UNIFICADO      TO DBO_CCM_PESSOAS;
GRANT SELECT ON DBO_UNI_PESSOAS.SEQ_DISTRITOS_UNIFICADO_CTRL TO DBO_CCM_PESSOAS;

-- Como DBO_CORREIOS:
GRANT SELECT, INSERT ON DBO_CORREIOS.DISTRITOS               TO DBO_CCM_PESSOAS;
GRANT SELECT         ON DBO_CORREIOS.SEQ_DISTRITOS_FICTICIOS TO DBO_CCM_PESSOAS;

-- Como dono de cada tabela de distritos de origem (conferir o objeto real):
SELECT SYNONYM_NAME, TABLE_OWNER, TABLE_NAME
  FROM ALL_SYNONYMS
 WHERE OWNER = 'DBO_UNI_PESSOAS'
   AND SYNONYM_NAME IN ('PES_DISTRITOS', 'RH_DISTRITOS', 'SANE_DISTRITOS',
                        'SAU_DISTRITOS', 'ESCO_DISTRITOS', 'COR_DISTRITOS');
-- GRANT SELECT ON <dono>.<tabela> TO DBO_CCM_PESSOAS;  -- para cada linha acima

-------------------------------------------------------------------------------
-- 5. Conferências antes de recomeçar (opcional)
-------------------------------------------------------------------------------

-- 5.1 Distritos dos Correios com CIDADE = 0 (criados pela procedure quando a
--     cidade não estava unificada). Não entram na base nova.
SELECT CIDADE, DISTRITO, NOME, ESTADO, STATUS
  FROM DBO_CORREIOS.DISTRITOS
 WHERE NVL(CIDADE, 0) = 0
 ORDER BY ESTADO, NOME;

-- 5.2 Pares de origem gravados em mais de uma linha de DISTRITOS_UNIFICADO
--     (a migração de pessoas usa getSingleResult e falha nesses casos).
SELECT 'PESSOAS' ORIGEM, CIDADE_PESSOA CIDADE, DISTRITO_PESSOA DISTRITO, COUNT(*) QTD
  FROM DBO_UNI_PESSOAS.DISTRITOS_UNIFICADO
 WHERE NVL(DISTRITO_PESSOA, 0) > 0
 GROUP BY CIDADE_PESSOA, DISTRITO_PESSOA HAVING COUNT(*) > 1
UNION ALL
SELECT 'RH', CIDADE_RH, DISTRITO_RH, COUNT(*)
  FROM DBO_UNI_PESSOAS.DISTRITOS_UNIFICADO
 WHERE NVL(DISTRITO_RH, 0) > 0
 GROUP BY CIDADE_RH, DISTRITO_RH HAVING COUNT(*) > 1
UNION ALL
SELECT 'SAUDE', CIDADE_SAUDE, DISTRITO_SAUDE, COUNT(*)
  FROM DBO_UNI_PESSOAS.DISTRITOS_UNIFICADO
 WHERE NVL(DISTRITO_SAUDE, 0) > 0
 GROUP BY CIDADE_SAUDE, DISTRITO_SAUDE HAVING COUNT(*) > 1;
