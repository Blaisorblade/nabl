package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.path.Paths;
import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphContext;
import org.metaborg.meta.nabl2.stratego.ITermIndex;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.Lists;

@Deprecated
public class SG_get_ast_resolution extends ScopeGraphPrimitive {

    public SG_get_ast_resolution() {
        super(SG_get_ast_resolution.class.getSimpleName(), 0, 1);
    }

    @Override public Optional<ITerm> call(IScopeGraphContext<?> context, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        if (terms.size() != 1) {
            throw new InterpreterException("Need one term argument: analysis");
        }
        final ITermIndex analysis = TermIndex.get(terms.get(0))
                .orElseThrow(() -> new InterpreterException("Not an analysis term."));
        return TermIndex.get(term).flatMap(index -> {
            return context.unit(analysis.getResource()).solution().<ITerm> flatMap(s -> {
                List<ITerm> entries = Lists.newArrayList();
                for (Occurrence ref : s.getScopeGraph().getAllRefs()) {
                    if (ref.getIndex().equals(index)) {
                        for (Occurrence decl : Paths.resolutionPathsToDecls(s.getNameResolution().resolve(ref))) {
                            entries.add(TB.newTuple(ref, decl.getName()));
                        }
                    }
                }
                if (entries.isEmpty()) {
                    return Optional.empty();
                }
                return Optional.of(TB.newList(entries));
            });
        });
    }

}