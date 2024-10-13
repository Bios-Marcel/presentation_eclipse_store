package link.biosmarcel.presentation.persistence;

import java.util.Iterator;

/**
 * Implementation von {@link Iterator}, die an einen anderen {@link Iterator} delegiert und transaktionalität
 * implementiert. Wird z.B. in Transaktionalen Collections benötigt, um zu verhindern, dass durch den Iterator unbemerkt
 * die Collection mutiert wird.
 */
public class TransactionalIterator<Type> implements Iterator<Type>
{
  private final TransactionalObject parent;
  private final Iterator<Type>      wrapped;

  public TransactionalIterator(
      final TransactionalObject parent,
      final Iterator<Type> wrapped )
  {
    this.parent = parent;
    this.wrapped = wrapped;
  }

  @Override
  public boolean hasNext()
  {
    return wrapped.hasNext();
  }

  @Override
  public Type next()
  {
    return wrapped.next();
  }

  @Override
  public void remove()
  {
    parent.markDirty();
    wrapped.remove();
  }
}
