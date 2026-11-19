# Unificação de Endereços — pacote de implementação (v7)

**Novo na v7:** tela de **Carga final** (fase 3), que substitui `SP_CARGA_ENDERECOS` e `SP_CARGA_CEPS`, e o script `11_carga_final_enderecos.sql`. Detalhes no fim deste arquivo.


**Novo na v6:**
- unificação de **Logradouros**, com escolha de CEP válido para logradouro novo;
- tela de **Consolidação** (fase 2), que substitui as procedures `SP_*_UNIFICA`;
- scripts `06_unificacao_logradouros.sql` e `07_consolidacao_enderecos.sql`.

O pacote traz de novo cidades, distritos e bairros. Bairros não mudou desde a v5 e ainda não tinha sido aplicado.

**Desde a v5:** unificação de **Bairros** e script de conferência `05`. **Desde a v4:** **Distritos**. **Desde a v2:** "Base dos Correios" com **Gerar base** e **Recomeçar do zero**.

## Sequência completa (o que substitui cada procedure)

| Fase | Tela (menu Unifica Endereços) | Substitui | Script |
|---|---|---|---|
| 1. Unificação | Cidades | `SP_CIDADES_GERA_UNIFICADO` | 01, 02 |
| 1. Unificação | Distritos | `SP_DISTRITOS_GERA_UNIFICADO` | 03 |
| 1. Unificação | Bairros | `SP_BAIRROS_GERA_UNIFICADO` | 04 |
| 1. Unificação | Logradouros | `SP_LOGRADOUROS_SEPARA_NOME*`, `SP_LOGRADOUROS_GERA_UNIFICADO`, `SP_LOGRADOUROS_GERA_UNI_ESCOLA` | 06 |
| 2. Consolidação | Consolidação | `SP_CIDADES_`, `SP_DISTRITOS_`, `SP_BAIRROS_`, `SP_LOGRADOUROS_`, `SP_CEPS_UNIFICA` | 07 |
| 3. Tabelas finais | Carga final | `SP_CARGA_ENDERECOS`, `SP_CARGA_CEPS` | 11 |

O script `05` é só de conferência e pode ser rodado a qualquer momento.

**Correção v6.1 (bairros):** a lista de pendentes de bairros ficava em "Carregando..." para sempre. Rode `08_bairros_desempenho.sql` como `DBO_UNI_PESSOAS`. Ele grava o nome normalizado em `BAIRROS_UNIFICADO.NOME_NORM`, cria o índice e atualiza as estatísticas. Depois, substitua os 3 arquivos Java de `unificacao/bairro` e reinicie a API.

**Correção v6.2 (lote de bairros e logradouros):** "Vincular todos os únicos" dava "0 Unknown Error" com muitos pendentes. O lote tratava um registro por vez e a requisição passava de horas. Agora são três comandos SQL numa transação, que levam segundos ou poucos minutos:
1. pendentes que apontam para o mesmo unificado: o de menor código é vinculado, os demais vão para o controle como duplicados (D);
2. grava o código da origem;
3. completa as colunas vazias.

O resultado é o mesmo do processamento um a um. Se algo falhar, nada é gravado.

**Correção v6.3 ("Recomeçar do zero" de logradouros):** passava de 20 minutos porque apagava e reinseria as 1,3 milhão de linhas, recalculando as chaves. Agora o zerar desfaz só os vínculos: zera os códigos das origens e os complementos nas linhas que tinham algo, apaga o controle, as linhas sem código dos Correios e as repetidas. Os logradouros dos Correios e as chaves ficam. Rode também o `09_logradouros_desempenho.sql`, que põe cache nas sequences e atualiza as estatísticas.

Para começar do zero: em cada tela, na ordem cidades → distritos → bairros → logradouros, clique em **Recomeçar do zero**, vincule os únicos e resolva o resto. Depois rode **Consolidar tudo** e, por fim, **Carregar tudo** na Carga final.

Para só acrescentar novos: o mesmo caminho, mas com **Gerar base / Completar base** no lugar de "Recomeçar do zero".

> ⚠️ A partir da v6, **nenhuma** das procedures da tabela acima deve ser rodada. As `*_UNIFICA` começam com `DELETE` nas tabelas consolidadas, o que apaga o `CODIGO_CCM` gravado pela fase 3.

Este pacote contém **só os arquivos novos e os alterados**, na mesma estrutura de pastas dos projetos.
Para aplicar, descompacte por cima da raiz de cada projeto:

- `cc-pessoas-api/` → raiz da API
- `cc-pessoas-ui/` → raiz da UI

## Ordem para testar

1. **Banco** — rode `cc-pessoas-api/sql/01_unificacao_cidades.sql` e depois `02_unificacao_cidades_base_correios.sql`. Cada bloco indica com qual usuário rodar.
   - Cria a tabela `DBO_UNI_PESSOAS.CIDADES_UNIFICADO_CTRL` e a sequence dela.
   - Cria a sequence `DBO_CORREIOS.SEQ_CIDADES_FICTICIAS` a partir do `MAX(CIDADE) + 1` atual.
   - Confere se a `SEQ_CIDADES_UNIFICADO` está à frente do `MAX(ID)`.
   - Lista os grants necessários para o usuário da API (`DBO_CCM_PESSOAS`).
2. **API** — compile e suba normalmente. A pasta `unificacao/cidade` é nova, e nenhuma classe existente foi alterada.
3. **UI** — `npm start`. O menu novo fica em **Atividades → Unifica Endereços → Cidades**.

## Fluxo na tela

**Passo 1 — Base dos Correios** (painel do topo):
- **Gerar base (acrescentar novas):** insere em `CIDADES_UNIFICADO` as cidades de `DBO_CORREIOS.CIDADES` que ainda não estão lá, com os códigos de todos os sistemas zerados. Não altera nem apaga nada e pode ser rodado quantas vezes quiser.
- **Recomeçar do zero:** apaga `CIDADES_UNIFICADO` e `CIDADES_UNIFICADO_CTRL` e gera a base de novo, tudo numa única transação. Exige digitar **ZERAR**.
  - Opcionalmente, apaga também as cidades fictícias (`STATUS = 'N'`) dos Correios que **não estão em uso**. São consideradas em uso as que aparecem em distritos, bairros ou logradouros dos Correios, em `DBO_UNI_PESSOAS.DISTRITOS` ou nos de-paras unificados.
  - As fictícias em uso voltam para a base com o mesmo código.
  - Para desligar a opção em produção: `unificacao.cidades.permitir-zerar=false`. Já está incluída no `application-prod.properties` deste pacote.

**Passo 2 — Unificação por sistema de origem:** escolha a origem e siga esta ordem:
1. **Vincular todas as únicas.**
2. Resolver as **Ambíguas**.
3. Resolver as **Sem correspondência**: pesquisar, criar cidade nova ou marcar não vincular.

> Antes de zerar, rode a consulta da seção 2 do script `02`. Ela lista as cidades fictícias que repetem uma cidade real, efeito de um bug da procedure antiga, e mostra se cada uma está em uso. As repetidas aparecem na tela como "Ambíguas".

> ⚠️ A partir daqui, **não rode mais a `SP_CIDADES_GERA_UNIFICADO`**. Ela apaga a `CIDADES_UNIFICADO` e as cidades, distritos, bairros e logradouros `STATUS = 'N'` dos Correios, o que desfaz o que a tela gravou.

## Arquivos

### API — novos
```
src/main/java/br/com/cc/pessoas/unificacao/cidade/
├── cidController/CidadeUnificadoController.java
├── cidDto/CidadeBaseCorreiosResultadoDTO.java
├── cidDto/CidadeBaseCorreiosResumoDTO.java
├── cidDto/CidadeCandidatoDTO.java
├── cidDto/CidadeOrigemDTO.java
├── cidDto/CidadeUnificadoLoteDTO.java
├── cidDto/CidadeUnificadoResumoDTO.java
├── cidDto/OrigemCidadeDTO.java
├── cidEnum/OrigemCidade.java          ← configuração de cada sistema de origem
├── cidFilter/CidadeUnificadoFilter.java
├── cidRepository/CidadeBaseCorreiosRepository.java
├── cidRepository/CidadeUnificadoRepository.java
├── cidService/CidadeBaseCorreiosService.java
└── cidService/CidadeUnificadoService.java
sql/01_unificacao_cidades.sql
sql/02_unificacao_cidades_base_correios.sql
```

### API — alterado
- `src/main/resources/application-prod.properties` — acrescentada a linha `unificacao.cidades.permitir-zerar=false`

### UI — novos
```
src/app/pages/unificacao/enderecos/cidades/
├── cidade-unificado.module.ts
├── cidade-unificado-routing.module.ts
├── cidade-unificado.component.ts
├── cidade-unificado.service.ts
├── cidade-unificado-iud/          (tela principal)
├── cidade-candidatos-dialog/      (diálogo de análise da cidade)
└── zerar-base-dialog/             (confirmação de "Recomeçar do zero")
```

### UI — alterados (só acréscimos)
- `src/app/pages/pages-menu.ts` — item "Unifica Endereços → Cidades"
- `src/app/pages/pages-routing.module.ts` — rota `unificacao/enderecos/cidades`

## Endpoints

Base: `/cidades-unificado`. `{origem}` = `PES`, `RH`, `SANE`, `SAU` ou `ESCO`.

| Método | Caminho | O que faz |
|---|---|---|
| GET | `/base-correios/resumo` | Totais dos Correios e da base unificada |
| POST | `/base-correios/gerar` | Acrescenta as cidades dos Correios que faltam |
| POST | `/base-correios/zerar?confirmacao=ZERAR&apagarFicticias=false` | Apaga tudo e gera a base de novo |
| GET | `/origens` | Lista as origens configuradas |
| GET | `/{origem}/resumo` | Totais da origem e pendentes por situação |
| GET | `/{origem}/pendentes?cidade&nome&uf&situacao&page&size` | Cidades ainda não unificadas, com sugestão |
| GET | `/{origem}/candidatos/{cidade}?pesquisa&uf` | Candidatos automáticos ou pesquisa manual |
| POST | `/{origem}/vincular/{cidade}?idUnificado=` | Grava o código da origem na cidade unificada |
| POST | `/{origem}/vincular-unicas` | Vincula todas as pendentes com situação ÚNICA |
| POST | `/{origem}/criar/{cidade}` | Cria a cidade nos Correios (`STATUS 'N'`) e no unificado |
| POST | `/{origem}/nao-vincular/{cidade}?motivo=` | Retira a cidade da lista |

## Regras implementadas

**Candidato.** Uma cidade de `CIDADES_UNIFICADO` é candidata quando tem a mesma UF da cidade de origem e:
- o mesmo nome por `FN_NORMALIZA_CIDADE`, ou
- o mesmo código IBGE, quando a origem tem IBGE (hoje só o RH).

**Situação de cada cidade pendente:**

| Situação | Condição | Ação na tela |
|---|---|---|
| ÚNICA | 1 candidato, ainda sem código desta origem | Pode ir no lote |
| DUPLICADA | 1 candidato que já tem outro código desta origem | Ao vincular, vira registro `D` no controle |
| AMBÍGUA | Mais de 1 candidato | O usuário escolhe |
| SEM CORRESPONDÊNCIA | Nenhum candidato | O usuário pesquisa outra cidade ou cria uma nova |

**O que mudou em relação à procedure:**
- **Processamento incremental.** Nada é apagado. O `CIDADE_CORREIOS` já gravado não muda.
- **Nenhum código sobrescrito.** O `UPDATE` só grava se a coluna da origem estiver vazia (null ou 0). Um código duplicado vai para `CIDADES_UNIFICADO_CTRL` com `STATUS = 'D'`.
- **Complementa sem sobrescrever.** DDD, CEP, IBGE, SICOM e INEP só são preenchidos quando estão vazios. Isso corrige a perda de DDD, SICOM e IBGE que acontecia na procedure.
- **Cidade nova só sem correspondência.** Ela só é criada quando não existe candidato (a API valida). O código vem de sequence, não de `MAX + 1`.
- **Lote seguro.** O lote lê a lista uma vez e grava cada cidade numa transação própria. Um erro não desfaz as outras cidades, e não há risco de laço infinito.
- **Origem nova sem tela nova.** Basta acrescentar uma constante em `OrigemCidade`.

## Premissas (conferir no ambiente)

1. **Sinônimos.** A API usa os sinônimos qualificados pelo schema: `DBO_UNI_PESSOAS.PES_CIDADES`, `RH_CIDADES`, `SANE_CIDADES`, `SAU_CIDADES` e `ESCO_CIDADES`. Se eles estiverem em outro schema, ajuste `OrigemCidade.SCHEMA_UNIFICADO`.
2. **Colunas das tabelas de origem.** Todas têm `CIDADE`, `NOME` e `ESTADO`. As demais colunas foram tiradas da procedure:

   | Origem | Colunas além de CIDADE, NOME e ESTADO |
   |---|---|
   | PES | CEP, DDD, CIDADE_SICOM |
   | RH | CEP, DDD, CODIGO_IBGE |
   | SANE | CEP, DDD |
   | SAU | DDD |
   | ESCO | CEP, DDD, CIDADE_SICOM, CIDADE_INEP |

   Se alguma origem não tiver uma dessas colunas, troque a expressão para `"null"` no enum.
3. **Correios.** A tabela `DBO_CORREIOS.CIDADES` tem as colunas `CIDADE, NOME, ESTADO, CEP, CD_IBGE, STATUS`.
4. **STATUS_COR das cidades criadas pela tela:**

   | Origem | STATUS_COR |
   |---|---|
   | PES | `P` |
   | RH | `R` |
   | SANE | `S` |
   | SAU | `A` |
   | ESCO | `E` |

   `P`, `R` e `E` são os mesmos da procedure. `S` e `A` são novos.
5. **Usuário.** `USUARIO_ALTERACAO` e `USUARIO` ficam nulos, porque a API ainda não tem autenticação (pendência nº 3).
6. **Colunas das tabelas de distrito, bairro e logradouro.** Na verificação de uso das fictícias, considerei que `DBO_CORREIOS.DISTRITOS`, `BAIRROS` e `LOGRADOUROS` e `DBO_UNI_PESSOAS.DISTRITOS` têm a coluna `CIDADE`, e que os `*_UNIFICADO` têm `CIDADE_CORREIOS`.
7. **STATUS_COR da base gerada:** `G` para cidade dos Correios e `N` para cidade fictícia.
8. **DDD na base gerada.** O DDD fica vazio e é preenchido ao vincular a primeira origem que tenha DDD.
9. **Tabela de controle.** A `CIDADES_UNIFICADO_AUXILIAR` não foi usada, porque não sei o conteúdo dela. O controle de duplicados ficou na tabela nova.


---

# Distritos (novo na v4)

## Aplicar

1. **Banco:** rode `cc-pessoas-api/sql/03_unificacao_distritos.sql`. Cada bloco indica com qual usuário rodar. O script:
   - cria a tabela `DISTRITOS_UNIFICADO_CTRL` e a sequence dela;
   - cria a sequence `DBO_CORREIOS.SEQ_DISTRITOS_FICTICIOS`;
   - lista os grants e duas consultas de conferência.
2. **API:** a pasta `unificacao/distrito` é nova.
3. **UI:** o menu fica em **Atividades → Unifica Endereços → Distritos**.

> ⚠️ Não rode mais a `SP_DISTRITOS_GERA_UNIFICADO`. Ela faz `TRUNCATE` em `DISTRITOS_UNIFICADO` e muda o `STATUS` de todos os distritos dos Correios.

## Ordem de trabalho

1. **Cidades primeiro.** Para achar a cidade dos Correios, o distrito usa o de-para de cidades: o código gravado em `CIDADES_UNIFICADO` ou registrado como duplicado em `CIDADES_UNIFICADO_CTRL`. Distrito cuja cidade não está unificada aparece como **Cidade não unificada** e não pode ser vinculado.
2. **Base dos Correios de distritos:** "Gerar base" ou "Recomeçar do zero".
3. **Por origem:** "Vincular todos os únicos", depois resolva os ambíguos e os sem correspondência pelo diálogo.

## Regra de correspondência

Um distrito da origem só é comparado com os distritos **da mesma cidade dos Correios**, pelo nome normalizado (`FN_NORMALIZA_CIDADE`).

| Situação | Condição |
|---|---|
| CIDADE_NAO_UNIFICADA | A cidade do distrito não está unificada, ou está unificada com mais de uma cidade dos Correios |
| UNICA | Um distrito com o mesmo nome na cidade, ainda sem código desta origem |
| DUPLICADA | Um distrito com o mesmo nome, mas ele já tem outro código desta origem. Ao vincular, vai para o controle com `STATUS = 'D'` |
| AMBIGUA | Mais de um distrito com o mesmo nome na cidade |
| SEM_CORRESPONDENCIA | Nenhum distrito com o mesmo nome na cidade. No diálogo dá para pesquisar, listar todos os distritos da cidade ou criar o distrito novo |

## Endpoints

Base: `/distritos-unificado`. O distrito da origem é `{cidade}/{distrito}`.

| Método | Caminho | O que faz |
|---|---|---|
| GET | `/base-correios/resumo` | Totais dos Correios e da base |
| POST | `/base-correios/gerar` | Acrescenta os distritos que faltam |
| POST | `/base-correios/zerar?confirmacao=ZERAR` | Apaga `DISTRITOS_UNIFICADO` e o controle e gera a base de novo |
| GET | `/origens` | Lista as origens |
| GET | `/{origem}/resumo` | Totais por situação |
| GET | `/{origem}/pendentes?cidade&distrito&nome&nomeCidade&uf&situacao&page&size` | Distritos pendentes, com sugestão |
| GET | `/{origem}/candidatos/{cidade}/{distrito}?pesquisa&todos` | Candidatos na mesma cidade |
| POST | `/{origem}/vincular/{cidade}/{distrito}?idUnificado=` | Grava o par cidade/distrito da origem |
| POST | `/{origem}/vincular-unicos` | Lote dos únicos |
| POST | `/{origem}/criar/{cidade}/{distrito}` | Cria o distrito nos Correios (`STATUS 'N'`) e no unificado |
| POST | `/{origem}/nao-vincular/{cidade}/{distrito}?motivo=` | Retira o distrito da lista |

## O que muda em relação à procedure

- **Compara só dentro da mesma cidade.** Nos remanescentes de RH e Saúde, a procedure comparava só por nome + UF e podia vincular a um distrito homônimo de outra cidade.
- **Não duplica linhas.** Nos remanescentes do PESSOAS, a procedure inseria uma linha nova mesmo quando o distrito dos Correios já existia na base. Assim, o mesmo distrito ficava em duas linhas.
- **Não apaga o que já estava vinculado.** Nos remanescentes da Saúde, a procedure sobrescrevia `CIDADE_PESSOA`, `DISTRITO_PESSOA`, `CIDADE_RH` e `DISTRITO_RH` com zero quando não achava o distrito.
- **Grava cada código uma vez só.** No RH, quando vários distritos batiam pelo nome, a procedure gravava o mesmo código em todos. No duplicado, ela registrava o código antigo e perdia o novo.
- **Não cria distrito sem cidade.** A procedure criava distrito nos Correios com `CIDADE = 0` quando a cidade não estava unificada.
- **Não altera o STATUS dos Correios.** A procedure mudava todos para `'I'`/`'L'` e assim perdia a marca `'N'` dos distritos fictícios.
- **Código do distrito novo por sequence.** Antes era `MAX + 10000` por cidade.
- **Complementa sem sobrescrever.** `DISTANCIA` (PESSOAS) e `CEP` (Saúde) só são preenchidos quando estão vazios. Antes o CEP da Saúde era zerado pela busca do Saneamento.
- **Inclui Saneamento e Escola** como origens, com a mesma tela.

## Premissas de distritos (conferir no ambiente)

1. **Colunas das tabelas de origem.** Os sinônimos `PES_`, `RH_`, `SANE_`, `SAU_` e `ESCO_DISTRITOS` ficam em `DBO_UNI_PESSOAS` e têm `CIDADE`, `DISTRITO` e `NOME`. A `PES_DISTRITOS` tem `DISTANCIA` e a `SAU_DISTRITOS` tem `CEP`. A UF e o nome da cidade vêm da tabela de cidades da mesma origem.
2. **Colunas dos Correios.** A `DBO_CORREIOS.DISTRITOS` tem `CIDADE, DISTRITO, NOME, ESTADO, TIPO_LOCALIDADE, STATUS`, os mesmos nomes do `INSERT` da procedure.
3. **Distritos fictícios antigos.** Como a procedure trocou o `STATUS` de todos os distritos dos Correios, os fictícios antigos entram na base como `G`. Só os criados pela tela daqui para frente ficam marcados como `N`.
4. **Código CCM do distrito.** A migração de pessoas busca o código CCM em `DBO_UNI_PESSOAS.DISTRITOS` (`codigo_ccm`). Um distrito novo criado pela tela só existe em `DBO_CORREIOS.DISTRITOS`. Se as duas tabelas forem diferentes, ele fica sem código CCM até ser cadastrado lá.
5. **Recomeçar do zero.** Enquanto os vínculos não forem refeitos, a migração de pessoas não acha o distrito, porque ela usa `DISTRITOS_UNIFICADO`. O "recomeçar" de distritos não mexe nos Correios nem nas cidades.
6. **Escola / INEP.** As colunas `CIDADE_INEP` e `DISTRITO_INEP` ficam com 0 nas linhas novas, porque a procedure não tratava Escola e não sei de onde vêm esses códigos.


---

# Bairros (novo na v5)

## Aplicar

1. **Banco:** rode `cc-pessoas-api/sql/04_unificacao_bairros.sql`. Ele cria:
   - a tabela `BAIRROS_UNIFICADO_CTRL` e a sequence dela;
   - os índices por origem;
   - a sequence `DBO_CORREIOS.SEQ_BAIRROS_FICTICIOS`;
   - os grants.

   A seção 5 do script conta as linhas antigas sem código dos Correios.
2. **API:** a pasta `unificacao/bairro` é nova.
3. **UI:** o menu fica em **Atividades → Unifica Endereços → Bairros**.

> ⚠️ Não rode mais a `SP_BAIRROS_GERA_UNIFICADO`.

## Recomendação: recomeçar do zero em bairros

A procedure gravou muitos bairros de origem em linhas **sem código dos Correios** (`CIDADE_CORREIOS = 0`, nos status `G`, `R`, `S` e `I`). Para a tela, esses bairros contam como "já vinculados". Mas a migração de pessoas, que faz o join com `DBO_UNI_PESSOAS.BAIRROS` pelo código dos Correios, não acha o `CODIGO_CCM` deles.

Se a seção 5 do script 04 mostrar uma quantidade relevante, use **Recomeçar do zero** antes de unificar.

## Ordem de trabalho

1. **Cidades, depois distritos, depois bairros.** O bairro acha cidade e distrito dos Correios pelo de-para de distritos: o par gravado em `DISTRITOS_UNIFICADO` ou registrado como duplicado em `DISTRITOS_UNIFICADO_CTRL`. Bairro cujo distrito não está unificado aparece como **Distrito não unificado**.
2. **Base dos Correios de bairros:** "Gerar base" ou "Recomeçar do zero".
3. **Por origem:** "Vincular todos os únicos", depois resolva o que sobrou pelo diálogo.

## Regra de correspondência

O vínculo automático compara o nome normalizado com os bairros **do mesmo distrito dos Correios**.

No diálogo, a **pesquisa procura na cidade inteira**, em qualquer distrito, e o botão "Listar todos do distrito" mostra os bairros do distrito. Ao vincular manualmente, a API exige só que o bairro unificado seja da mesma cidade.

## Complementos

Ao vincular ou criar um bairro, a tela preenche colunas de `BAIRROS_UNIFICADO` com dados da origem, **só quando estão vazias**:

| Origem | Colunas preenchidas |
|---|---|
| PESSOAS | `CIDADE_/DISTRITO_/MACRO_BAIRRO`, `DATA_CRIACAO`, `LEI_CRIACAO`, `DATA_DECRETO`, `DECRETO`, `DATA_PORTARIA`, `PORTARIA`, `NOME_VEREADOR`, `PESSOA_LOTEADORA`, `TIPO_BAIRRO` |
| RH | `ZONA_RURAL` |
| Saúde | `BAIRRO_DNE` |

Os campos de macro bairro vêm **só do PESSOAS**, porque cada sistema tem seus próprios códigos de macro bairro e eles não podem ser misturados. A lista de complementos fica no enum `OrigemBairro` e pode ser ajustada lá.

## Endpoints

Base: `/bairros-unificado`. O bairro da origem é `{cidade}/{distrito}/{bairro}`.

| Método | Caminho | O que faz |
|---|---|---|
| GET | `/base-correios/resumo` | Totais dos Correios e da base |
| POST | `/base-correios/gerar` | Acrescenta os bairros que faltam |
| POST | `/base-correios/zerar?confirmacao=ZERAR` | Apaga `BAIRROS_UNIFICADO` e o controle e gera a base de novo |
| GET | `/origens` e `/{origem}/resumo` | Lista as origens e os totais por situação |
| GET | `/{origem}/pendentes?cidade&distrito&bairro&nome&nomeCidade&uf&situacao&page&size` | Bairros pendentes, com sugestão |
| GET | `/{origem}/candidatos/{cidade}/{distrito}/{bairro}?pesquisa&todos` | Candidatos |
| POST | `/{origem}/vincular/{cidade}/{distrito}/{bairro}?idUnificado=` | Grava o código da origem |
| POST | `/{origem}/vincular-unicos` | Lote dos únicos |
| POST | `/{origem}/criar/{cidade}/{distrito}/{bairro}` | Cria o bairro nos Correios (`STATUS 'N'`) e no unificado |
| POST | `/{origem}/nao-vincular/{cidade}/{distrito}/{bairro}?motivo=` | Retira o bairro da lista |

## O que muda em relação à procedure

- **Nenhuma linha sem código dos Correios.** Todo bairro de origem fica ligado a um bairro dos Correios, que já existia ou foi criado com `STATUS 'N'`.
- **Escolha explícita no ambíguo.** Quando havia mais de um bairro com o mesmo nome, a procedure escolhia o menor código sem avisar. O `IF > 1` dela estava dentro do `IF = 0` e nunca executava. Na tela esse caso aparece como "Ambíguo" e o usuário escolhe.
- **Não sobrescreve códigos.** A procedure gravava os códigos do RH e da Saúde por cima de códigos já existentes. Quando dois bairros de origem caíam no mesmo bairro dos Correios, o primeiro se perdia. Agora ele vira registro duplicado (`D`).
- **Nenhum código de origem se perde.** Bairros do RH caíam num caminho que não gravava nada.
- **Não mexe nos Correios.** A procedure apagava os bairros `STATUS 'N'`, mesmo com logradouros apontando para eles, e reescrevia o `NOME_TRANSF` de todos os bairros dos Correios.
- **Grava direto na tabela final.** As tabelas intermediárias `BAIRROS_UNIFICADO_RH` e `_SAUDE` não são usadas.
- **Inclui Saneamento e Escola** como origens, com a mesma tela.

## Premissas de bairros (conferir no ambiente)

1. **Colunas das tabelas de origem.** Os sinônimos `PES_`, `RH_`, `SANE_`, `SAU_` e `ESCO_BAIRROS` têm `CIDADE`, `DISTRITO`, `BAIRRO` e `NOME`, além das colunas de complemento listadas acima. A UF e o nome da cidade vêm da tabela de cidades da mesma origem.
2. **Bairro novo nos Correios.** Ele é gravado com as colunas `CIDADE, DISTRITO, BAIRRO, NOME, NOME_REDUZIDO, NOME_TRANSF, STATUS`. O `NOME_REDUZIDO` recebe os primeiros 30 caracteres do nome. Se a coluna for menor ou houver outra coluna obrigatória, o "Criar bairro novo" dá erro `ORA-` e nada é gravado.
3. **Tamanho do código de distrito.** `BAIRROS_UNIFICADO.DISTRITO_CORREIOS` é `NUMBER(6)`. Os distritos fictícios novos usam a `SEQ_DISTRITOS_FICTICIOS`, que começa no maior código + 10000. Confira se esse valor cabe em 6 dígitos.
4. **Código CCM.** Vale a mesma dúvida de distritos: um bairro novo criado pela tela só existe em `DBO_CORREIOS.BAIRROS` e não tem `CODIGO_CCM` em `DBO_UNI_PESSOAS.BAIRROS` até ser cadastrado lá.
5. **Duplicados e a migração de pessoas.** Hoje a migração de pessoas lê **só** a tabela `*_UNIFICADO`. Um código de origem registrado como duplicado (`D`) nos `*_CTRL` não é encontrado por ela. A exceção é o Saneamento, que em bairros já consulta a `BAIRROS_UNIFICADO_AUXILIAR`. Ficou anotado como pendência.

---

# Conferência pós-carga (novo na v5)

O script `cc-pessoas-api/sql/05_conferencia_unificacao.sql` só tem consultas. Rode depois de cada carga, de cidades, distritos ou bairros:

| Seção | O que mostra | Resultado esperado |
|---|---|---|
| A | Por origem, total na origem × vinculados | O número de vinculados perto do total |
| B | Código de origem em mais de uma linha | Nenhuma linha. Se houver, a migração de pessoas falha naquele código |
| C | Linhas com código de origem e sem código dos Correios | Zero |
| D | Decisões registradas na tela (duplicados e não vincular) | Só conferência |
| E | Amostra aleatória de 30 distritos do PESSOAS vinculados, com o nome da origem ao lado do nome dos Correios | Os nomes conferem |


---

# Logradouros (novo na v6)

## Aplicar

1. **Banco:** rode `cc-pessoas-api/sql/06_unificacao_logradouros.sql`. Cada bloco indica com qual usuário rodar. O script:
   - cria a tabela `ABREVIATURAS_LOGRADOURO`, já com as abreviações que a `SP_LOGRADOUROS_SEPARA_NOME` tratava. Dá para acrescentar outras;
   - cria o pacote `PKG_LOGRADOURO_CHAVE`, que monta a chave de comparação do nome;
   - acrescenta `CHAVE_NOME` e `CHAVE_TIPO` em `LOGRADOUROS_UNIFICADO`, calcula as chaves da carga atual (alguns minutos) e cria o índice;
   - cria `LOGRADOUROS_UNIFICADO_CTRL`, a sequence `DBO_CORREIOS.SEQ_LOGRADOUROS_FICTICIOS` e índices em `CEPS_CARGA_DNE`;
   - lista os grants.

   A seção 7 do script conta as linhas antigas sem código dos Correios.
2. **API:** a pasta `unificacao/logradouro` é nova.
3. **UI:** o menu fica em **Atividades → Unifica Endereços → Logradouros**.

> **Sobre `TITULOS_PATENTES`:** combinamos usar `DBO_CC_ENDERECOS.TITULOS_PATENTES` como dicionário de abreviações, mas os metadados mostram que ela só tem `ID` e `DESCRICAO`, sem `SIGLA`. Por isso as abreviações (DR → DOUTOR, PROF → PROFESSOR...) ficaram na tabela nova `ABREVIATURAS_LOGRADOURO`. A tabela de tipos (`DBO_CC_ENDERECOS.TIPOS_LOGRADOUROS`) tem `SIGLA` e é usada para o tipo (R → RUA, AV → AVENIDA).

## Recomendação: recomeçar do zero em logradouros

Como em bairros, a procedure gravou logradouros de origem em linhas sem código dos Correios. Se a seção 7 do script mostrar uma quantidade relevante, use **Recomeçar do zero**. Com 1,3 milhão de logradouros, isso leva alguns minutos.

## Como a comparação funciona

A chave do nome é a mesma dos dois lados, origem e Correios:
1. tira acentos e pontuação (`FN_NORMALIZA_TEXTO`);
2. troca as abreviações pelo nome por extenso;
3. remove as preposições DE, DA, DO, DAS, DOS;
4. do lado dos Correios, junta título + preposição + nome, sem repetir o título se o nome já começa com ele.

Assim, `DR. JOÃO-DE SOUZA` e `Doutor Joao Souza` dão a mesma chave. Isso corrige o descasamento entre `FN_NORMALIZA_TEXTO` e os campos `*_TRANSF`, que eram feitos com `TRANSF_CARACT` e mantinham a pontuação.

O tipo é comparado à parte: a descrição do tipo na tabela de tipos da origem contra o `TIPO_LOGRADOURO` dos Correios, as duas pela sigla da `TIPOS_LOGRADOUROS`.

A comparação é sempre **no mesmo distrito dos Correios**, achado pelo de-para de distritos (inclusive os duplicados `D`).

| Situação | Condição | No lote? |
|---|---|---|
| DISTRITO_NAO_UNIFICADO | O distrito ainda não foi unificado | — |
| UNICA | Um logradouro com o mesmo nome e tipo compatível | Sim |
| FAIXAS | O mesmo nome e tipo em vários trechos, divididos por faixa de numeração (complemento "até 699/700", "lado ímpar"...). Vincula ao **trecho principal**: o sem faixa ou, se todos tiverem faixa, o de menor código. As faixas continuam valendo pelos CEPs | Sim |
| DUPLICADA | O sugerido já tem outro código desta origem | Não. Ao vincular, vai para o controle `D` |
| TIPO_DIFERENTE | Mesmo nome, tipo diferente (Rua x Avenida) | Não. Decida no diálogo |
| AMBIGUA | Mais de um logradouro com o mesmo nome que não são faixas do mesmo logradouro | Não |
| SEM_CORRESPONDENCIA | Nenhum com o mesmo nome no distrito | Não |

O botão **Vincular únicos e faixas** processa UNICA e FAIXAS, um logradouro por transação.

## Logradouro novo: só com CEP válido

O "Criar logradouro novo" abre a escolha do CEP:
- a lista vem de `DBO_CORREIOS.CEPS_CARGA_DNE`, só da cidade do logradouro;
- pesquise pelo começo do CEP (`38010`) ou pelo nome de um logradouro vizinho;
- se a origem tiver CEP (Saúde) e ele existir na tabela, já vem na lista;
- a API confere de novo que o CEP existe na cidade. A tela **nunca cria um valor de CEP**.

O que é gravado, seguindo a `SP_CARGA_CEPS`:
1. `DBO_CORREIOS.LOGRADOUROS` com código novo da sequence, `STATUS 'N'` e o bairro do CEP escolhido;
2. `DBO_CORREIOS.CEPS` com `STATUS 'N'`, ligando o CEP escolhido ao código novo. O que se cria é o ID, não o CEP;
3. `LOGRADOUROS_UNIFICADO` com o código da origem e o CEP.

Logradouro com o mesmo nome e tipo compatível no distrito bloqueia a criação. Os de tipo diferente não bloqueiam.

## Complementos

Preenchidos só quando a coluna está vazia, ao vincular ou criar:

| Origem | Colunas |
|---|---|
| PESSOAS | `NOME_LEGAL` |
| Saúde | `CEP` (só se existir na tabela de CEPs), `NUMERO_INI`, `NUMERO_FIM`, `LADO`, `LOGRADOURO_DNE`, `BAIRRO_DNE`, `BAIRRO_DNE_FINAL`, `COMPLEMENTO` |

## Endpoints

Base: `/logradouros-unificado`. O logradouro da origem é `{cidade}/{distrito}/{logradouro}`.

| Método | Caminho | O que faz |
|---|---|---|
| GET | `/base-correios/resumo` | Totais dos Correios, da base e das chaves vazias |
| POST | `/base-correios/gerar` | Acrescenta os logradouros que faltam e calcula as chaves vazias |
| POST | `/base-correios/zerar?confirmacao=ZERAR` | Apaga `LOGRADOUROS_UNIFICADO` e o controle e gera a base de novo |
| GET | `/origens` e `/{origem}/resumo` | Origens e totais por situação |
| GET | `/{origem}/pendentes?cidade&distrito&logradouro&nome&nomeCidade&uf&situacao&page&size` | Pendentes, com sugestão |
| GET | `/{origem}/candidatos/{cidade}/{distrito}/{logradouro}?pesquisa&todos` | Candidatos |
| GET | `/{origem}/ceps/{cidade}/{distrito}/{logradouro}?pesquisa` | CEPs válidos da cidade para o logradouro novo |
| POST | `/{origem}/vincular/{cidade}/{distrito}/{logradouro}?idUnificado=` | Grava o código da origem |
| POST | `/{origem}/vincular-unicos` | Lote de UNICA + FAIXAS |
| POST | `/{origem}/criar/{cidade}/{distrito}/{logradouro}?cep=` | Cria o logradouro com o CEP escolhido |
| POST | `/{origem}/nao-vincular/{cidade}/{distrito}/{logradouro}?motivo=` | Retira o logradouro da lista |

## O que muda em relação às procedures

- **Não depende mais de `LOGRADOUROS_PESSOAS` nem da `SP_LOGRADOUROS_SEPARA_NOME`.** A chave é calculada direto do nome.
- **Compara dentro do distrito.** Nada de casar logradouro homônimo de outro distrito ou outra cidade.
- **Logradouro novo com CEP real e código por sequence.** A procedure usava `MAX + 1` e não ligava CEP nenhum.
- **Não apaga nada nos Correios.** A procedure apagava os logradouros `STATUS 'N'`.
- **Inclui Saneamento e Escola** como origens.

## Premissas de logradouros (conferir no ambiente)

1. **Colunas das origens.** As origens têm `CIDADE, DISTRITO, LOGRADOURO, TIPO_LOGRADOURO, NOME`. O tipo vem de `<origem>_TIPOS_LOGRADOUROS` (`TIPO_LOGRADOURO`, `DESCRICAO`). Confirmado nos metadados.
2. **Nome na origem com o tipo junto.** Se alguma origem gravar o tipo dentro do nome (`R JOAO SOUZA`), a chave não casa e o logradouro cai em "Sem correspondência". Se acontecer muito, dá para tirar o tipo do começo do nome no pacote.
3. **`NOME` dos Correios sem o título.** A chave junta `TITULO_PATENTE + PREPOSICAO + NOME` e não repete o título quando o nome já começa com ele.
4. **Abreviações alteradas.** Depois de mexer em `ABREVIATURAS_LOGRADOURO`, reinicie a API, zere as chaves (comando no script 06) e clique em **Completar base**.
5. **Logradouro criado sem tipo nos Correios.** O tipo gravado é a descrição da origem (ex.: `RUA`).

---

# Consolidação (novo na v6)

## Aplicar

1. **Banco:** rode `cc-pessoas-api/sql/07_consolidacao_enderecos.sql`. Ele tem os grants, uma prévia e as conferências.
2. **API:** a pasta `unificacao/consolidacao` é nova.
3. **UI:** o menu fica em **Atividades → Unifica Endereços → Consolidação**.

## O que a tela faz

Para cada nível (cidades, distritos, bairros, logradouros, CEPs), mostra:
- quantas linhas o unificado tem (uma por código dos Correios);
- quantas já estão na consolidada;
- quantas faltam;
- quantas estão **só** na consolidada (cargas antigas). Essas nunca são apagadas, só aparecem para conferência.

**Consolidar** roda um `MERGE` por nível, numa transação:
- insere o que falta;
- nas linhas que já existem, preenche só colunas vazias;
- **nunca apaga** e **nunca altera o `CODIGO_CCM`**;
- não cria código, porque tudo já tem código dos Correios desde a unificação.

**Consolidar tudo** roda na ordem e para no primeiro nível com erro.

Os CEPs vêm de `CEPS_CARGA_DNE`, como na `SP_CEPS_UNIFICA`, somados aos vínculos de CEP dos logradouros criados pela tela (`DBO_CORREIOS.CEPS` com `STATUS 'N'`). `NUMERO_INI` vazio vira 0, porque faz parte da chave.

O mapeamento de colunas é o das procedures. Exemplo em logradouros: `TIPO_LOGRADOURO` é o código do PESSOAS achado pela descrição, ou `NEC`; `TIPO_LOGRAD_DESC` é a descrição; `NOME1` é o nome dos Correios.

## Endpoints

Base: `/consolidacao-enderecos`. `{nivel}` = `CIDADES`, `DISTRITOS`, `BAIRROS`, `LOGRADOUROS` ou `CEPS`.

| Método | Caminho | O que faz |
|---|---|---|
| GET | `/niveis` | Lista os níveis |
| GET | `/{nivel}/resumo` | Contagens do nível |
| POST | `/{nivel}/executar` | Consolida um nível |
| POST | `/executar-todos` | Consolida todos, na ordem |

## Premissas da consolidação (conferir)

1. **Linhas existentes não são sobrescritas.** Se um nome foi corrigido na unificação depois da primeira consolidação, a correção não vai para a consolidada. A seção 2 do script 07 mostra as diferenças de nome em logradouros.
2. **Tempo.** Logradouros e CEPs passam de 1 milhão de linhas. Se houver proxy ou timeout HTTP entre a tela e a API, ele precisa aguentar alguns minutos.


---

# Carga final (novo na v7)

## Aplicar

1. **Banco:** rode `sql/11_carga_final_enderecos.sql`.
   - **Seção 0 primeiro.** Ela conta quantos registros da consolidada estão sem `CODIGO_CCM`. O normal é um número pequeno: só o que entrou de novo pela unificação e pela consolidação.
   - Seção 1: grants.
   - Seção 2: confere se as sequences estão à frente dos IDs que já existem.
   - Seção 3: índice para reaproveitar bairro pelo nome.
   - Seção 4: conferências depois da carga.
2. **API:** a pasta `unificacao/cargafinal` é nova.
3. **UI:** o menu fica em **Atividades → Unifica Endereços → Carga final**.

## O que a tela faz

Para cada nível (cidades, distritos, bairros, logradouros e CEPs), mostra:
- quantas linhas estão na consolidada;
- quantas já têm `CODIGO_CCM`;
- quantas estão prontas para carregar;
- quantas estão bloqueadas, porque o nível de cima ainda não foi carregado;
- quantas têm código mas faltam numa das tabelas finais.

**Carregar** roda o nível inteiro numa transação, nos dois schemas:
1. dá um código novo, das mesmas sequences das procedures, a quem está sem `CODIGO_CCM`, e grava esse código na consolidada;
2. insere em `DBO_CC_ENDERECOS` e em `DBO_CCM_PESSOAS` com o **mesmo ID**, incluindo `DADOS_BAIRROS`, `DADOS_LOGRADOUROS`, `LOGRADOUROS_CEPS` e `BAIRROS_CEPS`;
3. completa as linhas que têm código mas não existem na tabela final.

O que já tem código **nunca muda**, porque os endereços das pessoas apontam para esses IDs. E **nada é apagado**.

**Trava:** se mais de 20% de um nível estiver sem `CODIGO_CCM`, a carga não roda. Isso indicaria que o código foi apagado, e a carga criaria IDs duplicados para endereços que já existem.

## Correções em relação às procedures

| Procedure | Problema | Na tela |
|---|---|---|
| `SP_CARGA_ENDERECOS` | Filtro de teste `upper(nome) like 'bairro 9%'` esquecido: só carregava parte dos bairros | Sem filtro |
| `SP_CARGA_ENDERECOS` | Tipo de logradouro por `INSTR` com IDs fixos: RODOVIA virava RUA (36), POV virava PRAÇA (29) | Tipo pela tabela `TIPOS_LOGRADOUROS`, pela descrição normalizada; se não achar, `NEC` |
| `SP_CARGA_ENDERECOS` | Bairro com o mesmo nome no mesmo distrito era pulado e ficava sem `CODIGO_CCM` | Reaproveita o ID do bairro que já existe |
| `SP_CARGA_ENDERECOS` | `exit` no primeiro distrito não encontrado interrompia a carga inteira | O registro fica como bloqueado e o resto carrega |
| `SP_CARGA_CEPS` | Pulava qualquer CEP cujo número já existisse, mesmo de outro logradouro | Compara logradouro + CEP + número inicial |
| `SP_CARGA_CEPS` | Cidade 6160 excluída fixo no código | Sem exclusão |
| Ambas | Laço linha a linha com commit parcial: um erro no meio deixava a carga pela metade | SQL de conjunto, uma transação por nível |

Mantido igual às procedures:
- o ID da cidade é o código dos Correios;
- `ESTADO` vem de `DBO_CC_ENDERECOS.ESTADOS`, pela sigla;
- o bairro do CEP é buscado pelo código ou, se não houver, pelo nome do bairro dos Correios (`PR_BUSCA_BAIRRO`);
- CEP sem bairro carregado não entra;
- o CEP é gravado como texto do número, como hoje.

## Endpoints

Base: `/carga-final-enderecos`. `{nivel}` = `CIDADES`, `DISTRITOS`, `BAIRROS`, `LOGRADOUROS` ou `CEPS`.

| Método | Caminho | O que faz |
|---|---|---|
| GET | `/niveis` | Lista os níveis |
| GET | `/{nivel}/resumo` | Contagens do nível |
| POST | `/{nivel}/executar` | Carrega um nível |
| POST | `/executar-todos` | Carrega todos, na ordem |

## Premissas (conferir)

1. **Tipo de logradouro.** A procedure gravava de volta na consolidada a sigla do tipo encontrado (`TIPO_LOGRADOURO`). A tela não regrava esse campo. Se algum processo depender disso, me avise.
2. **CEP sem zero à esquerda.** O CEP vai como texto do número, igual à procedure (`01001000` vira `1001000`). Isso fica igual ao que já está gravado.
