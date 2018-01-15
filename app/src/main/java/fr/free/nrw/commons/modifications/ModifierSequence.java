package fr.free.nrw.commons.modifications;

import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class ModifierSequence {
    private Uri mediaUri;
    private ArrayList<PageModifier> modifiers;
    private Uri contentUri;

    public ModifierSequence(Uri mediaUri) {
        this.mediaUri = mediaUri;
        modifiers = new ArrayList<>();
    }

    ModifierSequence(Uri mediaUri, JSONObject data) {
        this(mediaUri);
        JSONArray modifiersJSON = data.optJSONArray("modifiers");
        for (int i = 0; i < modifiersJSON.length(); i++) {
            modifiers.add(PageModifier.fromJSON(modifiersJSON.optJSONObject(i)));
        }
    }

    Uri getMediaUri() {
        return mediaUri;
    }

    public void queueModifier(PageModifier modifier) {
        modifiers.add(modifier);
    }

    String executeModifications(String pageName, String pageContents) {
        for (PageModifier modifier: modifiers) {
            pageContents = modifier.doModification(pageName,  pageContents);
        }
        return pageContents;
    }

    String getEditSummary() {
        StringBuilder editSummary = new StringBuilder();
        for (PageModifier modifier: modifiers) {
            editSummary.append(modifier.getEditSumary()).append(" ");
        }
        editSummary.append("Via Commons Mobile App");
        return editSummary.toString();
    }

    ArrayList<PageModifier> getModifiers() {
        return modifiers;
    }

    Uri getContentUri() {
        return contentUri;
    }

    void setContentUri(Uri contentUri) {
        this.contentUri = contentUri;
    }

}
