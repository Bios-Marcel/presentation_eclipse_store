package link.biosmarcel.presentation;

import link.biosmarcel.presentation.persistence.model.Note;

public final class Validator {
    private Validator() {
    }

    public static void validateNote(final Note note) {
        if (note.getName().isBlank()) {
            throw new IllegalStateException("Note name must not be blank");
        }

        if (note.getContent().contains("?")) {
            throw new IllegalStateException("Note content does not support question marks");
        }
    }
}
