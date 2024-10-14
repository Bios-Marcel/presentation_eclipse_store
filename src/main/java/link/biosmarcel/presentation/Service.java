package link.biosmarcel.presentation;

import link.biosmarcel.presentation.persistence.StorageAccess;
import link.biosmarcel.presentation.persistence.model.Note;
import link.biosmarcel.presentation.persistence.model.Permissions;
import link.biosmarcel.presentation.persistence.model.User;
import org.eclipse.jdt.annotation.Nullable;

import java.time.LocalDate;

public final class Service {
    private final StorageAccess storageAccess;

    public Service(final StorageAccess storageAccess) {
        this.storageAccess = storageAccess;
    }

    public void ensureDefaultUser() {
        storageAccess.write((storageRoot, _) -> {
            if (storageRoot.users().isEmpty()) {
                System.out.println("Creating initial user ...");
                final var user = new User("admin", "password", Permissions.ADMIN);
                storageRoot.users().add(user);
                System.out.println("User created, the credentials are `admin=password`.");
            }
        });
    }

    public @Nullable User findUser(final String name) {
        return storageAccess.readReturn(storageRoot -> storageRoot.users().stream()
                .filter(user -> name.equals(user.getName()))
                .findFirst()
                .orElse(null));
    }

    public void insertNote(final User user,
                           final String title,
                           final String content) {
        storageAccess.write((_ ,_) -> {
            final var note = new Note(user);
            user.notes().add(note);

            note.setName(title);
            note.setDate(LocalDate.now());
            note.setContent(content);

            Validator.validateNote(note);
        });
    }

}
