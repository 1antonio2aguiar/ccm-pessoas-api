/*
 * Unificação de logradouros — estruturas novas
 * Oracle 11g R1
 *
 * Rodar depois dos scripts 01 a 04.
 * Bloco 0 como DBO_CC_ENDERECOS; blocos 1 a 4 como DBO_UNI_PESSOAS;
 * bloco 5 como DBO_CORREIOS; bloco 6 com o dono de cada objeto (ou DBA).
 *
 * Observação: DBO_CC_ENDERECOS.TITULOS_PATENTES só tem ID e DESCRICAO
 * (não tem SIGLA). Por isso as abreviações ficam na tabela nova do bloco 1.
 *
 * IMPORTANTE: depois disso, NÃO rodar mais SP_LOGRADOUROS_SEPARA_NOME*,
 * SP_LOGRADOUROS_GERA_UNIFICADO nem SP_LOGRADOUROS_GERA_UNI_ESCOLA.
 */

-------------------------------------------------------------------------------
-- 0. Permissão usada pelo pacote  (DBO_CC_ENDERECOS)
-------------------------------------------------------------------------------
-- O pacote roda com os direitos do dono (DBO_UNI_PESSOAS) e precisa de grant
-- DIRETO (não via role) na tabela de tipos, antes de ser compilado.
GRANT SELECT ON DBO_CC_ENDERECOS.TIPOS_LOGRADOUROS TO DBO_UNI_PESSOAS;

-------------------------------------------------------------------------------
-- 1. Abreviações de títulos e patentes usadas na comparação  (DBO_UNI_PESSOAS)
-------------------------------------------------------------------------------
-- Substitui a lista fixa repetida dentro da SP_LOGRADOUROS_SEPARA_NOME.
-- ABREVIATURA e DESCRICAO são comparadas já normalizadas (sem acento e sem
-- pontuação): 'DR.' e 'DR' viram a mesma coisa. Pode incluir novas linhas à vontade.
-- Depois de alterar esta tabela:
--   1. reinicie a API (o pacote guarda a lista em memória por sessão);
--   2. recalcule as chaves:
--        UPDATE DBO_UNI_PESSOAS.LOGRADOUROS_UNIFICADO SET CHAVE_NOME = NULL, CHAVE_TIPO = NULL;
--        COMMIT;
--      e clique em "Completar base" na tela de logradouros (recalcula as chaves vazias).

CREATE TABLE DBO_UNI_PESSOAS.ABREVIATURAS_LOGRADOURO
(
    ABREVIATURA  VARCHAR2(30)  NOT NULL,
    DESCRICAO    VARCHAR2(60)  NOT NULL,
    CONSTRAINT PK_ABREVIATURAS_LOGRADOURO PRIMARY KEY (ABREVIATURA)
);

INSERT INTO DBO_UNI_PESSOAS.ABREVIATURAS_LOGRADOURO
SELECT 'ENG', 'ENGENHEIRO' FROM DUAL UNION ALL
SELECT 'ENGA', 'ENGENHEIRA' FROM DUAL UNION ALL
SELECT 'CAP', 'CAPITAO' FROM DUAL UNION ALL
SELECT 'FARM', 'FARMACEUTICO' FROM DUAL UNION ALL
SELECT 'VER', 'VEREADOR' FROM DUAL UNION ALL
SELECT 'TEN', 'TENENTE' FROM DUAL UNION ALL
SELECT 'CEL', 'CORONEL' FROM DUAL UNION ALL
SELECT 'EXP', 'EXPEDICIONARIO' FROM DUAL UNION ALL
SELECT 'MAJ', 'MAJOR' FROM DUAL UNION ALL
SELECT 'JORN', 'JORNALISTA' FROM DUAL UNION ALL
SELECT 'DEP', 'DEPUTADO' FROM DUAL UNION ALL
SELECT 'REV', 'REVERENDO' FROM DUAL UNION ALL
SELECT 'ENX', 'ENXADRISTA' FROM DUAL UNION ALL
SELECT 'DR', 'DOUTOR' FROM DUAL UNION ALL
SELECT 'DRA', 'DOUTORA' FROM DUAL UNION ALL
SELECT 'PROF', 'PROFESSOR' FROM DUAL UNION ALL
SELECT 'PROFA', 'PROFESSORA' FROM DUAL UNION ALL
SELECT 'PROFª', 'PROFESSORA' FROM DUAL UNION ALL
SELECT 'SGT', 'SARGENTO' FROM DUAL UNION ALL
SELECT 'PE', 'PADRE' FROM DUAL UNION ALL
SELECT 'DEL', 'DELEGADO' FROM DUAL UNION ALL
SELECT 'MAL', 'MARECHAL' FROM DUAL UNION ALL
SELECT 'GAL', 'GENERAL' FROM DUAL UNION ALL
SELECT 'PRES', 'PRESIDENTE' FROM DUAL UNION ALL
SELECT 'COM', 'COMANDANTE' FROM DUAL UNION ALL
SELECT 'CMTE', 'COMANDANTE' FROM DUAL UNION ALL
SELECT 'DESEMB', 'DESEMBARGADOR' FROM DUAL UNION ALL
SELECT 'PROM', 'PROMOTOR' FROM DUAL UNION ALL
SELECT 'GOV', 'GOVERNADOR' FROM DUAL UNION ALL
SELECT 'BRIG', 'BRIGADEIRO' FROM DUAL UNION ALL
SELECT 'ALM', 'ALMIRANTE' FROM DUAL UNION ALL
SELECT 'SEN', 'SENADOR' FROM DUAL UNION ALL
SELECT 'MONS', 'MONSENHOR' FROM DUAL UNION ALL
SELECT 'CONS', 'CONSELHEIRO' FROM DUAL UNION ALL
SELECT 'STA', 'SANTA' FROM DUAL UNION ALL
SELECT 'STO', 'SANTO' FROM DUAL UNION ALL
SELECT 'N SRA', 'NOSSA SENHORA' FROM DUAL UNION ALL
SELECT 'NSA SRA', 'NOSSA SENHORA' FROM DUAL UNION ALL
SELECT 'NS', 'NOSSA SENHORA' FROM DUAL;

COMMIT;

-------------------------------------------------------------------------------
-- 2. Pacote da chave de comparação  (DBO_UNI_PESSOAS)
-------------------------------------------------------------------------------
-- CHAVE(titulo, preposicao, nome):
--   normaliza com FN_NORMALIZA_TEXTO (sem acento, pontuação vira espaço),
--   expande abreviações (ABREVIATURAS_LOGRADOURO),
--   remove preposições soltas (DE, DA, DO, DAS, DOS, D).
--   'AV. DR. JOÃO-DE SOUZA' e 'Doutor Joao Souza' geram a mesma chave.
-- TIPO(tipo):
--   normaliza e expande a sigla pela DBO_CC_ENDERECOS.TIPOS_LOGRADOUROS
--   ('R' / 'R.' -> 'RUA', 'AV' -> 'AVENIDA').
--
-- O mesmo pacote é usado dos dois lados (origem e Correios): é isso que
-- corrige a diferença entre FN_NORMALIZA_TEXTO e os campos *_TRANSF.

CREATE OR REPLACE PACKAGE DBO_UNI_PESSOAS.PKG_LOGRADOURO_CHAVE AS

    FUNCTION CHAVE(
        P_TITULO      IN VARCHAR2,
        P_PREPOSICAO  IN VARCHAR2,
        P_NOME        IN VARCHAR2
    ) RETURN VARCHAR2 DETERMINISTIC;

    FUNCTION TIPO(
        P_TIPO IN VARCHAR2
    ) RETURN VARCHAR2 DETERMINISTIC;

    PROCEDURE RECARREGAR;

END PKG_LOGRADOURO_CHAVE;
/

CREATE OR REPLACE PACKAGE BODY DBO_UNI_PESSOAS.PKG_LOGRADOURO_CHAVE AS

    TYPE T_MAPA IS TABLE OF VARCHAR2(200) INDEX BY VARCHAR2(200);
    TYPE T_LISTA IS TABLE OF VARCHAR2(200) INDEX BY BINARY_INTEGER;

    G_PALAVRA      T_MAPA;   -- abreviação de uma palavra -> descrição
    G_FRASE_DE     T_LISTA;  -- abreviações com mais de uma palavra
    G_FRASE_PARA   T_LISTA;
    G_TIPO         T_MAPA;   -- sigla do tipo -> descrição do tipo
    G_CARREGADO    BOOLEAN := FALSE;

    FUNCTION NORM(P IN VARCHAR2) RETURN VARCHAR2 IS
    BEGIN
        RETURN DBO_UNI_PESSOAS.FN_NORMALIZA_TEXTO(P);
    END;

    PROCEDURE RECARREGAR IS
        V_DE   VARCHAR2(200);
        V_PARA VARCHAR2(200);
        N      PLS_INTEGER := 0;
    BEGIN
        G_PALAVRA.DELETE;
        G_FRASE_DE.DELETE;
        G_FRASE_PARA.DELETE;
        G_TIPO.DELETE;

        FOR R IN (SELECT ABREVIATURA, DESCRICAO
                    FROM DBO_UNI_PESSOAS.ABREVIATURAS_LOGRADOURO)
        LOOP
            V_DE   := NORM(R.ABREVIATURA);
            V_PARA := NORM(R.DESCRICAO);

            IF V_DE IS NOT NULL AND V_PARA IS NOT NULL AND V_DE <> V_PARA THEN
                IF INSTR(V_DE, ' ') > 0 THEN
                    N := N + 1;
                    G_FRASE_DE(N)   := ' ' || V_DE || ' ';
                    G_FRASE_PARA(N) := ' ' || V_PARA || ' ';
                ELSE
                    G_PALAVRA(V_DE) := V_PARA;
                END IF;
            END IF;
        END LOOP;

        FOR R IN (SELECT SIGLA, DESCRICAO
                    FROM DBO_CC_ENDERECOS.TIPOS_LOGRADOUROS
                   WHERE SIGLA IS NOT NULL
                     AND DESCRICAO IS NOT NULL)
        LOOP
            V_DE   := NORM(R.SIGLA);
            V_PARA := NORM(R.DESCRICAO);

            IF V_DE IS NOT NULL AND V_PARA IS NOT NULL
               AND INSTR(V_DE, ' ') = 0
               AND NOT G_TIPO.EXISTS(V_DE) THEN
                G_TIPO(V_DE) := V_PARA;
            END IF;
        END LOOP;

        G_CARREGADO := TRUE;
    END RECARREGAR;

    PROCEDURE GARANTE_CARGA IS
    BEGIN
        IF NOT G_CARREGADO THEN
            RECARREGAR;
        END IF;
    END;

    FUNCTION CHAVE(
        P_TITULO      IN VARCHAR2,
        P_PREPOSICAO  IN VARCHAR2,
        P_NOME        IN VARCHAR2
    ) RETURN VARCHAR2 DETERMINISTIC IS
        V_TXT    VARCHAR2(4000);
        V_SAIDA  VARCHAR2(4000);
        V_TOKEN  VARCHAR2(200);
        V_POS    PLS_INTEGER;
        V_TIT    VARCHAR2(400);
        V_NOME   VARCHAR2(4000);
    BEGIN
        GARANTE_CARGA;

        V_TIT  := NORM(P_TITULO);
        V_NOME := NORM(P_NOME);

        -- título já escrito no começo do nome: não repete
        IF V_TIT IS NOT NULL
           AND (V_NOME = V_TIT OR V_NOME LIKE V_TIT || ' %') THEN
            V_TIT := NULL;
        END IF;

        V_TXT := TRIM(V_TIT || ' ' || NORM(P_PREPOSICAO) || ' ' || V_NOME);

        IF V_TXT IS NULL THEN
            RETURN NULL;
        END IF;

        -- abreviações de mais de uma palavra (ex.: 'N SRA')
        IF G_FRASE_DE.COUNT > 0 THEN
            V_TXT := ' ' || V_TXT || ' ';
            FOR I IN 1 .. G_FRASE_DE.COUNT LOOP
                V_TXT := REPLACE(V_TXT, G_FRASE_DE(I), G_FRASE_PARA(I));
            END LOOP;
            V_TXT := TRIM(V_TXT);
        END IF;

        -- palavra a palavra: expande abreviação e descarta preposição
        V_TXT := V_TXT || ' ';

        WHILE V_TXT IS NOT NULL LOOP
            V_POS   := INSTR(V_TXT, ' ');
            V_TOKEN := SUBSTR(V_TXT, 1, V_POS - 1);
            V_TXT   := LTRIM(SUBSTR(V_TXT, V_POS + 1));

            IF V_TOKEN IS NOT NULL
               AND V_TOKEN NOT IN ('DE', 'DA', 'DO', 'DAS', 'DOS', 'D') THEN

                IF G_PALAVRA.EXISTS(V_TOKEN) THEN
                    V_TOKEN := G_PALAVRA(V_TOKEN);
                END IF;

                V_SAIDA := V_SAIDA || ' ' || V_TOKEN;
            END IF;

            EXIT WHEN V_TXT IS NULL;
        END LOOP;

        RETURN TRIM(V_SAIDA);
    END CHAVE;

    FUNCTION TIPO(
        P_TIPO IN VARCHAR2
    ) RETURN VARCHAR2 DETERMINISTIC IS
        V_TXT VARCHAR2(200);
    BEGIN
        GARANTE_CARGA;

        V_TXT := NORM(P_TIPO);

        IF V_TXT IS NULL THEN
            RETURN NULL;
        END IF;

        IF G_TIPO.EXISTS(V_TXT) THEN
            RETURN G_TIPO(V_TXT);
        END IF;

        RETURN V_TXT;
    END TIPO;

END PKG_LOGRADOURO_CHAVE;
/

-- Teste rápido (deve devolver duas linhas iguais):
SELECT DBO_UNI_PESSOAS.PKG_LOGRADOURO_CHAVE.CHAVE(NULL, NULL, 'DR. JOÃO-DE SOUZA') A,
       DBO_UNI_PESSOAS.PKG_LOGRADOURO_CHAVE.CHAVE('Doutor', NULL, 'Joao Souza') B,
       DBO_UNI_PESSOAS.PKG_LOGRADOURO_CHAVE.TIPO('AV.') C
  FROM DUAL;

-------------------------------------------------------------------------------
-- 2b. Chaves gravadas no unificado  (DBO_UNI_PESSOAS)
-------------------------------------------------------------------------------
-- São 1,3 milhão de logradouros: calcular a chave a cada consulta seria lento.
-- A chave fica gravada e indexada. A API preenche as chaves vazias ao
-- gerar/completar a base; o UPDATE abaixo adianta isso para a carga atual
-- (pode levar alguns minutos).

ALTER TABLE DBO_UNI_PESSOAS.LOGRADOUROS_UNIFICADO ADD
(
    CHAVE_NOME  VARCHAR2(200),
    CHAVE_TIPO  VARCHAR2(60)
);

UPDATE DBO_UNI_PESSOAS.LOGRADOUROS_UNIFICADO
   SET CHAVE_NOME = DBO_UNI_PESSOAS.PKG_LOGRADOURO_CHAVE.CHAVE(TITULO_PATENTE, PREPOSICAO, NOME),
       CHAVE_TIPO = DBO_UNI_PESSOAS.PKG_LOGRADOURO_CHAVE.TIPO(TIPO_LOGRADOURO)
 WHERE NVL(CIDADE_CORREIOS, 0) > 0;

COMMIT;

CREATE INDEX DBO_UNI_PESSOAS.IDX_LOGRADOUROS_UNIFICADO_7
    ON DBO_UNI_PESSOAS.LOGRADOUROS_UNIFICADO (CIDADE_CORREIOS, DISTRITO_CORREIOS, CHAVE_NOME);

-------------------------------------------------------------------------------
-- 3. Controle de duplicados e "não vincular"  (DBO_UNI_PESSOAS)
-------------------------------------------------------------------------------

CREATE TABLE DBO_UNI_PESSOAS.LOGRADOUROS_UNIFICADO_CTRL
(
    ID                 NUMBER(10)     NOT NULL,
    ORIGEM             VARCHAR2(10)   NOT NULL,
    CIDADE_ORIGEM      NUMBER(10)     NOT NULL,
    DISTRITO_ORIGEM    NUMBER(10)     NOT NULL,
    LOGRADOURO_ORIGEM  NUMBER(15)     NOT NULL,
    STATUS             CHAR(1)        NOT NULL,
    ID_UNIFICADO       NUMBER(10),
    MOTIVO             VARCHAR2(500),
    USUARIO            VARCHAR2(100),
    DATA_REGISTRO      DATE           DEFAULT SYSDATE NOT NULL,
    CONSTRAINT PK_LOGRADOUROS_UNIFICADO_CTRL PRIMARY KEY (ID),
    CONSTRAINT UK_LOGRADOUROS_UNIFICADO_CTRL UNIQUE (ORIGEM, CIDADE_ORIGEM, DISTRITO_ORIGEM, LOGRADOURO_ORIGEM),
    CONSTRAINT CK_LOGRADOUROS_UNIF_CTRL_ST CHECK (STATUS IN ('D', 'N'))
);

CREATE SEQUENCE DBO_UNI_PESSOAS.SEQ_LOGRADOUROS_UNIF_CTRL
    MINVALUE 1 MAXVALUE 9999999999 INCREMENT BY 1 START WITH 1 NOCACHE NOORDER NOCYCLE;

-------------------------------------------------------------------------------
-- 4. Índices  (DBO_UNI_PESSOAS)
-------------------------------------------------------------------------------
-- A tabela já tem índices por Correios, PESSOAS, RH, ESCOLA e SAÚDE; falta SANEAMENTO.
CREATE INDEX DBO_UNI_PESSOAS.IDX_LOGRADOUROS_UNIFICADO_6
    ON DBO_UNI_PESSOAS.LOGRADOUROS_UNIFICADO (CIDADE_SANE, DISTRITO_SANE, LOGRADOURO_SANE);

-- Conferência: SEQ_LOGRADOUROS_UNIFICADO precisa estar à frente do MAX(ID)
SELECT (SELECT MAX(ID) FROM DBO_UNI_PESSOAS.LOGRADOUROS_UNIFICADO) AS MAX_ID,
       S.LAST_NUMBER,
       CASE WHEN S.LAST_NUMBER > (SELECT NVL(MAX(ID), 0) FROM DBO_UNI_PESSOAS.LOGRADOUROS_UNIFICADO)
            THEN 'OK' ELSE 'AJUSTAR' END AS SITUACAO
  FROM ALL_SEQUENCES S
 WHERE S.SEQUENCE_OWNER = 'DBO_UNI_PESSOAS'
   AND S.SEQUENCE_NAME  = 'SEQ_LOGRADOUROS_UNIFICADO';

-------------------------------------------------------------------------------
-- 5. Logradouros criados fora dos Correios  (DBO_CORREIOS)
-------------------------------------------------------------------------------

DECLARE
    V_INICIO NUMBER;
BEGIN
    SELECT NVL(MAX(LOGRADOURO), 0) + 10000
      INTO V_INICIO
      FROM DBO_CORREIOS.LOGRADOUROS;

    EXECUTE IMMEDIATE
        'CREATE SEQUENCE DBO_CORREIOS.SEQ_LOGRADOUROS_FICTICIOS'
        || ' MINVALUE 1 MAXVALUE 9999999999 INCREMENT BY 1'
        || ' START WITH ' || V_INICIO
        || ' NOCACHE NOORDER NOCYCLE';
END;
/

-- CEPS_CARGA_DNE só tem a PK por ID. Índices para:
--   1. escolher um CEP válido da cidade na tela;
--   2. validar o CEP vindo da Saúde;
--   3. achar os CEPs de um logradouro na consolidação.
CREATE INDEX DBO_CORREIOS.IDX_CEPS_CARGA_DNE_1 ON DBO_CORREIOS.CEPS_CARGA_DNE (CIDADE, CEP);
CREATE INDEX DBO_CORREIOS.IDX_CEPS_CARGA_DNE_2 ON DBO_CORREIOS.CEPS_CARGA_DNE (CEP);
CREATE INDEX DBO_CORREIOS.IDX_CEPS_CARGA_DNE_3 ON DBO_CORREIOS.CEPS_CARGA_DNE (CIDADE, DISTRITO, LOGRADOURO);

-------------------------------------------------------------------------------
-- 6. Permissões
-------------------------------------------------------------------------------

-- Como DBO_UNI_PESSOAS, para o usuário da API:
GRANT EXECUTE ON DBO_UNI_PESSOAS.PKG_LOGRADOURO_CHAVE TO DBO_CCM_PESSOAS;
GRANT SELECT ON DBO_UNI_PESSOAS.ABREVIATURAS_LOGRADOURO TO DBO_CCM_PESSOAS;
GRANT SELECT, INSERT, UPDATE, DELETE ON DBO_UNI_PESSOAS.LOGRADOUROS_UNIFICADO      TO DBO_CCM_PESSOAS;
GRANT SELECT, INSERT, UPDATE, DELETE ON DBO_UNI_PESSOAS.LOGRADOUROS_UNIFICADO_CTRL TO DBO_CCM_PESSOAS;
GRANT SELECT ON DBO_UNI_PESSOAS.SEQ_LOGRADOUROS_UNIFICADO TO DBO_CCM_PESSOAS;
GRANT SELECT ON DBO_UNI_PESSOAS.SEQ_LOGRADOUROS_UNIF_CTRL TO DBO_CCM_PESSOAS;

-- Como DBO_CORREIOS:
GRANT SELECT         ON DBO_CORREIOS.BAIRROS        TO DBO_CCM_PESSOAS;
GRANT SELECT, INSERT ON DBO_CORREIOS.LOGRADOUROS    TO DBO_CCM_PESSOAS;
GRANT SELECT, INSERT ON DBO_CORREIOS.CEPS           TO DBO_CCM_PESSOAS;
GRANT SELECT         ON DBO_CORREIOS.CEPS_CARGA_DNE TO DBO_CCM_PESSOAS;
GRANT SELECT         ON DBO_CORREIOS.SEQ_LOGRADOUROS_FICTICIOS TO DBO_CCM_PESSOAS;
GRANT EXECUTE        ON DBO_CORREIOS.TRANSF_CARACT  TO DBO_CCM_PESSOAS;

-- Como dono de cada tabela de origem (sinônimos *_LOGRADOUROS e *_TIPOS_LOGRADOUROS):
-- DBO_PESSOAS, DBO_RH, DBO_SAUDE, DBO_CODAU_SANEAMENTO e DBO_ESCOLA
-- GRANT SELECT ON <dono>.LOGRADOUROS       TO DBO_CCM_PESSOAS;
-- GRANT SELECT ON <dono>.TIPOS_LOGRADOUROS TO DBO_CCM_PESSOAS;

-------------------------------------------------------------------------------
-- 7. Antes de usar a tela: situação da carga antiga (procedure)
-------------------------------------------------------------------------------
-- A procedure gravou logradouros de origem em linhas SEM código dos Correios
-- (status 'G' e 'D'). A tela só trabalha com linhas que têm código dos Correios;
-- se a contagem abaixo for relevante, use "Recomeçar do zero" na tela.

SELECT STATUS, COUNT(*) QTD
  FROM DBO_UNI_PESSOAS.LOGRADOUROS_UNIFICADO
 WHERE NVL(CIDADE_CORREIOS, 0) = 0
 GROUP BY STATUS
 ORDER BY STATUS;
