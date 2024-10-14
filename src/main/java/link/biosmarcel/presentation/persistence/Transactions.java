package link.biosmarcel.presentation.persistence;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Speichert die aktuell aktive Transaktion und erlaubt einfach Zugriff auf diese via
 * {@link Transactions#getTransaction()}. Dies ist nicht Teil des {@link StorageAccess}, sondern eine einzelne Klasse,
 * damit alle interessierten Instanzen einfach ohne SpringBeans und co darauf zugreifen können.
 */
@org.eclipse.jdt.annotation.NonNullByDefault
public final class Transactions
{
  private static @Nullable Transaction transaction;

  private Transactions()
  {
  }

  /**
   * @return aktive Transaktion, oder {@code null} falls es keine gibt. Jedoch kann eine existierende Transaktion auch
   *     read-only sein, falls eine write-Methode aktuell eine read-Methode aufruft.
   */
  public static @Nullable Transaction getTransaction()
  {
    return Transactions.transaction;
  }

  static void setTransaction( final @Nullable Transaction transaction )
  {
    Transactions.transaction = transaction;
  }
}
