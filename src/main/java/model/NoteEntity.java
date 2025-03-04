package model;

import java.util.Objects;

public record NoteEntity(int id, String message, String from, String to, long timestamp, int fame) {
    private static final int PLACEHOLDER_ID = -1;

    public NoteEntity {
        Objects.requireNonNull(message);
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);
    }

    public static NoteEntity createNormal(String message, String from, String to, long timestamp) {
        return new NoteEntity(PLACEHOLDER_ID, message, from, to, timestamp, 0);
    }

    public static NoteEntity createGift(String message, String from, String to, long timestamp) {
        return new NoteEntity(PLACEHOLDER_ID, message, from, to, timestamp, 1);
    }
}
