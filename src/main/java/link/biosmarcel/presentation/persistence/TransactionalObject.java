package link.biosmarcel.presentation.persistence;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Allgemeine Klasse für (fast) alle modifizierbaren Objekte, die in EclipseStore gelagert werden sollen. Dies schließt
 * also Klassen wie {@link String} und {@link java.time.LocalDate} aus, da diese bei Veränderung immer eine neue Instanz
 * erzeugen und somit automatisch neu abgespeichert werden.
 *
 * <p>Klassen die dieses Interface nicht implementierbar sind, sind zwar speicherbar, verhalten sich aber bei
 * Modifikation nicht wie erwartet, da Änderungen aus Sub-Objekten z.B. nicht übernommen werden und es so zu
 * Dateninkonsistenzen kommt.</p>
 */
public class TransactionalObject
{
  private static final Object[] NOTHING = new Object[ 0 ];

  private transient @Nullable SaveState state;

  protected TransactionalObject()
  {
    // Der initiale State ist immer FRESH, da Konstruktoren immer nur bei der ersten Initialisierung des Objekts
    // aufgerufen werden. Beim Laden eines Datensatzes durch EclipseStore wird der Konstruktor nicht aufgerufen.
    // Daher ist dann der state "null", was gleichbedeutend mit SAVED ist.
    setSaveState( SaveState.FRESH );

    // Nötig, da das Objekt sonst nach dem impliziten Speichern durch den Parent auf dem FRESH state bleibt, es aber
    // auf SAVED gesetzt werden muss.
    //noinspection ThisEscapedInObjectConstruction
    requireTransaction().register( this );
  }

  /**
   * <b>Wahrscheinlich sollte hier meist {@link #requireTransaction()} verwendet werden.</b>
   *
   * @return die aktive Transaktion, welche aber ggf. nicht in einem validen Zustand ist, kann aber auch {@code null}
   *     sein
   */
  protected final @Nullable Transaction getTransaction()
  {
    return Transactions.getTransaction();
  }

  /**
   * @return die aktive Transaktion
   *
   * @throws IllegalStateException falls die Transaktion nicht vorhanden / nicht im korrekten Zustand ist.
   */
  final Transaction requireTransaction()
  {
    final var transaction = getTransaction();
    if ( transaction == null )
    {
      throw new IllegalStateException( "Keine Transaktion vorhanden:" + getClass() );
    }
    return transaction;
  }

  /**
   * Markiert das Objekt als {@link SaveState#DIRTY} und registriert es in der aktuellen Transaktion.
   *
   * @throws IllegalStateException falls die Transaktion nicht vorhanden / nicht im korrekten Zustand ist.
   */
  protected final void markDirty()
  {
    requireTransaction().markDirty( this );
  }

  protected final void requireReadAccess()
  {
    requireTransaction().validateForReadAccess();
  }

  /**
   * Bestimmt welche Objekte bei Commit der Transaktion zusätzlich zur Root-Ebene dieses Objekts gespeichert werden
   * sollen. Dies ist nötig da wir kein deep-store, sondern ein shallow-store machen, und bereits bekannte Objekte nicht
   * abgespeichert werden, wenn die Objekt-IDs bereits bekannt sind.
   */
  public Object[] storeAdditionally()
  {
    return NOTHING;
  }

  /**
   * @return aktueller {@link SaveState} des Objekts, aber nie {@code null}.
   */
  final SaveState getSaveState()
  {
    return switch ( state )
    {
      // Nach dem Load eines Objekts aus EclipseStore ist dies immer null, daher sind diese States gleichbedeutend.
      case null -> SaveState.SAVED;
      default -> state;
    };
  }

  final void setSaveState( final SaveState state )
  {
    this.state = state;
  }
}
