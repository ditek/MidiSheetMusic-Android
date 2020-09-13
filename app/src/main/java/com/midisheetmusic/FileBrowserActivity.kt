/*
 * Copyright (c) 2011-2012 Madhav Vaidyanathan
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 */
package com.midisheetmusic

import android.app.ListActivity
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.ListView
import android.widget.TextView
import java.io.File
import java.util.*

class FileBrowserActivity : ListActivity() {
    private val LOG_TAG = FileBrowserActivity::class.java.simpleName
    private var directory /* Current directory being displayed */: String? = null
    private var directoryView /* TextView showing directory name */: TextView? = null
    private var rootdir /* The top level root directory */: String? = null
    public override fun onCreate(state: Bundle) {
        super.onCreate(state)
        setContentView(R.layout.file_browser)
        title = "MidiSheetMusic: Browse Files"
    }

    public override fun onResume() {
        super.onResume()
        rootdir = Environment.getExternalStorageDirectory().absolutePath
        directoryView = findViewById(R.id.directory)
        val settings = getPreferences(0)
        var lastBrowsedDirectory = settings.getString("lastBrowsedDirectory", null)
        if (lastBrowsedDirectory == null) {
            lastBrowsedDirectory = rootdir
        }
        loadDirectory(lastBrowsedDirectory)
    }

    /* Scan the files in the new directory, and store them in the filelist.
     * Update the UI by refreshing the list adapter.
     */
    private fun loadDirectory(newdirectory: String?) {
        if (newdirectory == "../") {
            try {
                directory = File(directory).parent
            } catch (e: Exception) {
                Log.e(LOG_TAG, Thread.currentThread().stackTrace[2].methodName, e)
            }
        } else {
            directory = newdirectory
        }
        // Do not navigate to root directory
        if (directory == "/" || directory == "//") {
            return
        }
        val editor = getPreferences(0).edit()
        editor.putString("lastBrowsedDirectory", directory)
        editor.apply()
        directoryView!!.text = directory

        /* List of files in the directory */
        val filelist = ArrayList<FileUri>()
        val sortedDirs = ArrayList<FileUri>()
        val sortedFiles = ArrayList<FileUri>()
        val dir = File(directory)
        // If we're not at the root directory, add parent directory to the list
        if (dir.compareTo(File(rootdir)) != 0) {
            val parentDirectory = File(directory).parent + "/"
            val uri = Uri.parse("file://$parentDirectory")
            sortedDirs.add(FileUri(uri, "../"))
        }
        try {
            Log.e(LOG_TAG, "is root?: " + dir.compareTo(File(rootdir)))
            val files = dir.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file == null) {
                        continue
                    }
                    val filename = file.name
                    if (file.isDirectory) {
                        val uri = Uri.parse("file://" + file.absolutePath + "/")
                        val fileuri = FileUri(uri, file.name)
                        sortedDirs.add(fileuri)
                    } else if (filename.endsWith(".mid") || filename.endsWith(".MID") ||
                            filename.endsWith(".midi") || filename.endsWith(".MIDI")) {
                        val uri = Uri.parse("file://" + file.absolutePath)
                        val fileuri = FileUri(uri, uri.lastPathSegment)
                        sortedFiles.add(fileuri)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, Thread.currentThread().stackTrace[2].methodName, e)
        }
        if (sortedDirs.size > 0) {
            Collections.sort(sortedDirs, sortedDirs[0])
        }
        if (sortedFiles.size > 0) {
            Collections.sort(sortedFiles, sortedFiles[0])
        }
        filelist.addAll(sortedDirs)
        filelist.addAll(sortedFiles)
        val adapter = IconArrayAdapter(this, android.R.layout.simple_list_item_1, filelist)
        this.listAdapter = adapter
    }

    /** When a user selects an item:
     * - If it's a directory, load that directory.
     * - If it's a file, open the SheetMusicActivity.
     */
    override fun onListItemClick(parent: ListView, view: View, position: Int, id: Long) {
        super.onListItemClick(parent, view, position, id)
        val file = this.listAdapter.getItem(position) as FileUri
        if (file.isDirectory) {
            val path = file.uri.path
            if (path != null) {
                loadDirectory(path)
            }
        } else {
            ChooseSongActivity.openFile(file)
        }
    }

    fun onHomeClick(view: View?) {
        loadDirectory(rootdir)
    }
}