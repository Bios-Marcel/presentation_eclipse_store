package link.biosmarcel.presentation.persistence;

import java.util.ListIterator;

/**
 * Implementation von {@link ListIterator}, die an einen anderen {@link ListIterator} delegiert und transaktionalität
 * implementiert. Wird z.B. in Transaktionalen Collections benötigt, um zu verhindern, dass durch den Iterator unbemerkt
 * die Collection mutiert wird.
 */
public class TransactionalListIterator<Type> implements ListIterator<Type>
{
  private final TransactionalObject parent;
  private final ListIterator<Type>  wrapped;

  public TransactionalListIterator(
      final TransactionalObject parent,
      final ListIterator<Type> wrapped )
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
  public boolean hasPrevious()
  {
    return wrapped.hasPrevious();
  }

  @Override
  public Type previous()
  {
    return wrapped.previous();
  }

  @Override
  public int nextIndex()
  {
    return wrapped.nextIndex();
  }

  @Override
  public int previousIndex()
  {
    return wrapped.previousIndex();
  }

  @Override
  public void remove()
  {
    parent.markDirty();
    wrapped.remove();
  }

  @Override
  public void set( final Type type )
  {
    parent.markDirty();
    wrapped.set( type );
  }

  @Override
  public void add( final Type type )
  {
    parent.markDirty();
    wrapped.add( type );
  }
}
