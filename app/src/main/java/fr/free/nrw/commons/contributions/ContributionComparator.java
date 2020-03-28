package fr.free.nrw.commons.contributions;

import java.util.Comparator;

public class ContributionComparator implements Comparator<Contribution> {

  @Override
  public int compare(Contribution c1, Contribution c2) {
    return c1.dateUploaded.compareTo(c2.dateUploaded);
  }
}
