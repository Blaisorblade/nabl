package org.metaborg.meta.nabl2.scopegraph.terms.path;

import java.util.Iterator;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.path.IStep;
import org.metaborg.util.iterators.Iterables2;
import org.pcollections.HashTreePSet;
import org.pcollections.PSequence;
import org.pcollections.PSet;
import org.pcollections.TreePVector;

import com.google.common.collect.Iterators;

@Value.Immutable
@Serial.Version(value = 42L)
abstract class EStep<S extends IScope, L extends ILabel, O extends IOccurrence> implements IStep<S, L, O> {

    @Value.Parameter @Override public abstract S getSource();

    @Value.Parameter @Override public abstract L getLabel();

    @Value.Parameter @Override public abstract S getTarget();

    @Value.Lazy @Override public int size() {
        return 1;
    }

    @Value.Lazy @Override public PSet<O> getImports() {
        return HashTreePSet.empty();
    }

    @Override public Iterable<IResolutionPath<S, L, O>> getImportPaths() {
        return Iterables2.empty();
    }

    @Value.Lazy @Override public PSet<S> getScopes() {
        return HashTreePSet.singleton(getSource()).plus(getTarget());
    }

    @Value.Lazy @Override public PSequence<L> getLabels() {
        return TreePVector.singleton(getLabel());
    }

    @Override public Iterator<IStep<S, L, O>> iterator() {
        return Iterators.singletonIterator(this);
    }

    @Override public <T> T match(IStep.ICases<S, L, O, T> cases) {
        return cases.caseE(getSource(), getLabel(), getTarget());
    }

    @Override public String toString(boolean includeTo, boolean includeFrom) {
        StringBuilder sb = new StringBuilder();
        if(includeFrom) {
            sb.append(getSource());
            sb.append(Paths.PATH_SEPERATOR);
        }
        sb.append("E(");
        sb.append(getLabel());
        sb.append(")");
        if(includeTo) {
            sb.append(Paths.PATH_SEPERATOR);
            sb.append(getTarget());
        }
        return sb.toString();
    }

    @Override public String toString() {
        return toString(true, true);
    }

}