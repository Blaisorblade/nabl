package mb.nabl2.terms;

import com.google.common.collect.ImmutableClassToInstanceMap;

public interface INilTerm extends IListTerm {

    @Override
    INilTerm withAttachments(ImmutableClassToInstanceMap<Object> value);

}