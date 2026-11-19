/*
 * Bairros — correção de desempenho da lista de pendentes
 * Oracle 11g R1 — rodar como DBO_UNI_PESSOAS
 *
 * Problema: a tela de bairros ficava em "Carregando bairros..." porque a
 * consulta chamava FN_NORMALIZA_CIDADE em todos os ~90 mil bairros de
 * BAIRROS_UNIFICADO a cada listagem.
 *
 * Correção: o nome normalizado fica gravado em NOME_NORM, com índice.
 * A API preenche a coluna nos bairros novos (base e "criar bairro");
 * o UPDATE abaixo preenche a carga atual.
 *
 * Pode rodar com a API no ar. Nenhum vínculo é alterado.
 */

-- 1. Coluna
ALTER TABLE DBO_UNI_PESSOAS.BAIRROS_UNIFICADO ADD (NOME_NORM VARCHAR2(200));

-- 2. Preenche a carga atual (1 a 2 minutos)
UPDATE DBO_UNI_PESSOAS.BAIRROS_UNIFICADO
   SET NOME_NORM = DBO_UNI_PESSOAS.FN_NORMALIZA_CIDADE(NOME)
 WHERE NOME IS NOT NULL;

COMMIT;

-- 3. Índice usado na comparação (cidade + distrito dos Correios + nome)
CREATE INDEX DBO_UNI_PESSOAS.IDX_BAIRROS_UNIFICADO_NORM
    ON DBO_UNI_PESSOAS.BAIRROS_UNIFICADO (CIDADE_CORREIOS, DISTRITO_CORREIOS, NOME_NORM);

-- 4. Estatísticas atualizadas, para o Oracle escolher o plano certo
BEGIN
    DBMS_STATS.GATHER_TABLE_STATS(
        OWNNAME => 'DBO_UNI_PESSOAS',
        TABNAME => 'BAIRROS_UNIFICADO',
        CASCADE => TRUE
    );

    DBMS_STATS.GATHER_TABLE_STATS(
        OWNNAME => 'DBO_UNI_PESSOAS',
        TABNAME => 'BAIRROS_UNIFICADO_CTRL',
        CASCADE => TRUE
    );

    DBMS_STATS.GATHER_TABLE_STATS(
        OWNNAME => 'DBO_UNI_PESSOAS',
        TABNAME => 'DISTRITOS_UNIFICADO',
        CASCADE => TRUE
    );
END;
/

-- 5. Conferência: deve dar 0
SELECT COUNT(*) SEM_NOME_NORM
  FROM DBO_UNI_PESSOAS.BAIRROS_UNIFICADO
 WHERE NOME IS NOT NULL
   AND NOME_NORM IS NULL;
