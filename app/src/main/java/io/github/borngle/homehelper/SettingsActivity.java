package io.github.borngle.homehelper;

import android.os.Bundle;
import android.text.InputType;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.appbar.MaterialToolbar;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.settings_activity);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        if(savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            EditTextPreference heatingLimit = findPreference("daily_heating_limit");
            if(heatingLimit != null) {
                heatingLimit.setOnBindEditTextListener(editText -> {
                    editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    editText.selectAll();
                });
                heatingLimit.setSummaryProvider(preference -> {
                    String value = ((EditTextPreference) preference).getText();
                    if(value == null || value.isEmpty() || value.equals("0")) {
                        return "No limit set";
                    }
                    return value + " hours";
                });
                heatingLimit.setOnPreferenceChangeListener((preference, newValue) -> {
                    try {
                        float value = Float.parseFloat((String) newValue);
                        return value >= 0 && value <= 24;
                    }
                    catch(NumberFormatException e) {
                        return false;
                    }
                });
            }
        }
    }
}