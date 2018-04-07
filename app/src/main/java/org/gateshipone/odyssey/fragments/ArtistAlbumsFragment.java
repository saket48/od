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

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.activities.GenericActivity;
import org.gateshipone.odyssey.artworkdatabase.ArtworkManager;
import org.gateshipone.odyssey.loaders.AlbumLoader;
import org.gateshipone.odyssey.models.AlbumModel;
import org.gateshipone.odyssey.models.ArtistModel;
import org.gateshipone.odyssey.utils.CoverBitmapLoader;
import org.gateshipone.odyssey.utils.ThemeUtils;

import java.util.List;

public class ArtistAlbumsFragment extends GenericAlbumsFragment implements CoverBitmapLoader.CoverBitmapReceiver, ArtworkManager.onNewArtistImageListener {
    private static final String TAG = ArtistAlbumsFragment.class.getSimpleName();
    /**
     * {@link ArtistModel} to show albums for
     */
    private ArtistModel mArtist;

    /**
     * key values for arguments of the fragment
     */
    private final static String ARG_ARTISTMODEL = "artistmodel";

    private final static String ARG_BITMAP = "bitmap";

    private CoverBitmapLoader mBitmapLoader;

    private Bitmap mBitmap;

    private boolean mHideArtwork;

    public static ArtistAlbumsFragment newInstance(@NonNull final ArtistModel artistModel, @Nullable final Bitmap bitmap) {
        final Bundle args = new Bundle();
        args.putParcelable(ARG_ARTISTMODEL, artistModel);
        if (bitmap != null) {
            args.putParcelable(ARG_BITMAP, bitmap);
        }

        final ArtistAlbumsFragment fragment = new ArtistAlbumsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Called to create instantiate the UI of the fragment.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = super.onCreateView(inflater, container, savedInstanceState);

        // read arguments
        Bundle args = getArguments();
        mArtist = args.getParcelable(ARG_ARTISTMODEL);
        mBitmap = args.getParcelable(ARG_BITMAP);

        setHasOptionsMenu(true);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        mHideArtwork = sharedPreferences.getBoolean(requireContext().getString(R.string.pref_hide_artwork_key), requireContext().getResources().getBoolean(R.bool.pref_hide_artwork_default));

        mBitmapLoader = new CoverBitmapLoader(requireContext(), this);

        return rootView;
    }

    /**
     * Called when the fragment resumes.
     * <p/>
     * Set up toolbar and play button.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (mToolbarAndFABCallback != null) {
            // set up play button
            mToolbarAndFABCallback.setupFAB(v -> playArtist());
        }

        // set toolbar behaviour and title
        if (!mHideArtwork && mBitmap == null) {
            mToolbarAndFABCallback.setupToolbar(mArtist.getArtistName(), false, false, false);
            final View rootView = getView();
            if (rootView != null) {
                getView().post(() -> {
                    int width = rootView.getWidth();
                    mBitmapLoader.getArtistImage(mArtist, width, width);
                });
            }

        } else if (!mHideArtwork) {
            mToolbarAndFABCallback.setupToolbar(mArtist.getArtistName(), false, false, true);
            mToolbarAndFABCallback.setupToolbarImage(mBitmap);
            final View rootView = getView();
            if (rootView != null) {
                getView().post(() -> {
                    int width = rootView.getWidth();

                    // Image too small
                    if (mBitmap.getWidth() < width) {
                        mBitmapLoader.getArtistImage(mArtist, width, width);
                    }
                });
            }
        } else {
            mToolbarAndFABCallback.setupToolbar(mArtist.getArtistName(), false, false, false);
        }

        ArtworkManager.getInstance(requireContext().getApplicationContext()).registerOnNewArtistImageListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        ArtworkManager.getInstance(requireContext().getApplicationContext()).unregisterOnNewArtistImageListener(this);
    }

    /**
     * This method creates a new loader for this fragment.
     *
     * @param id     The id of the loader
     * @param bundle Optional arguments
     * @return Return a new Loader instance that is ready to start loading.
     */
    @NonNull
    @Override
    public Loader<List<AlbumModel>> onCreateLoader(int id, Bundle bundle) {
        return new AlbumLoader(getActivity(), mArtist.getArtistID());
    }

    /**
     * Create the context menu.
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = requireActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_artist_albums_fragment, menu);
    }

    /**
     * Hook called when an menu item in the context menu is selected.
     *
     * @param item The menu item that was selected.
     * @return True if the hook was consumed here.
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        if (info == null) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
            case R.id.fragment_artist_albums_action_enqueue:
                enqueueAlbum(info.position);
                return true;
            case R.id.fragment_artist_albums_action_play:
                playAlbum(info.position);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * Initialize the options menu.
     * Be sure to call {@link #setHasOptionsMenu} before.
     *
     * @param menu         The container for the custom options menu.
     * @param menuInflater The inflater to instantiate the layout.
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.options_menu_artist_albums_fragment, menu);

        // get tint color
        int tintColor = ThemeUtils.getThemeColor(requireContext(), R.attr.odyssey_color_text_accent);

        Drawable drawable = menu.findItem(R.id.action_add_artist_albums).getIcon();
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, tintColor);
        menu.findItem(R.id.action_add_artist_albums).setIcon(drawable);

        super.onCreateOptionsMenu(menu, menuInflater);
    }

    /**
     * Hook called when an menu item in the options menu is selected.
     *
     * @param item The menu item that was selected.
     * @return True if the hook was consumed here.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_reset_artwork:
                mToolbarAndFABCallback.setupToolbar(mArtist.getArtistName(), false, false, false);
                ArtworkManager.getInstance(requireContext().getApplicationContext()).resetArtistImage(mArtist, requireContext().getApplicationContext());
                return true;
            case R.id.action_add_artist_albums:
                enqueueArtist();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        getArguments().remove(ARG_BITMAP);
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Call the PBS to enqueue artist.
     */
    private void enqueueArtist() {
        // Read order preference
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String orderKey = sharedPref.getString(getString(R.string.pref_album_sort_order_key), getString(R.string.pref_artist_albums_sort_default));

        // enqueue artist
        try {
            ((GenericActivity) requireActivity()).getPlaybackService().enqueueArtist(mArtist.getArtistID(), orderKey);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Call the PBS to play artist.
     * A previous playlist will be cleared.
     */
    private void playArtist() {
        // Read order preference
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String orderKey = sharedPref.getString(getString(R.string.pref_album_sort_order_key), getString(R.string.pref_artist_albums_sort_default));

        // play artist
        try {
            ((GenericActivity) requireActivity()).getPlaybackService().playArtist(mArtist.getArtistID(), orderKey);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void receiveArtistBitmap(final Bitmap bm) {
        if (bm != null && mToolbarAndFABCallback != null) {
            requireActivity().runOnUiThread(() -> {
                // set toolbar behaviour and title
                mToolbarAndFABCallback.setupToolbar(mArtist.getArtistName(), false, false, true);
                // set toolbar image
                mToolbarAndFABCallback.setupToolbarImage(bm);
                getArguments().putParcelable(ARG_BITMAP, bm);
            });
        }
    }

    @Override
    public void receiveAlbumBitmap(final Bitmap bm) {

    }

    @Override
    public void newArtistImage(ArtistModel artist) {
        if (artist.equals(mArtist)) {
            if (!mHideArtwork) {
                int width = getView().getWidth();
                mBitmapLoader.getArtistImage(mArtist, width, width);
            }
        }
    }
}
