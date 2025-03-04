package model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NoteTest {

    @Test
    void requireNonNullMessage() {
        assertThrows(NullPointerException.class, () -> new NoteEntity(1, null, "from", "to", System.currentTimeMillis(), 0));
    }

    @Test
    void requireNonNullFrom() {
        assertThrows(NullPointerException.class, () -> new NoteEntity(2, "message", null, "to", System.currentTimeMillis(), 0));
    }

    @Test
    void requireNonNullTo() {
        assertThrows(NullPointerException.class, () -> new NoteEntity(3, "message", "from", null, System.currentTimeMillis(), 0));
    }

    @Test
    void createNew() {
        assertDoesNotThrow(() -> new NoteEntity(4, "message", "from", "to", System.currentTimeMillis(), 5));
    }
}
