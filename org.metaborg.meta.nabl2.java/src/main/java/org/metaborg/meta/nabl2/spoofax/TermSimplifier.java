package org.metaborg.meta.nabl2.spoofax;

import org.metaborg.meta.nabl2.scopegraph.terms.ImmutableScope;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.GenericTerms;
import org.metaborg.meta.nabl2.terms.generic.ImmutableTermIndex;
import org.metaborg.meta.nabl2.terms.generic.TermIndex;

public class TermSimplifier {

    public static ITerm focus(String resource, ITerm term) {
        return M.somebu(M.cases(
            // @formatter:off
            M.var(var -> {
                String r = (resource ==  null || var.getResource().equals(resource)) ? "" : var.getResource();
                return GenericTerms.newVar(r, var.getName()).setAttachments(var.getAttachments());  
            }),
            t -> Scope.matcher().match(t).map(s -> {
                String r = (resource == null || s.getResource().equals(resource)) ? "" : s.getResource();
                return ImmutableScope.of(r, s.getName()).setAttachments(t.getAttachments());
            }),
            t -> TermIndex.matcher().match(t).map(i -> {
                String r = (resource == null || i.getResource().equals(resource)) ? "" : i.getResource();
                return ImmutableTermIndex.of(r, i.getId()).setAttachments(t.getAttachments());
            }),
            Occurrence.matcher()
            // @formatter:on
        )).apply(term);
    }

}