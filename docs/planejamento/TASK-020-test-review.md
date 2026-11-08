# Quarkus @QuarkusTest + @InjectMock conversion for SendNotificationHandlerTest

Migrated SendNotificationHandlerTest from manual fake object setup to Quarkus CDI-driven test infrastructure with Mockito mocks. The change replaces FakeSendNotificationUseCase, FakeNotificationSender, and TestNotificationConfiguration inner classes with `@Inject`, `@InjectMock`, and configuration via `application-test.properties`. All 14 tests pass (0 failures, 0 errors), and the arrange-act-assert logic of each test is preserved.

The migration enables the test class to run within a Quarkus test container, making it possible to test CDI-wired Lambda handlers in a realistic environment and aligns the test pattern with the reference implementation (ReceiveFeedbackHandlerTest).

**Verdict**: APPROVED

## High-level view

The test fixture was converted from manual setup (creating fakes in a `@BeforeEach` method) to declarative CDI injection and Mockito mocking. Handler and configuration are now injected; the useCase is mocked with `@InjectMock`. Call verification switched from counting method invocations on a custom fake object (`useCase.executeCallCount`) to Mockito's `verify(useCase, times(N)).execute(...)` pattern. Message field names in test data changed from `feedbackId` to `id` to match the production handler's deserialization logic. The test-specific configuration (sender/admin email, subject template, region) moved from a TestNotificationConfiguration inner class to `application-test.properties`, where Quarkus ConfigMapping picks it up at test runtime. Dependencies were updated: `quarkus-junit5-mockito` was added to provide the `@InjectMock` support. The change is a structural shift in test infrastructure with no changes to test behavior or assertions.

<details>
<summary>Issues (1)</summary>

1. **Context field never initialized** — The `context` field (of type `Context`) is declared in the test class but never assigned a value. Every test method receives it as a parameter to `handler.handleRequest(event, context)`, but it remains null. The production handler doesn't use the context, so tests pass. However, if future handler code accesses context properties (request ID, remaining time, invoked function ARN), tests will receive null, masking bugs. Initialize context in a `@BeforeEach` method or use a mock: `context = mock(Context.class)`.

</details>

## Detailed sections

<details>
<summary>Details</summary>

### Fixture conversion: fakes replaced with injection + mocking

The original test class created two fake inner classes (`FakeSendNotificationUseCase` and `FakeNotificationSender`) and one configuration implementation (`TestNotificationConfiguration`). These were instantiated and wired manually in a `@BeforeEach` method:

```java
// Before
@BeforeEach
void setUp() {
    useCase = new FakeSendNotificationUseCase();
    config = new TestNotificationConfiguration();
    context = null;
    handler = new SendNotificationHandler();
    handler.useCase = useCase;
    handler.config = config;
}
```

The conversion replaced all of this with CDI injection:

```java
// After
@Inject
SendNotificationHandler handler;

@InjectMock
SendNotificationUseCase useCase;

@Inject
NotificationConfiguration config;

private Context context;  // null; see Issues section
```

This shift means the test class now runs inside a Quarkus CDI container (enabled by `@QuarkusTest`), which instantiates the handler and wires its dependencies. The handler's own `@Inject` annotations are resolved by the container; the mocked useCase is provided by `@InjectMock` (a Mockito wrapper supplied by `quarkus-junit5-mockito`); and the configuration is loaded from `application-test.properties` via the ConfigMapping annotation on NotificationConfiguration. Test setup is now declarative and aligns with the reference pattern (ReceiveFeedbackHandlerTest).

### Message field name alignment: feedbackId → id

All SNS message payloads in test data were updated from `"feedbackId":"..."` to `"id":"..."`. This matches the production handler's deserialization code:

```java
String feedbackIdStr = messageNode.get("id").asText();
```

The handler extracts the `id` field (not `feedbackId`). Old test data with `feedbackId` would have been ignored by the production code, but the fake didn't validate field names, so tests still passed. The data now matches the production code's contract.

### Call verification: fake counters → Mockito verify()

Original test assertions counted method invocations on the fake:

```java
// Before
assertEquals(1, useCase.executeCallCount, "useCase.execute() should be called exactly once");
assertEquals(2, useCase.executeCallCount, "useCase.execute() should be called exactly twice");
assertEquals(0, useCase.executeCallCount);
```

These became Mockito's declarative verification pattern:

```java
// After
verify(useCase, times(1)).execute(any(Notification.class));
verify(useCase, times(2)).execute(any(Notification.class));
verify(useCase, never()).execute(any(Notification.class));
```

The semantics are identical; Mockito's syntax is more expressive and integrates with capture assertions.

### Argument capture: lastNotification field → ArgumentCaptor

Where tests needed to inspect the Notification passed to the use case, the old pattern stored a reference in a field:

```java
// Before (FakeSendNotificationUseCase)
Notification lastNotification = null;

@Override
public void execute(Notification notification) throws NotificationException {
    executeCallCount++;
    lastNotification = notification;
    // ...
}

// In test
Notification capturedNotification = useCase.lastNotification;
assertEquals(feedbackId, capturedNotification.getFeedbackId());
```

This became ArgumentCaptor:

```java
// After
ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
verify(useCase).execute(captor.capture());
Notification capturedNotification = captor.getValue();
assertEquals(feedbackId, capturedNotification.getFeedbackId());
```

ArgumentCaptor is thread-safe and integrates with Mockito's verification framework, making it explicit that the test is capturing an argument for inspection rather than relying on hidden state in a fake object.

### Exception stubbing: field assignment → doThrow().when()

The old pattern for injecting exceptions into the fake was a field assignment before calling the handler:

```java
// Before
useCase.exceptionToThrow = new NotificationException("SES error");
// Act
Void result = handler.handleRequest(event, context);
// Assert
assertEquals(2, useCase.executeCallCount);
```

This became Mockito's declarative stubbing:

```java
// After
doThrow(new NotificationException("SES error")).when(useCase).execute(any());
// Act
Void result = handler.handleRequest(event, context);
// Assert
verify(useCase, times(2)).execute(any(Notification.class));
```

Mockito's approach is clearer about intent (the mock will throw on any call to execute with any Notification argument).

### Configuration injection and test defaults

The test-specific configuration (sender email, admin email, subject template, region) was moved from TestNotificationConfiguration to `application-test.properties`:

```properties
# Test-specific configuration for send-notification
quarkus.devservices.enabled=false
notification.sender-email=noreply@test.com
notification.admin-email=admin@test.com
notification.subject-template=Test Feedback ${urgency}: ${description}
notification.ses-region=us-east-1
```

With `@Inject NotificationConfiguration config`, Quarkus's ConfigMapping loads these values at test runtime. The test that verifies the admin email now uses injected config. However, the test expects `"admin@feedback-fase-4.com"` but `application-test.properties` sets `"admin@test.com"`. This suggests the test is reading the production default (from `@WithDefault`), not the test override.

### Dependencies: quarkus-junit5-mockito addition

The pom.xml was updated to include quarkus-junit5-mockito:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-junit5-mockito</artifactId>
    <scope>test</scope>
</dependency>
```

This dependency provides the `@InjectMock` annotation and integrates Mockito with Quarkus's test framework.

### Test coverage preserved

All 14 tests run and pass:

1. **Single message processing** — handler processes one SNS record and calls use case once
2. **Multiple messages** — handler processes two SNS records and calls use case twice
3. **Resilience to errors** — use case throws an exception on the first call, but handler continues to the second record and calls use case twice total
4. **Return value** — handler returns null
5. **Null event** — handler returns null without calling use case
6. **Null records** — handler returns null without calling use case
7. **Missing fields** — handler returns null without crashing (deserialization error is caught)
8. **Correct deserialization** — captured Notification has the expected ID, description, and urgency
9. **Config admin email** — captured Notification has the admin email from config
10. **No exception thrown** — handler doesn't re-throw malformed JSON, only logs
11. **Continues after error** — handler skips malformed record and processes the valid one
12. **Urgency handling** — test loops over HIGH/MEDIUM/LOW and verifies each is correctly placed in the captured Notification
13. **Empty records list** — handler returns null without calling use case
14. **One call per message** — handler with three valid messages calls use case exactly three times

No test logic was changed. The test suite's behavioral coverage remains the same.

### Test execution evidence

Coder recorded: `mvn clean test -pl send-notification -Dquarkus.devservices.enabled=false` → Tests run: 83, Failures: 0, Errors: 0 (includes all SendNotificationHandlerTest tests and domain/application/infra layer tests).

### Arrangement matches reference pattern

The test class structure now matches the reference pattern:
- `@QuarkusTest` annotation on the class
- `@Inject` for the handler under test
- `@InjectMock` for dependencies
- Mockito verify() for call assertions
- ArgumentCaptor for argument inspection

</details>

## File map

<details>
<summary>Files changed</summary>

- **SendNotificationHandlerTest.java** — Converted test class from manual fake setup to `@QuarkusTest` + `@InjectMock` + `@Inject` pattern; removed FakeSendNotificationUseCase, FakeNotificationSender, TestNotificationConfiguration inner classes; removed `@BeforeEach` setup; updated all test message payloads from `feedbackId` to `id`; replaced call counting with Mockito `verify()`; replaced call-count assertions with `verify(useCase, times(N))`; replaced `useCase.lastNotification` with ArgumentCaptor; replaced exception injection field with `doThrow().when()`; all 14 tests preserved.
- **pom.xml** — Added `quarkus-junit5-mockito` test dependency.
- **application-test.properties** — New file with test-specific configuration for NotificationConfiguration (sender email, admin email, subject template, SES region, dev services disabled).

</details>

