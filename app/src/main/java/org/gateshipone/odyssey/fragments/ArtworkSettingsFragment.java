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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.artworkdatabase.ArtworkDatabaseManager;
import org.gateshipone.odyssey.artworkdatabase.ArtworkManager;
import org.gateshipone.odyssey.artworkdatabase.BulkDownloadService;
import org.gateshipone.odyssey.dialogs.BulkDownloaderDialog;
import org.gateshipone.odyssey.listener.ToolbarAndFABCallback;
import org.gateshipone.odyssey.playbackservice.PlaybackServiceConnection;

public class ArtworkSettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    /**
     * Callback to setup toolbar and fab
     */
    private ToolbarAndFABCallback mToolbarAndFABCallback;

    /**
     * Connection to the PBS to notify it about artwork hide changes
     */
    private PlaybackServiceConnection mServiceConnection = null;

    public static ArtworkSettingsFragment newInstance() {
        return new ArtworkSettingsFragment();
    }

    /**
     * Called to do initial creation of a fragment.
     * <p>
     * This method will setup a listener to start the system audio equalizer.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // add listener to clear album data
        Preference clearAlbums = findPreference(getString(R.string.pref_clear_album_key));
        clearAlbums.setOnPreferenceClickListener(preference -> {
            final Context applicationContext = requireContext().getApplicationContext();
            ArtworkDatabaseManager.getInstance(applicationContext).clearAlbumImages(applicationContext);
            return true;
        });

        // add listener to clear artist data
        Preference clearArtist = findPreference(getString(R.string.pref_clear_artist_key));
        clearArtist.setOnPreferenceClickListener(preference -> {
            final Context applicationContext = requireContext().getApplicationContext();
            ArtworkDatabaseManager.getInstance(applicationContext).clearArtistImages(applicationContext);
            return true;
        });

        Preference clearBlockedAlbums = findPreference(getString(R.string.pref_clear_blocked_album_key));
        clearBlockedAlbums.setOnPreferenceClickListener(preference -> {
            ArtworkDatabaseManager.getInstance(requireContext().getApplicationContext()).clearBlockedAlbumImages();
            return true;
        });

        Preference clearBlockedArtists = findPreference(getString(R.string.pref_clear_blocked_artist_key));
        clearBlockedArtists.setOnPreferenceClickListener(preference -> {
            ArtworkDatabaseManager.getInstance(requireContext().getApplicationContext()).clearBlockedArtistImages();
            return true;
        });

        Preference bulkLoad = findPreference(getString(R.string.pref_bulk_load_key));
        bulkLoad.setOnPreferenceClickListener(preference -> {
            BulkDownloaderDialog bulkDownloaderDialog = BulkDownloaderDialog.newInstance(R.string.bulk_download_notice_title, R.string.bulk_download_notice_text, R.string.error_dialog_ok_action);
            bulkDownloaderDialog.show(requireActivity().getSupportFragmentManager(), "BulkDownloaderDialog");

            return true;
        });


        // get the playbackservice, when the connection is successfully established the timer gets restarted
        mServiceConnection = new PlaybackServiceConnection(requireContext().getApplicationContext());
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

        mServiceConnection.openConnection();

        if (mToolbarAndFABCallback != null) {
            // set toolbar behaviour and title
            mToolbarAndFABCallback.setupToolbar(getString(R.string.fragment_title_settings), false, false, false);
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

        mServiceConnection.closeConnection();
    }

    /**
     * Create the preferences from an xml resource file
     */
    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.odyssey_artwork_settings);
        PreferenceManager.setDefaultValues(getActivity(), R.xml.odyssey_artwork_settings, false);
    }

    /**
     * Called when a shared preference is changed, added, or removed.
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        String albumProviderKey = getString(R.string.pref_album_provider_key);
        String artistProviderKey = getString(R.string.pref_artist_provider_key);
        String downloadWifiOnlyKey = getString(R.string.pref_download_wifi_only_key);

        if (key.equals(albumProviderKey) || key.equals(artistProviderKey) || key.equals(downloadWifiOnlyKey)) {
            Intent nextIntent = new Intent(BulkDownloadService.ACTION_CANCEL_BULKDOWNLOAD);
            requireContext().getApplicationContext().sendBroadcast(nextIntent);

            ArtworkManager artworkManager = ArtworkManager.getInstance(requireContext().getApplicationContext());

            artworkManager.cancelAllRequests(requireContext());

            if (key.equals(albumProviderKey)) {
                artworkManager.setAlbumProvider(sharedPreferences.getString(albumProviderKey, getString(R.string.pref_artwork_provider_album_default)));
            } else if (key.equals(artistProviderKey)) {
                artworkManager.setArtistProvider(sharedPreferences.getString(artistProviderKey, getString(R.string.pref_artwork_provider_artist_default)));
            } else if (key.equals(downloadWifiOnlyKey)) {
                artworkManager.setWifiOnly(sharedPreferences.getBoolean(downloadWifiOnlyKey, getResources().getBoolean(R.bool.pref_download_wifi_default)));
            }
        } else if (key.equals(getString(R.string.pref_hide_artwork_key))) {
            boolean hideArtwork = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.pref_hide_artwork_default));
            try {
                mServiceConnection.getPBS().hideArtworkChanged(hideArtwork);
            } catch (RemoteException e) {

            }
        }
    }

}
