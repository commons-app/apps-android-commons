package fr.free.nrw.commons.depictions.subClass;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.android.support.DaggerFragment;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.depictions.WikidataItemDetailsActivity;
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import kotlin.Unit;

/**
 * Fragment for parent classes and child classes of Depicted items in Explore
 */
public class SubDepictionListFragment extends DaggerFragment implements
    SubDepictionListContract.View {

  @BindView(R.id.imagesListBox)
  RecyclerView depictionsRecyclerView;
  @BindView(R.id.imageSearchInProgress)
  ProgressBar progressBar;
  @BindView(R.id.imagesNotFound)
  TextView depictionNotFound;
  @BindView(R.id.bottomProgressBar)
  ProgressBar bottomProgressBar;
  /**
   * Keeps a record of whether current instance of the fragment if of SubClass or ParentClass
   */
  private boolean isParentClass = false;
  private SubDepictionAdapter depictionsAdapter;
  RecyclerView.LayoutManager layoutManager;
  /**
   * Stores entityId for the depiction
   */
  private String entityId;
  /**
   * Stores name of the depiction searched
   */
  private String depictsName;

  @Inject
  SubDepictionListPresenter presenter;

  private void initViews() {
    if (getArguments() != null) {
      depictsName = getArguments().getString("wikidataItemName");
      entityId = getArguments().getString("entityId");
      isParentClass = getArguments().getBoolean("isParentClass");
      if (entityId != null) {
        initList(entityId, isParentClass);
      }
    }
  }

  private void initList(String qid, Boolean isParentClass) {
    if (!NetworkUtils.isInternetConnectionEstablished(getContext())) {
      handleNoInternet();
    } else {
      progressBar.setVisibility(View.VISIBLE);
      try {
        presenter.initSubDepictionList(qid, isParentClass);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.fragment_browse_image, container, false);
    ButterKnife.bind(this, v);
    presenter.onAttachView(this);
    isParentClass = false;
    depictionNotFound.setVisibility(GONE);
    if (getActivity().getResources().getConfiguration().orientation
        == Configuration.ORIENTATION_PORTRAIT) {
      layoutManager = new LinearLayoutManager(getContext());
    } else {
      layoutManager = new GridLayoutManager(getContext(), 2);
    }
    initViews();
    depictionsRecyclerView.setLayoutManager(layoutManager);
    depictionsAdapter = new SubDepictionAdapter(depictedItem -> {
      // Open SubDepiction Details page
      getActivity().finish();
      WikidataItemDetailsActivity.startYourself(getContext(), depictedItem);
      return Unit.INSTANCE;
    });
    depictionsRecyclerView.setAdapter(depictionsAdapter);
    return v;
  }

  private void handleNoInternet() {
    progressBar.setVisibility(GONE);
    ViewUtil.showShortSnackbar(depictionsRecyclerView, R.string.no_internet);
  }

  @Override
  public void onSuccess(List<DepictedItem> mediaList) {
    progressBar.setVisibility(View.GONE);
    depictionNotFound.setVisibility(GONE);
    bottomProgressBar.setVisibility(GONE);
    depictionsAdapter.addAll(mediaList);
  }

  @Override
  public void initErrorView() {
    progressBar.setVisibility(GONE);
    bottomProgressBar.setVisibility(GONE);
    depictionNotFound.setVisibility(VISIBLE);
    String no_depiction = getString(
        isParentClass ? R.string.no_parent_classes : R.string.no_child_classes);
    depictionNotFound.setText(String.format(Locale.getDefault(), no_depiction, depictsName));

  }

  @Override
  public void showSnackbar() {
    ViewUtil.showShortSnackbar(depictionsRecyclerView, R.string.error_loading_depictions);
  }

  @Override
  public void setIsLastPage(boolean b) {
  }

}
