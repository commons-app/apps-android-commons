package fr.free.nrw.commons.mwapi;

import org.wikipedia.dataclient.mwapi.MwQueryLogEvent;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.mwapi.MwQueryResult;

import java.util.Collections;

import javax.inject.Inject;

import io.reactivex.Observable;

public class UserClient {
    private final UserInterface userInterface;

    @Inject
    public UserClient(UserInterface userInterface) {
        this.userInterface = userInterface;
    }

    public Observable<MwQueryLogEvent> logEvents(String user) {
        try {
            return userInterface.getUserLogEvents(user, Collections.emptyMap())
                    .map(MwQueryResponse::query)
                    .map(MwQueryResult::logevents)
                    .flatMap(Observable::fromIterable);
        } catch (Throwable throwable) {
            return Observable.empty();
        }

    }
}
