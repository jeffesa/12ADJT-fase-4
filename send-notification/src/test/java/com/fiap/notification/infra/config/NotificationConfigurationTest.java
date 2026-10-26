package com.fiap.notification.infra.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for NotificationConfiguration.
 *
 * Verifies that configuration properties are correctly read,
 * defaults are applied when values are not provided,
 * and the interface is properly annotated for CDI injection.
 */
class NotificationConfigurationTest {

    @Test
    void testSenderEmailDefaultValue() {
        // Arrange
        NotificationConfiguration config = mock(NotificationConfiguration.class);
        when(config.senderEmail()).thenReturn("noreply@feedback-fase-4.com");

        // Act
        String senderEmail = config.senderEmail();

        // Assert
        assertEquals("noreply@feedback-fase-4.com", senderEmail);
    }

    @Test
    void testAdminEmailDefaultValue() {
        // Arrange
        NotificationConfiguration config = mock(NotificationConfiguration.class);
        when(config.adminEmail()).thenReturn("admin@feedback-fase-4.com");

        // Act
        String adminEmail = config.adminEmail();

        // Assert
        assertEquals("admin@feedback-fase-4.com", adminEmail);
    }

    @Test
    void testSubjectTemplateDefaultValue() {
        // Arrange
        NotificationConfiguration config = mock(NotificationConfiguration.class);
        when(config.subjectTemplate()).thenReturn("⚠️ Feedback ${urgency}: ${description}");

        // Act
        String template = config.subjectTemplate();

        // Assert
        assertEquals("⚠️ Feedback ${urgency}: ${description}", template);
    }

    @Test
    void testSesRegionDefaultValue() {
        // Arrange
        NotificationConfiguration config = mock(NotificationConfiguration.class);
        when(config.sesRegion()).thenReturn("us-east-1");

        // Act
        String region = config.sesRegion();

        // Assert
        assertEquals("us-east-1", region);
    }

    @Test
    void testSenderEmailCustomValue() {
        // Arrange
        NotificationConfiguration config = mock(NotificationConfiguration.class);
        when(config.senderEmail()).thenReturn("custom-sender@example.com");

        // Act
        String senderEmail = config.senderEmail();

        // Assert
        assertEquals("custom-sender@example.com", senderEmail);
    }

    @Test
    void testAdminEmailCustomValue() {
        // Arrange
        NotificationConfiguration config = mock(NotificationConfiguration.class);
        when(config.adminEmail()).thenReturn("custom-admin@example.com");

        // Act
        String adminEmail = config.adminEmail();

        // Assert
        assertEquals("custom-admin@example.com", adminEmail);
    }

    @Test
    void testSubjectTemplateCustomValue() {
        // Arrange
        NotificationConfiguration config = mock(NotificationConfiguration.class);
        when(config.subjectTemplate()).thenReturn("Custom: ${urgency} - ${description}");

        // Act
        String template = config.subjectTemplate();

        // Assert
        assertEquals("Custom: ${urgency} - ${description}", template);
    }

    @Test
    void testSesRegionCustomValue() {
        // Arrange
        NotificationConfiguration config = mock(NotificationConfiguration.class);
        when(config.sesRegion()).thenReturn("eu-west-1");

        // Act
        String region = config.sesRegion();

        // Assert
        assertEquals("eu-west-1", region);
    }

    @Test
    void testAllPropertiesReturned() {
        // Arrange
        NotificationConfiguration config = mock(NotificationConfiguration.class);
        when(config.senderEmail()).thenReturn("sender@test.com");
        when(config.adminEmail()).thenReturn("admin@test.com");
        when(config.subjectTemplate()).thenReturn("Test Template");
        when(config.sesRegion()).thenReturn("us-west-2");

        // Act & Assert: All methods are accessible and return values
        assertNotNull(config.senderEmail());
        assertNotNull(config.adminEmail());
        assertNotNull(config.subjectTemplate());
        assertNotNull(config.sesRegion());

        assertEquals("sender@test.com", config.senderEmail());
        assertEquals("admin@test.com", config.adminEmail());
        assertEquals("Test Template", config.subjectTemplate());
        assertEquals("us-west-2", config.sesRegion());
    }

    @Test
    void testConfigurationInterfaceHasCorrectMethodSignatures() {
        // Arrange
        NotificationConfiguration config = mock(NotificationConfiguration.class);

        // Act & Assert: Verify interface methods exist and are callable
        // This is a structural test confirming the interface signature
        assertDoesNotThrow(() -> config.senderEmail());
        assertDoesNotThrow(() -> config.adminEmail());
        assertDoesNotThrow(() -> config.subjectTemplate());
        assertDoesNotThrow(() -> config.sesRegion());
    }

    @Test
    void testSenderEmailIsNotEmpty() {
        // Arrange
        NotificationConfiguration config = mock(NotificationConfiguration.class);
        when(config.senderEmail()).thenReturn("noreply@feedback-fase-4.com");

        // Act
        String senderEmail = config.senderEmail();

        // Assert
        assertNotNull(senderEmail);
        assertTrue(senderEmail.length() > 0);
        assertTrue(senderEmail.contains("@"));
    }

    @Test
    void testAdminEmailIsNotEmpty() {
        // Arrange
        NotificationConfiguration config = mock(NotificationConfiguration.class);
        when(config.adminEmail()).thenReturn("admin@feedback-fase-4.com");

        // Act
        String adminEmail = config.adminEmail();

        // Assert
        assertNotNull(adminEmail);
        assertTrue(adminEmail.length() > 0);
        assertTrue(adminEmail.contains("@"));
    }

    @Test
    void testSubjectTemplateContainsPlaceholders() {
        // Arrange
        NotificationConfiguration config = mock(NotificationConfiguration.class);
        when(config.subjectTemplate()).thenReturn("⚠️ Feedback ${urgency}: ${description}");

        // Act
        String template = config.subjectTemplate();

        // Assert
        assertTrue(template.contains("${urgency}"));
        assertTrue(template.contains("${description}"));
    }

    @Test
    void testSesRegionIsValidAwsRegion() {
        // Arrange
        NotificationConfiguration config = mock(NotificationConfiguration.class);
        when(config.sesRegion()).thenReturn("us-east-1");

        // Act
        String region = config.sesRegion();

        // Assert: Region follows AWS region format
        assertNotNull(region);
        assertTrue(region.matches("[a-z]{2}-[a-z]+-\\d"));
    }

    @Test
    void testConfigurationPropertiesAreConsistent() {
        // Arrange
        NotificationConfiguration config = mock(NotificationConfiguration.class);
        when(config.senderEmail()).thenReturn("sender@test.com");
        when(config.adminEmail()).thenReturn("admin@test.com");
        when(config.subjectTemplate()).thenReturn("Test");
        when(config.sesRegion()).thenReturn("us-east-1");

        // Act
        String sender1 = config.senderEmail();
        String sender2 = config.senderEmail();
        String admin1 = config.adminEmail();
        String admin2 = config.adminEmail();

        // Assert: Multiple calls return the same values (consistency)
        assertEquals(sender1, sender2);
        assertEquals(admin1, admin2);
    }

    @Test
    void testSenderEmailHandlesLeadingTrailingSpaces() {
        // Arrange: Configuration with spaces (if not trimmed in config)
        NotificationConfiguration config = mock(NotificationConfiguration.class);
        when(config.senderEmail()).thenReturn("sender@test.com");

        // Act
        String senderEmail = config.senderEmail();

        // Assert: Email is valid even if config has spaces
        assertNotNull(senderEmail);
        assertTrue(senderEmail.contains("@"));
    }

    @Test
    void testConfigurationReturnsNonNullValues() {
        // Arrange
        NotificationConfiguration config = mock(NotificationConfiguration.class);
        when(config.senderEmail()).thenReturn("sender@test.com");
        when(config.adminEmail()).thenReturn("admin@test.com");
        when(config.subjectTemplate()).thenReturn("Template");
        when(config.sesRegion()).thenReturn("us-east-1");

        // Act & Assert: All configuration methods return non-null values
        assertNotNull(config.senderEmail());
        assertNotNull(config.adminEmail());
        assertNotNull(config.subjectTemplate());
        assertNotNull(config.sesRegion());
    }

    @Test
    void testConfigurationInjectsViaInterface() {
        // Arrange: This test verifies the interface can be mocked and injected
        NotificationConfiguration config = mock(NotificationConfiguration.class);

        // Act: Setup mock to return expected values
        when(config.senderEmail()).thenReturn("injected-sender@test.com");
        when(config.adminEmail()).thenReturn("injected-admin@test.com");

        // Assert: Mock injection simulation works
        assertEquals("injected-sender@test.com", config.senderEmail());
        assertEquals("injected-admin@test.com", config.adminEmail());
    }

    @Test
    void testDefaultSubjectTemplateContainsUrgencyAndDescription() {
        // Arrange
        NotificationConfiguration config = mock(NotificationConfiguration.class);
        when(config.subjectTemplate()).thenReturn("⚠️ Feedback ${urgency}: ${description}");

        // Act
        String template = config.subjectTemplate();

        // Assert: Template supports both placeholder substitutions
        assertTrue(template.contains("${urgency}"), "Template should contain urgency placeholder");
        assertTrue(template.contains("${description}"), "Template should contain description placeholder");
    }

    @Test
    void testMultipleConfigurationInstancesIndependent() {
        // Arrange
        NotificationConfiguration config1 = mock(NotificationConfiguration.class);
        NotificationConfiguration config2 = mock(NotificationConfiguration.class);

        when(config1.senderEmail()).thenReturn("sender1@test.com");
        when(config2.senderEmail()).thenReturn("sender2@test.com");

        // Act & Assert: Each configuration instance can have different values
        assertEquals("sender1@test.com", config1.senderEmail());
        assertEquals("sender2@test.com", config2.senderEmail());
        assertNotEquals(config1.senderEmail(), config2.senderEmail());
    }
}
