package link.biosmarcel.presentation.persistence;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Diese Klasse ist ein Wrapper um {@link Map}. Zusätzlich wird von {@link TransactionalObject} geerbt, damit bei allen
 * Methoden, durch welche die unterliegende Map mutiert wird, diese als {@link TransactionalObject#markDirty() dirty}
 * markiert wird.
 *
 * <p>Methoden die bereits in {@link Map} implementiert werden, werden hier nochmal reimplementiert, da wir nicht
 * wissen ob die gewrappte Map eine eigene Funktionalität bietet, welche wir somit umgehen würden.</p>
 */
public class TransactionalMap<Key, Value> extends TransactionalObject implements Map<Key, Value>
{
  private final Map<Key, Value> wrapped;

  public TransactionalMap()
  {
    this.wrapped = new HashMap<>();
  }

  @Override
  public Value put( final Key key, final Value value )
  {
    markDirty();
    return wrapped.put( key, value );
  }

  @Override
  public Value remove( final Object key )
  {
    markDirty();
    return wrapped.remove( key );
  }

  @Override
  public void putAll( final Map<? extends Key, ? extends Value> m )
  {
    markDirty();
    wrapped.putAll( m );
  }

  @Override
  public void clear()
  {
    markDirty();
    wrapped.clear();
  }

  @Override
  public void replaceAll( final BiFunction<? super Key, ? super Value, ? extends Value> function )
  {
    markDirty();
    wrapped.replaceAll( function );
  }

  @Override
  public Value putIfAbsent( final Key key, final Value value )
  {
    markDirty();
    return wrapped.putIfAbsent( key, value );
  }

  @Override
  public boolean remove( final Object key, final Object value )
  {
    markDirty();
    return wrapped.remove( key, value );
  }

  @Override
  public boolean replace( final Key key, final Value oldValue, final Value newValue )
  {
    markDirty();
    return wrapped.replace( key, oldValue, newValue );
  }

  @Override
  public Value replace( final Key key, final Value value )
  {
    markDirty();
    return wrapped.replace( key, value );
  }

  @Override
  public Value computeIfAbsent( final Key key, final Function<? super Key, ? extends Value> mappingFunction )
  {
    markDirty();
    return wrapped.computeIfAbsent( key, mappingFunction );
  }

  @Override
  public Value computeIfPresent( final Key key,
                                 final BiFunction<? super Key, ? super Value, ? extends Value> remappingFunction )
  {
    markDirty();
    return wrapped.computeIfPresent( key, remappingFunction );
  }

  @Override
  public Value compute( final Key key, final BiFunction<? super Key, ? super Value, ? extends Value> remappingFunction )
  {
    markDirty();
    return wrapped.compute( key, remappingFunction );
  }

  @Override
  public Value merge( final Key key, final Value value,
                      final BiFunction<? super Value, ? super Value, ? extends Value> remappingFunction )
  {
    markDirty();
    return wrapped.merge( key, value, remappingFunction );
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
  public boolean containsKey( final Object key )
  {
    return wrapped.containsKey( key );
  }

  @Override
  public boolean containsValue( final Object value )
  {
    return wrapped.containsValue( value );
  }

  @Override
  public Value get( final Object key )
  {
    return wrapped.get( key );
  }

  @Override
  public Set<Key> keySet()
  {
    return wrapped.keySet();
  }

  @Override
  public Collection<Value> values()
  {
    return wrapped.values();
  }

  @Override
  public Set<Entry<Key, Value>> entrySet()
  {
    return wrapped.entrySet();
  }

  @Override
  public Object[] storeAdditionally()
  {
    return new Object[]{ wrapped };
  }
}
