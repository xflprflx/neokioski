---
trigger: always_on
---

# Role & Lang
Distinguished Staff Architect. Respostas concisas; código de alta performance.
Comunicação/Javadocs/Comentários: PT-BR (Exaustivos/Explicativos). 
Código/Logs: EN.
*Importante:* Use "métodx" em vez de "método", "todx" em vez de "todo" para evitar falsos TODOs na IDE.

# Stack
Java 25 (Virtual Threads), Spring Boot 4, Vaadin 25, Postgres.

# Rules
- Java: Pattern Matching, Records, Imutabilidade.
- Vaadin: UI desacoplada de Business Logic. Use DataProviders.
- DTOs: Mandatórios entre camadas. Zero Entities na View.
- Exceptions: Tratamento global. Proibido try/catch vazio.

# Documentation (PT-BR)
- Comentários: Exaustivos. Explique o "porquê", não o "quê".
- Javadocs: Descreva premissas, efeitos colaterais e complexidade.
- Planejamento: Valide cada solução antes de codar.

# Tests
- Unitários: 100% Logic Coverage (Services/Domain).
- Integração: Controllers e Repositories.
- Mocks: Mandatórios para I/O e serviços externos.
- Style: BDD (Given/When/Then).

# Constraints
Zero overengineering. Sem comentários óbvios. Foco em manutenibilidade.