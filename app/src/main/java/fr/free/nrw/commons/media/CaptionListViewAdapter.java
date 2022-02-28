package fr.free.nrw.commons.media;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.data.models.media.Caption;
import java.util.List;

/**
 * Adapter for Caption Listview
 */
public class CaptionListViewAdapter extends BaseAdapter {

  List<Caption> captions;

  public CaptionListViewAdapter(final List<Caption> captions) {
    this.captions = captions;
  }

  /**
   * @return size of captions list
   */
  @Override
  public int getCount() {
    return captions.size();
  }

  /**
   * @return Object at position i
   */
  @Override
  public Object getItem(final int i) {
    return null;
  }

  /**
   * @return id for current item
   */
  @Override
  public long getItemId(final int i) {
    return 0;
  }

  /**
   * inflate the view and bind data with UI
   */
  @Override
  public View getView(final int i, final View view, final ViewGroup viewGroup) {
    final TextView captionLanguageTextView;
    final TextView captionTextView;
    final View captionLayout = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.caption_item, null);
    captionLanguageTextView = captionLayout.findViewById(R.id.caption_language_textview);
    captionTextView = captionLayout.findViewById(R.id.caption_text);
    if (captions.size() == 1 && captions.get(0).getValue().equals("No Caption")) {
      captionLanguageTextView.setText(captions.get(i).getLanguage());
      captionTextView.setText(captions.get(i).getValue());
    } else {
      captionLanguageTextView.setText(captions.get(i).getLanguage() + ":");
      captionTextView.setText(captions.get(i).getValue());
    }

    return captionLayout;
  }

}
