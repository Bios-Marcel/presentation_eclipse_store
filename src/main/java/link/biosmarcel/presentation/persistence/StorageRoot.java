package link.biosmarcel.presentation.persistence;

import link.biosmarcel.presentation.persistence.model.License;
import link.biosmarcel.presentation.persistence.model.User;
import org.eclipse.jdt.annotation.Nullable;

import java.util.List;

/**
 * Diese Klasse repräsentiert die komplette serverseitige Datenstruktur. Es ist unser Root-Objekt, sprich alle
 * anderen DomainObjects sind Teil dieser Hierarchie. Die meisten davon sind in {@link List Listen} und stellen quasi
 * "Tabellen" dar. Eine Ausnahme zum Beispiel ist {@link License}, da diese immer nur 0 bis 1 benötigt wird.
 *
 * <p>Wichtig ist, dass alle Felder, die nicht flach / immutable sind, auch
 * {@link TransactionalObject transaktionale Objekte} sein müssen. Zusätzlich gibt es noch
 * {@link java.util.Collections} die transaktional sind, wie zum Beispiel {@link TransactionalList}. Typen wie
 * {@link String} und {@link java.time.LocalDate} sind immutable, sprich sobald es eine neue Referenz gibt, sind diese
 * Objekte unbekannt und müssen daher nicht explizit gespeichert werden und müssen somit auch nicht transaktional
 * sein.</p>
 *
 * <p>Jegliche Änderungen an diesem Objekt-Graphen, sind Änderungen an den Live-Daten des Servers. Allerdings sind
 * Änderungen nur innerhalb einer {@link Transaction} möglich. Diese kann über den {@link StorageAccess} erstellt
 * werden.</p>
 */
public class StorageRoot extends TransactionalObject
{
  private final List<User>                           users;

  private @Nullable License license;

  public StorageRoot()
  {
    this.users = new TransactionalList<>();
  }

  public List<User> users()
  {
    requireReadAccess();
    return users;
  }

  public @Nullable License getLicense() {
    requireReadAccess();
    return license;
  }

  public void setLicense(@Nullable License license) {
    markDirty();
    this.license = license;
  }
}
