# Security Specification (`security_spec.md`)

## 1. Data Invariants
1. **Strict Tenant Isolation**: Every document in `/users/{userId}` and its subcollections (`members`, `messages`, `alerts`, `safezones`) can only be read, listed, created, updated, or deleted when `request.auth != null && request.auth.uid == userId`.
2. **Identity Integrity**: Every document payload must include `userId == request.auth.uid` on creation and preserve `incoming().userId == existing().userId` and `incoming().createdAt == existing().createdAt` on updates.
3. **Path Variable Hardening**: Every single-document path parameter (`userId`, `memberId`, `messageId`, `alertId`, `zoneId`) must satisfy `isValidId(id)` (`^[a-zA-Z0-9_\-]+$` and `<= 128` chars).
4. **Schema & Bounds Enforcement**: Every `create` and `update` operation must validate all required keys via `hasAll()` / `hasOnly()`, enforce string length bounds, numeric ranges, enum allowlists, and server timestamps (`<= request.time`).

## 2. The "Dirty Dozen" Payloads
1. **Unauthenticated Read**: `unauthDb.collection("users/alice/members").get()` -> Reject (`request.auth != null`).
2. **Cross-User Read (PII Leak)**: Alice (`alice_123`) attempts `get` or `list` on `/users/bob_456` or `/users/bob_456/members` -> Reject (`request.auth.uid == userId`).
3. **Identity Spoofing on Create**: Alice writes to `/users/alice_123/members/m1` with `userId: "bob_456"` -> Reject (`data.userId == request.auth.uid`).
4. **Shadow Update (Ghost Field Injection)**: Alice updates `/users/alice_123` adding `"isAdmin": true` -> Reject (`hasOnly()` key allowlist).
5. **Immutable Field Mutation**: Alice updates `createdAt` or `userId` on an existing `FamilyMember` -> Reject (`incoming().createdAt == existing().createdAt`).
6. **Value Poisoning (Type Mismatch)**: Alice updates `batteryLevel` to `"100%"` (string instead of int) -> Reject (`isValidFamilyMember(incoming())`).
7. **Out-of-Bounds Battery / Speed**: Alice sets `batteryLevel: 999` or `speedMph: -10` -> Reject (`0 <= batteryLevel <= 100`).
8. **Invalid Enum Injection**: Alice sets `planTier: "LIFETIME_HACK"` or `alertType: "UNKNOWN"` -> Reject (enum check).
9. **Oversized String DoS**: Alice writes a 5,000-character string into `text` on `ChatMessage` -> Reject (`text.size() <= 1000`).
10. **Future Timestamp Spoofing**: Alice writes `createdAt` 1 hour in the future (`> request.time`) -> Reject (`createdAt <= request.time`).
11. **Partial Delete of Required Key**: Alice calls `FieldValue.delete()` on `name` in `FamilyMember` -> Reject (`isValidFamilyMember` requires `name is string`).
12. **Malformed Document ID**: Alice creates document with spaces or special chars in ID -> Reject (`isValidId()`).
