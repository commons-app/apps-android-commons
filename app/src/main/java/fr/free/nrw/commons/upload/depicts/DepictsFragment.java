package fr.free.nrw.commons.upload.depicts;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.upload.UploadBaseFragment;
import fr.free.nrw.commons.upload.UploadContract;

public class DepictsFragment extends UploadBaseFragment implements  DepictsContract.View {

    @BindView(R.id.depicts_title)
    TextView depictsTitle;
    @BindView(R.id.depicts_subtitle)
    TextView depictsSubtitile;
    @BindView(R.id.depicts_search_container)
    TextInputLayout depictsSearchContainer;
    @BindView(R.id.depicts_search)
    TextInputEditText depictsSearch;
    @BindView(R.id.depictsSearchInProgress)
    ProgressBar depictsSearchInProgress;
    @BindView(R.id.depicts_recycler_view)
    RecyclerView depictsRecyclerView;
    @BindView(R.id.depicts_next)
    Button depictsNext;
    @BindView(R.id.depicts_previous)
    Button depictsPrevious;

    @Inject
    DepictsContract.UserActionListener presenter;

    @Nullable
    @Override
    public android.view.View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                          @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.upload_depicts_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull android.view.View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        initPresenter();
    }
    private void initPresenter() {
        presenter.onAttachView(this);
    }

    @Override
    public void goToNextScreen() {
        callback.onNextButtonClicked(callback.getIndexInViewFlipper(this));
    }

    @OnClick(R.id.depicts_next)
    public void onNextButtonClicked(){
        presenter.onNextButtonPressed();
    }
}
