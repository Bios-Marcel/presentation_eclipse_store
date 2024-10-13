package link.biosmarcel.presentation.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Diese Klasse ist ein Wrapper um {@link ArrayList}. Zusätzlich wird von {@link TransactionalObject} geerbt, damit bei
 * allen Methoden, durch welche die unterliegende Liste mutiert wird, diese als
 * {@link TransactionalObject#markDirty() dirty} markiert wird.
 *
 * @param <Type> in der Liste enthaltener Datentyp
 */
public class TransactionalList<Type> extends TransactionalObject implements List<Type>
{
  private final List<Type> wrapped;

  public TransactionalList()
  {
    wrapped = new ArrayList<>();
  }

  @Override
  public boolean add( final Type type )
  {
    markDirty();
    return wrapped.add( type );
  }

  @Override
  public void add( final int index, final Type element )
  {
    markDirty();
    wrapped.add( index, element );
  }

  @Override
  public void addFirst( final Type element )
  {
    markDirty();
    wrapped.addFirst( element );
  }

  @Override
  public void addLast( final Type element )
  {
    markDirty();
    wrapped.addLast( element );
  }

  @Override
  public Type set( final int index, final Type element )
  {
    markDirty();
    return wrapped.set( index, element );
  }

  @Override
  public boolean addAll( final Collection<? extends Type> c )
  {
    markDirty();
    return wrapped.addAll( c );
  }

  @Override
  public boolean addAll( final int index, final Collection<? extends Type> c )
  {
    markDirty();
    return wrapped.addAll( index, c );
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
  public boolean remove( final Object o )
  {
    markDirty();
    return wrapped.remove( o );
  }

  @Override
  public Type remove( final int index )
  {
    markDirty();
    return wrapped.remove( index );
  }

  @Override
  public Type removeFirst()
  {
    markDirty();
    return wrapped.removeFirst();
  }

  @Override
  public Type removeLast()
  {
    markDirty();
    return wrapped.removeLast();
  }

  @Override
  public boolean retainAll( final Collection<?> c )
  {
    markDirty();
    return wrapped.retainAll( c );
  }

  @Override
  public void replaceAll( final UnaryOperator<Type> operator )
  {
    markDirty();
    wrapped.replaceAll( operator );
  }

  @Override
  public void sort( final Comparator<? super Type> c )
  {
    markDirty();
    wrapped.sort( c );
  }

  // Die Iteratoren können zwar Mutation herbeiführen, da sie u.a. add, remove und set implementieren, da jedoch
  // an die unterliegenden Implementationen der Liste weitergeleitet wird, ist die Transaktionalität automatisch
  // gegeben. Dementsprechend sind diese Methoden quasi write-able, brauchen aber keine extra Behandlung.

  @Override
  public ListIterator<Type> listIterator()
  {
    return wrapped.listIterator();
  }

  @Override
  public ListIterator<Type> listIterator( final int index )
  {
    return wrapped.listIterator( index );
  }

  @Override
  public Iterator<Type> iterator()
  {
    return wrapped.iterator();
  }

  // READ ONLY
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
  public int indexOf( final Object o )
  {
    return wrapped.indexOf( o );
  }

  @Override
  public int lastIndexOf( final Object o )
  {
    return wrapped.lastIndexOf( o );
  }

  @Override
  public List<Type> subList( final int fromIndex, final int toIndex )
  {
    return wrapped.subList( fromIndex, toIndex );
  }

  @Override
  public Type get( final int index )
  {
    return wrapped.get( index );
  }

  @Override
  public boolean containsAll( final Collection<?> c )
  {
    //noinspection SlowListContainsAll
    return wrapped.containsAll( c );
  }

  @Override
  public Object[] storeAdditionally()
  {
    return new Object[]{ wrapped };
  }
}
