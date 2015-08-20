/*******************************************************************************
 * Copyright 2015 ShopGun
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.shopgun.android.sdk.pageflip;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.shopgun.android.sdk.Constants;
import com.shopgun.android.sdk.R;
import com.shopgun.android.sdk.ShopGun;
import com.shopgun.android.sdk.log.SgnLog;
import com.shopgun.android.sdk.model.Branding;
import com.shopgun.android.sdk.model.Catalog;
import com.shopgun.android.sdk.model.Hotspot;
import com.shopgun.android.sdk.network.Response.Listener;
import com.shopgun.android.sdk.network.ShopGunError;
import com.shopgun.android.sdk.network.impl.JsonObjectRequest;
import com.shopgun.android.sdk.pageflip.utils.PageflipUtils;
import com.shopgun.android.sdk.pageflip.widget.LoadingTextView;
import com.shopgun.android.sdk.request.RequestAutoFill.AutoFillParams;
import com.shopgun.android.sdk.request.impl.CatalogObjectRequest.CatalogAutoFill;
import com.shopgun.android.sdk.utils.Api.Endpoint;
import com.shopgun.android.sdk.utils.Utils;

import org.json.JSONObject;

import java.util.List;

public class PageflipFragment extends Fragment {

    public static final String TAG = Constants.getTag(PageflipFragment.class);
    public static final String ARG_CATALOG = Constants.getArg(PageflipFragment.class, "catalog");
    public static final String ARG_CATALOG_ID = Constants.getArg(PageflipFragment.class, "catalog-id");
    public static final String ARG_PAGE = Constants.getArg(PageflipFragment.class, "page");
    public static final String ARG_VIEW_SESSION = Constants.getArg(PageflipFragment.class, "view-session");
    public static final String ARG_BRANDING = Constants.getArg(PageflipFragment.class, "branding");

    private static final double PAGER_SCROLL_FACTOR = 0.5d;

    // Requests
    CatalogAutoFill mCatalogAutoFill;
    // Need this
    private Catalog mCatalog;
    private String mCatalogId;
    private Branding mBranding;
    // Views
    private LayoutInflater mInflater;
    private ViewGroup mContainer;
    private FrameLayout mFrame;
    private LoadingTextView mLoader;
    private PageflipViewPager mPager;
    private CatalogPagerAdapter mAdapter;
    // State
    private int mCurrentPosition = 0;
    private boolean mLandscape = false;
    private boolean mPagesReady = false;
    private boolean mPageflipStarted = false;

    private String mViewSessionUuid;

    // Callbacks and stats
    private PageflipListenerWrapper mWrapperListener = new PageflipListenerWrapper();
    private Runnable mOnCatalogComplete = new Runnable() {

        public void run() {

            if (!isAdded()) {
                return;
            }

            int heap = PageflipUtils.getMaxHeap(getActivity());
            mAdapter = new CatalogPagerAdapter(getChildFragmentManager(), heap, mCatalogPageCallback, mLandscape);
            mPager.setAdapter(mAdapter);
            // force the first page change if needed
            if (mPager.getCurrentItem() != mCurrentPosition) {
                mPager.setCurrentItem(mCurrentPosition);
            } else {
                mWrapperListener.onPageChange(PageflipUtils.positionToPages(mCurrentPosition, mCatalog.getPageCount(), mLandscape));
            }
            showContent(true);

            mWrapperListener.onReady();

        }

    };

    private Listener<Catalog> mCatalogListener = new Listener<Catalog>() {

        public void onComplete(Catalog c, ShopGunError error) {

            if (isAdded()) {

                if (isCatalogReady()) {

                    getActivity().runOnUiThread(mOnCatalogComplete);

                } else if (error != null) {

                    // TODO improve error stuff 1 == network error
                    mLoader.error();
                    mWrapperListener.onError(error);

                } else {

                    ShopGunError e = null;
                    if (mCatalog != null) {

                        if (mCatalog.getPages() != null && mCatalog.getPages().isEmpty()) {
                            // Got empty pages again, this shouldn't ever happen
                            String message = "Catalog pages missing.";
                            String details = "The api didn't return a valid set of pages for catalog " + mCatalog.getErn();
                            e = new ShopGunError(ShopGunError.Code.PAGEFLIP_LOADING_PAGES_FAILED, message, details);
                        }

                    }

                    if (e == null) {
                        e = new ShopGunError(ShopGunError.Code.PAGEFLIP_CATALOG_LOADING_FAILED, "Unknown error", "An error occoured while loading data for pageflip");
                    }

                    // TODO improve error stuff 1 == network error
                    mLoader.error();
                    mWrapperListener.onError(e);

                }

            }

        }
    };

    private CatalogPageCallback mCatalogPageCallback = new CatalogPageCallback() {

        @Override
        public boolean isPositionSet() {
            return mPager.getCurrentItem() == mCurrentPosition;
        }

        public void onReady(int position) {
            if (position == mCurrentPosition) {
                CatalogPageFragment old = getPage(position);
                old.onVisible();
                mPagesReady = true;
            }
        }

        @Override
        public void onSingleClick(View v, int page, float x, float y, List<Hotspot> hotspots) {
            mWrapperListener.onSingleClick(v, page, x, y, hotspots);
        }

        @Override
        public void onDoubleClick(View v, int page, float x, float y, List<Hotspot> hotspots) {
            mWrapperListener.onDoubleClick(v, page, x, y, hotspots);
        }

        @Override
        public void onLongClick(View v, int page, float x, float y, List<Hotspot> hotspots) {
            mWrapperListener.onLongClick(v, page, x, y, hotspots);
        }

        @Override
        public void onZoom(View v, int[] pages, boolean isZoomed) {
            mWrapperListener.onZoom(v, pages, isZoomed);
        }

        @Override
        public Catalog getCatalog() {
            return mCatalog;
        }

        @Override
        public String getViewSession() {
            return mViewSessionUuid;
        }
    };

    private OnPageChangeListener mOnPageChangeListener = new OnPageChangeListener() {

        @Override
        public void onPageSelected(int position) {
            int oldPos = mCurrentPosition;
            mCurrentPosition = position;
            if (mPagesReady) {
                CatalogPageFragment old = getPage(oldPos);
                old.onInvisible();
                CatalogPageFragment current = getPage(mCurrentPosition);
                current.onVisible();
            }
            mWrapperListener.onPageChange(PageflipUtils.positionToPages(mCurrentPosition, mCatalog.getPageCount(), mLandscape));
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageScrollStateChanged(int state) {
            mWrapperListener.onDragStateChanged(state);
        }

    };

    private PageflipViewPager.OnPageBoundListener mPageBoundListener = new PageflipViewPager.OnPageBoundListener() {
        @Override
        public void onLeftBound() {
            mWrapperListener.onOutOfBounds(true);
        }

        @Override
        public void onRightBound() {
            mWrapperListener.onOutOfBounds(false);
        }
    };

    /**
     * Creates a new instance of {@link PageflipFragment}, to replace or insert into a current layout.
     *
     * @param c The catalog to show
     * @return A Fragment
     */
    public static PageflipFragment newInstance(Catalog c) {
        return newInstance(c, 1);
    }

    /**
     * Creates a new instance of {@link PageflipFragment}, to replace or insert into a current layout.
     *
     * @param c    The catalog to show
     * @param page the page number to start at
     * @return A Fragment
     */
    public static PageflipFragment newInstance(Catalog c, int page) {
        Bundle b = new Bundle();
        b.putParcelable(ARG_CATALOG, c);
        b.putInt(ARG_PAGE, page);
        return newInstance(b);
    }

    /**
     * Creates a new instance of {@link PageflipFragment}, to replace or insert into a current layout.
     *
     * @param catalogId The id of the catalog to display
     * @return A Fragment
     */
    public static PageflipFragment newInstance(String catalogId) {
        return newInstance(catalogId, 1);
    }

    /**
     * Creates a new instance of {@link PageflipFragment}, to replace or insert into a current layout.
     *
     * @param catalogId The is of the catalog to show
     * @param page      the page number to start at
     * @return A Fragment
     */
    public static PageflipFragment newInstance(String catalogId, int page) {
        return newInstance(catalogId, page, null);
    }

    /**
     * Creates a new instance of {@link PageflipFragment}, to replace or insert into a current layout.
     *
     * @param catalogId The is of the catalog to show
     * @param page      the page number to start at
     * @return A Fragment
     */
    public static PageflipFragment newInstance(String catalogId, int page, Branding initialBranding) {
        Bundle b = new Bundle();
        b.putString(ARG_CATALOG_ID, catalogId);
        b.putInt(ARG_PAGE, page);
        b.putParcelable(ARG_BRANDING, initialBranding);
        return newInstance(b);
    }

    private static PageflipFragment newInstance(Bundle args) {
        PageflipFragment f = new PageflipFragment();
        f.setArguments(args);
        if (!f.getArguments().containsKey(ARG_VIEW_SESSION)) {
            f.getArguments().putString(ARG_VIEW_SESSION, Utils.createUUID());
        }
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        mLandscape = PageflipUtils.isLandscape(getActivity());
        Bundle b = new Bundle();
        if (getArguments() != null) {
            b.putAll(getArguments());
        }
        if (savedInstanceState != null) {
            b.putAll(savedInstanceState);
        }
        setState(b);
        super.onCreate(savedInstanceState);
    }

    private void setState(Bundle args) {

        int page = args.getInt(ARG_PAGE, 1);
        setPage(page);

        if (args.containsKey(ARG_CATALOG)) {
            setCatalog((Catalog) args.getParcelable(ARG_CATALOG));
            mBranding = mCatalog.getBranding();
        } else if (args.containsKey(ARG_CATALOG_ID)) {
            setCatalogId(args.getString(ARG_CATALOG_ID));
            mBranding = args.getParcelable(ARG_BRANDING);
        } else {
            mLoader.error("No catalog provided");
        }

        mViewSessionUuid = args.getString(ARG_VIEW_SESSION);

        if (mViewSessionUuid == null) {
            mViewSessionUuid = Utils.createUUID();
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mInflater = inflater;
        mContainer = container;
        setUpView(true);
        return mFrame;

    }

    /**
     * Called to setup the view, on create and resume events.
     *
     * @param removeParent Whether to remove the View from the parent view (on e.g. configuration changes)
     */
    private void setUpView(boolean removeParent) {

        if (mFrame == null) {
            mFrame = (FrameLayout) mInflater.inflate(R.layout.shopgun_sdk_layout_pageflip, mContainer, false);
        } else {
            // Remove self from parent view, to avoid attaching to two different vie
            ViewGroup parent = (ViewGroup) mFrame.getParent();
            if (parent != null && removeParent) {
                parent.removeView(mFrame);
            }
        }

        mLoader = (LoadingTextView) mFrame.findViewById(R.id.shopgun_sdk_layout_pageflip_loader);
        mPager = (PageflipViewPager) mFrame.findViewById(R.id.shopgun_sdk_layout_pageflip_viewpager);
        mPager.setScrollDurationFactor(PAGER_SCROLL_FACTOR);
        mPager.addOnPageChangeListener(mOnPageChangeListener);
        mPager.setOnPageBound(mPageBoundListener);

        showContent(false);
        setBranding(mBranding);
        mLoader.start();

    }

    private void showContent(boolean show) {
        mLoader.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
        mPager.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Get the {@link PageflipListener}.
     *
     * @return The listener, or <code>null</code>.
     */
    public PageflipListener getListener() {
        return mWrapperListener.getListener();
    }

    /**
     * Set a listener to call on {@link PageflipFragment} events.
     *
     * @param l The listener
     */
    public void setPageflipListener(PageflipListener l) {
        mWrapperListener.setListener(l);
    }

    /**
     * Get the pages currently being displayed in the {@link PageflipFragment}.
     *
     * @return An array of pages being displayed
     */
    public int[] getPages() {
        return PageflipUtils.positionToPages(mCurrentPosition, mCatalog.getPageCount(), mLandscape);
    }

    /**
     * Set the {@link PageflipFragment} to show the given page number in the catalog.
     * Note that page number doesn't directly correlate to the position of the {@link PageflipViewPager}.
     *
     * @param page The page to turn to
     */
    public void setPage(int page) {
        if (PageflipUtils.isValidPage(mCatalog, page)) {
            setPosition(PageflipUtils.pageToPosition(page, mLandscape));
        }
    }

    /**
     * Get the current position of the {@link PageflipViewPager}.
     *
     * @return The current position
     */
    public int getPosition() {
        return mCurrentPosition;
    }

    /**
     * Set the position of the {@link PageflipViewPager}.
     * Note that this does not correlate directly to the catalog page number.
     *
     * @param position A position
     */
    public void setPosition(int position) {
        mCurrentPosition = position;
        if (mPager != null) {
            mPager.setCurrentItem(mCurrentPosition);
        }
    }

    /**
     * Go to the next page in the catalog
     */
    public void nextPage() {
        mPager.setCurrentItem(mCurrentPosition + 1, true);
    }

    /**
     * Go to the previous page in the catalog
     */
    public void previousPage() {
        mPager.setCurrentItem(mCurrentPosition - 1, true);
    }

    /**
     * Return the current branding, used by {@link PageflipFragment}.
     *
     * @return A {@link Branding}
     */
    public Branding getBranding() {
        return mBranding;
    }

    private void setBranding(Branding b) {

        if (b == null) {
            return;
        }

        mBranding = b;
        mLoader.setLoadingText(mBranding.getName());
        int text = PageflipUtils.getTextColor(mBranding.getColor(), getActivity());
        mLoader.setTextColor(text);
        mFrame.setBackgroundColor(mBranding.getColor());
        mContainer.setBackgroundColor(mBranding.getColor());

    }

    /**
     * Method for determining if the {@link PageflipFragment} is ready.
     * It checks if the {@link PageflipViewPager} has an {@link FragmentStatelessPagerAdapter} attached.
     *
     * @return true if the fragment if ready, else false.
     */
    public boolean isReady() {
        return mAdapter != null;
    }

    /**
     * Method for determining if the catalog is ready;
     *
     * @return true, if the catalog is fully loaded, including pages and hotspots
     */
    public boolean isCatalogReady() {
        return PageflipUtils.isCatalogReady(mCatalog);
    }

    /**
     * Set the id of the {@link Catalog#getId() catalog} that you want to display.
     * This is unnecessary if you have created the fragment with one of the provided
     * {@link PageflipFragment} newInstance methods.
     *
     * @param catalogId A catalog id
     */
    public void setCatalogId(String catalogId) {
        mCatalogId = catalogId;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        boolean land = PageflipUtils.isLandscape(newConfig);
        if (land != mLandscape) {
            internalPause();

            // Get the old page
            int[] pages = PageflipUtils.positionToPages(mCurrentPosition, mCatalog.getPageCount(), mLandscape);
            // switch to landscape mode
            mLandscape = land;
            // set new current position accordingly
            mCurrentPosition = PageflipUtils.pageToPosition(pages[0], mLandscape);

            mAdapter.clearState();
            mPager.setAdapter(null);

            setUpView(false);
            internalResume();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(ARG_PAGE, getPages()[0]);
        outState.putParcelable(ARG_CATALOG, mCatalog);
        outState.putString(ARG_CATALOG_ID, mCatalogId);
        outState.putString(ARG_VIEW_SESSION, mViewSessionUuid);
        outState.putParcelable(ARG_BRANDING, mBranding);
        if (mAdapter != null) {
            mAdapter.clearState();
        }
        mPager.setAdapter(null);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        internalResume();
    }

    /**
     * Method for instantiating the {@link PageflipFragment}.
     * This will perform all needed actions in order to get the show started.
     */
    private void internalResume() {
        synchronized (this) {
            if (mPageflipStarted) {
                return;
            }
            mPageflipStarted = true;
        }
        ensureCatalog();
    }

    private void ensureCatalog() {

        if (mCatalog != null) {
            setBrandingAndFillCatalog();
            return;
        }

        Listener<JSONObject> l = new Listener<JSONObject>() {

            public void onComplete(JSONObject response, ShopGunError error) {

                if (!isAdded()) {
                    // Ignore callback
                    return;
                }

                if (response != null) {
                    setCatalog(Catalog.fromJSON(response));
                    setBrandingAndFillCatalog();
                } else {
                    mLoader.error();
                }

            }
        };

        String url = Endpoint.catalogId(mCatalogId);
        JsonObjectRequest r = new JsonObjectRequest(url, l);
        r.setIgnoreCache(true);
        ShopGun.getInstance().add(r);

    }

    private void setBrandingAndFillCatalog() {

        if (mCatalog != null) {
            setBranding(mCatalog.getBranding());
            mCatalogAutoFill = new CatalogAutoFill();
            mCatalogAutoFill.setLoadHotspots(!PageflipUtils.isHotspotsReady(mCatalog));
            mCatalogAutoFill.setLoadPages(!PageflipUtils.isPagesReady(mCatalog));
            mCatalogAutoFill.prepare(new AutoFillParams(), mCatalog, null, mCatalogListener);
            mCatalogAutoFill.execute(ShopGun.getInstance().getRequestQueue());
        }

    }

    @Override
    public void onPause() {
        internalPause();
        super.onPause();
    }

    private void internalPause() {
        mLoader.stop();
        mCatalogAutoFill.cancel();
        mPagesReady = false;
        mPageflipStarted = false;
    }

    public Catalog getCatalog() {
        return mCatalog;
    }

    private CatalogPageFragment getPage(int position) {
        return (CatalogPageFragment) mAdapter.instantiateItem(mContainer, position);
    }

    /**
     * Set the {@link Catalog} that you want to display.
     * This is unnecessary if you have created the fragment with one of the provided
     * {@link PageflipFragment} newInstance methods.
     *
     * @param c A catalog to display
     */
    public void setCatalog(Catalog c) {
        if (c != null) {
            mCatalog = c;
            mCatalogId = mCatalog.getId();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        int page = PageOverviewDialog.parseOnActivityResult(requestCode, resultCode, data);
        if (page != -1) {
            setPage(page);
        }
    }

    /**
     * This will display a DialogFragment, that lets the user choose a page to go to.
     * When an item has been selected, PageflipFragment, will automatically navigate to the
     * selected page.
     */
    public void showPageOverview() {
        if (isCatalogReady()) {
            int page = getPages()[0];
            PageOverviewDialog f = PageOverviewDialog.newInstance(PageflipFragment.this, mCatalog, page);
            f.show(getChildFragmentManager(), PageOverviewDialog.TAG);
        }
    }

    /**
     * A wrapper class for the users {@link PageflipListener}. Used to do some debugging.
     */
    protected class PageflipListenerWrapper implements PageflipListener {

        private static final boolean LOG = false;
        protected PageflipListener mListener;
        long s = -1;

        private boolean post() {
            return mListener != null;
        }

        public PageflipListener getListener() {
            return mListener;
        }

        public void setListener(PageflipListener l) {
            mListener = l;
        }

        public void onReady() {
            log("onReady");
            if (post()) mListener.onReady();
        }

        public void onPageChange(int[] pages) {
            log("onPageChange: " + PageflipUtils.join(",", pages));
            if (post()) mListener.onPageChange(pages);
        }

        public void onOutOfBounds(boolean left) {
            log("onOutOfBounds." + (left ? "left" : "right"));
            if (post()) mListener.onOutOfBounds(left);
        }

        public void onError(ShopGunError error) {
            error = (error == null) ? new ShopGunError(0, "Unknown Error", "No details available") : error;
            log("onError: " + error.toJSON().toString());
            if (post()) mListener.onError(error);
        }

        public void onDragStateChanged(int state) {
            log("onDragStateChanged: " + state);
            if (post()) mListener.onDragStateChanged(state);
        }

        public void onSingleClick(View v, int page, float x, float y, List<Hotspot> hotspots) {
            log("single", page, x, y, hotspots);
            if (post()) mListener.onSingleClick(v, page, x, y, hotspots);
        }

        public void onDoubleClick(View v, int page, float x, float y, List<Hotspot> hotspots) {
            log("double", page, x, y, hotspots);
            if (post()) mListener.onDoubleClick(v, page, x, y, hotspots);
        }

        public void onLongClick(View v, int page, float x, float y, List<Hotspot> hotspots) {
            log("long", page, x, y, hotspots);
            if (post()) mListener.onLongClick(v, page, x, y, hotspots);
        }

        public void onZoom(View v, int[] pages, boolean zoonIn) {
            log("onZoom.pages: " + PageflipUtils.join(",", pages) + ", zoomIn: " + zoonIn);
            if (post()) mListener.onZoom(v, pages, zoonIn);
        }

        private void log(String method, int page, float x, float y, List<Hotspot> hotspots) {
            StringBuilder sb = new StringBuilder();
            sb.append(method).append("[");
            sb.append("page").append(page);
//			sb.append(", x:").append(x).append(", y:").append(y);
            sb.append(", hotspot:");
            boolean first = true;
            for (Hotspot h : hotspots) {
                if (!first) {
                    sb.append(", ");
                }
                first = false;
                sb.append(h.getOffer().getHeading());
            }
            sb.append("]");
            String msg = sb.toString();
            log(msg);
            if (LOG) {
                Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
            }
        }

        private void log(String message) {
            if (LOG) {
                SgnLog.d(TAG, message);
            }
        }

    }

}
