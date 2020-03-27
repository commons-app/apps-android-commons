package fr.free.nrw.commons.wikidata;

import fr.free.nrw.commons.BuildConfig;

enum WikidataProperties {
  IMAGE("P18"),
  DEPICTS(BuildConfig.DEPICTS_PROPERTY);

  private final String propertyName;

  WikidataProperties(final String propertyName) {
    this.propertyName = propertyName;
  }

  public String getPropertyName() {
    return propertyName;
  }
}
