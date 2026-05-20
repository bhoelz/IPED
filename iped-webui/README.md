# IPED Web UI

Base inicial da nova interface web do IPED, criada em Angular 21 para sustentar o `EPIC-WEB-01` do backlog de modernização.

## Scripts

```bash
npm start
npm run build
npm test
npm run format:check
npm run generate:api
```

## Estrutura Inicial

- `src/app/core`: integrações transversais, configuração, HTTP e client gerado.
- `src/app/layout`: shell e componentes estruturais da aplicação.
- `src/app/domains`: domínios funcionais da UI (`session`, `search`, `results`, `viewer`, `filters`, `bookmarks`, `jobs`, `layout`).
- `scripts`: automações locais do workspace, incluindo geração do client OpenAPI.

## Convenções Iniciais

- O shell principal deve permanecer fino; fluxo de negócio vive em `domains`.
- Código gerado a partir do OpenAPI deve ficar isolado em `src/app/core/api/generated`.
- Novas telas devem ser standalone components.
- Estado deve ser organizado por domínio funcional, não por tipo técnico.
- `npm run generate:api` escolhe um JDK moderno antes de executar o gerador, evitando o `java` 8 presente no PATH global.

## Próximos Passos

- Conectar o client gerado aos primeiros fluxos de sessão e busca.
- Implementar o domínio `session` para bootstrap de caso.
- Substituir placeholders da shell pelos primeiros painéis reais de análise.
