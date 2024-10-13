package link.biosmarcel.presentation.persistence;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Diese Klasse ist ein Wrapper um {@link HashSet}. Zusätzlich wird von {@link TransactionalObject} geerbt, damit bei
 * allen Methoden, durch welche das unterliegende Set mutiert wird, diese als
 * {@link TransactionalObject#markDirty() dirty} markiert wird.
 *
 * @param <Type> im Set enthaltener Datentyp
 */
public class TransactionalSet<Type> extends TransactionalObject implements Set<Type>
{
  private final Set<Type> wrapped;

  public TransactionalSet()
  {
    this.wrapped = new HashSet<>();
  }

  @Override
  public boolean addAll( final Collection<? extends Type> c )
  {
    markDirty();
    return wrapped.addAll( c );
  }

  @Override
  public boolean retainAll( final Collection<?> c )
  {
    markDirty();
    return wrapped.retainAll( c );
  }

  @Override
  public boolean removeAll( final Collection<?> c )
  {
    markDirty();
    return wrapped.removeAll( c );
  }

  @Override
  public boolean removeIf( final Predicate<? super Type> filter )
  {
    markDirty();
    return wrapped.removeIf( filter );
  }

  @Override
  public void clear()
  {
    markDirty();
    wrapped.clear();
  }

  @Override
  public boolean add( final Type type )
  {
    markDirty();
    return wrapped.add( type );
  }

  @Override
  public boolean remove( final Object o )
  {
    markDirty();
    return wrapped.remove( o );
  }

  // READ

  @Override
  public int size()
  {
    return wrapped.size();
  }

  @Override
  public boolean isEmpty()
  {
    return wrapped.isEmpty();
  }

  @Override
  public boolean contains( final Object o )
  {
    return wrapped.contains( o );
  }

  @Override
  public Iterator<Type> iterator()
  {
    return wrapped.iterator();
  }

  @Override
  public Object[] toArray()
  {
    return wrapped.toArray();
  }

  @Override
  public <T> T[] toArray( final T[] a )
  {
    return wrapped.toArray( a );
  }

  @Override
  public boolean containsAll( final Collection<?> c )
  {
    return wrapped.containsAll( c );
  }

  @Override
  public Object[] storeAdditionally()
  {
    return new Object[]{ wrapped };
  }
}
