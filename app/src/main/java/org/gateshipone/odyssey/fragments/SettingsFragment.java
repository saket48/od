/*
 * Copyright (C) 2018 Team Gateship-One
 * (Hendrik Borghorst & Frederik Luetkes)
 *
 * The AUTHORS.md file contains a detailed contributors list:
 * <https://github.com/gateship-one/odyssey/blob/master/AUTHORS.md>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.gateshipone.odyssey.fragments;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.audiofx.AudioEffect;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.activities.GenericActivity;
import org.gateshipone.odyssey.activities.OdysseyMainActivity;
import org.gateshipone.odyssey.dialogs.ErrorDialog;
import org.gateshipone.odyssey.listener.ToolbarAndFABCallback;
import org.gateshipone.odyssey.utils.FileExplorerHelper;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    /**
     * Callback for artwork request
     */
    private OnArtworkSettingsRequestedCallback mArtworkCallback;

    /**
     * Callback to setup toolbar and fab
     */
    private ToolbarAndFABCallback mToolbarAndFABCallback;

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    /**
     * Called to do initial creation of a fragment.
     * <p/>
     * This method will setup a listener to start the system audio equalizer.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // add listener to open equalizer
        Preference openEqualizer = findPreference(getString(R.string.pref_open_equalizer_key));
        openEqualizer.setOnPreferenceClickListener(preference -> {
            // Start the equalizer
            Intent startEqualizerIntent = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
            startEqualizerIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, requireContext().getPackageName());
            startEqualizerIntent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC);

            try {
                startEqualizerIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, ((GenericActivity) requireActivity()).getPlaybackService().getAudioSessionID());
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            try {
                requireActivity().startActivityForResult(startEqualizerIntent, 0);
            } catch (ActivityNotFoundException e) {
                ErrorDialog equalizerNotFoundDlg = ErrorDialog.newInstance(R.string.dialog_equalizer_not_found_title, R.string.dialog_equalizer_not_found_message);
                equalizerNotFoundDlg.show(requireActivity().getSupportFragmentManager(), "EqualizerNotFoundDialog");
            }

            return true;
        });

        // add listener to open artwork settings
        Preference openArtwork = findPreference(getString(R.string.pref_artwork_settings_key));
        openArtwork.setOnPreferenceClickListener(preference -> {
            mArtworkCallback.openArtworkSettings();
            return true;
        });

        // add listener to clear the default directory
        Preference clearDefaultDirectory = findPreference(getString(R.string.pref_clear_default_directory_key));
        clearDefaultDirectory.setOnPreferenceClickListener(preference -> {
            SharedPreferences.Editor sharedPrefEditor = android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences(requireContext()).edit();
            sharedPrefEditor.putString(getString(R.string.pref_file_browser_root_dir_key), FileExplorerHelper.getInstance().getStorageVolumes(requireContext()).get(0));
            sharedPrefEditor.apply();
            return true;
        });

    }

    /**
     * Called when the fragment is first attached to its context.
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mArtworkCallback = (OnArtworkSettingsRequestedCallback) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnArtworkSettingsRequestedCallback");
        }

        try {
            mToolbarAndFABCallback = (ToolbarAndFABCallback) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement ToolbarAndFABCallback");
        }
    }

    /**
     * Called when the fragment resumes.
     * <p/>
     * Register listener and setup the toolbar.
     */
    @Override
    public void onResume() {
        super.onResume();

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        if (mToolbarAndFABCallback != null) {
            // set toolbar behaviour and title
            mToolbarAndFABCallback.setupToolbar(getString(R.string.fragment_title_settings), false, true, false);
            // set up play button
            mToolbarAndFABCallback.setupFAB(null);
        }
    }

    /**
     * Called when the Fragment is no longer resumed.
     * <p/>
     * Unregister listener.
     */
    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * Create the preferences from an xml resource file
     */
    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.odyssey_main_settings);
        PreferenceManager.setDefaultValues(getActivity(), R.xml.odyssey_main_settings, false);
    }

    /**
     * Called when a shared preference is changed, added, or removed.
     * <p/>
     * This method will restart the activity if the theme was changed.
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_theme_key)) || key.equals(getString(R.string.pref_dark_theme_key))) {
            Intent intent = requireActivity().getIntent();
            intent.putExtra(OdysseyMainActivity.MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW, OdysseyMainActivity.REQUESTEDVIEW.SETTINGS.ordinal());
            requireActivity().finish();
            startActivity(intent);
        }
    }

    public interface OnArtworkSettingsRequestedCallback {
        void openArtworkSettings();
    }
}
