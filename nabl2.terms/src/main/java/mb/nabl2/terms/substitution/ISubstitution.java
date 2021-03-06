package mb.nabl2.terms.substitution;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;

public interface ISubstitution {

    boolean isEmpty();

    boolean contains(ITermVar var);

    Set<ITermVar> varSet();

    Set<Entry<ITermVar, ITerm>> entrySet();

    ITerm apply(ITerm term);

    default List<ITerm> apply(List<ITerm> terms) {
        return terms.stream().map(this::apply).collect(Collectors.toList());
    }

    interface Immutable extends ISubstitution {

        Immutable put(ITermVar var, ITerm term);

        Immutable remove(ITermVar var);

        Immutable removeAll(Iterable<ITermVar> var);

        Immutable compose(ISubstitution.Immutable other);

        ISubstitution.Transient melt();

    }

    interface Transient extends ISubstitution {

        void put(ITermVar var, ITerm term);

        void remove(ITermVar var);

        void removeAll(Iterable<ITermVar> var);

        void compose(ISubstitution.Immutable other);

        ISubstitution.Immutable freeze();

    }

}