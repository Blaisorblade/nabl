package mb.nabl2.solver.messages;

import java.util.stream.Collectors;

import io.usethesource.capsule.Set;
import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.constraints.messages.MessageKind;

public interface IMessages {

    default java.util.Set<IMessageInfo> getErrors() {
        return getAll().stream().filter(m -> m.getKind().equals(MessageKind.ERROR)).collect(Collectors.toSet());
    }

    default java.util.Set<IMessageInfo> getWarnings() {
        return getAll().stream().filter(m -> m.getKind().equals(MessageKind.WARNING)).collect(Collectors.toSet());
    }

    default java.util.Set<IMessageInfo> getNotes() {
        return getAll().stream().filter(m -> m.getKind().equals(MessageKind.NOTE)).collect(Collectors.toSet());
    }

    Set<IMessageInfo> getAll();

    interface Immutable extends IMessages {

        Set.Immutable<IMessageInfo> getAll();

        IMessages.Transient melt();

    }

    interface Transient extends IMessages {

        boolean add(IMessageInfo message);

        boolean addAll(Iterable<? extends IMessageInfo> messages);

        boolean addAll(IMessages messages);

        IMessages.Immutable freeze();

    }

}