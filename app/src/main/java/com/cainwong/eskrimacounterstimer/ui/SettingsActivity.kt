package com.cainwong.eskrimacounterstimer.ui

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.MultiSelectListPreference
import androidx.preference.PreferenceFragmentCompat
import com.cainwong.eskrimacounterstimer.R
class SettingsActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.main_content, SettingsFragment())
            .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (android.R.id.home == item?.itemId) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}

class MultiSelectListPreferenceWithSummaryValues(context: Context?,
                                                 attrs: AttributeSet? = null,
                                                 defStyleAttr: Int) :
    MultiSelectListPreference(context, attrs) {

    override fun getSummary(): CharSequence {
        val cs = super.getSummary()
        var summary = cs.toString()

        if (summary.contains("%s")) {
            var text = ""
            val builder = StringBuilder()
            val entries = entries
            if (entries.size > 0) {
                val entryValues = entryValues
                val values = values
                var pos = 0

                for (value in values) {
                    pos++
                    var index = -1
                    for (i in entryValues.indices) {
                        if (entryValues[i] == value) {
                            index = i
                            break
                        }
                    }
                    builder.append(entries[index])
                    if (pos < values.size)
                        builder.append(", ")
                }
                text = builder.toString()
            }
            summary = String.format(summary, text)
        }

        return summary
    }
}