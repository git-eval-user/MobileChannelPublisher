/*
 * Copyright 2020 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.channelpublisher.common

import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.phenixrts.suite.channelpublisher.R
import com.phenixrts.suite.channelpublisher.common.enums.ExpressError
import kotlin.system.exitProcess

private fun AppCompatActivity.closeApp() {
    finishAffinity()
    finishAndRemoveTask()
    exitProcess(0)
}

private fun AppCompatActivity.getErrorMessage(error: ExpressError): String {
    return when (error) {
        ExpressError.DEEP_LINK_ERROR -> getString(R.string.err_invalid_deep_link)
        ExpressError.UNRECOVERABLE_ERROR -> getString(R.string.err_unrecoverable_error)
        ExpressError.STREAM_ERROR -> getString(R.string.err_stream_error)
        ExpressError.CONFIGURATION_CHANGED_ERROR -> getString(R.string.err_configuration_changed)
    }
}

fun AppCompatActivity.showErrorDialog(error: ExpressError) {
    AlertDialog.Builder(this, R.style.AlertDialogTheme)
        .setCancelable(false)
        .setMessage(getErrorMessage(error))
        .setPositiveButton(getString(R.string.popup_ok)) { dialog, _ ->
            dialog.dismiss()
            if (error != ExpressError.STREAM_ERROR) {
                closeApp()
            }
        }
        .create()
        .show()
}

fun Spinner.observableSelection(): MutableLiveData<Int> {
    val selection = MutableLiveData<Int>()
    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {
            /* Ignored */
        }

        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            launchMain {
                selection.value = position
            }
        }
    }
    return selection
}
