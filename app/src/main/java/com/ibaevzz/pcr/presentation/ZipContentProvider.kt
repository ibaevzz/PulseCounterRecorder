package com.ibaevzz.pcr.presentation

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.ibaevzz.pcr.ZIP
import net.lingala.zip4j.ZipFile

class ZipContentProvider : ContentProvider() {

    override fun getType(uri: Uri): String = "application/zip"

    override fun onCreate(): Boolean = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val type = uri.encodedPath
        if(type == "/${ZIP}") {
            val file = ZipFile(context?.filesDir?.path+"/$ZIP").file
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE)
        }
        return null
    }

    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor? = null
    override fun update(
        uri: Uri, values: ContentValues?, selection: String?,
        selectionArgs: Array<String>?
    ): Int = -1
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = -1
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

}