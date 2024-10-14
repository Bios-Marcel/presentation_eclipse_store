package link.biosmarcel.presentation.persistence.model;

import link.biosmarcel.presentation.persistence.TransactionalList;
import link.biosmarcel.presentation.persistence.TransactionalObject;

import java.util.List;
import java.util.UUID;

public class User extends TransactionalObject {
    private final UUID id;
    private final References references;
    private final ImmutableReferences immutableReferences;

    public User(final String name,
                final String password,
                final Permissions permissions) {
        this.id = UUID.randomUUID();
        this.references = new References(
                name,
                password,
                permissions
        );
        this.immutableReferences = new ImmutableReferences();
    }

    private static class References {
        private String name;
        private String password;
        private Permissions permissions;

        private References(final String name,
                           final String password,
                           final Permissions permissions) {
            this.name = name;
            this.password = password;
            this.permissions = permissions;
        }
    }

    private static class ImmutableReferences {
        private final List<Note> notes = new TransactionalList<>();
    }

    public UUID getId() {
        requireReadAccess();
        return this.id;
    }

    public List<Note> notes() {
        requireReadAccess();
        return this.immutableReferences.notes;
    }

    public String getName() {
        requireReadAccess();
        return this.references.name;
    }

    public void setName(String name) {
        markDirty();
        this.references.name = name;
    }

    public String getPassword() {
        requireReadAccess();
        return this.references.password;
    }

    public void setPassword(String password) {
        markDirty();
        this.references.password = password;
    }

    public Permissions getPermissions() {
        requireReadAccess();
        return this.references.permissions;
    }

    public void setPermissions(Permissions permissions) {
        markDirty();
        this.references.permissions = permissions;
    }

    @Override
    public Object[] storeAdditionally() {
        return new Object[]{references};
    }
}
