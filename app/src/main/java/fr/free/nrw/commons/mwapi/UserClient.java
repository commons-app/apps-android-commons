package fr.free.nrw.commons.mwapi;

import javax.inject.Inject;

public class UserClient {
    private final UserInterface userInterface;

    @Inject
    public UserClient(UserInterface userInterface) {
        this.userInterface = userInterface;
    }
}
