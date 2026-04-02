# Product Vision

## Vision Statement

Suppergeist is a local-first desktop app that takes the cognitive load out of weekly meal planning. It uses a locally-running AI model to generate personalised meal plans based on your dietary preferences, then gives you a ready-to-go shopping list — with no cloud account, no subscription, and no data leaving your machine.

## Purpose

Meal planning is genuinely useful but tedious. Most solutions are either too manual (a spreadsheet) or too intrusive (a subscription app harvesting food data). Suppergeist sits in the middle: smart enough to be useful, private enough to be trusted.

This is also a portfolio project, built to demonstrate clean layered architecture, practical AI integration, and maintainable Java/JavaFX code.

## Core Value Proposition

- **Local AI generation** — Ollama runs entirely on-device; no API keys, no monthly cost
- **Preference-aware** — plans adapt to likes, dislikes, and dietary constraints over time
- **Frictionless** — one action to get a week of meals and a shopping list
- **Nutritional estimates** — rough macro guidance backed by CoFID 2021 data, not false precision

## Non-Goals

- Cloud sync or multi-device support
- Microservice architecture or backend server
- Precise calorie tracking (estimates only)
- Real-time collaboration or social features

## Success Criteria (Portfolio)

- A reviewer can follow the codebase from entry point to Ollama call to rendered result in under 30 minutes
- Every layer (UI, service, data) is independently comprehensible
- The app runs on a fresh machine with only Java + Ollama installed
