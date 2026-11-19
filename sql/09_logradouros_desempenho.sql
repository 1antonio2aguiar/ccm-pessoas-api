/*
 * Logradouros — desempenho do "Recomeçar do zero" e da geração da base
 * Oracle 11g R1
 */

-------------------------------------------------------------------------------
-- 0. O zerar que está rodando agora (como DBA ou usuário com acesso às V$)
-------------------------------------------------------------------------------
-- Sessão da API trabalhando:
SELECT S.SID, S.SERIAL#, S.STATUS, S.LAST_CALL_ET SEGUNDOS_NO_COMANDO,
       SUBSTR(Q.SQL_TEXT, 1, 80) COMANDO
  FROM V$SESSION S
  LEFT JOIN V$SQL Q
    ON Q.SQL_ID = S.SQL_ID
   AND Q.CHILD_NUMBER = S.SQL_CHILD_NUMBER
 WHERE S.USERNAME = 'DBO_CCM_PESSOAS'
   AND S.STATUS = 'ACTIVE';

-- Tamanho da transação aberta (linhas de undo; vai crescendo enquanto trabalha):
SELECT S.SID, T.USED_UREC LINHAS_UNDO, T.START_TIME
  FROM V$TRANSACTION T
  JOIN V$SESSION S
    ON S.TADDR = T.ADDR
 WHERE S.USERNAME = 'DBO_CCM_PESSOAS';

-- Se decidir interromper: o Oracle desfaz tudo (rollback), o que pode levar
-- quase o mesmo tempo que já rodou. Nada fica pela metade.
-- ALTER SYSTEM KILL SESSION '<sid>,<serial#>' IMMEDIATE;

-------------------------------------------------------------------------------
-- 1. Cache nas sequences (como DBO_UNI_PESSOAS)
-------------------------------------------------------------------------------
-- Sem cache, cada NEXTVAL grava no dicionário: em 1,3 milhão de linhas isso
-- sozinho custa muitos minutos. As sequences dos Correios (códigos fictícios)
-- continuam sem cache.
ALTER SEQUENCE DBO_UNI_PESSOAS.SEQ_LOGRADOUROS_UNIFICADO   CACHE 1000;
ALTER SEQUENCE DBO_UNI_PESSOAS.SEQ_LOGRADOUROS_UNIF_CTRL   CACHE 100;
ALTER SEQUENCE DBO_UNI_PESSOAS.SEQ_BAIRROS_UNIFICADO       CACHE 1000;
ALTER SEQUENCE DBO_UNI_PESSOAS.SEQ_BAIRROS_UNIFICADO_CTRL  CACHE 100;

-------------------------------------------------------------------------------
-- 2. Estatísticas (como DBO_UNI_PESSOAS), depois que o zerar atual terminar
-------------------------------------------------------------------------------
BEGIN
    DBMS_STATS.GATHER_TABLE_STATS(
        OWNNAME => 'DBO_UNI_PESSOAS',
        TABNAME => 'LOGRADOUROS_UNIFICADO',
        CASCADE => TRUE
    );

    DBMS_STATS.GATHER_TABLE_STATS(
        OWNNAME => 'DBO_UNI_PESSOAS',
        TABNAME => 'LOGRADOUROS_UNIFICADO_CTRL',
        CASCADE => TRUE
    );
END;
/

-------------------------------------------------------------------------------
-- 3. Conferência depois do zerar
-------------------------------------------------------------------------------
SELECT COUNT(*) TOTAL,
       SUM(CASE WHEN NVL(CIDADE_CORREIOS, 0) = 0 THEN 1 ELSE 0 END) SEM_CODIGO_CORREIOS,
       SUM(CASE WHEN CHAVE_NOME IS NULL AND NOME IS NOT NULL THEN 1 ELSE 0 END) SEM_CHAVE,
       SUM(CASE WHEN NVL(LOGRADOURO_PESSOA, 0) + NVL(LOGRADOURO_SAUDE, 0)
                   + NVL(LOGRADOURO_RH, 0) + NVL(LOGRADOURO_ESCOLA, 0)
                   + NVL(LOGRADOURO_SANE, 0) > 0 THEN 1 ELSE 0 END) VINCULADOS
  FROM DBO_UNI_PESSOAS.LOGRADOUROS_UNIFICADO;
