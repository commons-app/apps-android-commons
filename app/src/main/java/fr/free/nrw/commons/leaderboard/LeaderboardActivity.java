package fr.free.nrw.commons.leaderboard;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.explore.ViewPagerAdapter;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import fr.free.nrw.commons.theme.NavigationBaseActivity;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class LeaderboardActivity extends NavigationBaseActivity implements
        AdapterView.OnItemSelectedListener{

    private FragmentManager supportFragmentManager;
    private ViewPagerAdapter adapter;
    private LeaderboardFragment leaderboardUploadFragment;
    private LeaderboardFragment leaderboardNearbyFragment;
    private LeaderboardFragment leaderboardUsedFragment;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    String[] duration_array = { "All-time", "Monthly", "Weekly", "Daily"};
    private String duration = null;
    @BindView(R.id.user_rank)
    TextView user_rank;
    @BindView(R.id.viewPagerLeaderboard)
    ViewPager viewPager;
    @BindView(R.id.tab_layout)
    TabLayout tabLayout;
    @Inject
    SessionManager sessionManager;
    @Inject
    OkHttpJsonApiClient okHttpJsonApiClient;

    /**
     * This method helps in the creation Leaderboard screen
     *
     * @param savedInstanceState Data bundle
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);
        ButterKnife.bind(this);
        initDrawer();

        //Getting the instance of Spinner and applying OnItemSelectedListener on it
        Spinner spin = (Spinner) findViewById(R.id.duration_spinner);
        spin.setOnItemSelectedListener(this);

        //Creating the ArrayAdapter instance having the duration array
        ArrayAdapter aa = new ArrayAdapter(this,android.R.layout.simple_spinner_item,duration_array);
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //Setting the ArrayAdapter data on the Spinner
        spin.setAdapter(aa);

        // Activity can call methods in the fragment by acquiring a
        // reference to the Fragment from FragmentManager, using findFragmentById()
        supportFragmentManager = getSupportFragmentManager();
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);
        setTabs();
        setUserRank();
    }

    @SuppressLint("CheckResult")
    private void setUserRank() {
        String userName = sessionManager.getUserName();
        if (StringUtils.isBlank(userName)) {
            return;
        }
        compositeDisposable.add(okHttpJsonApiClient.getUserRank(userName,"upload",duration)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(edits -> user_rank.setText(user_rank.getText()+" : " + String.valueOf(edits)), e -> {
                    Timber.e("Error:" + e);
                }));
    }

    /**
     * Sets the titles in the tabLayout and fragments in the viewPager
     */
    public void setTabs() {
        List<Fragment> fragmentList = new ArrayList<>();
        List<String> titleList = new ArrayList<>();

        leaderboardUploadFragment = new LeaderboardFragment();
        Bundle uploadArguments = new Bundle();
        uploadArguments.putString("FragmentName","upload");
        leaderboardUploadFragment.setArguments(uploadArguments);
        fragmentList.add(leaderboardUploadFragment);
        titleList.add(getResources().getString(R.string.leaderBoard_upload_title));

        leaderboardNearbyFragment = new LeaderboardFragment();
        Bundle nearbyArguments = new Bundle();
        nearbyArguments.putString("FragmentName","nearby");
        leaderboardNearbyFragment.setArguments(nearbyArguments);
        fragmentList.add(leaderboardNearbyFragment);
        titleList.add(getResources().getString(R.string.leaderBoard_nearby_title));

        leaderboardUsedFragment = new LeaderboardFragment();
        Bundle usedArguments = new Bundle();
        usedArguments.putString("FragmentName","used");
        leaderboardUsedFragment.setArguments(usedArguments);
        fragmentList.add(leaderboardUsedFragment);
        titleList.add(getResources().getString(R.string.leaderBoard_used_title));

        adapter.setTabData(fragmentList, titleList);
        adapter.notifyDataSetChanged();
    }

    //Performing action onItemSelected and onNothing selected
    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
        duration=duration_array[position];
      //  Toast.makeText(getApplicationContext(),duration_array[position] , Toast.LENGTH_LONG).show();
    }
    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
    }
}
