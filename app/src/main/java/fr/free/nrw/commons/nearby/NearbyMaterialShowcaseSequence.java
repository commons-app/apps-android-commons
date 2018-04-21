package fr.free.nrw.commons.nearby;

import android.app.Activity;

import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;


public class NearbyMaterialShowcaseSequence extends MaterialShowcaseSequence {

    public NearbyMaterialShowcaseSequence(Activity activity, String sequenceID) {
        super(activity, sequenceID);
        ShowcaseConfig config = new ShowcaseConfig();
        config.setDelay(500); // half second between each showcase view
        this.setConfig(config);
        this.singleUse(sequenceID); // Display tutorial only once
    }

}
