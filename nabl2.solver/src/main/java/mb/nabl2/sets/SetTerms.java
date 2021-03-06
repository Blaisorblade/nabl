package mb.nabl2.sets;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Optional;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;

public class SetTerms {

    private static final String NO_PROJECTION = "NoProjection";
    private static final String PROJECTION = "Projection";

    public static IMatcher<Optional<String>> projectionMatcher() {
        return M.cases(
            // @formatter:off
            M.appl0(NO_PROJECTION, t -> Optional.empty()),
            M.appl1(PROJECTION, M.stringValue(), (t, p) -> Optional.of(p))
            // @formatter:on
        );
    }

    public static ITerm buildProjection(Optional<String> projection) {
        return projection.map(p -> B.newAppl(PROJECTION, B.newString(p))).orElseGet(() -> B.newAppl(NO_PROJECTION));
    }

}
