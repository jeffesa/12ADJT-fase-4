# TASK-020: Fix SendNotificationHandlerTest with @QuarkusTest + @InjectMock

## Root Cause
The `SendNotificationHandlerTest` test manually assigns `@Inject` dependencies in `@BeforeEach` without starting a Quarkus CDI container, causing all dependencies to remain `null` at test execution time. The handler `SendNotificationHandler` is `@ApplicationScoped` and uses `@Inject` for `SendNotificationUseCase` and `NotificationConfiguration`, which requires CDI initialization.

**Current test result:** 5 failures (useCase call counts wrong) + 3 errors (NullPointerException on capturedNotification access).

## Solution Overview
Convert the test to use Quarkus's `@QuarkusTest` annotation with `@InjectMock` for dependency mocking. This enables the CDI container to properly initialize the handler and inject mocked dependencies during test execution.

## Implementation Plan

### 1. Update SendNotificationHandlerTest class-level annotations and add imports
- Add `@QuarkusTest` annotation to the test class.
- Replace manual field setup with `@InjectMock` annotations.
- Import `io.quarkus.test.junit.QuarkusTest`, `io.quarkus.test.InjectMock`, and optionally `org.mockito.Spy` if needed for partial mocking of the handler itself.
- Keep all existing test imports; add only Quarkus test infrastructure imports.

**Files:** `/Users/jefferson/Documents/FIAP/fase-4/send-notification/src/test/java/com/fiap/notification/infra/handler/SendNotificationHandlerTest.java`

**Verify:** Run `cd /Users/jefferson/Documents/FIAP/fase-4 && mvn clean test -pl send-notification -q` and confirm the compilation succeeds with no import errors.

---

### 2. Convert SendNotificationUseCase field to @InjectMock
- Remove the `useCase` field declaration of type `FakeSendNotificationUseCase`.
- Add a new field of type `SendNotificationUseCase` with `@InjectMock` annotation (the actual production class, not the fake).
- Remove the `FakeSendNotificationUseCase` inner class entirely — it is no longer needed because the real `SendNotificationUseCase` is injected and mocked.
- Keep the mock behavior logic in the test methods by using Mockito's `when()` and `verify()` to configure and assert on the mocked use case.

**Rationale:** `@InjectMock` will instantiate a Mockito mock of `SendNotificationUseCase` and inject it into the handler via CDI. The test methods will configure mock behavior per-test instead of maintaining a fake subclass.

**Files:** `/Users/jefferson/Documents/FIAP/fase-4/send-notification/src/test/java/com/fiap/notification/infra/handler/SendNotificationHandlerTest.java`

**Verify:** Compile succeeds; imports resolve; no "cannot find symbol" errors for SendNotificationUseCase.

---

### 3. Convert NotificationConfiguration field to @InjectMock
- Remove the `config` field declaration and the `TestNotificationConfiguration` inner class.
- Add a new field of type `NotificationConfiguration` with `@InjectMock` annotation.
- In each test method that needs specific config values, use `when(config.senderEmail()).thenReturn(...)`, etc., to configure the mock's behavior.

**Rationale:** `@InjectMock` creates a Mockito mock of the interface. Test methods will configure return values per-test using standard Mockito stubbing.

**Files:** `/Users/jefferson/Documents/FIAP/fase-4/send-notification/src/test/java/com/fiap/notification/infra/handler/SendNotificationHandlerTest.java`

**Verify:** Compile succeeds; `NotificationConfiguration` interface is properly mocked; no "cannot resolve method" errors.

---

### 4. Inject SendNotificationHandler and remove setUp() boilerplate
- Add a field `@Inject SendNotificationHandler handler;` (or use `@Spy` if partial mocking is needed; plain `@Inject` is sufficient for testing a handler).
- Delete the `@BeforeEach setUp()` method entirely — CDI initialization at class start handles dependency setup.
- Delete the `FakeSendNotificationUseCase` and `FakeNotificationSender` inner classes.
- Delete the `TestNotificationConfiguration` inner class.

**Rationale:** With `@QuarkusTest`, the CDI container initializes the handler and injects its dependencies (useCase and config) automatically before each test. No manual field assignment is needed.

**Files:** `/Users/jefferson/Documents/FIAP/fase-4/send-notification/src/test/java/com/fiap/notification/infra/handler/SendNotificationHandlerTest.java`

**Verify:** Class compiles; no "cannot find symbol" errors; setUp() method is gone.

---

### 5. Update each test method to use Mockito stubbing and verification
- In each test method that was previously verifying `useCase.executeCallCount`, use `verify(useCase, times(N)).execute(any())` instead.
- In each test method that was checking `useCase.lastNotification`, capture the argument using `ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class); verify(useCase).execute(captor.capture()); Notification captured = captor.getValue();`.
- For tests that expect the use case to throw `NotificationException`, use `doThrow(new NotificationException(...)).when(useCase).execute(any())`.
- Tests that check config values: use `when(config.adminEmail()).thenReturn("admin@company.com")` at the start of the test.
- Ensure `context` field is initialized to `null` (no change needed; it is unused in all tests).

**Examples:**
- **Old:** `assertEquals(1, useCase.executeCallCount, "useCase.execute() should be called exactly once");`
  **New:** `verify(useCase, times(1)).execute(any(Notification.class));`
  
- **Old:** `Notification capturedNotification = useCase.lastNotification; assertEquals(feedbackId, capturedNotification.getFeedbackId());`
  **New:** `ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class); verify(useCase).execute(captor.capture()); Notification captured = captor.getValue(); assertEquals(feedbackId, captured.getFeedbackId());`

- **Old:** `useCase.exceptionToThrow = new NotificationException("SES error");`
  **New:** `doThrow(new NotificationException("SES error")).when(useCase).execute(any());`

**Rationale:** Mockito provides these standard patterns for assertion and configuration of mock behavior. This aligns the test with Quarkus best practices and removes dependency on custom fake classes.

**Files:** `/Users/jefferson/Documents/FIAP/fase-4/send-notification/src/test/java/com/fiap/notification/infra/handler/SendNotificationHandlerTest.java`

**Verify:** All test methods compile; `verify()`, `ArgumentCaptor`, and `doThrow()` imports are available (they come from existing `org.mockito:mockito-junit-jupiter` dependency in pom.xml).

---

### 6. Add necessary Mockito imports
- Ensure the test imports include:
  - `import org.mockito.ArgumentCaptor;`
  - `import org.mockito.Mockito.doThrow;` (or use `Mockito.doThrow()` with full package path)
  - `import org.mockito.verify;` (or use `Mockito.verify()` with full package path)
  - `import static org.mockito.Mockito.*;` (recommended for cleaner code)

**Files:** `/Users/jefferson/Documents/FIAP/fase-4/send-notification/src/test/java/com/fiap/notification/infra/handler/SendNotificationHandlerTest.java`

**Verify:** All Mockito methods resolve (no "cannot find symbol"); imports appear at the top of the file.

---

### 7. Run the full test suite and verify all tests pass
- Execute `cd /Users/jefferson/Documents/FIAP/fase-4 && mvn clean test -pl send-notification -q`.
- Confirm that all 83 tests in the module pass (SendNotificationHandlerTest should now pass all 16 test methods without failures or errors).
- Verify that other test classes in send-notification (NotificationConfigurationTest, SendNotificationUseCaseTest, SesNotificationSenderTest, NotificationTest) continue to pass.

**Rationale:** The Quarkus CDI container now properly initializes the handler with injected mocked dependencies, allowing all assertions to execute correctly.

**Files:** No file changes in this step; verification only.

**Verify:** Command output shows "Tests run: 83, Failures: 0, Errors: 0, Skipped: 0" and "BUILD SUCCESS".

---

## Key Changes Summary

| Item | Old | New |
|------|-----|-----|
| Class annotation | (none) | `@QuarkusTest` |
| Handler injection | Manual `new SendNotificationHandler()` + field assign in setUp | `@Inject SendNotificationHandler handler` |
| UseCase injection | `FakeSendNotificationUseCase extends SendNotificationUseCase` | `@InjectMock SendNotificationUseCase useCase` |
| Config injection | `TestNotificationConfiguration` inner class | `@InjectMock NotificationConfiguration config` |
| Call verification | `useCase.executeCallCount++` | `verify(useCase, times(N)).execute(any())` |
| Argument capture | `useCase.lastNotification` field | `ArgumentCaptor<Notification>.capture()` |
| Exception stubbing | `useCase.exceptionToThrow = ...` | `doThrow(...).when(useCase).execute(any())` |
| setUp() method | Required for manual init | Deleted; CDI handles init |

---

## Dependencies Already Available
The `send-notification/pom.xml` already includes:
- `io.quarkus:quarkus-junit5` (test scope) — provides `@QuarkusTest` annotation
- `org.mockito:mockito-core` (test scope) — provides `Mockito`, `ArgumentCaptor`, etc.
- `org.mockito:mockito-junit-jupiter` (test scope) — provides JUnit 5 integration for Mockito

No new dependencies need to be added to pom.xml.

---

## No Production Code Changes
This task modifies **only the test file**. No changes to handler, use case, configuration, or any other production code are required. The handler's `@ApplicationScoped` and `@Inject` annotations remain unchanged.

---

## Verification Command
```bash
cd /Users/jefferson/Documents/FIAP/fase-4 && mvn clean test -pl send-notification -q
```

**Expected outcome:**
```
Tests run: 83, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

All 16 test methods in `SendNotificationHandlerTest` will pass, including:
- testHandleRequest_SuccessfulSingleMessageProcessing
- testHandleRequest_ProcessesMultipleMessages
- testHandleRequest_ResilienceToErrorInOneMessage
- testHandleRequest_ReturnsNull
- testHandleRequest_HandlesNullEvent
- testHandleRequest_HandlesNullRecords
- testHandleRequest_HandlesMessageWithMissingFields
- testHandleRequest_DeserializesNotificationCorrectly
- testHandleRequest_UsesConfigAdminEmail
- testHandleRequest_NoExceptionThrownOnMessageError
- testHandleRequest_ContinuesProcessingAfterFirstMessageError
- testHandleRequest_HandlesUrgencyCorrectly
- testHandleRequest_HandlesEmptyRecordsList
- testHandleRequest_UsesCaseExecutedOncePerMessage
