# Licenciamento - Reconciliação

## Tabela-resumo

| Escopo | Item | Licença declarada | SPDX | Evidência |
|---|---|---|---|---|
| IPED (principal) | IPED | GNU GPL v3 ou posterior + permissões adicionais seção 7 | `GPL-3.0-or-later` | `LICENSE.txt` |
| Terceiros (permissivas) | Apache family (Lucene, Tika, PDFBox, etc.) | Apache 2.0 | `Apache-2.0` | `ThirdParty.txt`, `licenses/Apache 2.0.txt` |
| Terceiros (permissivas) | BSD family (TwelveMonkeys, Aho-Corasick, etc.) | BSD-2/BSD-3 | `BSD-2-Clause` / `BSD-3-Clause` | `licenses/*` |
| Terceiros (permissivas) | MIT family (SLF4J, MSSQL JDBC, Lottie, Leaflet.markercluster) | MIT | `MIT` | `licenses/*` |
| Terceiros (copyleft fraco) | c3p0, mchange, libyal libs, etc. | LGPL 2.1 / 3 | `LGPL-2.1-only` / `LGPL-3.0-only` | `licenses/LGPL 2.1.txt`, `licenses/LGPL 3.txt` |
| Terceiros (legadas/especiais) | Sleuthkit | IPL/CPL | `IPL-1.0 OR CPL-1.0` | `licenses/IPL 1.0.txt`, `licenses/CPL 1.0.txt` |
| Terceiros (não-OSI) | JUnRAR | UnRAR license | `LicenseRef-UnRAR` | `licenses/JUNRAR.txt` |
| Terceiros (proprietária/freeware) | CaffViewer | termos próprios (não open source) | `LicenseRef-CaffViewer-Freeware` | `licenses/CaffViewer.txt` |

| Ação recomendada | Status | Resultado |
|---|---|---|
| Consolidar inventário SPDX | Concluída | `compliance/THIRD_PARTY_SPDX.csv` |
| Garantir correspondência `ThirdParty.txt` x `licenses` | Parcialmente concluída | `ThirdParty.txt` atualizado com `CaffViewer`, `Leaflet`, `Leaflet.markercluster` |
| Revisar JUNRAR/CaffViewer | Concluída (documentação) | Riscos registrados neste relatório e no inventário SPDX |
| Validar LGPL (linkagem) | Concluída (validação técnica inicial) | Evidências de uso/build mapeadas; validação final no artefato distribuído pendente |

## Escopo
- Arquivos analisados: `LICENSE.txt`, `ThirdParty.txt`, diretório `licenses/`.
- Data da análise: 2026-05-13.

## Resultado 1: mapeamento ThirdParty -> arquivo de licença local
- Total de componentes em `ThirdParty.txt`: 43
- Com licença local claramente mapeada: 44
- Sem licença local claramente mapeada: 2

Sem licença local claramente mapeada:
1. ICE JNI Registry (declarado Public Domain)
2. Rifiuti2 (declarado BSD-3-Clause)

## Resultado 2: arquivos em licenses sem referência explícita em ThirdParty
1. `licenses/GPL 2.txt`
2. `licenses/GPL 3.txt`

Observação:
- `GPL 3.txt` é esperado como licença principal do projeto (IPED).
- `GPL 2.txt` pode ser texto de apoio para dependências legadas; não está explicitamente citado em `ThirdParty.txt`.
- `CaffViewer` e `Leaflet*` foram adicionados em `ThirdParty.txt` para reduzir divergência documental.

## Resultado 4: validação técnica (LGPL e componentes críticos)
- Há uso explícito de componentes LGPL no build e runtime, por exemplo:
  - `mchange-commons-java` e `c3p0` em `iped-engine/pom.xml`.
  - `libesedb`, `libpff` e outros utilitários externos em `iped-app/pom.xml`.
- Há uso explícito de `Sleuthkit` em `iped-engine/pom.xml` e empacotamento de DLLs em `iped-app/pom.xml`.
- Há uso explícito de `Leaflet` e `Leaflet.markercluster` em `iped-geo/src/main/resources/iped/geo/openstreet/main.html`.
- Há empacotamento explícito de `CaffViewer` em `iped-app/pom.xml`.

Interpretação prática:
- O projeto combina dependências Java e ferramentas nativas externas, com indícios de linkagem dinâmica/separada para parte das libs LGPL.
- A confirmação final de conformidade LGPL depende de verificar o artefato distribuído final e seus avisos/licenças embarcadas.

## Resultado 3: classificação de risco (compliance)
- Alto: `JUnRAR` (licença própria UnRAR, não-OSI, restrições específicas), `IPED` (GPL-3.0-or-later por ser copyleft forte no produto principal).
- Médio: componentes LGPL 2.1/3.0 e EPL/CPL/IPL (obrigações de redistribuição e compatibilidade já tratada em parte pelo aviso adicional do IPED).
- Baixo: Apache-2.0, BSD-2/3, MIT, Zlib.

## Ações realizadas
1. Inventário SPDX criado: `compliance/THIRD_PARTY_SPDX.csv`.
2. Reconciliação documental criada neste arquivo.
3. Atualização de `ThirdParty.txt` para mapear `CaffViewer` e `Leaflet*`.
4. Registro explícito de pendências de conformidade para validação jurídica/técnica.

## Pendências objetivas para fechamento
1. Incluir no diretório `licenses/` o texto de licença do ICE JNI Registry (ou evidência formal de domínio público) e adicionar referência em `ThirdParty.txt`.
2. Incluir em `licenses/` a licença do Rifiuti2 (BSD-3-Clause) e adicionar referência em `ThirdParty.txt`.
3. Confirmar se versões listadas em `ThirdParty.txt` refletem o build atual (há componentes com versões antigas declaradas).
4. Validar e documentar estratégia de linkagem para bibliotecas LGPL (dinâmica/estática) e artefatos para atender obrigações de relink/substituição quando aplicável.
