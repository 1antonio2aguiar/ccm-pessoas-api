/*
 * Logradouros — diagnóstico do "Recomeçar do zero" travado
 * e zerar manual, passo a passo
 * Oracle 11g R1
 *
 * PARTE A: rodar como DBA (ou usuário com acesso às V$). Não altera nada.
 * PARTE B: só depois que a PARTE A mostrar que não há sessão trabalhando
 *          nem transação sendo desfeita. Rodar como DBO_UNI_PESSOAS,
 *          com a API PARADA.
 */

-------------------------------------------------------------------------------
-- PARTE A — DIAGNÓSTICO
-------------------------------------------------------------------------------

-- A1. Sessões da API ativas, o que estão fazendo e por quem estão bloqueadas.
--     EVENTO = 'enq: TX - row lock contention' => esperando outra sessão
--     (veja BLOQUEADA_POR).
SELECT S.SID,
       S.SERIAL#,
       S.STATUS,
       S.EVENT              EVENTO,
       S.BLOCKING_SESSION   BLOQUEADA_POR,
       S.LAST_CALL_ET       SEGUNDOS_NO_COMANDO,
       SUBSTR(Q.SQL_TEXT, 1, 100) COMANDO
  FROM V$SESSION S
  LEFT JOIN V$SQL Q
    ON Q.SQL_ID = S.SQL_ID
   AND Q.CHILD_NUMBER = S.SQL_CHILD_NUMBER
 WHERE S.USERNAME IN ('DBO_CCM_PESSOAS', 'DBO_UNI_PESSOAS')
 ORDER BY S.STATUS, S.SID;

-- A2. Transações abertas: rode duas vezes com 1 minuto de intervalo.
--     LINHAS_UNDO subindo   => ainda trabalhando;
--     LINHAS_UNDO descendo  => desfazendo (rollback); espere chegar a zero.
SELECT S.SID, S.SERIAL#, S.USERNAME, T.USED_UREC LINHAS_UNDO, T.START_TIME
  FROM V$TRANSACTION T
  JOIN V$SESSION S
    ON S.TADDR = T.ADDR;

-- A3. Rollback de sessão já encerrada (feito pelo Oracle em segundo plano).
--     Se aparecer linha aqui, espere terminar antes da PARTE B.
SELECT USN, STATE, UNDOBLOCKSTOTAL, UNDOBLOCKSDONE,
       ROUND(UNDOBLOCKSDONE / NULLIF(UNDOBLOCKSTOTAL, 0) * 100) PERCENTUAL
  FROM V$FAST_START_TRANSACTIONS
 WHERE STATE <> 'RECOVERED';

-- A4. Encerrar uma sessão da API presa (SID e SERIAL# da consulta A1).
--     Atenção: o que ela fez será desfeito, e isso leva tempo (veja A2/A3).
-- ALTER SYSTEM KILL SESSION '<sid>,<serial#>' IMMEDIATE;

-------------------------------------------------------------------------------
-- PARTE B — ZERAR MANUAL (mesmos passos da tela, com COMMIT a cada passo)
-------------------------------------------------------------------------------
-- Rodar como script (F5 no SQL Developer). O tempo de cada passo aparece
-- na saída: me mande essa saída se algum passo demorar.

SET TIMING ON

-- B1. Controle de duplicados / não vincular
DELETE FROM DBO_UNI_PESSOAS.LOGRADOUROS_UNIFICADO_CTRL;
COMMIT;

-- B2. Linhas sem código dos Correios (carga antiga da procedure)
DELETE FROM DBO_UNI_PESSOAS.LOGRADOUROS_UNIFICADO
 WHERE NVL(CIDADE_CORREIOS, 0) = 0
    OR NVL(LOGRADOURO_CORREIOS, 0) = 0;
COMMIT;

-- B3. Linhas repetidas para o mesmo logradouro dos Correios (fica a de menor ID)
DELETE FROM DBO_UNI_PESSOAS.LOGRADOUROS_UNIFICADO
 WHERE ROWID IN (SELECT RID
                   FROM (SELECT ROWID RID,
                                ROW_NUMBER() OVER (PARTITION BY CIDADE_CORREIOS,
                                                                DISTRITO_CORREIOS,
                                                                LOGRADOURO_CORREIOS
                                                       ORDER BY ID) RN
                           FROM DBO_UNI_PESSOAS.LOGRADOUROS_UNIFICADO)
                  WHERE RN > 1);
COMMIT;

-- B4. Quantas linhas o passo B5 vai alterar (só as que têm vínculo ou complemento)
SELECT COUNT(*) LINHAS_A_ZERAR
  FROM DBO_UNI_PESSOAS.LOGRADOUROS_UNIFICADO U
 WHERE NVL(U.CIDADE_PESSOA, 0) <> 0 OR NVL(U.DISTRITO_PESSOA, 0) <> 0 OR NVL(U.LOGRADOURO_PESSOA, 0) <> 0
    OR NVL(U.CIDADE_RH, 0) <> 0     OR NVL(U.DISTRITO_RH, 0) <> 0     OR NVL(U.LOGRADOURO_RH, 0) <> 0
    OR NVL(U.CIDADE_SANE, 0) <> 0   OR NVL(U.DISTRITO_SANE, 0) <> 0   OR NVL(U.LOGRADOURO_SANE, 0) <> 0
    OR NVL(U.CIDADE_SAUDE, 0) <> 0  OR NVL(U.DISTRITO_SAUDE, 0) <> 0  OR NVL(U.LOGRADOURO_SAUDE, 0) <> 0
    OR NVL(U.CIDADE_ESCOLA, 0) <> 0 OR NVL(U.DISTRITO_ESCOLA, 0) <> 0 OR NVL(U.LOGRADOURO_ESCOLA, 0) <> 0
    OR U.NOME_LEGAL IS NOT NULL OR U.CEP IS NOT NULL OR U.NUMERO_INI IS NOT NULL
    OR U.NUMERO_FIM IS NOT NULL OR U.LADO IS NOT NULL OR U.LOGRADOURO_DNE IS NOT NULL
    OR U.BAIRRO_DNE IS NOT NULL OR U.BAIRRO_DNE_FINAL IS NOT NULL;

-- B5. Zera vínculos e complementos
UPDATE DBO_UNI_PESSOAS.LOGRADOUROS_UNIFICADO U
   SET U.CIDADE_PESSOA = 0, U.DISTRITO_PESSOA = 0, U.LOGRADOURO_PESSOA = 0,
       U.CIDADE_RH = 0,     U.DISTRITO_RH = 0,     U.LOGRADOURO_RH = 0,
       U.CIDADE_SANE = 0,   U.DISTRITO_SANE = 0,   U.LOGRADOURO_SANE = 0,
       U.CIDADE_SAUDE = 0,  U.DISTRITO_SAUDE = 0,  U.LOGRADOURO_SAUDE = 0,
       U.CIDADE_ESCOLA = 0, U.DISTRITO_ESCOLA = 0, U.LOGRADOURO_ESCOLA = 0,
       U.NOME_LEGAL = NULL, U.CEP = NULL, U.NUMERO_INI = NULL, U.NUMERO_FIM = NULL,
       U.LADO = NULL, U.LOGRADOURO_DNE = NULL, U.BAIRRO_DNE = NULL, U.BAIRRO_DNE_FINAL = NULL,
       U.COMPLEMENTO = CASE WHEN NVL(U.CIDADE_SAUDE, 0) <> 0
                            THEN (SELECT L.COMPLEMENTO
                                    FROM DBO_CORREIOS.LOGRADOUROS L
                                   WHERE L.CIDADE = U.CIDADE_CORREIOS
                                     AND L.DISTRITO = U.DISTRITO_CORREIOS
                                     AND L.LOGRADOURO = U.LOGRADOURO_CORREIOS)
                            ELSE U.COMPLEMENTO END,
       U.DATA_ALTERACAO = SYSDATE
 WHERE NVL(U.CIDADE_PESSOA, 0) <> 0 OR NVL(U.DISTRITO_PESSOA, 0) <> 0 OR NVL(U.LOGRADOURO_PESSOA, 0) <> 0
    OR NVL(U.CIDADE_RH, 0) <> 0     OR NVL(U.DISTRITO_RH, 0) <> 0     OR NVL(U.LOGRADOURO_RH, 0) <> 0
    OR NVL(U.CIDADE_SANE, 0) <> 0   OR NVL(U.DISTRITO_SANE, 0) <> 0   OR NVL(U.LOGRADOURO_SANE, 0) <> 0
    OR NVL(U.CIDADE_SAUDE, 0) <> 0  OR NVL(U.DISTRITO_SAUDE, 0) <> 0  OR NVL(U.LOGRADOURO_SAUDE, 0) <> 0
    OR NVL(U.CIDADE_ESCOLA, 0) <> 0 OR NVL(U.DISTRITO_ESCOLA, 0) <> 0 OR NVL(U.LOGRADOURO_ESCOLA, 0) <> 0
    OR U.NOME_LEGAL IS NOT NULL OR U.CEP IS NOT NULL OR U.NUMERO_INI IS NOT NULL
    OR U.NUMERO_FIM IS NOT NULL OR U.LADO IS NOT NULL OR U.LOGRADOURO_DNE IS NOT NULL
    OR U.BAIRRO_DNE IS NOT NULL OR U.BAIRRO_DNE_FINAL IS NOT NULL;
COMMIT;

-- B6. Logradouros dos Correios que faltam na base (no print: 1)
INSERT INTO DBO_UNI_PESSOAS.LOGRADOUROS_UNIFICADO
       (ID, CIDADE_CORREIOS, DISTRITO_CORREIOS, LOGRADOURO_CORREIOS,
        CIDADE_PESSOA, DISTRITO_PESSOA, LOGRADOURO_PESSOA,
        CIDADE_RH, DISTRITO_RH, LOGRADOURO_RH,
        CIDADE_SANE, DISTRITO_SANE, LOGRADOURO_SANE,
        CIDADE_SAUDE, DISTRITO_SAUDE, LOGRADOURO_SAUDE,
        CIDADE_ESCOLA, DISTRITO_ESCOLA, LOGRADOURO_ESCOLA,
        BAIRRO, TIPO_LOGRADOURO, PREPOSICAO, TITULO_PATENTE,
        NOME, NOME_CORREIOS, NOME_REDUZIDO, COMPLEMENTO,
        CHAVE_NOME, CHAVE_TIPO, DATA_ALTERACAO, USUARIO_ALTERACAO, STATUS_COR)
SELECT DBO_UNI_PESSOAS.SEQ_LOGRADOUROS_UNIFICADO.NEXTVAL,
       L.CIDADE, L.DISTRITO, L.LOGRADOURO,
       0, 0, 0,  0, 0, 0,  0, 0, 0,  0, 0, 0,  0, 0, 0,
       L.BAIRRO, L.TIPO_LOGRADOURO, L.PREPOSICAO, L.TITULO_PATENTE,
       L.NOME,
       SUBSTR(NVL(L.NOME_COMPLETO, TRIM(L.TITULO_PATENTE || ' ' || L.PREPOSICAO || ' ' || L.NOME)), 1, 80),
       SUBSTR(L.NOME_REDUZIDO, 1, 90),
       L.COMPLEMENTO,
       DBO_UNI_PESSOAS.PKG_LOGRADOURO_CHAVE.CHAVE(L.TITULO_PATENTE, L.PREPOSICAO, L.NOME),
       DBO_UNI_PESSOAS.PKG_LOGRADOURO_CHAVE.TIPO(L.TIPO_LOGRADOURO),
       SYSDATE, NULL,
       CASE WHEN L.STATUS = 'N' THEN 'N' ELSE 'G' END
  FROM DBO_CORREIOS.LOGRADOUROS L
 WHERE NVL(L.CIDADE, 0) > 0
   AND NVL(L.LOGRADOURO, 0) > 0
   AND NOT EXISTS (SELECT 1 FROM DBO_UNI_PESSOAS.LOGRADOUROS_UNIFICADO LU
                    WHERE LU.CIDADE_CORREIOS = L.CIDADE
                      AND LU.DISTRITO_CORREIOS = L.DISTRITO
                      AND LU.LOGRADOURO_CORREIOS = L.LOGRADOURO);
COMMIT;

-- B7. Chaves vazias (no print: 1176)
UPDATE DBO_UNI_PESSOAS.LOGRADOUROS_UNIFICADO LU
   SET LU.CHAVE_NOME = DBO_UNI_PESSOAS.PKG_LOGRADOURO_CHAVE.CHAVE(LU.TITULO_PATENTE, LU.PREPOSICAO, LU.NOME),
       LU.CHAVE_TIPO = DBO_UNI_PESSOAS.PKG_LOGRADOURO_CHAVE.TIPO(LU.TIPO_LOGRADOURO)
 WHERE NVL(LU.CIDADE_CORREIOS, 0) > 0
   AND LU.NOME IS NOT NULL
   AND LU.CHAVE_NOME IS NULL;
COMMIT;

-- B8. Estatísticas
BEGIN
    DBMS_STATS.GATHER_TABLE_STATS('DBO_UNI_PESSOAS', 'LOGRADOUROS_UNIFICADO', CASCADE => TRUE);
END;
/

-- B9. Conferência: VINCULADOS = 0, SEM_CODIGO = 0, TOTAL próximo de 1.315.446
SELECT COUNT(*) TOTAL,
       SUM(CASE WHEN NVL(CIDADE_CORREIOS, 0) = 0 THEN 1 ELSE 0 END) SEM_CODIGO,
       SUM(CASE WHEN CHAVE_NOME IS NULL AND NOME IS NOT NULL THEN 1 ELSE 0 END) SEM_CHAVE,
       SUM(CASE WHEN NVL(LOGRADOURO_PESSOA, 0) + NVL(LOGRADOURO_SAUDE, 0)
                   + NVL(LOGRADOURO_RH, 0) + NVL(LOGRADOURO_ESCOLA, 0)
                   + NVL(LOGRADOURO_SANE, 0) > 0 THEN 1 ELSE 0 END) VINCULADOS
  FROM DBO_UNI_PESSOAS.LOGRADOUROS_UNIFICADO;

SELECT COUNT(*) CONTROLE FROM DBO_UNI_PESSOAS.LOGRADOUROS_UNIFICADO_CTRL;
