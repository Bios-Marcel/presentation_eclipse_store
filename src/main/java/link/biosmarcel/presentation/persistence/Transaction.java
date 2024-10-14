package link.biosmarcel.presentation.persistence;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.serializer.persistence.types.Storer;
import org.eclipse.serializer.persistence.util.Reloader;
import org.eclipse.serializer.reference.Lazy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * Hier wird eine Transaktion implementiert, angelehnt an die Transaktionen die man aus SQL kennt.
 *
 * <p>Sprich, Daten werden geholt, gegebenenfalls editiert und am Ende {@link #commit() gespeichert} oder
 * {@link #rollback() wiederhergestellt}. Dies ist eine atomare Aktion, die nicht durch andere Transaktionen gestört
 * werden kann.
 *
 * <p>Ein Unterschied zu z.B. SQL ist jedoch, dass die Daten "manuell" in der Transaktion registriert werden müssen.
 * Dies ist allerdings nicht die Aufgabe der Services, sondern die der DomainObjects. Jedes DomainObject registriert
 * sich intern, sobald es eine Änderung an sich selbst bemerkt. Dies ist nötig, da der Live-Datenbestand verändert wird
 * und wir niemals auf Kopien der Daten arbeiten.
 */
@org.eclipse.jdt.annotation.NonNullByDefault
public final class Transaction
{
  private final @Nullable Storer   storer;
  private final           Reloader reloader;

  /**
   * Alle potenziell zu speichernden Objekte. In einer ersten Iteration haben wir hier über ein {@link HashSet}
   * dedupliziert. Da wir aber nun bereits in {@link #markDirty(TransactionalObject)} und
   * {@link #register(TransactionalObject)} über die {@link SaveState SaveStates} deduplizieren, ist dies nicht mehr
   * nötig.
   */
  private final Collection<TransactionalObject> registered             = new ArrayList<>();
  /**
   * Beinhaltet Objekte, die nach dem Beenden der Transaktion aus dem speicher befreit werden sollen. Anders als bei
   * {@link #registered}, haben wir hier keinen Mechanismus für Deduplizierung. Wir gehen davon aus das wiederholte
   * {@link Lazy#clear()} Calls weniger kostspielig sind, als die Lazy-Objekte zu deduplizieren.
   */
  private final Collection<Lazy<?>>             unloadAfterTransaction = new HashSet<>();

  /** Markiert die Transaction als beendet, um zu verhindern, dass wir mehrmals Rollback oder commit ausführen. */
  private boolean finished = false;
  private boolean writable = true;

  private final Object usagesLock = new Object();
  private       int    usages     = 0;

  Transaction(
      final @Nullable Storer lazyStorer,
      final Reloader reloader
  )
  {
    this.storer = lazyStorer;
    this.reloader = reloader;
  }

  /**
   * Da wir einige Daten haben, die wir selten im Speicher wünschen, z.B. aufgrund dessen Größe, nutzen wir für diese
   * Felder {@link Lazy}. Diese Felder werden nach Ablauf einer bestimmten Zeit aus dem Speicher entfernt, sind jedoch
   * nicht über {@link java.lang.ref.WeakReference} realisiert, da sonst gegebenenfalls ungespeicherte Daten aus dem
   * Speicher entfernt werden würden.
   *
   * <p>Zwar gibt es {@link Lazy#clear()}, jedoch dürfen wir auch dies theoretisch nicht vor Abschluss einer
   * Transaktion aufrufen, da wir sonst gegebenenfalls ungespeicherte Daten vor Ende der Transaktion verworfen werden.
   * Stattdessen soll man diese Methode aufrufen, um der Transaktion mitzuteilen, welche Felder bei Abschluss entladen
   * werden müssen.</p>
   *
   * <p>Dies ist z.B. bei Fällen wie der Generierung von großen Mengen an Demo-Daten von Nöten, da wir dort sonst in
   * einen {@link OutOfMemoryError} laufen würden.</p>
   *
   * <p>Um zu verhindern dass Objekte, die vorher sowieso bereits geladen waren, unnötig neu geladen werden müssen,
   * kann {@link Lazy#isLoaded(Lazy)} verwendet werden.</p>
   * <p>
   *
   * <b>Beispiel</b>
   * {@snippet :
   * for (final var projekt : data.projekt()) {
   *   // Falls die Relations bereits geladen sind, weil z.B. ein Benutzer aktiv in diesem Projekt arbeitet, entladen
   *   // wir diese Daten nicht.
   *   if ( Lazy.isLoaded( projekt.relations() ) ){
   *     transaction.deferUnload( projekt.relations() );
   *   }
   *
   *   //... use relations
   * }
   *}
   */
  public void deferUnload( final Lazy<?> anyLazy )
  {
    unloadAfterTransaction.add( anyLazy );
  }

  /**
   * Registriert ein Objekt in der Transaktion als zu speicherndes Objekt. Dieser Aufruf sorgt noch nicht für eine
   * Speicherung, dies passiert erst bei Aufruf von {@link #commit()}. <b>ES IST WICHTIG, DASS REGISTER SO FRÜH WIE
   * MÖGLICH AUFGERUFEN WIRD.</b> Es muss vor Änderung des Datenobjektes aufgerufen werden. Dies stellt sicher, dass im
   * Falle eines {@link #rollback()}-Aufrufs auch alle mutierten Objekte wieder zurückgesetzt werden.
   *
   * <p>Zudem muss beachtet werden, dass bereits gespeicherte Objekte nicht "deep" gespeichert werden. Sprich, es wird
   * nur die erste Ebene der Felder serialisiert. Haben wir zum Beispiel ein Projekt, welches einen Benutzer
   * referenziert und wir ändern nun den Namen des Benutzers, so wird das {@code register(projekt)} nicht die Änderung
   * des Benutzernamens speichern. Um dies zu erreichen muss der Benutzer explizit registriert werden.</p>
   */
  void markDirty( final TransactionalObject object )
  {
    // Da dies überprüft ob Mutationen erlaubt sind, muss dies als am Anfang passieren, egal wann die Transaktion verwendet wird.
    validateForWriteAccess();

    final SaveState saveState = object.getSaveState();
    if ( saveState != SaveState.DIRTY )
    {
      if ( saveState != SaveState.REGISTERED )
      {
        if ( saveState == SaveState.FRESH )
        {
          throw new IllegalStateException(
              "FRESH Entities sollten über register bereits bei new() hinzugefügt werden" );
        }
        registered.add( object );
      }

      // Noch nicht bekannte Daten werden eh durch das Speichern des Parents (Hinzufügen zu einer Collection z.B.) gespeichert.
      if ( saveState == SaveState.SAVED )
      {
        object.setSaveState( SaveState.DIRTY );
      }
    }
  }

  /**
   * Registriert ein Objekt in der Transaktion, sorgt aber nicht für eine Speicherung. Die alleinige Registrierung sorgt
   * lediglich dafür, dass die Objekte bei Speicherung das Parents ebenfalls als gespeichert markiert werden.
   */
  void register( final TransactionalObject object )
  {
    // Da dies überprüft ob Mutationen erlaubt sind, muss dies als am Anfang passieren, egal wann die Transaktion verwendet wird.
    validateForWriteAccess();

    final SaveState saveState = object.getSaveState();
    if ( saveState != SaveState.DIRTY && saveState != SaveState.REGISTERED )
    {
      if ( saveState == SaveState.SAVED )
      {
        throw new IllegalStateException(
            "Bereits gespeichertes Objekt sollte als DIRTY markiert werden, nicht REGISTERED" );
      }

      registered.add( object );
      object.setSaveState( SaveState.REGISTERED );
    }
  }

  /**
   * @throws IllegalStateException falls die Transaktion nicht im korrekten Zustand ist.
   */
  void validateForWriteAccess()
  {
    if ( isFinished() )
    {
      throw new IllegalStateException( "Die Transaktion ist bereits abgeschlossen" );
    }
    if ( !isWritable() )
    {
      throw new IllegalStateException( "Die Transaktion ist read-only" );
    }
  }

  void validateForReadAccess()
  {
    if ( isFinished() )
    {
      throw new IllegalStateException( "Die Transaktion ist bereits abgeschlossen" );
    }
  }

  /***
   * @return {@code true} wenn diese Transaktion bereits committed oder rollbacked wurde.
   */
  public boolean isFinished()
  {
    return finished;
  }

  void setWritable( final boolean writable )
  {
    this.writable = writable;
  }

  /**
   * @return {@code true} wenn die Transaktion in einem Kontext verwendet wird, in dem die Benutzung von mutierenden
   *     Methoden erlaubt ist.
   */
  public boolean isWritable()
  {
    return writable;
  }

  /** Inkrementiert wie viele Nutzer (lockende Aufrufe) es gerade für diese Transaktion gibt. */
  void incUsages()
  {
    synchronized ( usagesLock )
    {
      usages++;
    }
  }

  /**
   * Dekrementiert wie viele Nutzer (lockende Aufrufe) es gerade für diese Transaktion gibt. Wenn es keine usages mehr
   * gibt, wird die Transaktion beendet und es wird {@code true} geliefert.
   */
  boolean decUsages()
  {
    synchronized ( usagesLock )
    {
      usages--;
      if ( usages == 0 )
      {
        finish();
        return true;
      }

      return false;
    }
  }

  /**
   * Speichert alle in der Transaktion registrierten Objekte in einer atomaren Aktion in den persistenten Storage.
   * Sollte währenddessen ein Fehler auftreten, wird {@link #rollback()} aufgerufen.
   */
  void commit()
  {
    if ( finished )
    {
      return;
    }

    assert storer != null;

    //noinspection OverlyBroadCatchBlock
    try
    {
      for ( final var object : registered )
      {
        switch ( object.getSaveState() )
        {
          case FRESH ->
          {
            throw new IllegalStateException(
                "Objekt ist frisch, aber Teil einer Transaktion. Es sollte erst auf registriert gesetzt werden" );
          }
          case REGISTERED ->
          {
            // Noch nicht gespeichert, passiert aber implizit durch save des Parents.
            object.setSaveState( SaveState.SAVED );
          }
          case SAVED ->
          {
            // Bereits gespeichert und unverändert.
          }
          case DIRTY ->
          {
            for ( final var toStore : object.storeAdditionally() )
            {
              storer.store( toStore );
              // Die Objekte sind nun bekannt, bedeutet ab jetzt können sie dirty werden und sind aktualisierbar.
            }
            storer.store( object );
            object.setSaveState( SaveState.SAVED );
          }
        }
      }

      storer.commit();
    }
    catch ( final RuntimeException exception )
    {
      rollback();
      throw exception;
    }
    finally
    {
      finish();
    }
  }

  /**
   * Macht die Änderungen an allen in der Transaktion veränderten Daten wieder rückgängig, indem die Daten aus dem
   * unterliegenden Speicher-System neu-geladen werden und in die im Speicher lebenden Objekte überführt werden.
   */
  public void rollback()
  {
    if ( finished )
    {
      return;
    }

    try
    {
      for ( final var object : registered )
      {
        for ( final var toStore : object.storeAdditionally() )
        {
          reloader.reloadFlat( toStore );
        }

        reloader.reloadFlat( object );

        // Theoretisch kann es passieren, dass wir ein zuvor als NOT_STORED markiertes Objekt als STORED markieren.
        // Da wir jedoch die parents sowieso reverten, werden diese Objekte nicht mehr referenziert und sorgen somit
        // nicht für weitere Probleme.
        object.setSaveState( SaveState.SAVED );
      }
    }
    finally
    {
      finish();
    }
  }

  /**
   * Markiert die Transaktion unabhängig davon, ob erfolgreich oder nicht als beendet und räumt auf. Außerdem werden
   * hier die in {@link #deferUnload(Lazy)} angegebenen {@link Lazy}-Instanzen {@link Lazy#clear() gecleart}.
   */
  void finish()
  {
    if ( finished )
    {
      return;
    }

    for ( final var lazy : unloadAfterTransaction )
    {
      if ( lazy.isStored() )
      {
        lazy.clear();
      }
    }

    unloadAfterTransaction.clear();
    if ( storer != null )
    {
      storer.clear();
    }
    registered.clear();

    finished = true;
  }
}
