package link.biosmarcel.presentation.persistence;

/**
 * Repräsentiert den aktuellen Persistenz-State in EclipseStore. Dieser State ist für die Business-Logik nicht relevant,
 * er dient lediglich dazu, nicht unnötigerweise unveränderte Objekte immer wieder zu speichern.
 */
public enum SaveState
{
  /**
   * Frischer Datensatz, der noch nie an EclipseStore übergeben wurde. Der Datensatz wird also nicht separat committet,
   * sondern durch die Referenz in seinem Parent gespeichert.
   */
  FRESH,
  /** Das Objekt ist FRESH, aber wurde bereits in einer Transaktion registriert. */
  REGISTERED,
  /** Datensatz ist EclipseStore bereits bekannt und hat keine ungespeicherten Änderungen. */
  SAVED,
  /** Datensatz ist EclipseStore bereits bekannt, hat aber ungespeicherte Änderungen. */
  DIRTY,
}
