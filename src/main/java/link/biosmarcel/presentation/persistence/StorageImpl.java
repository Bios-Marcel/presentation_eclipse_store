package link.biosmarcel.presentation.persistence;

import org.eclipse.serializer.persistence.util.Reloader;
import org.eclipse.store.storage.embedded.types.EmbeddedStorageManager;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Implementiert (vorerst) als einzige Klasse {@link StorageAccess} und ist somit im Production-Code und im Test-Code
 * zuständig für den Datenzugriff. Diese Implementation behandelt dementsprechend auch das Locking und die
 * Transaktionen.
 */
public class StorageImpl implements StorageManager
{
  private static final System.Logger logger = System.getLogger( StorageImpl.class.getName() );

  @SuppressWarnings( "FieldCanBeLocal" )
  private final ReentrantReadWriteLock           lock;
  private final ReentrantReadWriteLock.ReadLock  readLock;
  private final ReentrantReadWriteLock.WriteLock writeLock;
  private final EmbeddedStorageManager           manager;

  // Aufgrund dessen, dass wir Transaktionen nun sowohl für read als auch write verwenden, müssen wir beim Erstellen
  // der Transaktion locken, da wir sonst ggf. im read-Fall mehrere Transaktionen gleichzeitig öffnen.
  private final Object transactionLock = new Object();

  private final Reloader    reloader;
  private final StorageRoot storageRoot;

  /**
   * Dieser Konstruktor ruft auch direkt {@link EmbeddedStorageManager#start()} auf, sprich der Datenzugriff ist direkt
   * nach dem Konstruktor-Aufruf möglich. Hierzu ist es wichtig, dass wir nicht bereits eine gestartete Instanz
   * übergeben.
   */
  public StorageImpl( final EmbeddedStorageManager manager )
  {
    this.lock = new ReentrantReadWriteLock();
    this.readLock = lock.readLock();
    this.writeLock = lock.writeLock();

    this.manager = manager;

    manager.start();

    this.reloader = Reloader.New( manager.persistenceManager() );

    // Wenn wir den Storage das erste Mal starten, gibt es noch kein Objekt.
    if ( manager.root() == null )
    {
      // Da Data auch Transactional ist, brauchen wir hier eine Transaktion.
      final var transaction = new Transaction(
          // Wir gehen davon aus, dass der LazyStorer früher oder später eh benötigt wird, da wir Transaktionen
          // immer nur im Write-Kontext verwenden, nicht aber im Read-Kontext, daher initialisieren wir diesen eager.
          // Falls sich das in der Zukunft ändert, sollten wir das in ein Lambda auslagern.
          manager.createLazyStorer(),
          reloader );
      Transactions.setTransaction( transaction );

      // Wir setzen dieses dann um uns in der Zukunft darauf verlassen zu können, dass es vom korrekten Typ ist.
      // Nach diesem Call sollte dieser Code-Pfad nie wieder aufgerufen werden können.
      manager.setRoot( new StorageRoot() );
      manager.storeRoot();

      // Theoretisch ist das Objekt bereits gestored, jedoch setzt die Transaktion auch die korrekte stored states.
      transaction.commit();
      Transactions.setTransaction( null );
    }

    storageRoot = (StorageRoot) manager.root();
  }

  @Override
  public void read( final Consumer<StorageRoot> reader )
  {
    // Der Einfachheit halber, rufen wir hier readReturn auf, um keinen Code zu duplizieren.
    readReturn( ( d ) ->
    {
      reader.accept( d );
      return null;
    } );
  }

  @Override
  public <Result> Result readReturn( final Function<StorageRoot, Result> reader )
  {
    Transaction currentTransaction = null;
    try
    {
      readLock.lock();

      // Wenn wir geschachtelte Calls in der Form Write(Read(...), Write(...)) haben, dann müssen wir die Transaktion
      // temporär (während des Read-Calls) als Read-Only markieren und bei Eintritt des zweiten (geschachtelten)
      // Write-Calls wieder als beschreibbar.
      boolean oldAllowWrite = false;

      synchronized ( transactionLock )
      {
        currentTransaction = Transactions.getTransaction();
        if ( currentTransaction == null )
        {
          currentTransaction = new Transaction( null /* Wir schreiben eh nicht */, reloader );
          Transactions.setTransaction( currentTransaction );
        }
        else
        {
          oldAllowWrite = currentTransaction.isWritable();
        }
        currentTransaction.setWritable( false );
        currentTransaction.incUsages();
      }

      final Result result = reader.apply( storageRoot );

      currentTransaction.setWritable( oldAllowWrite );

      return result;
    }
    // Ein Catch für Rollback ist nicht nötig, da die Exception an den umliegenden Write-Call weitergeleitet wird.
    finally
    {
      synchronized ( transactionLock )
      {
        // Im Read-Fall dürfen wir die Transaktion nur abräumen, wenn es keine andere lesende Instanz mehr gibt.
        // Dies wird durch die getrackten Usages sichergestellt.
        if ( currentTransaction != null && currentTransaction.decUsages() )
        {
          Transactions.setTransaction( null );
        }
      }
      readLock.unlock();
    }
  }

  @Override
  public void write( final BiConsumer<StorageRoot, Transaction> writer )
  {
    // Der Einfachheit halber, rufen wir hier writeReturn auf, um keinen Code zu duplizieren.
    writeReturn( ( d, transaction ) ->
    {
      writer.accept( d, transaction );
      return null;
    } );
  }

  @Override
  public <Result> Result writeReturn( final BiFunction<StorageRoot, Transaction, Result> writer )
  {
    if ( lock.getReadHoldCount() > 0 )
    {
      throw new IllegalStateException(
          "Read(Write(...)) wird nicht unterstützt, da so writes im read-Kontext möglich wären." );
    }

    try
    {
      writeLock.lock();

      Transaction currentTransaction;
      synchronized ( transactionLock )
      {
        currentTransaction = Transactions.getTransaction();
        if ( currentTransaction == null )
        {
          currentTransaction = new Transaction( manager.createLazyStorer(), reloader );
          Transactions.setTransaction( currentTransaction );
        }
        currentTransaction.setWritable( true );
        currentTransaction.decUsages();
      }
      final Result result = writer.apply( storageRoot, currentTransaction );

      // Wenn wir mehrere nested Calls haben, wollen wir erst vor dem Lösen des letzten Locks committen.
      if ( writeLock.getHoldCount() == 1 )
      {
        currentTransaction.commit();
      }

      return result;
    }
    catch ( final RuntimeException exception )
    {
      final var currentTransaction = Transactions.getTransaction();
      if ( currentTransaction != null )
      {
        logger.log( System.Logger.Level.ERROR, "Automatisches Rollback ist aufgetreten ..." );
        currentTransaction.rollback();
      }
      throw exception;
    }
    finally
    {
      // Egal ob fehler oder nicht, wir müssen die Transaktion abräumen, wenn wir die oberste Transaktion sind
      // (nesting).
      if ( writeLock.getHoldCount() == 1 )
      {
        // Da wir keine Parallelen write-Calls haben, können wir die Usages hier ignorieren.
        Transactions.setTransaction( null );
      }
      writeLock.unlock();
    }
  }

  @Override
  public EmbeddedStorageManager manager()
  {
    return manager;
  }

  @SuppressWarnings( "unused" ) // Wird durch Spring aufgerufen
  public void destroy()
  {
    logger.log( System.Logger.Level.INFO, "Storage Manager wird beendet ..." );
    // Wichtig dass wir hier write-locken, da sonst ggf. laufende Transactions nicht committed werden können.
    write( ( __, ___ ) ->
    {
      manager.close();
    } );
    logger.log( System.Logger.Level.INFO, "Storage Manager wurde beendet." );
  }
}
