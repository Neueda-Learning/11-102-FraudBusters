# FraudBustersDB Live Frontend Wiring

This project now uses Spring Boot APIs to feed live DB data into:
- `dashboard.html`
- `rules.html`

## What was added

Backend (Spring Boot):
- `GET /api/dashboard/summary`
- `GET /api/dashboard/recent-alerts`
- `GET /api/rules`

Frontend (plain HTML/CSS/JS):
- `dashboard.html` fetches summary + recent alerts from backend
- `rules.html` fetches rule cards and counts from backend

## API base URL used in frontend

`http://localhost:8081/api`

If your backend runs on a different port, update `API_BASE` in:
- `dashboard.html`
- `rules.html`

## Run steps

1. Start MySQL and ensure database `FraudBustersDB` exists.
2. Start backend:

```powershell
cd "C:\Users\Administrator\IdeaProjects\11-102-FraudBusters\FraudBuster\backend\TransactionMonitoring"
.\mvnw spring-boot:run
```

3. Open frontend pages in browser:
- `dashboard.html`
- `rules.html`

## Quick API checks

```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/dashboard/summary"
Invoke-RestMethod -Uri "http://localhost:8081/api/dashboard/recent-alerts"
Invoke-RestMethod -Uri "http://localhost:8081/api/rules"
```

## Notes

- CORS is enabled for `/api/**` in `WebConfig`.
- Rule parameters are derived from `rules.config_json`.
- If tables are empty, frontend shows graceful "no data" messages.

