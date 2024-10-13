package link.biosmarcel.presentation.persistence;

import org.eclipse.store.storage.embedded.types.EmbeddedStorageManager;

/**
 * Der Manager händelt die Live-Daten auf Low-Level Ebene. Hier werden Daten serialisiert, deserialisiert, persistiert
 * und geladen. Die direkte Interaktion mit dem Manager sollte außerhalb von Fällen wie Server-Start/Stop, Migration und
 * ähnliche selten nötig sein. Für den Datenzugriff sollte {@link StorageAccess} verwendet werden.
 */
public interface StorageManager extends StorageAccess
{
  /**
   * @return unterliegende Instanz von EclipseStore; <b>sollte spärlich verwendet werden</b>
   */
  EmbeddedStorageManager manager();
}
