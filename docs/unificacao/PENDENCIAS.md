# Pendências — Unificação do Cadastro de Pessoas (Codiub / PMU)

Registradas em 13/11/2026. Resolver em um segundo momento.

## API / Unificação de pessoas

1. **Carga em lote sem fim** — `PesPessoaService.executarCargaCpfUnico` sempre busca a página 0. Registro que dá erro ou cujo CPF já existe em `DADOS_PF` (retorno `false`) continua na consulta de "não migradas" e o laço nunca termina. Hoje nada chama o método, mas precisa corrigir antes de ligar a carga.
2. **Regra de unificação automática só no front** — `UnificacaoAutomaticaService` (Angular). Os endpoints `processar-ja-existe` aceitam o vínculo sem validar nome e nascimento no back-end.
3. **API sem autenticação/autorização** — sem Spring Security. `CONTROLE_MIGRACAO_PESSOA.USUARIO` e `MOTIVO` gravados nulos, então não há registro de quem decidiu cada unificação.
4. **Credenciais no código** — `application-dev.properties` e `.env` com senha do banco em texto puro dentro do pacote. Tirar do pacote e trocar a senha se o zip já circulou.
5. **Validação de CPF fraca** — confere só tamanho e alguns valores fixos, sem dígito verificador (ex.: `22222222222` passa).
6. **Consolidação de duplicados inconsistente** — "primeiro valor preenchido" com ordenações diferentes: PES por `p.pessoa`, RH por `p.nome`.
7. **Código legado e lixo no projeto** — duas implementações para PES (`pesService/PesCarga*` e `pessoa/pesService/*`), zips de backup dentro do `src`, sobras do template ngx-admin na UI (mocks, competições, modalidades), nenhum teste automatizado.
8. **Unicidade de CPF** — confirmar se `DADOS_PF.CPF` tem constraint `UNIQUE`. Sem ela, dois processamentos simultâneos do mesmo CPF podem gerar duplicidade.

9. **Duplicados não são vistos pela migração de pessoas** — os códigos de origem registrados como duplicados (`STATUS = 'D'`) em `CIDADES_`, `DISTRITOS_` e `BAIRROS_UNIFICADO_CTRL` não são consultados pela migração de pessoas, que lê só as tabelas `*_UNIFICADO`. A exceção é o Saneamento, que em bairros já consulta a `BAIRROS_UNIFICADO_AUXILIAR`. A migração deve passar a consultar o `*_CTRL` (status D) como segunda opção, do mesmo jeito que o Saneamento faz.
10. **Código CCM de endereços criados pela tela** — *em andamento na v6.* A tela de Consolidação já leva cidades, distritos, bairros e logradouros criados pela tela para as tabelas de `DBO_UNI_PESSOAS`, com `CODIGO_CCM` vazio. Falta a fase 3 (v7), que gera o `CODIGO_CCM` nas tabelas finais e grava de volta.
11. **Procedures antigas** — aposentar `SP_CIDADES_`, `SP_DISTRITOS_`, `SP_BAIRROS_` e `SP_LOGRADOUROS_GERA_UNIFICADO`, `SP_LOGRADOUROS_GERA_UNI_ESCOLA`, `SP_LOGRADOUROS_SEPARA_NOME*` e todas as `SP_*_UNIFICA`. Rodar qualquer uma delas desfaz o que as telas gravaram, e as `*_UNIFICA` apagam o `CODIGO_CCM`.
12. **Fase 3 (v7)** — tela para a carga das tabelas finais (`DBO_CC_ENDERECOS` e `DBO_CCM_PESSOAS`), no lugar de `SP_CARGA_ENDERECOS` e `SP_CARGA_CEPS`. Deve ser incremental, mapear o tipo pela tabela, não parar no primeiro erro e não ter o filtro de teste `upper(nome) like 'bairro 9%'` que está na procedure.
13. **Consolidação não propaga correções** — a consolidação só preenche colunas vazias em linhas existentes. Definir se correções de nome feitas depois da primeira consolidação devem ser propagadas (e para quais colunas).
14. **Tipo dentro do nome do logradouro na origem** — se alguma origem gravar `R JOAO SOUZA` no nome, a chave não casa. Avaliar depois da primeira carga, pelos "Sem correspondência".
15. **Tipo `NEC` na consolidada** — logradouros cujo tipo dos Correios não está na tabela de tipos do PESSOAS ficam com `TIPO_LOGRADOURO = 'NEC'`, como na procedure. A seção 3.3 do script 07 lista esses casos.

## Contexto confirmado

- Instância Oracle 11g R1. Schemas `DBO_PESSOAS` e `DBO_TRIBUTOS` (`DBO_TRIBUTOS` usa views do `DBO_PESSOAS`).
- Os prefixos são sinônimos para o schema de origem:
  - `PES_` → `DBO_PESSOAS`
  - `RH_` → `DBO_RH`
  - `SANE_` → `DBO_CODAU_SANEAMENTO`
  - `SAU_` → Saúde
  - `ESCO_` → Escola
  - `COR_` → Correios
- Demais origens: o Antonio informa conforme forem aparecendo.
