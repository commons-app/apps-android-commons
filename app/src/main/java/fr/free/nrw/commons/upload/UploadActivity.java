package fr.free.nrw.commons.upload;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.facebook.drawee.view.SimpleDraweeView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.AuthenticatedActivity;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.utils.AbstractTextWatcher;

public class UploadActivity extends AuthenticatedActivity
        implements UploadThumbnailRenderer.ThumbnailClickedListener, UploadView {
    @Inject
    MediaWikiApi mwApi;
    @Inject
    UploadPresenter presenter;
    @Inject
    UploadThumbnailsAdapterFactory adapterFactory;

    // Main GUI
    @BindView(R.id.backgroundImage)
    SimpleDraweeView background;
    @BindView(R.id.view_flipper)
    ViewFlipper viewFlipper;
    @BindView(R.id.category_next)
    Button categoryNext;
    @BindView(R.id.category_previous)
    Button categoryPrevious;
    @BindView(R.id.submit)
    Button submit;
    @BindView(R.id.license_previous)
    Button licensePrevious;

    // Top Card
    @BindView(R.id.top_card)
    CardView topCard;
    @BindView(R.id.top_card_expand_button)
    ImageView topCardExpandButton;
    @BindView(R.id.top_card_title)
    TextView topCardTitle;
    @BindView(R.id.top_card_thumbnails)
    RecyclerView topCardThumbnails;

    // Bottom Card
    @BindView(R.id.bottom_card_expand_button)
    ImageView bottomCardExpandButton;
    @BindView(R.id.bottom_card_title)
    TextView bottomCardTitle;
    @BindView(R.id.bottom_card_content)
    View bottomCardContent;
    @BindView(R.id.bottom_card_next)
    Button next;
    @BindView(R.id.bottom_card_previous)
    Button previous;
    @BindView(R.id.image_title)
    EditText imageTitle;
    @BindView(R.id.image_description)
    EditText imageDescription;

    private AbstractTextWatcher titleWatcher;
    private AbstractTextWatcher descriptionWatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);
        ButterKnife.bind(this);

        titleWatcher = new AbstractTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                presenter.imageTitleChanged(imageTitle.getText().toString());
            }
        };
        descriptionWatcher = new AbstractTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                presenter.descriptionChanged(imageDescription.getText().toString());
            }
        };
        adapterFactory.setListener(this);
        topCardThumbnails.setLayoutManager(new LinearLayoutManager(this,
                LinearLayoutManager.HORIZONTAL, false));

        topCardExpandButton.setOnClickListener(v -> presenter.toggleTopCardState());
        bottomCardExpandButton.setOnClickListener(v -> presenter.toggleBottomCardState());
        next.setOnClickListener(v -> presenter.handleNext());
        previous.setOnClickListener(v -> presenter.handlePrevious());
        categoryNext.setOnClickListener(v -> presenter.handleNext());
        categoryPrevious.setOnClickListener(v -> presenter.handlePrevious());
        licensePrevious.setOnClickListener(v -> presenter.handlePrevious());
        submit.setOnClickListener(v -> presenter.handleSubmit());

        presenter.init(savedInstanceState);
        receiveSharedItems();
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.addView(this);
        imageTitle.addTextChangedListener(titleWatcher);
        imageDescription.addTextChangedListener(descriptionWatcher);
    }

    @Override
    protected void onPause() {
        presenter.removeView();
        imageTitle.removeTextChangedListener(titleWatcher);
        imageDescription.removeTextChangedListener(descriptionWatcher);
        super.onPause();
    }

    @Override
    public void updateThumbnails(List<UploadModel.UploadItem> uploads) {
        int uploadCount = uploads.size();
        topCardThumbnails.setAdapter(adapterFactory.create(uploads));
        topCardTitle.setText(getResources().getQuantityString(R.plurals.upload_count_title, uploadCount, uploadCount));
    }

    @Override
    public void updateBottomCardContent(int currentStep, int stepCount, UploadModel.UploadItem uploadItem) {
        bottomCardTitle.setText(getResources().getString(R.string.step_count, currentStep, stepCount));
        imageTitle.setText(uploadItem.title);
        imageDescription.setText(uploadItem.description);
    }

    @Override
    public void updateTopCardContent() {
        RecyclerView.Adapter adapter = topCardThumbnails.getAdapter();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void setNextEnabled(boolean available) {
        next.setEnabled(available);
        categoryNext.setEnabled(available);
        submit.setEnabled(available);
    }

    @Override
    public void setPreviousEnabled(boolean available) {
        previous.setEnabled(available);
        categoryPrevious.setEnabled(available);
        licensePrevious.setEnabled(available);
    }

    @Override
    public void setTopCardState(boolean state) {
        updateCardState(state, topCardExpandButton, topCardThumbnails);
    }

    @Override
    public void setTopCardVisibility(boolean visible) {
        topCard.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setBottomCardVisibility(@UploadPage int page) {
        if (page == TITLE_CARD) {
            viewFlipper.setDisplayedChild(0);
        } else if (page == CATEGORIES) {
            viewFlipper.setDisplayedChild(1);
        } else if (page == LICENSE) {
            viewFlipper.setDisplayedChild(2);
        }
    }

    @Override
    public void setBottomCardState(boolean state) {
        updateCardState(state, bottomCardExpandButton, bottomCardContent);
    }

    @Override
    public void setBackground(Uri mediaUri) {
        background.setImageURI(mediaUri);
    }

    @Override
    public void dismissKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(imageTitle.getWindowToken(), 0);
    }

    private void receiveSharedItems() {
        Intent intent = getIntent();
        String mimeType = intent.getType();
        String source;

        if (intent.hasExtra(UploadService.EXTRA_SOURCE)) {
            source = intent.getStringExtra(UploadService.EXTRA_SOURCE);
        } else {
            source = Contribution.SOURCE_EXTERNAL;
        }

        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            Uri mediaUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            presenter.receive(mediaUri, mimeType, source);
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
            ArrayList<Uri> urisList = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            presenter.receive(urisList, mimeType, source);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle state = presenter.getSavedState();
        outState.putAll(state);
    }

    @Override
    protected void onAuthCookieAcquired(String authCookie) {
        mwApi.setAuthCookie(authCookie);
    }

    @Override
    protected void onAuthFailure() {
        Toast failureToast = Toast.makeText(this, R.string.authentication_failed, Toast.LENGTH_LONG);
        failureToast.show();
        finish();
    }

    @Override
    public void thumbnailClicked(UploadModel.UploadItem content) {
        presenter.thumbnailClicked(content);
    }

    private void updateCardState(boolean state, ImageView button, View... content) {
        button.setImageResource(state
                ? R.drawable.ic_expand_less_black_24dp
                : R.drawable.ic_expand_more_black_24dp);
        if (content != null) {
            for (View view : content) {
                view.setVisibility(state
                        ? View.VISIBLE : View.GONE);
            }
        }
    }
}
