/**
 * @fileoverview	API.
 * @author			Danny Hvam <danny@etilbudsavis.dk>
 */
package com.eTilbudsavis.etasdk;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.os.Bundle;
import android.text.TextUtils;

import com.eTilbudsavis.etasdk.EtaObjects.Catalog;
import com.eTilbudsavis.etasdk.EtaObjects.Dealer;
import com.eTilbudsavis.etasdk.EtaObjects.Offer;
import com.eTilbudsavis.etasdk.EtaObjects.Store;

public class Api implements Serializable {
	
	private static final long serialVersionUID = 1L;

	public static final String TAG = "API";

	/** String identifying the order by parameter for all list calls to the API */
	public static final String ORDER_BY = "order_by";
	
	public static final String API_KEY = "api_key";
	
	public static final String MAIN_URL = "https://etilbudsavis.dk";

	public static final String PROVIDER_URL = MAIN_URL + "/connect/";

	public static final String HEADER_X_TOKEN = "X-Token";
	
	public static final String HEADER_X_SIGNATURE = "X-Signature";
	
	/** String identifying the offset parameter for all list calls to the API */
	public static final String OFFSET = "offset";
	
	/** String identifying the offset parameter for all list calls to the API */
	public static final String LIMIT = "offset";
	
	/** The default page limit for API calls */
	public static final int OFFSET_DEFAULT = 0;

	/** The default page limit for API calls */
	public static final int LIMIT_DEFAULT = 25;
	
	private Eta mEta;
	
	private HttpHelper httpHelper;
	
	private String mUrl = null;
	
	private RequestListener mListener = null;
	
	private Bundle mOptionalKeys = null;

	private RequestType mRequestType = null;

	private Bundle mHeaders = new Bundle();
	
	private boolean useLocation = true;
	
	private String[] mOrderBy = null;
	
	/**
	 * TODO: Write proper JavaDoc<br>
	 * <code>new String[] {Api.SORT_DISTANCE, Api.SORT_PUBLISHED}</code>
	 * @param order
	 * @return
	 */
	public Api setOrderBy(String[] order) {
		mOrderBy = order;
		return this;
	}
	
	public String[] getOrderBy() {
		return mOrderBy;
	}
	
	public Api setHeader(String name, String value) {
		mHeaders.putString(name, value);
		return this;
	}
	
	public Bundle getHeaders() {
		return mHeaders;
	}
	
	/**
	 * The type expected to return;
	 * Default is JSON, other types are not implemented yet
	 */
	public enum AcceptType {
		XML { public String toString() { return "application/xml, text/xml"; } },
		CSV { public String toString() { return "application/csv"; } },
		JSON { public String toString() { return "application/json"; } }
	}
	
	public enum RequestType {
		HEAD, POST, GET, PUT, DELETE, OPTIONS
	}
	
	/**
	 * The content type to use in requests. This feature is not implemented yet.
	 */
	public enum ContentType {
		JSON { public String toString() { return "application/json; charset=utf-8"; } },
		URLENCODED { public String toString() { return "application/x-www-form-urlencoded; charset=utf-8"; } },
		FORMDATA { public String toString() { return "multipart/form-data; charset=utf-8"; } }
	}

	/**
	 * Tell, whether to use location for this API call
	 * @param useLocation
	 * @return
	 */
	public Api setUseLocation(boolean useLocation) {
		this.useLocation = useLocation;
		return this;
	}

	/**
	 * Determine whether location is being used, for this API call
	 * @return True if location is being used, false otherwise.
	 */
	public boolean useLocation() {
		return useLocation;
	}
	
	/**
	 * Default constructor for API
	 * @param Eta object with relevant information e.g. location
	 */
	public Api(Eta eta) {
		mEta = eta;
	}
	
	/**
	 * Makes a request to the server, with the given parameters.
	 * The result will return via the RequestListener.
	 *
	 * @param url - This can be any URL, but optionalKeys are only sent if the URL points to the ETA API
	 * @param requestListener - API.RequestListener
	 */
	public Api request(String url, RequestListener requestListener) {
		return request(url, requestListener, new Bundle());
	}

	/**
	 * Makes a request to the server, with the given parameters.
	 * The result will return via the RequestListener.
	 *
	 * @param url - This can be any URL, but optionalKeys are only sent if the URL points to the ETA API
	 * @param requestListener - API.RequestListener
	 * @param optionalKeys - Bundle containing parameters specified on https://etilbudsavis.dk/developers/docs/
	 */
	public Api request(String url, RequestListener requestListener, Bundle optionalKeys) {
		return request(url, requestListener, optionalKeys, Api.RequestType.GET);
	}

	/**
	 * Make a request to the server, with the given parameters.
	 * The result will return via the RequestListener.
	 *
	 * @param url - This can be any URL, but optionalKeys are only sent if the URL points to the ETA API
	 * @param requestListener - API.RequestListener
	 * @param optionalKeys - Bundle containing parameters specified on https://etilbudsavis.dk/developers/docs/
	 * @param requestType - API.RequestType
	 */
	public Api request(String url, RequestListener requestListener, Bundle optionalKeys, RequestType requestType) {
		request(url, requestListener, optionalKeys, requestType, new Bundle());
		return this;
	}

	/**
	 * Make a request to the server, with the given parameters.
	 * The result will return via the RequestListener.
	 *
	 * @param url - This can be any URL, but optionalKeys are only sent if the URL points to the ETA API
	 * @param requestListener - API.RequestListener
	 * @param optionalKeys - Bundle containing parameters specified on https://etilbudsavis.dk/developers/docs/
	 * @param requestType - API.RequestType
	 */
	public Api request(String url, RequestListener requestListener, Bundle optionalKeys, RequestType requestType, Bundle headers) {
		if (url == null || requestListener == null || optionalKeys == null || requestType == null || headers == null) {
			Utilities.logd(TAG, "Api parameters cannot be null");
			return null;
		} 
		mUrl = url;
		mListener = requestListener;
		mOptionalKeys = optionalKeys;
		mRequestType = requestType;
		mHeaders = headers;
		return this;
	}

	/**
	 * This will start executing the request.
	 * Note that if the <b>optionalKeys</b> bundle contains options that have also been set by
	 * any of the Api-setters, then the setters will be used.
	 * @return HttpHelper, så execution of background task can be cancelled.
	 */
	public HttpHelper execute() {

		if (mUrl == null || mListener == null || mOptionalKeys == null || mRequestType == null || mHeaders == null) {
			Utilities.logd(TAG, "A request must be made before execute");
			return null;
		}
		
		// Prepare data.
		List<NameValuePair> params = new ArrayList<NameValuePair>();

		// Add optional data.
		if (!mOptionalKeys.isEmpty()) {
			Iterator<String> iterator = mOptionalKeys.keySet().iterator();
			while (iterator.hasNext()) {
				String s = iterator.next();
				Utilities.putNameValuePair(params, s, mOptionalKeys.get(s));
			}
		}

		// Required API key.
		Utilities.putNameValuePair(params, API_KEY, mEta.getApiKey());

		if (useLocation) {

			EtaLocation l = mEta.getLocation();
			Utilities.putNameValuePair(params, EtaLocation.LATITUDE, l.getLatitude());
			Utilities.putNameValuePair(params, EtaLocation.LONGITUDE, l.getLongitude());
			Utilities.putNameValuePair(params, EtaLocation.SENSOR, l.getSensor());
			Utilities.putNameValuePair(params, EtaLocation.RADIUS, l.getRadius());

			// Determine whether to include bounds.
			if (mEta.getLocation().isBoundsSet()) {
				Utilities.putNameValuePair(params, EtaLocation.BOUND_EAST, l.getBoundEast());
				Utilities.putNameValuePair(params, EtaLocation.BOUND_NORTH, l.getBoundNorth());
				Utilities.putNameValuePair(params, EtaLocation.BOUND_SOUTH, l.getBoundSouth());
				Utilities.putNameValuePair(params, EtaLocation.BOUND_WEST, l.getBoundWest());
			}

		}

		if (mOrderBy != null) {
			Utilities.putNameValuePair(params, ORDER_BY, TextUtils.join(",", mOrderBy));
		}

		// Prefix URL?
		if (!mUrl.matches("^http.*"))
			mUrl = MAIN_URL + mUrl;
		
		// Set headers if session is OK
		if (mEta.getSession().getToken() != null) {
			mHeaders.putString(HEADER_X_TOKEN, mEta.getSession().getToken());
			String sha256 = Utilities.generateSHA256(mEta.getApiKey() + mEta.getSession().getToken());
			mHeaders.putString(HEADER_X_SIGNATURE, sha256);
		}

		// Create a new HttpHelper.
		httpHelper = new HttpHelper(mUrl, params, mHeaders, mRequestType, mListener);

		// Execute the AsyncTask in HttpHelper to ensure a new thread.
		httpHelper.execute();

		return httpHelper;
	}
	
    /** Callback interface for API requests */
    public static interface RequestListener {
        public void onComplete(int responseCode, Object object);
    }

    /** Callback interface for catalogs requests */
    public static interface CatalogsListener extends RequestListener {
        public void onComplete(int responseCode, ArrayList<Catalog> catalogs);
    }

    /** Callback interface for catalogs requests */
    public static interface OffersListener extends RequestListener {
        public void onComplete(int responseCode, ArrayList<Offer> offers);
    }

    /** Callback interface for catalogs requests */
    public static interface DealersListener extends RequestListener {
        public void onComplete(int responseCode, ArrayList<Dealer> dealers);
    }

    /** Callback interface for catalogs requests */
    public static interface StoresListener extends RequestListener {
        public void onComplete(int responseCode, ArrayList<Store> stores);
    }
    
}