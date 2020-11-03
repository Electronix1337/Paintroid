package org.catrobat.paintroid.iotasks;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CatrobatImage implements Serializable {
	public List<String> bitmapStringList = new ArrayList<>();

	public CatrobatImage(List<Bitmap> bitmapList) {
		for (Bitmap bitmap : bitmapList) {
			bitmapStringList.add(bitMapToString(bitmap));
		}
	}

	private String bitMapToString(Bitmap bitmap) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
		byte[] b = baos.toByteArray();
		return Base64.encodeToString(b, Base64.DEFAULT);
	}

	private Bitmap stringToBitMap(String encodedString) {
		try {
			byte[] encodeByte = Base64.decode(encodedString, Base64.DEFAULT);
			return BitmapFactory.decodeByteArray(encodeByte, 0,
					encodeByte.length);
		} catch (Exception e) {
			e.getMessage();
			return null;
		}
	}

	public List<Bitmap> parseStringListToBitmapList() {
		List<Bitmap> bitmapList = new ArrayList<>();

		for (String current : bitmapStringList) {
			bitmapList.add(stringToBitMap(current));
		}

		return bitmapList;
	}

	public String buildJsonString() {
		Moshi moshi = new Moshi.Builder().build();
		JsonAdapter<CatrobatImage> jsonAdapter = moshi.adapter(CatrobatImage.class);
		String json = jsonAdapter.toJson(this);

		return json;
	}

	public static CatrobatImage convertToCatrobatImageObjectFromJson(String jsonString) throws IOException {
		Moshi moshi = new Moshi.Builder().build();
		JsonAdapter<CatrobatImage> jsonAdapter = moshi.adapter(CatrobatImage.class);

		return jsonAdapter.fromJson(jsonString);
	}
}
