package org.videolan.vlc.gui.preferences.hack

import android.annotation.TargetApi
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.preference.DialogPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceDialogFragmentCompat
import java.util.*

class MultiSelectListPreferenceDialogFragmentCompat : PreferenceDialogFragmentCompat(), DialogPreference.TargetFragment {
    private val newValues = HashSet<String>()
    private var preferenceChanged: Boolean = false

    private val listPreference: MultiSelectListPreference
        get() = this.preference as MultiSelectListPreference

    private val selectedItems: BooleanArray
        get() {
            val preference = listPreference
            val entries = preference.entryValues
            val values = preference.values
            val result = BooleanArray(entries.size)

            for (i in entries.indices) {
                result[i] = values.contains(entries[i].toString())
            }

            return result
        }

    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder?) {
        super.onPrepareDialogBuilder(builder)
        val preference = listPreference
        if (preference.entries != null && preference.entryValues != null) {
            val checkedItems = selectedItems
            builder!!.setMultiChoiceItems(preference.entries, checkedItems) { dialog, which, isChecked ->
                preferenceChanged = true
                if (isChecked) {
                    newValues.add(preference.entryValues[which].toString())
                } else {
                    newValues.remove(preference.entryValues[which].toString())
                }
            }
            this.newValues.clear()
            this.newValues.addAll(preference.values)
        } else {
            throw IllegalStateException("MultiSelectListPreference requires an entries array and an entryValues array.")
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        val preference = listPreference
        if (positiveResult && preferenceChanged) {
            val values = newValues
            if (preference.callChangeListener(values)) {
                preference.values = values
            }
        }
        this.preferenceChanged = false
    }

    override fun <T : Preference?> findPreference(key: CharSequence): T? {
        return preference as? T
    }

    companion object {

        fun newInstance(key: String): MultiSelectListPreferenceDialogFragmentCompat {
            val fragment = MultiSelectListPreferenceDialogFragmentCompat()
            val b = Bundle(1)
            b.putString("key", key)
            fragment.arguments = b
            return fragment
        }
    }
}

