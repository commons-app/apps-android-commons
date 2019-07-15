package fr.free.nrw.commons.contributions.model;

import fr.free.nrw.commons.contributions.Contribution;

public class DisplayableContribution extends Contribution {
    private int position;
    public DisplayableContribution(Contribution contribution,
                                    int position) {
        super(contribution.getContentUri(),
                contribution.getFilename(),
                contribution.getLocalUri(),
                contribution.getImageUrl(),
                contribution.getDateCreated(),
                contribution.getState(),
                contribution.getDataLength(),
                contribution.getDateUploaded(),
                contribution.getTransferred(),
                contribution.getSource(),
                contribution.getCaptions(),
                contribution.getDescription(),
                contribution.getCreator(),
                contribution.getMultiple(),
                contribution.getWidth(),
                contribution.getHeight(),
                contribution.getLicense());
        this.position = position;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
