package fr.free.nrw.commons.contributions;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static fr.free.nrw.commons.di.NetworkingModule.NAMED_LANGUAGE_WIKI_PEDIA_WIKI_SITE;

import android.content.Context;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.utils.DialogUtil;
import fr.free.nrw.commons.media.MediaClient;
import fr.free.nrw.commons.utils.ViewUtil;
import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Named;
import org.wikipedia.dataclient.WikiSite;
import timber.log.Timber;

/**
 * Created by root on 01.06.2018.
 */

public class ContributionsListFragment extends CommonsDaggerSupportFragment implements
    ContributionsListContract.View, ContributionsListAdapter.Callback,
    WikipediaInstructionsDialogFragment.Callback {

  private static final String RV_STATE = "rv_scroll_state";

  @BindView(R.id.contributionsList)
  RecyclerView rvContributionsList;
  @BindView(R.id.loadingContributionsProgressBar)
  ProgressBar progressBar;
  @BindView(R.id.fab_plus)
  FloatingActionButton fabPlus;
  @BindView(R.id.fab_camera)
  FloatingActionButton fabCamera;
  @BindView(R.id.fab_gallery)
  FloatingActionButton fabGallery;
  @BindView(R.id.noContributionsYet)
  TextView noContributionsYet;
  @BindView(R.id.fab_layout)
  LinearLayout fab_layout;

  @Inject
  ContributionController controller;
  @Inject
  MediaClient mediaClient;

  @Named(NAMED_LANGUAGE_WIKI_PEDIA_WIKI_SITE)
  @Inject
  WikiSite languageWikipediaSite;

  @Inject
  ContributionsListPresenter contributionsListPresenter;

  private Animation fab_close;
  private Animation fab_open;
  private Animation rotate_forward;
  private Animation rotate_backward;


  private boolean isFabOpen;

  private ContributionsListAdapter adapter;

  private Callback callback;

  private final int SPAN_COUNT_LANDSCAPE = 3;
  private final int SPAN_COUNT_PORTRAIT = 1;


  @Override
  public View onCreateView(
      final LayoutInflater inflater, @Nullable final ViewGroup container,
      @Nullable final Bundle savedInstanceState) {
    final View view = inflater.inflate(R.layout.fragment_contributions_list, container, false);
    ButterKnife.bind(this, view);
    contributionsListPresenter.onAttachView(this);
    initAdapter();
    return view;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    if (getParentFragment() != null && getParentFragment() instanceof ContributionsFragment) {
      callback = ((ContributionsFragment) getParentFragment());
    }
  }

  @Override
  public void onDetach() {
    super.onDetach();
    callback = null;//To avoid possible memory leak
  }

  private void initAdapter() {
    adapter = new ContributionsListAdapter(this, mediaClient);
  }

  @Override
  public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    initRecyclerView();
    initializeAnimations();
    setListeners();
  }

  private void initRecyclerView() {
    final GridLayoutManager layoutManager = new GridLayoutManager(getContext(),
        getSpanCount(getResources().getConfiguration().orientation));
    rvContributionsList.setLayoutManager(layoutManager);
    contributionsListPresenter.setup();
    contributionsListPresenter.contributionList.observe(this.getViewLifecycleOwner(), adapter::submitList);
    rvContributionsList.setAdapter(adapter);
    adapter.setHasStableIds(true);
    adapter.registerAdapterDataObserver(new AdapterDataObserver() {
      @Override
      public void onItemRangeInserted(int positionStart, int itemCount) {
        super.onItemRangeInserted(positionStart, itemCount);
        if (itemCount > 0 && positionStart == 0) {
          rvContributionsList.scrollToPosition(0);//Newly upload items are always added to the top
        }
      }
    });
  }

  private int getSpanCount(final int orientation) {
    return orientation == Configuration.ORIENTATION_LANDSCAPE ?
        SPAN_COUNT_LANDSCAPE : SPAN_COUNT_PORTRAIT;
  }

  @Override
  public void onConfigurationChanged(final Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    // check orientation
    fab_layout.setOrientation(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE ?
        LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
    rvContributionsList
        .setLayoutManager(new GridLayoutManager(getContext(), getSpanCount(newConfig.orientation)));
  }

  private void initializeAnimations() {
    fab_open = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_open);
    fab_close = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_close);
    rotate_forward = AnimationUtils.loadAnimation(getActivity(), R.anim.rotate_forward);
    rotate_backward = AnimationUtils.loadAnimation(getActivity(), R.anim.rotate_backward);
  }

  private void setListeners() {
    fabPlus.setOnClickListener(view -> animateFAB(isFabOpen));
    fabCamera.setOnClickListener(view -> {
      controller.initiateCameraPick(getActivity());
      animateFAB(isFabOpen);
    });
    fabGallery.setOnClickListener(view -> {
      controller.initiateGalleryPick(getActivity(), true);
      animateFAB(isFabOpen);
    });
  }

  private void animateFAB(final boolean isFabOpen) {
    this.isFabOpen = !isFabOpen;
    if (fabPlus.isShown()) {
      if (isFabOpen) {
        fabPlus.startAnimation(rotate_backward);
        fabCamera.startAnimation(fab_close);
        fabGallery.startAnimation(fab_close);
        fabCamera.hide();
        fabGallery.hide();
      } else {
        fabPlus.startAnimation(rotate_forward);
        fabCamera.startAnimation(fab_open);
        fabGallery.startAnimation(fab_open);
        fabCamera.show();
        fabGallery.show();
      }
      this.isFabOpen = !isFabOpen;
    }
  }

  /**
   * Shows welcome message if user has no contributions yet i.e. new user.
   */
  @Override
  public void showWelcomeTip(final boolean shouldShow) {
    noContributionsYet.setVisibility(shouldShow ? VISIBLE : GONE);
  }

  /**
   * Responsible to set progress bar invisible and visible
   *
   * @param shouldShow True when contributions list should be hidden.
   */
  @Override
  public void showProgress(final boolean shouldShow) {
    progressBar.setVisibility(shouldShow ? VISIBLE : GONE);
  }

  @Override
  public void showNoContributionsUI(final boolean shouldShow) {
    noContributionsYet.setVisibility(shouldShow ? VISIBLE : GONE);
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    final GridLayoutManager layoutManager = (GridLayoutManager) rvContributionsList
        .getLayoutManager();
    outState.putParcelable(RV_STATE, layoutManager.onSaveInstanceState());
  }

  @Override
  public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
    super.onViewStateRestored(savedInstanceState);
    if (null != savedInstanceState) {
      final Parcelable savedRecyclerLayoutState = savedInstanceState.getParcelable(RV_STATE);
      rvContributionsList.getLayoutManager().onRestoreInstanceState(savedRecyclerLayoutState);
    }
  }

  @Override
  public void retryUpload(final Contribution contribution) {
    if (null != callback) {//Just being safe, ideally they won't be called when detached
      callback.retryUpload(contribution);
    }
  }

  @Override
  public void deleteUpload(final Contribution contribution) {
    contributionsListPresenter.deleteUpload(contribution);
  }

  @Override
  public void openMediaDetail(final int position, boolean isWikipediaButtonDisplayed) {
    if (null != callback) {//Just being safe, ideally they won't be called when detached
      callback.showDetail(position, isWikipediaButtonDisplayed);
    }
  }

  /**
   * Handle callback for wikipedia icon clicked
   *
   * @param contribution
   */
  @Override
  public void addImageToWikipedia(Contribution contribution) {
    DialogUtil.showAlertDialog(getActivity(),
        getString(R.string.add_picture_to_wikipedia_article_title),
        String.format(getString(R.string.add_picture_to_wikipedia_article_desc),
            Locale.getDefault().getDisplayLanguage()),
        () -> {
          showAddImageToWikipediaInstructions(contribution);
        }, () -> {
          // do nothing
        });
  }

  /**
   * Pauses the current upload
   * @param contribution
   */
  @Override
  public void pauseUpload(Contribution contribution) {
    ViewUtil.showShortToast(getContext(), R.string.pausing_upload);
    callback.pauseUpload(contribution);
  }

  /**
   * Resumes the current upload
   * @param contribution
   */
  @Override
  public void resumeUpload(Contribution contribution) {
    ViewUtil.showShortToast(getContext(), R.string.resuming_upload);
    callback.retryUpload(contribution);
  }

  /**
   * Display confirmation dialog with instructions when the user tries to add image to wikipedia
   *
   * @param contribution
   */
  private void showAddImageToWikipediaInstructions(Contribution contribution) {
    FragmentManager fragmentManager = getFragmentManager();
    WikipediaInstructionsDialogFragment fragment = WikipediaInstructionsDialogFragment
        .newInstance(contribution);
    fragment.setCallback(this::onConfirmClicked);
    fragment.show(fragmentManager, "WikimediaFragment");
  }


  public Media getMediaAtPosition(final int i) {
    return adapter.getContributionForPosition(i).getMedia();
  }

  public int getTotalMediaCount() {
    return adapter.getItemCount();
  }

  /**
   * Open the editor for the language Wikipedia
   *
   * @param contribution
   */
  @Override
  public void onConfirmClicked(@Nullable Contribution contribution, boolean copyWikicode) {
    if (copyWikicode) {
      String wikicode = contribution.getMedia().getWikiCode();
      Utils.copy("wikicode", wikicode, getContext());
    }

    final String url =
        languageWikipediaSite.mobileUrl() + "/wiki/" + contribution.getWikidataPlace()
            .getWikipediaPageTitle();
    Utils.handleWebUrl(getContext(), Uri.parse(url));
  }

  public Integer getContributionStateAt(int position) {
    return adapter.getContributionForPosition(position).getState();
  }

  public interface Callback {

    void retryUpload(Contribution contribution);

    void showDetail(int position, boolean isWikipediaButtonDisplayed);

    void pauseUpload(Contribution contribution);
  }
}
