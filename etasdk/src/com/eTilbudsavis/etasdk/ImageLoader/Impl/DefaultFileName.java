package com.eTilbudsavis.etasdk.ImageLoader.Impl;

import java.net.URLEncoder;

import com.eTilbudsavis.etasdk.Eta;
import com.eTilbudsavis.etasdk.ImageLoader.FileNameGenerator;
import com.eTilbudsavis.etasdk.ImageLoader.ImageRequest;
import com.eTilbudsavis.etasdk.Utils.HashUtils;

public class DefaultFileName implements FileNameGenerator {
	
	public static final String TAG = Eta.TAG_PREFIX + DefaultFileName.class.getSimpleName();
	
	public String getFileName(ImageRequest ir) {
		return HashUtils.md5(ir.getUrl());
	}

	@SuppressWarnings("deprecation")
	private static String getName(String url) {
		return URLEncoder.encode(url);
	}
	
}
