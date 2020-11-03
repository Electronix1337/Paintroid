package org.catrobat.paintroid.iotasks;

import android.graphics.Bitmap;

import java.util.List;

public class BitmapReturnValue {
	public List<Bitmap> bitmapList;
	public Bitmap bitmap;

	BitmapReturnValue(List<Bitmap> list, Bitmap singleBitmap) {
		bitmapList = list;
		bitmap = singleBitmap;
	}
}
