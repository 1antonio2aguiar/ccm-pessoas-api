/*
 * Consolidação dos endereços (fase 2) — permissões e conferências
 * Oracle 11g R1
 *
 * A tela "Consolidação" substitui SP_CIDADES_UNIFICA, SP_DISTRITOS_UNIFICA,
 * SP_BAIRROS_UNIFICA, SP_LOGRADOUROS_UNIFICA e SP_CEPS_UNIFICA.
 *
 * Diferenças importantes em relação às procedures:
 *   - não apaga nada (nem a consolidada, nem os itens STATUS = 'N' dos Correios);
 *   - não cria código (MAX + 1): tudo já tem código dos Correios desde a unificação;
 *   - nas linhas que já existem, só preenche colunas vazias;
 *   - não altera CODIGO_CCM.
 *
 * IMPORTANTE: depois disso, NÃO rodar mais as procedures *_UNIFICA: elas apagam
 * as tabelas consolidadas (e o CODIGO_CCM gravado pela carga final).
 */

-------------------------------------------------------------------------------
-- 1. Permissões (como DBO_UNI_PESSOAS)
-------------------------------------------------------------------------------
GRANT SELECT, INSERT, UPDATE ON DBO_UNI_PESSOAS.CIDADES     TO DBO_CCM_PESSOAS;
GRANT SELECT, INSERT, UPDATE ON DBO_UNI_PESSOAS.DISTRITOS   TO DBO_CCM_PESSOAS;
GRANT SELECT, INSERT, UPDATE ON DBO_UNI_PESSOAS.BAIRROS     TO DBO_CCM_PESSOAS;
GRANT SELECT, INSERT, UPDATE ON DBO_UNI_PESSOAS.LOGRADOUROS TO DBO_CCM_PESSOAS;
GRANT SELECT, INSERT, UPDATE ON DBO_UNI_PESSOAS.CEPS        TO DBO_CCM_PESSOAS;

-- Como DBO_CORREIOS:
GRANT SELECT  ON DBO_CORREIOS.CIDADES   TO DBO_CCM_PESSOAS;
GRANT SELECT  ON DBO_CORREIOS.DISTRITOS TO DBO_CCM_PESSOAS;

-- Como DBO_PESSOAS (tipo de logradouro gravado na consolidada, como na procedure):
-- GRANT SELECT ON DBO_PESSOAS.TIPOS_LOGRADOUROS TO DBO_CCM_PESSOAS;

-------------------------------------------------------------------------------
-- 2. Antes da primeira consolidação: o que mudaria
-------------------------------------------------------------------------------
-- A tela mostra os mesmos números por nível. Aqui, a conferência de logradouros
-- por amostra: linhas que JÁ estão na consolidada, comparando o nome gravado
-- pela procedure com o que a tela gravaria (a tela não sobrescreve, só mostra).

SELECT *
  FROM (SELECT L.CIDADE, L.DISTRITO, L.LOGRADOURO,
               L.TIPO_LOGRADOURO  TIPO_ATUAL,
               L.NOME             NOME_ATUAL,
               UPPER(LU.NOME)     NOME_UNIFICADO,
               L.CODIGO_CCM
          FROM DBO_UNI_PESSOAS.LOGRADOUROS L
          JOIN DBO_UNI_PESSOAS.LOGRADOUROS_UNIFICADO LU
            ON LU.CIDADE_CORREIOS     = L.CIDADE
           AND LU.DISTRITO_CORREIOS   = L.DISTRITO
           AND LU.LOGRADOURO_CORREIOS = L.LOGRADOURO
         WHERE NVL(L.NOME, '#') <> NVL(UPPER(LU.NOME), '#'))
 WHERE ROWNUM <= 100;

-------------------------------------------------------------------------------
-- 3. Depois da consolidação
-------------------------------------------------------------------------------

-- 3.1 Totais por nível: unificado (por chave) x consolidado
SELECT 'CIDADES' NIVEL,
       (SELECT COUNT(DISTINCT CIDADE_CORREIOS) FROM DBO_UNI_PESSOAS.CIDADES_UNIFICADO
         WHERE NVL(CIDADE_CORREIOS, 0) > 0) UNIFICADO,
       (SELECT COUNT(*) FROM DBO_UNI_PESSOAS.CIDADES) CONSOLIDADO
  FROM DUAL
UNION ALL
SELECT 'DISTRITOS',
       (SELECT COUNT(*) FROM (SELECT DISTINCT CIDADE_CORREIOS, DISTRITO_CORREIOS
                                FROM DBO_UNI_PESSOAS.DISTRITOS_UNIFICADO
                               WHERE NVL(CIDADE_CORREIOS, 0) > 0)),
       (SELECT COUNT(*) FROM DBO_UNI_PESSOAS.DISTRITOS)
  FROM DUAL
UNION ALL
SELECT 'BAIRROS',
       (SELECT COUNT(*) FROM (SELECT DISTINCT CIDADE_CORREIOS, DISTRITO_CORREIOS, BAIRRO_CORREIOS
                                FROM DBO_UNI_PESSOAS.BAIRROS_UNIFICADO
                               WHERE NVL(CIDADE_CORREIOS, 0) > 0)),
       (SELECT COUNT(*) FROM DBO_UNI_PESSOAS.BAIRROS)
  FROM DUAL
UNION ALL
SELECT 'LOGRADOUROS',
       (SELECT COUNT(*) FROM (SELECT DISTINCT CIDADE_CORREIOS, DISTRITO_CORREIOS, LOGRADOURO_CORREIOS
                                FROM DBO_UNI_PESSOAS.LOGRADOUROS_UNIFICADO
                               WHERE NVL(CIDADE_CORREIOS, 0) > 0)),
       (SELECT COUNT(*) FROM DBO_UNI_PESSOAS.LOGRADOUROS)
  FROM DUAL;

-- 3.2 Linhas da consolidada que não vieram do unificado (cargas antigas, MAX + 1).
--     A tela nunca apaga; avalie caso a caso.
SELECT 'CIDADES' NIVEL, COUNT(*) QTD
  FROM DBO_UNI_PESSOAS.CIDADES C
 WHERE NOT EXISTS (SELECT 1 FROM DBO_UNI_PESSOAS.CIDADES_UNIFICADO U
                    WHERE U.CIDADE_CORREIOS = C.CIDADE)
UNION ALL
SELECT 'DISTRITOS', COUNT(*)
  FROM DBO_UNI_PESSOAS.DISTRITOS D
 WHERE NOT EXISTS (SELECT 1 FROM DBO_UNI_PESSOAS.DISTRITOS_UNIFICADO U
                    WHERE U.CIDADE_CORREIOS = D.CIDADE
                      AND U.DISTRITO_CORREIOS = D.DISTRITO)
UNION ALL
SELECT 'BAIRROS', COUNT(*)
  FROM DBO_UNI_PESSOAS.BAIRROS B
 WHERE NOT EXISTS (SELECT 1 FROM DBO_UNI_PESSOAS.BAIRROS_UNIFICADO U
                    WHERE U.CIDADE_CORREIOS = B.CIDADE
                      AND U.DISTRITO_CORREIOS = B.DISTRITO
                      AND U.BAIRRO_CORREIOS = B.BAIRRO)
UNION ALL
SELECT 'LOGRADOUROS', COUNT(*)
  FROM DBO_UNI_PESSOAS.LOGRADOUROS L
 WHERE NOT EXISTS (SELECT 1 FROM DBO_UNI_PESSOAS.LOGRADOUROS_UNIFICADO U
                    WHERE U.CIDADE_CORREIOS = L.CIDADE
                      AND U.DISTRITO_CORREIOS = L.DISTRITO
                      AND U.LOGRADOURO_CORREIOS = L.LOGRADOURO);

-- 3.3 Logradouros consolidados com tipo não encontrado no PESSOAS ('NEC')
SELECT TIPO_LOGRAD_DESC, COUNT(*) QTD
  FROM DBO_UNI_PESSOAS.LOGRADOUROS
 WHERE TIPO_LOGRADOURO = 'NEC'
 GROUP BY TIPO_LOGRAD_DESC
 ORDER BY 2 DESC;

-- 3.4 Logradouros criados pela tela e os CEPs que receberam
SELECT L.CIDADE, L.DISTRITO, L.LOGRADOURO, L.TIPO_LOGRADOURO, L.NOME, C.CEP
  FROM DBO_CORREIOS.LOGRADOUROS L
  LEFT JOIN DBO_CORREIOS.CEPS C
    ON C.CIDADE = L.CIDADE
   AND C.DISTRITO = L.DISTRITO
   AND C.LOGRADOURO = L.LOGRADOURO
 WHERE L.STATUS = 'N'
 ORDER BY L.CIDADE, L.NOME;

-- 3.5 Logradouro criado SEM CEP (não deveria existir)
SELECT L.CIDADE, L.DISTRITO, L.LOGRADOURO, L.NOME
  FROM DBO_CORREIOS.LOGRADOUROS L
 WHERE L.STATUS = 'N'
   AND NOT EXISTS (SELECT 1 FROM DBO_CORREIOS.CEPS C
                    WHERE C.CIDADE = L.CIDADE
                      AND C.DISTRITO = L.DISTRITO
                      AND C.LOGRADOURO = L.LOGRADOURO);
