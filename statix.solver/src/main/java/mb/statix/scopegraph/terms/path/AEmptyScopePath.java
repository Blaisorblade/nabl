package mb.statix.scopegraph.terms.path;

import java.util.Collections;
import java.util.Iterator;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.Iterators;

import io.usethesource.capsule.Set;
import mb.nabl2.util.collections.PSequence;
import mb.statix.scopegraph.path.IScopePath;
import mb.statix.scopegraph.path.IStep;

@Value.Immutable
@Serial.Version(value = 42L)
abstract class AEmptyScopePath<V, L> implements IScopePath<V, L> {

    @Value.Parameter public abstract V getScope();

    @Override public V getSource() {
        return getScope();
    }

    @Override public V getTarget() {
        return getScope();
    }

    @Value.Lazy @Override public int size() {
        return 0;
    }

    @Value.Lazy @Override public PSequence<V> scopes() {
        return PSequence.of(getScope());
    }

    @Value.Lazy @Override public Set.Immutable<V> scopeSet() {
        return Set.Immutable.of(getScope());
    }

    @Value.Lazy @Override public PSequence<L> labels() {
        return PSequence.of();
    }

    @Override public Iterator<IStep<V, L>> iterator() {
        return Collections.emptyIterator();
    }

    @Override public int hashCode() {
        return 0;
    }

    @Override public boolean equals(Object obj) {
        if(obj == null)
            return false;
        if(!(obj instanceof IScopePath<?, ?>))
            return false;
        IScopePath<?, ?> other = (IScopePath<?, ?>) obj;
        if(!getScope().equals(other.getSource()))
            return false;
        if(!getScope().equals(other.getTarget()))
            return false;
        return Iterators.size(other.iterator()) == 0;
    }

    @Override public String toString(boolean includeSource, boolean includeTarget) {
        return (includeSource && includeTarget) ? getScope().toString() : "";
    }

    @Override public String toString() {
        return toString(true, true);
    }

}