package fr.free.nrw.commons.media;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import fr.free.nrw.commons.R;
import java.util.List;

public class CaptionListViewAdapter extends BaseAdapter {

  List<Caption> captions;

  public CaptionListViewAdapter(final List<Caption> captions) {
    this.captions = captions;
  }

  @Override
  public int getCount() {
    return captions.size();
  }

  @Override
  public Object getItem(final int i) {
    return null;
  }

  @Override
  public long getItemId(final int i) {
    return 0;
  }

  @Override
  public View getView(final int i, final View view, final ViewGroup viewGroup) {
    final TextView captionLanguageTextView;
    final TextView captionTextView;
    final View captionLayout = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.caption_item, null);
    captionLanguageTextView = captionLayout.findViewById(R.id.caption_language_textview);
    captionTextView = captionLayout.findViewById(R.id.caption_text);
    captionLanguageTextView.setText(captions.get(i).getLanguage() + ":");
    captionTextView.setText(captions.get(i).getValue());
    return captionLayout;
  }

}
