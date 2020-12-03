/*
 * Paintroid: An image manipulation application for Android.
 * Copyright (C) 2010-2015 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.catrobat.paintroid.iotasks;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import org.catrobat.paintroid.common.Constants;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

//
//example.ora  [considered as a folder-like object]
//        ├ mimetype
//        ├ stack.xml
//        ├ data/
//        │  ├ [image data files referenced by stack.xml, typically layer*.png]
//        │  └ [other data files, indexed elsewhere]
//        ├ Thumbnails/
//        │  └ thumbnail.png
//        └ mergedimage.png
public class OpenRasterFileFormatConversion {
    private static final String TAG = OpenRasterFileFormatConversion.class.getSimpleName();


    public static Uri exportToOraFile(List<Bitmap> bitmapList, String fileName, Bitmap bitmapAllLayers, ContentResolver resolver) throws IOException {

        Uri imageUri = null;
        OutputStream outputStream;
        ContentValues contentValues = new ContentValues();
        float wholeSize = 0;
        File file = new File(Constants.MEDIA_DIRECTORY, fileName);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
            contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/openraster");

            imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            outputStream = resolver.openOutputStream(Objects.requireNonNull(imageUri));
        } else {

            outputStream = new FileOutputStream(file);

            contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/openraster");

            long date = System.currentTimeMillis();
            contentValues.put(MediaStore.MediaColumns.DATE_MODIFIED, date / 1000);
        }

        ZipOutputStream streamZip = new ZipOutputStream(outputStream);
        ZipEntry mimetypeEntry = new ZipEntry("mimetype");
        mimetypeEntry.setMethod(ZipEntry.DEFLATED);
        streamZip.putNextEntry(mimetypeEntry);

        streamZip.putNextEntry(new ZipEntry("stack.xml"));
        byte[] xmlByteArray = getXmlStack(bitmapList);
        streamZip.write(xmlByteArray, 0, xmlByteArray.length);


        String mimetype = "image/openraster";
        byte[] mimeByteArray = mimetype.getBytes(); //check
        streamZip.write(mimeByteArray, 0, mimeByteArray.length);

        int counter = 0;
        for(Bitmap current: bitmapList) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            current.compress(Bitmap.CompressFormat.PNG, 100, bos);
            byte[] byteArray = bos.toByteArray();

            streamZip.putNextEntry(new ZipEntry("data/layer" + counter + ".png"));
            wholeSize += byteArray.length;
            streamZip.write(byteArray, 0, byteArray.length);

            counter++;
        }

        streamZip.putNextEntry(new ZipEntry("Thumbnails/thumbnail.png"));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmapAllLayers.compress(Bitmap.CompressFormat.PNG, 100, bos);
        byte[] bitmapByteArray = bos.toByteArray();

        Bitmap bitmapthumb = Bitmap.createScaledBitmap(bitmapAllLayers, 256, 256, false);
        //check

        ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
        bitmapthumb.compress(Bitmap.CompressFormat.PNG, 100, bos2);
        byte[] bitmapThumbArray = bos2.toByteArray();

        streamZip.write(bitmapThumbArray, 0, bitmapThumbArray.length);

        streamZip.putNextEntry(new ZipEntry("mergedimage.png"));
        streamZip.write(bitmapByteArray, 0, bitmapByteArray.length);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            wholeSize += xmlByteArray.length;
            wholeSize += mimeByteArray.length;
            wholeSize += bitmapByteArray.length;
            wholeSize += bitmapThumbArray.length;
            contentValues.put(MediaStore.Images.Media.SIZE, wholeSize);
            imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            imageUri = Uri.fromFile(file);
            outputStream.close();

            return imageUri;
        } else {
            outputStream.close();
            return imageUri;
        }
    }

    private static byte[] getXmlStack(List<Bitmap> bitmapList) {

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("image");
            Attr attr1 = doc.createAttribute("version");
            Attr attr2 = doc.createAttribute("w");
            Attr attr3 = doc.createAttribute("h");

            attr1.setValue("0.0.1");
            attr2.setValue(String.valueOf(bitmapList.get(0).getWidth()));
            attr3.setValue(String.valueOf(bitmapList.get(0).getHeight()));

            rootElement.setAttributeNode(attr1);
            rootElement.setAttributeNode(attr2);
            rootElement.setAttributeNode(attr3);
            doc.appendChild(rootElement);

            Element stack = doc.createElement("stack");
            rootElement.appendChild(stack);

            for(int i = bitmapList.size()-1; i >= 0; i--) {
                Element layer = doc.createElement("layer");
//                Attr attr4 = doc.createAttribute("name");
//                Attr attr5 = doc.createAttribute("src");
//                layer.setAttributeNode(attr4);
//                layer.setAttributeNode(attr5);

                layer.setAttribute("name", "layer" + i);
                layer.setAttribute("src", "data/layer" + i + ".png");
                stack.appendChild(layer);
            }


            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            StreamResult result= new StreamResult(stream);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);

            transformer.transform(source, result);

            return stream.toByteArray();
        } catch (ParserConfigurationException e) {
            Log.e(TAG, "Could not create document.");
            return null;
        } catch (TransformerException e) {
            Log.e(TAG, "Could not transform Xml file.");
            return null;
        }
    }

    public static void importOraFile() {

    }

}
