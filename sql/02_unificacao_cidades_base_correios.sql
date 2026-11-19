/*
 * Unificação de cidades — base dos Correios e "recomeçar do zero"
 * Oracle 11g R1
 *
 * Rodar depois do 01_unificacao_cidades.sql.
 */

-------------------------------------------------------------------------------
-- 1. Permissões adicionais para o usuário da API (DBO_CCM_PESSOAS)
-------------------------------------------------------------------------------

-- Como DBO_UNI_PESSOAS ("recomeçar do zero" apaga estas tabelas):
GRANT DELETE ON DBO_UNI_PESSOAS.CIDADES_UNIFICADO      TO DBO_CCM_PESSOAS;
GRANT DELETE ON DBO_UNI_PESSOAS.CIDADES_UNIFICADO_CTRL TO DBO_CCM_PESSOAS;

-- Como DBO_UNI_PESSOAS (consulta de uso antes de apagar cidades fictícias):
GRANT SELECT ON DBO_UNI_PESSOAS.DISTRITOS             TO DBO_CCM_PESSOAS;
GRANT SELECT ON DBO_UNI_PESSOAS.DISTRITOS_UNIFICADO   TO DBO_CCM_PESSOAS;
GRANT SELECT ON DBO_UNI_PESSOAS.BAIRROS_UNIFICADO     TO DBO_CCM_PESSOAS;
GRANT SELECT ON DBO_UNI_PESSOAS.LOGRADOUROS_UNIFICADO TO DBO_CCM_PESSOAS;

-- Como DBO_CORREIOS (apagar cidades fictícias sem uso — opcional na tela):
GRANT DELETE ON DBO_CORREIOS.CIDADES     TO DBO_CCM_PESSOAS;
GRANT SELECT ON DBO_CORREIOS.DISTRITOS   TO DBO_CCM_PESSOAS;
GRANT SELECT ON DBO_CORREIOS.BAIRROS     TO DBO_CCM_PESSOAS;
GRANT SELECT ON DBO_CORREIOS.LOGRADOUROS TO DBO_CCM_PESSOAS;

-------------------------------------------------------------------------------
-- 2. Conferência: cidades fictícias (STATUS = 'N') que repetem uma cidade real
-------------------------------------------------------------------------------
-- A procedure antiga criava cidade fictícia mesmo quando já tinha encontrado
-- a cidade. Estas repetições aparecem na tela como "Ambíguas".
-- A coluna EM_USO indica se a fictícia é referenciada (não pode ser apagada).

SELECT F.CIDADE                                   AS COD_FICTICIA,
       F.NOME                                     AS NOME_FICTICIA,
       F.ESTADO,
       R.CIDADE                                   AS COD_REAL,
       R.NOME                                     AS NOME_REAL,
       CASE
           WHEN EXISTS (SELECT 1 FROM DBO_CORREIOS.DISTRITOS D WHERE D.CIDADE = F.CIDADE)
             OR EXISTS (SELECT 1 FROM DBO_CORREIOS.BAIRROS B WHERE B.CIDADE = F.CIDADE)
             OR EXISTS (SELECT 1 FROM DBO_CORREIOS.LOGRADOUROS L WHERE L.CIDADE = F.CIDADE)
             OR EXISTS (SELECT 1 FROM DBO_UNI_PESSOAS.DISTRITOS DD WHERE DD.CIDADE = F.CIDADE)
             OR EXISTS (SELECT 1 FROM DBO_UNI_PESSOAS.DISTRITOS_UNIFICADO DU WHERE DU.CIDADE_CORREIOS = F.CIDADE)
             OR EXISTS (SELECT 1 FROM DBO_UNI_PESSOAS.BAIRROS_UNIFICADO BU WHERE BU.CIDADE_CORREIOS = F.CIDADE)
             OR EXISTS (SELECT 1 FROM DBO_UNI_PESSOAS.LOGRADOUROS_UNIFICADO LU WHERE LU.CIDADE_CORREIOS = F.CIDADE)
           THEN 'SIM'
           ELSE 'NAO'
       END                                        AS EM_USO
  FROM DBO_CORREIOS.CIDADES F
  JOIN DBO_CORREIOS.CIDADES R
    ON R.STATUS <> 'N' OR R.STATUS IS NULL
 WHERE F.STATUS = 'N'
   AND UPPER(TRIM(R.ESTADO)) = UPPER(TRIM(F.ESTADO))
   AND DBO_UNI_PESSOAS.FN_NORMALIZA_CIDADE(R.NOME) = DBO_UNI_PESSOAS.FN_NORMALIZA_CIDADE(F.NOME)
 ORDER BY F.ESTADO, F.NOME;
