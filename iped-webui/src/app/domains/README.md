# Domains

Os domínios iniciais planejados para a UI são:

- `session`
- `search`
- `results`
- `viewer`
- `filters`
- `bookmarks`
- `jobs`
- `layout`
- `workspace`

Regras:
- cada domínio é dono do seu estado e dos seus fluxos;
- componentes compartilhados entre domínios sobem para `layout` ou `core`, nunca o contrário.
