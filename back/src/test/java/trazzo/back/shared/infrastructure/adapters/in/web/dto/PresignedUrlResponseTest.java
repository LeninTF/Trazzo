package trazzo.back.shared.infrastructure.adapters.in.web.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PresignedUrlResponseTest {

    @Test
    void shouldCreateRecordWithAllFields() {
        var response = new PresignedUrlResponse("https://r2.example.com/upload", "evidences/123/doc.pdf");

        assertEquals("https://r2.example.com/upload", response.presignedUrl());
        assertEquals("evidences/123/doc.pdf", response.objectKey());
    }

    @Test
    void shouldAllowNullValues() {
        var response = new PresignedUrlResponse(null, null);

        assertNull(response.presignedUrl());
        assertNull(response.objectKey());
    }

    @Test
    void shouldBeEqualForSameValues() {
        var r1 = new PresignedUrlResponse("url", "key");
        var r2 = new PresignedUrlResponse("url", "key");

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void shouldHaveDescriptiveToString() {
        var response = new PresignedUrlResponse("url", "key");

        String str = response.toString();
        assertTrue(str.contains("url"));
        assertTrue(str.contains("key"));
    }
}
