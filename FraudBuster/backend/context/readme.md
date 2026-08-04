# Backend Context README

This folder contains handover and reference documents for the FraudBuster backend Transaction Monitoring project.

## Quick Start

- Start with `project_context_handover.md` for overall product/business context.
- Read `schema_backend_handover.md` for the compact backend schema overview.
- Use `datamodel_context.md` for table-by-table design and relationships.
- Use `schema_backend_details.md` for detailed field-level semantics.
- Check `backend_parallel_todo_plan.md` for work planning and execution notes.

## Key Decisions Locked for MVP

- Architecture: Spring Boot backend with MySQL.
- Scope: dummy/static data first, then API integration, then scale improvements.
- Alert lifecycle: `OPEN -> ACKNOWLEDGED -> INVESTIGATING -> CLOSED` and dismissal path.
- Hold window policy: 10-minute investigation SLA with auto-decline timeout.
- Current assumptions: no authentication and single-operator workflow.

## Purpose of This Folder

Keep project decisions, schema rationale, and handover notes in one place so team members can onboard quickly and stay aligned.

