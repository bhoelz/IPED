---
name: parser-submodule-authoring
description: Crie novos parsers do IPED como submódulos de iped-parsers, com isolamento de dependências, sem conflitos com Tika/Lucene do core, e com registro correto em ParserConfig.xml.
---

# Parser Submodule Authoring

Use esta skill quando for adicionar novos parsers ao IPED sem acoplar implementações novas diretamente em `iped-parsers-impl`.

## Regra Arquitetural

- Novos parsers no repositório do IPED devem ser implementados como submódulos de `iped-parsers` (ex.: `iped-parsers/iped-parsers-<feature>`), com `pom.xml` próprio.
- Evite adicionar novas dependências pesadas/conflitantes em `iped-parsers-impl`.
- O parser deve ser integrado por dependência Maven e por registro explícito no `ParserConfig.xml`.

## O que o código atual mostra

1. Registro de execução de parsers Java/Tika:
- O carregamento efetivo acontece via `tika.config` apontado para `ParserConfig.xml` em `ParsingTask.setupParsingOptions()`.
- Referência: `iped-engine/src/main/java/iped/engine/task/ParsingTask.java`.

2. Lista de parsers ativos:
- Parsers são declarados em `iped-app/resources/config/conf/ParserConfig.xml` (e perfil triage equivalente).
- O parser só entra no fluxo se estiver listado nesse XML.

3. Parsers Python:
- Há suporte nativo via `iped.parsers.python.PythonParser`.
- Scripts são lidos de `<appRoot>/scripts/parsers` (property `PYTHON_PARSERS_FOLDER`).
- Cada arquivo `.py` deve expor classe com o mesmo nome do arquivo e métodos `getSupportedTypes()` e `parse()`.

4. Javascript como parser Tika genérico:
- Não existe suporte genérico para parser Tika em JS como existe para Python.
- JS existe para outros pontos: tasks (`scripts/tasks`), regex validators e carvers.
- Existe uso específico de JS no parser de Registry (`RegistryKeyParserManager`), não é mecanismo geral de parser de arquivos.

5. Plugins e carregamento externo:
- Com parsing externo habilitado, `ForkParser` inclui `pluginDir/*` no classpath do processo de parsing (`ForkClient`).
- Isso permite carregar JARs de parser como plugins, desde que classes/deps estejam no plugin.

## Fluxo Recomendado para Novo Parser

1. Criar submódulo novo em `iped-parsers`.
- Nome sugerido: `iped-parsers-<feature>`.
- Estrutura Maven padrão (`src/main/java`, `src/test/java`, `pom.xml`).

2. Declarar dependências com isolamento.
- Reutilize versões já usadas no parent (`tika.version`, `lucene.version`) quando precisar de Tika/Lucene.
- Se precisar de libs novas, prefira escopo restrito ao submódulo.
- Em dependências transitivas problemáticas, use `exclusions`.
- Não subir versão de Tika/Lucene só para um parser novo.

3. Implementar parser.
- Implementar classe `org.apache.tika.parser.Parser` (normalmente `AbstractParser`).
- Definir `getSupportedTypes(ParseContext)` e `parse(...)`.
- Se gerar subitens, respeitar metadados usados pelo fluxo do `ParsingTask`/`EmbeddedDocumentExtractor`.

4. Integrar submódulo no build.
- Adicionar `<module>iped-parsers-<feature></module>` em `iped-parsers/pom.xml`.
- Adicionar dependência do novo submódulo em quem instancia o parser (tipicamente `iped-engine` via classpath final do app).

5. Registrar parser para execução.
- Incluir `<parser class="seu.pacote.SeuParser"></parser>` em:
- `iped-app/resources/config/conf/ParserConfig.xml`
- `iped-app/resources/config/profiles/triage/conf/ParserConfig.xml` (se aplicável ao perfil).
- Se houver parâmetros, usar `<params><param .../></params>` como nos exemplos existentes.

6. Validar resolução de parser.
- Executar testes do submódulo e testes de integração de parsing.
- Confirmar que o MIME alvo resolve para o parser esperado via `StandardParser`.

## Estratégia para evitar conflitos de dependência

- Preferir as versões de BOM/propriedades já adotadas no projeto.
- Em bibliotecas com alto risco de colisão (Jackson, Guava, logging, parsers XML, JNI), avaliar:
- `exclusions` de transitivas desnecessárias.
- execução em parser externo/fork quando isolamento adicional for necessário.
- Para integrações mais complexas, considerar parser externo/plugin JAR carregado via pasta de plugins.

## Checklist de PR

1. Novo submódulo criado sob `iped-parsers`.
2. `iped-parsers/pom.xml` atualizado com o módulo.
3. Dependências do módulo revisadas para evitar conflitos.
4. Parser registrado no `ParserConfig.xml` (e triage, se aplicável).
5. Testes cobrindo MIME suportado e saída mínima (texto/metadados/subitens).
6. Sem upgrade incidental de Tika/Lucene no projeto inteiro.

## Referências rápidas no código

- `iped-engine/src/main/java/iped/engine/task/ParsingTask.java`
- `iped-app/resources/config/conf/ParserConfig.xml`
- `iped-app/resources/config/profiles/triage/conf/ParserConfig.xml`
- `iped-parsers/iped-parsers-impl/src/main/java/iped/parsers/python/PythonParser.java`
- `SCRIPTING.md`
- `iped-parsers/iped-parsers-impl/src/main/java/iped/parsers/fork/ForkClient.java`
- `iped-engine/src/main/java/iped/engine/config/PluginConfig.java`
- `iped-parsers/iped-parsers-impl/src/main/java/iped/parsers/registry/keys/RegistryKeyParserManager.java`
