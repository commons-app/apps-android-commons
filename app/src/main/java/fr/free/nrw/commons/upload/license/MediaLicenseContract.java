package fr.free.nrw.commons.upload.license;

import fr.free.nrw.commons.BasePresenter;
import java.util.List;

/**
 * The contract with with MediaLicenseFragment and its presenter would talk to each other
 */
public interface MediaLicenseContract {

  interface View {

    void setLicenses(List<String> licenses);

    void setSelectedLicense(String license);

    void updateLicenseSummary(String selectedLicense, int numberOfItems);
  }

  interface UserActionListener extends BasePresenter<View> {

    void getLicenses();

    void selectLicense(String licenseName);
  }

}
