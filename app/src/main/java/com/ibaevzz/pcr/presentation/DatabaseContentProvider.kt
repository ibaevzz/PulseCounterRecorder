package com.ibaevzz.pcr.presentation

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.ibaevzz.pcr.DATABASE
import java.io.File

class DatabaseContentProvider : ContentProvider() {

    override fun getType(uri: Uri): String = "application/octet-stream"

    override fun onCreate(): Boolean = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val typeDb = uri.encodedPath
        var path = context?.getDatabasePath(DATABASE)?.parent?:""
        when (typeDb) {
            "/${DATABASE}.sqlite" -> {
                path += "/${DATABASE}"
            }
            "/${DATABASE}.sqlite-shm" -> {
                path += "/${DATABASE}-shm"
            }
            "/${DATABASE}.sqlite-wal" -> {
                path += "/${DATABASE}-wal"
            }
        }
        return ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_WRITE)
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