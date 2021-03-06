package openfoodfacts.github.scrachx.openfood.views;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import openfoodfacts.github.scrachx.openfood.BuildConfig;
import openfoodfacts.github.scrachx.openfood.R;
import openfoodfacts.github.scrachx.openfood.models.Product;
import openfoodfacts.github.scrachx.openfood.models.Search;
import openfoodfacts.github.scrachx.openfood.network.OpenFoodAPIClient;
import openfoodfacts.github.scrachx.openfood.views.adapters.ProductsRecyclerViewAdapter;
import openfoodfacts.github.scrachx.openfood.views.listeners.EndlessRecyclerViewScrollListener;
import openfoodfacts.github.scrachx.openfood.views.listeners.RecyclerItemClickListener;

public class BrandActivity extends BaseActivity {

    private String brand;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.products_recycler_view)
    RecyclerView productsRecyclerView;
    @BindView(R.id.textCountProduct)
    TextView countProductsView;
    @BindView(R.id.offlineCloudLinearLayout)
    LinearLayout offlineCloudLayout;
    ProgressBar progressBar;
    private EndlessRecyclerViewScrollListener scrollListener;
    private List<Product> mProducts;
    private OpenFoodAPIClient api;
    private OpenFoodAPIClient apiClient;
    private int mCountProducts = 0;
    private int pageAddress = 1;


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home){
           finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_brand);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        countProductsView.setVisibility(View.INVISIBLE);
        Bundle extras = getIntent().getExtras();

        brand = extras.getString("brand");

        getSupportActionBar().setTitle(brand);
        getSupportActionBar().setSubtitle(R.string.brand_string);
        apiClient = new OpenFoodAPIClient(BrandActivity.this, BuildConfig.OFWEBSITE);
        api = new OpenFoodAPIClient(BrandActivity.this);
        productsRecyclerView = (RecyclerView) findViewById(R.id.products_recycler_view);
        setup();
    }

    @OnClick(R.id.buttonTryAgain)
    public void setup() {
        progressBar.setVisibility(View.VISIBLE);
        offlineCloudLayout.setVisibility(View.INVISIBLE);
        countProductsView.setVisibility(View.INVISIBLE);
        getDataFromAPI();
    }

    public void getDataFromAPI() {
        apiClient.getBrand(brand, pageAddress, new OpenFoodAPIClient.OnBrandCallback() {
            @Override
            public void onBrandResponse(boolean value, Search brandObject) {
                if (value) {
                    mCountProducts = Integer.parseInt(brandObject.getCount());
                    if (pageAddress == 1) {
                        countProductsView.append(String.valueOf(brandObject.getCount()));
                        mProducts = new ArrayList<>();
                        mProducts.addAll(brandObject.getProducts());
                        if (mProducts.size() < mCountProducts) {
                            mProducts.add(null);
                        }
                        setUpRecyclerView();
                    } else {
                        if (mProducts.size() - 1 < mCountProducts + 1) {
                            final int posStart = mProducts.size();
                            mProducts.remove(mProducts.size() - 1);
                            mProducts.addAll(brandObject.getProducts());
                            if (mProducts.size() < mCountProducts) {
                                mProducts.add(null);
                            }
                            productsRecyclerView.getAdapter().notifyItemRangeChanged(posStart - 1, mProducts.size() - 1);
                        }
                    }
                } else {
                    progressBar.setVisibility(View.INVISIBLE);
                    offlineCloudLayout.setVisibility(View.VISIBLE);
                }

            }
        });
    }

    private void setUpRecyclerView() {

        progressBar.setVisibility(View.INVISIBLE);
        countProductsView.setVisibility(View.VISIBLE);

        offlineCloudLayout.setVisibility(View.INVISIBLE);

        productsRecyclerView.setVisibility(View.VISIBLE);
        productsRecyclerView.setHasFixedSize(true);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(BrandActivity.this, LinearLayoutManager.VERTICAL, false);
        productsRecyclerView.setLayoutManager(mLayoutManager);

        ProductsRecyclerViewAdapter adapter = new ProductsRecyclerViewAdapter(mProducts);
        productsRecyclerView.setAdapter(adapter);


        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(productsRecyclerView.getContext(),
                DividerItemDecoration.VERTICAL);
        productsRecyclerView.addItemDecoration(dividerItemDecoration);

        // Retain an instance so that you can call `resetState()` for fresh searches
        scrollListener = new EndlessRecyclerViewScrollListener(mLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                if (mProducts.size() < mCountProducts) {
                    pageAddress = page;
                    getDataFromAPI();
                }
            }
        };
        // Adds the scroll listener to RecyclerView
        productsRecyclerView.addOnScrollListener(scrollListener);


        productsRecyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(BrandActivity.this, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        Product p = ((ProductsRecyclerViewAdapter) productsRecyclerView.getAdapter()).getProduct(position);
                        if (p != null) {
                            String barcode = p.getCode();
                            api.getProduct(barcode, BrandActivity.this);
                            try {
                                View view1 = BrandActivity.this.getCurrentFocus();
                                if (view != null) {
                                    InputMethodManager imm = (InputMethodManager) BrandActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                                    imm.hideSoftInputFromWindow(view1.getWindowToken(), 0);
                                }
                            } catch (NullPointerException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                })
        );

    }
}
