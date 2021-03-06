package com.dangth.foodrecipe.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Parcelable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.AppBarLayout;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.dangth.foodrecipe.GlideApp;
import com.dangth.foodrecipe.R;
import com.dangth.foodrecipe.adapter.CarouselAdapter;
import com.dangth.foodrecipe.adapter.RecipeListAdapter;
import com.dangth.foodrecipe.fragment.OptionDialogFragment;
import com.dangth.foodrecipe.searchview.SuggestionHandler;
import com.dangth.foodrecipe.services.FeedAPI;
import com.dangth.foodrecipe.services.RetrofitClientInstance;
import com.dangth.foodrecipe.services.model.Compilation;
import com.dangth.foodrecipe.services.model.FeedResponse;
import com.dangth.foodrecipe.services.model.Recipe;
import com.dangth.foodrecipe.searchview.SearchViewQuery;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        SwipeRefreshLayout.OnRefreshListener, AppBarLayout.OnOffsetChangedListener {
    private FloatingSearchView searchView;
    private RecipeListAdapter adapter;
    private CarouselAdapter carouselAdapter;
    private ImageView ibFeatureItem;
    private TextView tvFeatureTitle;
    private NestedScrollView nestedScrollView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ConstraintLayout errorView;
    private LinearLayout retryButton;
    private FeedAPI feedAPI;
    private AppBarLayout appBarLayout;
    private Recipe featureItem;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindView();
//        Toolbar toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//        getSupportActionBar().setDisplayShowTitleEnabled(false);
//        getSupportActionBar().setHomeButtonEnabled(false);
        swipeRefreshLayout.setRefreshing(true);
        loadFeedData();

    }
    @Override
    public void onBackPressed() {

        super.onBackPressed();
    }

    /**
     * bind view for widget
     */

    private void bindView() {
        /* FeedAPI Instance */
        feedAPI = RetrofitClientInstance.getRetrofitInstance().create(FeedAPI.class);

        /* ImageButton Feature */
        ibFeatureItem = findViewById(R.id.ivFeatureItem);
        ibFeatureItem.setOnClickListener(this);

        /* Feature title */
        tvFeatureTitle = findViewById(R.id.featureTitle);

        /* SearchView */
        searchView = findViewById(R.id.floating_search_view);
        searchView.setOnSearchListener(new SearchViewQuery(MainActivity.this, searchView));
        searchView.setOnQueryChangeListener(new SuggestionHandler(searchView));
        searchView.setOnMenuItemClickListener(item -> {
            OptionDialogFragment optionDialogFragment = OptionDialogFragment.newInstance();
            optionDialogFragment.show(getSupportFragmentManager(), optionDialogFragment.getTag());
            optionDialogFragment.setOnDialogDismissListener(() -> {
                swipeRefreshLayout.setRefreshing(true);
                loadFeedData();
            });
        });

        /* RecyclerView Recent */
        RecyclerView rvRecent = findViewById(R.id.rvRecent);
        rvRecent.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        rvRecent.setItemAnimator(new DefaultItemAnimator());
        adapter = new RecipeListAdapter(this, R.layout.recycler_view_item);
        rvRecent.setAdapter(adapter);

        /* RecyclerView Carousel*/
        RecyclerView rvCarousel = findViewById(R.id.carouselList);
        rvCarousel.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        rvCarousel.setItemAnimator(new DefaultItemAnimator());
        carouselAdapter = new CarouselAdapter(this);
        rvCarousel.setAdapter(carouselAdapter);

        /* NSV */
        nestedScrollView = findViewById(R.id.scrollMainView);
        nestedScrollView.setVisibility(View.INVISIBLE);

        /* SwipeRefreshLayout */
        swipeRefreshLayout = findViewById(R.id.mainSwipeRefresh);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorAccent, R.color.colorPrimary, R.color.colorPrimaryDark);
        swipeRefreshLayout.setOnRefreshListener(this);

        /* Error View */
        errorView = findViewById(R.id.errorView);

        /* RetryButton */
        retryButton = findViewById(R.id.retryButton);
        retryButton.setOnClickListener(view ->{
            swipeRefreshLayout.setRefreshing(true);
            loadFeedData();
        });

        /* appbar */
        appBarLayout = findViewById(R.id.appbar);
        appBarLayout.addOnOffsetChangedListener(this);
    }

    /**
     * Get feed data from API through Retrofit
     * send data to loadDataFeedView()
     *
     */
    public void loadFeedData() {
        SharedPreferences sharedPreferences = getSharedPreferences(OptionDialogFragment.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        boolean vegetarian = sharedPreferences.getBoolean(OptionDialogFragment.SHARED_PREFERENCES_VEGETARIAN, false);
        Call<FeedResponse> call = feedAPI.getFeedData(vegetarian, 15, 0);
        call.enqueue(new Callback<FeedResponse>() {
            @Override
            public void onResponse(Call<FeedResponse> call, Response<FeedResponse> response) {
                if (response.body() != null) {
                    if (nestedScrollView.getVisibility() == View.GONE) {
                        nestedScrollView.setVisibility(View.VISIBLE);
                    }
                    errorView.setVisibility(View.GONE);
                    loadDataFeedView(response.body());
                }
                else {
                    Toast.makeText(MainActivity.this, "Something went wrong...", Toast.LENGTH_LONG).show();
                }
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onFailure(Call<FeedResponse> call, Throwable t) {
                Log.e("Fail", t.getMessage());
                swipeRefreshLayout.setRefreshing(false);
                nestedScrollView.setVisibility(View.GONE);
                errorView.setVisibility(View.VISIBLE);

            }
        });
    }

    /**
     * set data for FeatureItem and Recipe RecyclerView
     * @param feedResponse feed data
     */

    private void loadDataFeedView(FeedResponse feedResponse) {
        Log.i("DEMO", "loadDataFeedView: load start");
        List<FeedResponse.FeedItem> feedItemList = feedResponse.getResults();
        List<Recipe> recipeList = new ArrayList<>();
        List<FeedResponse.FeedItem> carouselList = new ArrayList<>();
        Log.i("DEMO", "loadDataFeedView: " + feedItemList);
        for (FeedResponse.FeedItem feedItem : feedItemList) {
            if (feedItem.getContent()!=null) {
                if (feedItem.getType().equals("featured")) {
                    featureItem = feedItem.getContent();
                    tvFeatureTitle.setText(feedItem.getContent().getName());
                    GlideApp.with(MainActivity.this)
                            .load(feedItem.getContent().getThumbnail_url())
                            .transition(withCrossFade())
                            .centerCrop()
                            .into(ibFeatureItem);
                }
                else if (feedItem.getType().equals("item")) {
                    recipeList.add(feedItem.getContent());
                }
            } else if (feedItem.getCarouselItem() != null) {
                carouselList.add(feedItem);
            }
        }
        if (nestedScrollView.getVisibility() == View.INVISIBLE) {
            nestedScrollView.setVisibility(View.VISIBLE);
        }
        adapter.setRecipeList(recipeList);
        adapter.notifyDataSetChanged();
        carouselAdapter.setCarouselList(carouselList);
        carouselAdapter.notifyDataSetChanged();
        Log.i("DEMO", "loadDataFeedView: " + carouselAdapter.getItemCount());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ivFeatureItem :
                if (!featureItem.isComplition()) {
                    Intent intent = new Intent(MainActivity.this, RecipeActivity.class);
                    intent.putExtra(RecipeListAdapter.INTENT_RECIPE_ACTIVITY, (Parcelable) featureItem);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(this, CompilationActivity.class);
                    Compilation compilation = new Compilation();
                    compilation.setRecipes(featureItem.getRecipes());
                    compilation.setThumbnail_url(featureItem.getThumbnail_url());
                    compilation.setName(featureItem.getName());
                    compilation.setVideo_url(featureItem.getVideo_url());
                    intent.putExtra(RecipeListAdapter.INTENT_COMPILATION_ACTIVITY, (Parcelable) compilation);
                    startActivity(intent);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onRefresh() {
        swipeRefreshLayout.setRefreshing(true);
        loadFeedData();
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
        searchView.setTranslationY(i);
    }
}
