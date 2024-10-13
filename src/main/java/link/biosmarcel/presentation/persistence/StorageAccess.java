package link.biosmarcel.presentation.persistence;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * <p>
 * Der {@link StorageAccess} bietet den Zugriff auf die Live-Daten des Servers. Dabei werden Locking und
 * {@link Transaction Transaktionalität} behandelt.
 * </p>
 */
public interface StorageAccess
{
  /**
   * Read bietet lesenden Zugriff auf den Datenbestand <b>UND VERHINDERT SCHREIBZUGRIFFE</b>.
   *
   * <p>Für schreibenden Zugriff, siehe {@link #write(BiConsumer)} und {@link #writeReturn(BiFunction)}.
   */
  void read( final Consumer<StorageRoot> reader );

  /**
   * Read bietet lesenden Zugriff auf den Datenbestand <b>UND VERHINDERT SCHREIBZUGRIFFE</b>.
   *
   * <p>Für schreibenden Zugriff, siehe {@link #write(BiConsumer)} und {@link #writeReturn(BiFunction)}.
   *
   * <p>Zusätzlich zu {@link #read(Consumer)} kann hier noch ein Wert zurückgeliefert werden. Hierbei muss jedoch
   * beachtet werden, dass die Daten außerhalb eines Lock-Kontextes nicht mehr garantiert konsistent sind. Die bedeutet,
   * dass man sich innerhalb geschachtelter Calls auf die Werte verlassen kann, außerhalb eines solchen Aufrufs jedoch
   * nicht.
   */
  <Result> Result readReturn( final Function<StorageRoot, Result> reader );

  /**
   * Write bietet sowohl lesenden als auch schreibenden Zugriff auf den Datenbestand. Veränderte Daten melden sich über
   * {@link TransactionalObject#markDirty()} bei der Transaktion an.
   */
  void write( final BiConsumer<StorageRoot, Transaction> reader );

  /**
   * Write bietet sowohl lesenden als auch schreibenden Zugriff auf den Datenbestand. Veränderte Daten melden sich über
   * * {@link TransactionalObject#markDirty()} bei der Transaktion an.
   *
   * <p>Zusätzlich zu {@link #write(BiConsumer)} kann hier noch ein Wert zurückgeliefert werden. Hierbei
   * muss jedoch beachtet werden, dass die Daten außerhalb eines Lock-Kontextes nicht mehr garantiert konsistent sind.
   * Die bedeutet, dass man sich innerhalb geschachtelter Calls auf die Werte verlassen kann, außerhalb eines solchen
   * Aufrufs jedoch nicht.
   */
  <Result> Result writeReturn( final BiFunction<StorageRoot, Transaction, Result> reader );
}
