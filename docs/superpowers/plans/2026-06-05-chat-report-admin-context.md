# Chat Report Admin Context Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give admins enough member context to review chat message reports without relying on numeric ids.

**Architecture:** Backend report results will enrich existing report rows with reporter, reported member, and handler nicknames. The frontend will keep the existing admin report panel and status filters, but render the report as an operator-facing card with member context, status, timestamps, reason, and original message.

**Tech Stack:** Spring Boot, JPA repositories, MockMvc, React, TypeScript, CSS.

---

### Task 1: Backend Report Context Contract

**Files:**
- Modify: `src/main/java/com/studywithme/chat/application/ChatMessageReportResult.java`
- Modify: `src/main/java/com/studywithme/chat/application/ChatService.java`
- Modify: `src/main/java/com/studywithme/chat/presentation/ChatMessageReportResponse.java`
- Test: `src/test/java/com/studywithme/chat/application/ChatServiceTest.java`
- Test: `src/test/java/com/studywithme/chat/presentation/ChatControllerTest.java`

- [ ] Add focused service and controller assertions for `reporterNickname`, `reportedNickname`, and `handlerNickname`.
- [ ] Add nullable nickname fields to the application result and HTTP response.
- [ ] Enrich report rows by loading related members with `memberRepository.findAllById`.
- [ ] Preserve current admin authorization and message-content exposure rules.
- [ ] Run focused chat report tests, then full backend test suite.

### Task 2: Frontend Admin Report Panel Context

**Files:**
- Modify: `src/types.ts`
- Modify: `src/App.tsx`
- Modify: `src/App.css`
- Modify: `docs/product/moderation.md`

- [ ] Add optional nickname fields to `ChatMessageReport`.
- [ ] Render admin report rows as context cards: 신고자, 피신고자, 처리자, status, timestamps, reason, and original message.
- [ ] Keep handling actions only on pending reports.
- [ ] Keep status filters and notification navigation behavior unchanged.
- [ ] Run frontend lint, build, and whitespace checks.

### Task 3: Documentation And Completion

**Files:**
- Modify: `docs/product/moderation.md`
- Modify: `docs/handoff.md`
- Notion work log under the StudyWithMe page.

- [ ] Update backend and frontend moderation docs with the new admin context policy.
- [ ] Update handoff with the current slice.
- [ ] Create a dated Notion work log.
- [ ] Run PR readiness gate, push, open backend/frontend PRs, wait for CI, and merge to `develop` if green.
