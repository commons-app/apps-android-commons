package fr.free.nrw.commons.depictions.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Model class for object obtained while parsing depiction response
 * <p>
 * the getSearch() function is used to parse media
 */
public class Query {

  @SerializedName("searchinfo")
  @Expose
  private Searchinfo searchinfo;
  @SerializedName("search")
  @Expose
  private List<Search> search = null;

  /**
   * No args constructor for use in serialization
   */
  public Query() {
  }

  /**
   * @param search
   * @param searchinfo
   */
  public Query(Searchinfo searchinfo, List<Search> search) {
    super();
    this.searchinfo = searchinfo;
    this.search = search;
  }

  /**
   * return searchInfo
   */
  public Searchinfo getSearchinfo() {
    return searchinfo;
  }

  public void setSearchinfo(Searchinfo searchinfo) {
    this.searchinfo = searchinfo;
  }

  /**
   * the getSearch() function is used to parse media
   */
  public List<Search> getSearch() {
    return search;
  }

  public void setSearch(List<Search> search) {
    this.search = search;
  }

}
