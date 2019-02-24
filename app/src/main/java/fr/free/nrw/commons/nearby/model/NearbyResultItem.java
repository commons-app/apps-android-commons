package fr.free.nrw.commons.nearby.model;

import com.google.gson.annotations.SerializedName;

public class NearbyResultItem {
    private final ResultTuple item;
    private final ResultTuple wikipediaArticle;
    private final ResultTuple commonsArticle;
    private final ResultTuple location;
    private final ResultTuple label;
    private final ResultTuple icon;
    @SerializedName("class") private final ResultTuple className;
    @SerializedName("class_label") private final ResultTuple classLabel;
    @SerializedName("Commons_category") private final ResultTuple commonsCategory;

    public NearbyResultItem(ResultTuple item,
                            ResultTuple wikipediaArticle,
                            ResultTuple commonsArticle,
                            ResultTuple location,
                            ResultTuple label,
                            ResultTuple icon, ResultTuple className,
                            ResultTuple classLabel,
                            ResultTuple commonsCategory) {
        this.item = item;
        this.wikipediaArticle = wikipediaArticle;
        this.commonsArticle = commonsArticle;
        this.location = location;
        this.label = label;
        this.icon = icon;
        this.className = className;
        this.classLabel = classLabel;
        this.commonsCategory = commonsCategory;
    }

    public ResultTuple getItem() {
        return item == null ? new ResultTuple(): item;
    }

    public ResultTuple getWikipediaArticle() {
        return wikipediaArticle == null ? new ResultTuple():wikipediaArticle;
    }

    public ResultTuple getCommonsArticle() {
        return commonsArticle == null ? new ResultTuple():commonsArticle;
    }

    public ResultTuple getLocation() {
        return location == null ? new ResultTuple():location;
    }

    public ResultTuple getLabel() {
        return label == null ? new ResultTuple():label;
    }

    public ResultTuple getIcon() {
        return icon == null ? new ResultTuple():icon;
    }

    public ResultTuple getClassName() {
        return className == null ? new ResultTuple():className;
    }

    public ResultTuple getClassLabel() {
        return classLabel == null ? new ResultTuple():classLabel;
    }

    public ResultTuple getCommonsCategory() {
        return commonsCategory == null ? new ResultTuple():commonsCategory;
    }
}
