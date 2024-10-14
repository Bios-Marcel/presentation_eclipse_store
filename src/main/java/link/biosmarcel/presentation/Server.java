import link.biosmarcel.presentation.Service;
import link.biosmarcel.presentation.persistence.StorageImpl;
import org.eclipse.store.afs.nio.types.NioFileSystem;
import org.eclipse.store.storage.embedded.types.EmbeddedStorageFoundation;
import org.eclipse.store.storage.types.StorageConfiguration;
import org.eclipse.store.storage.types.StorageLiveFileProvider;

void main() {
    final var storageManager = EmbeddedStorageFoundation
            .New()
            .setConfiguration(
                    StorageConfiguration
                            .Builder()
                            .setStorageFileProvider(StorageLiveFileProvider.New(
                                    NioFileSystem.New().ensureDirectoryPath("./storage")
                            ))
                            .createConfiguration()
            )
            .createEmbeddedStorageManager();

    final var storageImpl = new StorageImpl(storageManager);

    try {
        runDemonstration(storageImpl);
    } finally {
        storageManager.shutdown();
    }
}

private static void runDemonstration(StorageImpl storageImpl) {
    final var service = new Service(storageImpl);

    service.ensureDefaultUser();

    final var admin = service.findUser("admin");
    if(admin==null) {
        throw new IllegalStateException("The admin should've been created on startup, what's going on?");
    }

    service.insertNote(admin, "Genesis", "Let there be note");
    try {
        service.insertNote(admin, "Genesis?", "Let there be note?"); // <- Invalid note, no '?' allowed
    } catch (final Exception exception) {
        System.out.println("Error adding the second note!");
        exception.printStackTrace();
        // It's okay, let's keep going!
    }

    // List all users and their notes
    storageImpl.read(storageRoot -> {
        for (final var user : storageRoot.users()) {
            System.out.printf("Notes for user '%s':%n", user.getName());
            for(final var note : user.notes()) {
                System.out.printf("\t%s%n",note.getName());
            }
        }
    });
}
