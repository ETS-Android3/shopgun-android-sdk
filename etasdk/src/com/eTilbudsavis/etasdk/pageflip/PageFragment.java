package com.eTilbudsavis.etasdk.pageflip;

import java.util.List;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.DecelerateInterpolator;

import com.eTilbudsavis.etasdk.Eta;
import com.eTilbudsavis.etasdk.R;
import com.eTilbudsavis.etasdk.imageloader.BitmapDisplayer;
import com.eTilbudsavis.etasdk.imageloader.BitmapProcessor;
import com.eTilbudsavis.etasdk.imageloader.ImageLoader;
import com.eTilbudsavis.etasdk.imageloader.ImageRequest;
import com.eTilbudsavis.etasdk.imageloader.LoadSource;
import com.eTilbudsavis.etasdk.log.EtaLog;
import com.eTilbudsavis.etasdk.model.Dimension;
import com.eTilbudsavis.etasdk.model.Hotspot;
import com.eTilbudsavis.etasdk.model.HotspotMap;
import com.eTilbudsavis.etasdk.model.Images;
import com.eTilbudsavis.etasdk.pageflip.utils.PageflipUtils;
import com.eTilbudsavis.etasdk.pageflip.widget.LoadingTextView;
import com.eTilbudsavis.etasdk.pageflip.widget.ZoomPhotoView;
import com.eTilbudsavis.etasdk.pageflip.widget.ZoomPhotoView.OnZoomChangeListener;

public abstract class PageFragment extends Fragment {
	
	public static final String TAG = Eta.TAG_PREFIX + PageFragment.class.getSimpleName();

	protected static final int FADE_IN_DURATION = 150;
	protected static final float MAX_SCALE = 3.0f;
	
	protected static final String ARG_PAGE = Eta.ARG_PREFIX + "pagefragment.page";
	protected static final String ARG_POSITION = Eta.ARG_PREFIX + "pagefragment.position";

	private static final Object HOTSPOT_LOCK = new Object();
	
	private int[] mPages;
	private ZoomPhotoView mPhotoView;
	private LoadingTextView mLoader;
	private PageCallback mCallback;
	private boolean mHasZoomImage = false;
	private boolean mDebugRects = false;
	private PageStat mStats;
	private boolean mPageVisible = false;
	private int mPosition = -1;
	
	OnZoomChangeListener mZoomListener = new OnZoomChangeListener() {
		
		public void onZoomChange(boolean isZoomed) {
			if (isZoomed && !mHasZoomImage) {
				mHasZoomImage = true;
				loadZoom();
			}
			
			if (isZoomed) {
				getStat().startZoom();
			} else {
				getStat().collectZoom();
			}
			
			getCallback().getWrapperListener().onZoom(mPhotoView, mPages, isZoomed);
		}
	};
	
	private void updateBranding() {
		PageCallback cb = getCallback();
		if (!isAdded() || cb==null || cb.getCatalog()==null) {
			return;
		}
		int brandingColor = getCallback().getCatalog().getBranding().getColor();
		int complimentColor = PageflipUtils.getTextColor(brandingColor, getActivity());
		mLoader.setTextColor(complimentColor);
	}
	
	private void runLoader() {
		updateBranding();
		mLoader.setLoadingText(PageflipUtils.join("-", mPages));
		mLoader.start();
	}
	
	public static PageFragment newInstance(int position, int[] pages) {
		Bundle b = new Bundle();
		b.putIntArray(ARG_PAGE, pages);
		b.putInt(ARG_POSITION, position);
		PageFragment f = pages.length == 1 ? new SinglePageFragment() : new DoublePageFragment();
		f.setArguments(b);
		return f;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (getArguments()!=null) {
			mPages = getArguments().getIntArray(ARG_PAGE);
			mPosition = getArguments().getInt(ARG_POSITION, 0);
		}
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		View v = inflater.inflate(R.layout.etasdk_layout_page, container, false);
		mPhotoView = (ZoomPhotoView) v.findViewById(R.id.etasdk_layout_page_photoview);
		mLoader = (LoadingTextView) v.findViewById(R.id.etasdk_layout_page_pagenum);
		
		mPhotoView.setMaximumScale(MAX_SCALE);
		mPhotoView.setOnZoomListener(mZoomListener);
		toggleContentVisibility(true);
		return v;
	}
	
	private void toggleContentVisibility(boolean isLoading) {
		int content = isLoading ? View.GONE : View.VISIBLE;
		int loader = isLoading ? View.VISIBLE : View.INVISIBLE;
		mPhotoView.setVisibility(content);
		mLoader.setVisibility(loader);
		if (isLoading) {
			runLoader();
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
//		outState.putIntArray(ARG_PAGE, mPages);
		super.onSaveInstanceState(outState);
	}
	
	private PageStat getStat() {
		if (mStats==null) {
//			EtaLog.d(TAG, "stat.ViewSession: " + mCallback.getViewSession());
			mStats = new PageStat(mCallback.getCatalog().getId(), mCallback.getViewSession(), mPages, land());
		}
		return mStats;
	}
	
	protected void addRequest(ImageRequest ir) {
		ir.setMemoryCache(false);
		ImageLoader.getInstance().displayImage(ir);
	}
	
	protected void onSingleClick(int page, float x, float y) {
		List<Hotspot> list = mCallback.getCatalog().getHotspots().getHotspots(page, x, y, mCallback.isLandscape());
		getCallback().getWrapperListener().onSingleClick(mPhotoView, page, x, y, list);
	}
	
	protected void onDoubleClick(int page, float x, float y) {
		List<Hotspot> list = mCallback.getCatalog().getHotspots().getHotspots(page, x, y, mCallback.isLandscape());
		getCallback().getWrapperListener().onDoubleClick(mPhotoView, page, x, y, list);
	}
	
	protected void onLongClick(int page, float x, float y) {
		List<Hotspot> list = mCallback.getCatalog().getHotspots().getHotspots(page, x, y, mCallback.isLandscape());
		getCallback().getWrapperListener().onLongClick(mPhotoView, page, x, y, list);
	}
	
	protected ZoomPhotoView getPhotoView() {
		return mPhotoView;
	}
	
	public void setPageCallback(PageCallback callback) {
		mCallback = callback;
	}
	
	public PageCallback getCallback() {
		return mCallback;
	}

	public int[] getPages() {
		return mPages;
	}
	
	protected Images getPage(int page) {
		// Offset the given page number by one. Real-world to array number
		return mCallback.getCatalog().getPages().get(page-1);
	}

	protected Images getFirst() {
		return getPage(getFirstNum());
	}

	protected Images getSecond() {
		return getPage(getSecondNum());
	}

	protected int getFirstNum() {
		return mPages[0];
	}
	
	protected boolean lowMem() {
		return mCallback.isLowMemory();
	}
	
	protected boolean land() {
		return mCallback.isLandscape();
	}
	
	protected int getSecondNum() {
		return mPages[1];
	}
	
	/**
	 * When called, start loading view images into the {@link ZoomPhotoView}.
	 */
	public abstract void loadView();

	/**
	 * When called, start loading zoom images into the {@link ZoomPhotoView}.
	 */
	public abstract void loadZoom();
	
	private void loadImage() {
		if ( mPhotoView!=null && !mPhotoView.isBitmapValid() && mCallback!=null && mCallback.isPositionSet() ) {
			loadView();
		}
	}
	
	@Override
	public void onResume() {
//		EtaLog.d(TAG, String.format("pos: %s, onResume", mPosition));
		updateBranding();
		
		if (getCallback()!=null) {
			getCallback().onReady(mPosition);
		}
		
		loadImage();
		super.onResume();
	}
	
	/**
	 * Tell if the fragment is current visible in the {@link PageflipViewPager}.
	 * @return true if visible, else false.
	 */
	public boolean isPageVisible() {
		return mPageVisible;
	}
	
	/**
	 * called once the {@link PageFragment} becomes visible in the {@link PageflipViewPager}
	 */
	public void onVisible() {
		updateBranding();
		loadImage();
		if (!mPageVisible) {
			if (mPhotoView != null && mPhotoView.getBitmap()!=null) {
				getStat().startView();
			}
			// TODO do performance stuff, low memory devices can start loading here instead of onResume
		}
		mPageVisible = true;
	}

	/**
	 * called once the {@link PageFragment} becomes invisible in the {@link PageflipViewPager}
	 */
	public void onInvisible() {
//		EtaLog.d(TAG, String.format("pos: %s, onInvisible, isAdded: %s", mPosition, isAdded()));
		if (mCallback!=null) {
			getStat().collectView();
		}
		mPageVisible = false;
	}
	
	@Override
	public void onPause() {
//		EtaLog.d(TAG, String.format("pos: %s, onPause", mPosition));
		mLoader.stop();
		mPhotoView.recycle();
		onInvisible();
		super.onPause();
	}
	
	public class PageBitmapProcessor implements BitmapProcessor {
		
		int page = 0;
		
		public PageBitmapProcessor(int page) {
			this.page = page;
		}
		
		private Dimension createDimension(Bitmap b) {
			Dimension d = new Dimension();
			double h = (double)((float)b.getHeight()/(float)b.getWidth());
			d.setHeight(h);
			return d;
		}
		
		public Bitmap process(Bitmap b) {
			
			HotspotMap m = getCallback().getCatalog().getHotspots(); 
			if (!m.isNormalized()) {
				synchronized (HOTSPOT_LOCK) {
					if (!m.isNormalized()) {
						Dimension d = createDimension(b);
						m.normalize(d);
					}
				}
			}
			
			if (mDebugRects) {
				try {
					return PageflipUtils.drawDebugRects(getCallback().getCatalog(), page, getCallback().isLandscape(), b);
				} catch (Exception e) {
					EtaLog.d(TAG, e.getMessage(), e);
				}
			}
			return b;
		}
		
	}
	
	/**
	 * A displayer to show images at the correct time in the {@link ZoomPhotoView}
	 */
	public class PageFadeBitmapDisplayer implements BitmapDisplayer {

		private boolean mFadeFromMemory = true;
		private boolean mFadeFromFile = false;
		private boolean mFadeFromWeb = false;
		
		public void display(ImageRequest ir) {
			
			if(ir.getBitmap() != null) {
				
				if (isPageVisible()) {
					getStat().startView();
				}
				mPhotoView.setImageBitmap(ir.getBitmap());
				toggleContentVisibility(false);
			} else {
				mLoader.error();
			}
			
//			} else if (ir.getPlaceholderError() != 0) {
//				mPhotoView.setImageResource(ir.getPlaceholderError());
//			}
			
			if ( (ir.getLoadSource() == LoadSource.WEB && mFadeFromWeb) ||
					(ir.getLoadSource() == LoadSource.FILE && mFadeFromFile) ||
					(ir.getLoadSource() == LoadSource.MEMORY && mFadeFromMemory) ) {

				AlphaAnimation fadeImage = new AlphaAnimation(0, 1);
				fadeImage.setDuration(FADE_IN_DURATION);
				fadeImage.setInterpolator(new DecelerateInterpolator());
				mPhotoView.startAnimation(fadeImage);
				
			}
		}

	}
	
}
