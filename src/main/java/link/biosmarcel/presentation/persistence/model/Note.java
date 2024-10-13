package link.biosmarcel.presentation.persistence.model;

import link.biosmarcel.presentation.persistence.TransactionalList;
import link.biosmarcel.presentation.persistence.TransactionalObject;
import org.eclipse.serializer.ObjectCopier;
import org.eclipse.serializer.reference.Lazy;

import java.sql.Ref;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class Note extends TransactionalObject {
    private final UUID id;
    private final References references;
    private final ImmutableReferences immutableReferences;

    public Note(final User parentUser) {
        this.id = UUID.randomUUID();
        this.references = new References();
        this.immutableReferences = new ImmutableReferences(parentUser);
    }

    public Note(final Note copyFrom, final User parentUser) {
        this.id = UUID.randomUUID();
        this.references = copyFrom.references.clone();
        this.immutableReferences = new ImmutableReferences(parentUser);
    }

    private static class References {
        private String name;
        private Lazy<String> content;
        private LocalDate date;

        public References clone() {
            try ( final ObjectCopier copier = ObjectCopier.New() )
            {
                return copier.copy(this);
            }
        }
    }

    private static class ImmutableReferences {
        private final User parentUser;

        private ImmutableReferences(final User parentUser) {
            this.parentUser = parentUser;
        }
    }

    public UUID getId() {
        requireReadAccess();
        return this.id;
    }

    public User getParentUser() {
        requireReadAccess();
        return this.immutableReferences.parentUser;
    }

    public String getName() {
        requireReadAccess();
        return this.references.name;
    }

    public void setName(String name) {
        markDirty();
        this.references.name = name;
    }

    public String getContent() {
        requireReadAccess();
        return Lazy.get(this.references.content);
    }

    public void setContent(String content) {
        markDirty();
        this.references.content = Lazy.Reference(content);
    }

    public LocalDate getDate() {
        requireReadAccess();
        return this.references.date;
    }

    public void setDate(LocalDate date) {
        markDirty();
        this.references.date = date;
    }

    @Override
    public Object[] storeAdditionally() {
        return new Object[]{references};
    }
}

