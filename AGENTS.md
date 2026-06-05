# Here's a **shorter, focused version of AGENTS.md** that retains critical rules while removing redundancies and streamlining workflows

---

# AGENTS.md — AI Agent Operating Manual (Shortened)

## 🔐 Golden Rules (Non-Negotiable)

1. **No code change without tests** → Write tests first.
2. **Update `README.md`/`PROGRESS.md`/`api/ARCHITECTURE.md`** after every task.
3. **Never break the build** → Run full test suite.
4. **Enforce `userId` scoping** on all queries (no cross-manager data access).
5. **No exceptions for "small" changes** → Follow full workflow.

---

## 🧠 Mandatory Workflow (Every Task)

1. **Branch** → Create a feature/fix branch from `main`.
2. **Read** → Check `PROGRESS.md` for context.
3. **Plan** → Outline goal and testing strategy.
4. **Test** → Write tests first (TDD).
5. **Code** → Implement with security in mind.
6. **Verify** → Run all tests.
7. **Docs** → Update documentation.
8. **Log** → Update `PROGRESS.md`.
9. **Commit** → Use conventional commit messages.
10. **Done** → Summarize task completion.

---

## 📦 Git Branching & Commit Guidelines

- **Branch names**: `feat/<description>`, `fix/<description>`, etc.
- **Commit format**:

```text
  <type>(<scope>): <subject>
```

- Example: `feat(api): add person endpoints`

---

## 🔍 Testing Requirements

- **All layers must be tested** (domain, API, frontend).
- **Security invariants**:
  - Managers cannot access other managers' data.
  - Unauthenticated requests return `401`.
  - Sensitive flags are respected in responses.

---

## 🛡️ Security & Architecture Rules

- **Hexagonal architecture**: Dependencies flow inward (no framework code in `domain`).
- **Data scoping**: All queries require `userId`.
- **API conventions**:
  - Require `Authorization: Bearer <token>`.
  - Use `/api/v1/` base path.
  - Return `404` for cross-manager resource access.

---

## 📝 Checklist: Task Completion

✅ **Tests**  

- New tests written.  
- All existing tests pass.  

✅ **Docs**  

- `README.md` updated.  
- `PROGRESS.md` updated.  

✅ **Code Quality**  

- No hardcoded secrets.  
- Input validation present.  

✅ **Security**  

- `userId` enforced on all queries.  
- Sensitive data not logged.

---

## 🚨 What to Avoid

❌ Delete or modify Flyway migrations.  
❌ Use H2 for tests (use Testcontainers).  
❌ Store tokens in `localStorage`.  
❌ Add HRIS features or Slack integrations (out of scope).

---

**This version focuses on **actionable rules** and **quick reference**, while preserving the core principles from the original AGENTS.md.** Use it as a guide to stay aligned with the project’s standards without getting lost in details. 🚀
