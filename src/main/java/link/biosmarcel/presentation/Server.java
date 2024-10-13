import link.biosmarcel.presentation.persistence.StorageImpl;
import link.biosmarcel.presentation.persistence.model.Permissions;
import link.biosmarcel.presentation.persistence.model.User;
import org.eclipse.store.afs.nio.types.NioFileSystem;
import org.eclipse.store.storage.embedded.types.EmbeddedStorageFoundation;
import org.eclipse.store.storage.types.StorageConfiguration;
import org.eclipse.store.storage.types.StorageLiveFileProvider;

public static void main(String[] args) {
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

    storageImpl.write((storageRoot, transaction) -> {
        if (storageRoot.users().isEmpty()) {
            System.out.println("Creating initial user ...");
            final var user = new User("admin", "password", Permissions.ADMIN);
            storageRoot.users().add(user);
            System.out.println("User created, the credentials are `admin=password`.");
        }
    });

    System.out.println("Please login!");
}
