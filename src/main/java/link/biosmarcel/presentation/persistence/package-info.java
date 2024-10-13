/**
 * Enthält sämtlichen Code bezüglich Storage von
 * {@link link.biosmarcel.model.DomainObject DomainObjects}, Locking und
 * {@link link.biosmarcel.presentation.persistence.Transaction Transaktionen}.
 *
 * <p>Dies beinhaltet unter anderem auch transaktionale Implementationen von Collections:
 * <ul>
 *   <li>{@link link.biosmarcel.presentation.persistence.TransactionalList} (ArrayList)</li>
 *   <li>{@link link.biosmarcel.presentation.persistence.TransactionalSet} (HashSet)</li>
 *   <li>{@link link.biosmarcel.presentation.persistence.TransactionalMap} (HashMap)</li>
 * </ul>
 *
 * <p>
 * Aktuell wird hier jeweils eine fest definierte unterliegende Implementation genutzt. Falls in der Zukunft zum
 * Beispiel eine {@link java.util.LinkedList} benötigt wird, sollten wir die
 * {@link link.biosmarcel.presentation.persistence.TransactionalList} anpassen um die unterliegende Collection anpassbar
 * zu machen.
 */
@org.eclipse.jdt.annotation.NonNullByDefault
package link.biosmarcel.presentation.persistence;