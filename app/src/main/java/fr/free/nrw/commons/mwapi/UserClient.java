package fr.free.nrw.commons.mwapi;

import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse;
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResult;
import fr.free.nrw.commons.wikidata.mwapi.UserInfo;
import fr.free.nrw.commons.utils.DateUtil;

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
}
