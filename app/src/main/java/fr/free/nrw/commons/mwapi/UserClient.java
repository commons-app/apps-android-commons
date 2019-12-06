package fr.free.nrw.commons.mwapi;

import org.wikipedia.dataclient.mwapi.MwQueryLogEvent;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.mwapi.MwQueryResult;
import org.wikipedia.dataclient.mwapi.UserInfo;
import org.wikipedia.util.DateUtil;

import java.util.Collections;
import java.util.Date;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.Single;

public class UserClient {
    private final UserInterface userInterface;

    @Inject
    public UserClient(UserInterface userInterface) {
        this.userInterface = userInterface;
    }

    /**
     * Checks to see if a user is currently blocked from Commons
     *
     * @return whether or not the user is blocked from Commons
     */
    public Single<Boolean> isUserBlockedFromCommons() {
        return userInterface.getUserBlockInfo()
                .map(MwQueryResponse::query)
                .map(MwQueryResult::userInfo)
                .map(UserInfo::blockexpiry)
                .map(blockExpiry -> {
                    if (blockExpiry.isEmpty())
                        return false;
                    else if ("infinite".equals(blockExpiry))
                        return true;
                    else {
                        Date endDate = DateUtil.iso8601DateParse(blockExpiry);
                        Date current = new Date();
                        return endDate.after(current);
                    }
                }).single(false);
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
