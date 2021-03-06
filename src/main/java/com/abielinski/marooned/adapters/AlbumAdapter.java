/*******************************************************************************
 * This file is part of Marooned.
 *
 * Marooned is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Marooned is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Marooned.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package com.abielinski.marooned.adapters;

import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.abielinski.marooned.R;
import com.abielinski.marooned.account.RedditAccountManager;
import com.abielinski.marooned.activities.BaseActivity;
import com.abielinski.marooned.cache.CacheManager;
import com.abielinski.marooned.cache.CacheRequest;
import com.abielinski.marooned.cache.downloadstrategy.DownloadStrategyIfNotCached;
import com.abielinski.marooned.common.AndroidCommon;
import com.abielinski.marooned.common.Constants;
import com.abielinski.marooned.common.General;
import com.abielinski.marooned.common.LinkHandler;
import com.abielinski.marooned.common.PrefsUtility;
import com.abielinski.marooned.common.RRError;
import com.abielinski.marooned.image.AlbumInfo;
import com.abielinski.marooned.image.ImageInfo;
import com.abielinski.marooned.viewholders.VH3TextIcon;

import java.util.Locale;
import java.util.UUID;

public class AlbumAdapter extends RecyclerView.Adapter<VH3TextIcon> {

	private final BaseActivity activity;
	private final AlbumInfo albumInfo;

	public AlbumAdapter(final BaseActivity activity, final AlbumInfo albumInfo) {
		this.activity = activity;
		this.albumInfo = albumInfo;
	}

	@NonNull
	@Override
	public VH3TextIcon onCreateViewHolder(final ViewGroup parent, final int viewType) {
		final View v = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.list_item_3_text_icon, parent, false);
		return new VH3TextIcon(v);
	}

	@Override
	public void onBindViewHolder(final VH3TextIcon vh, final int position) {

		final long bindingId = ++vh.bindingId;

		final ImageInfo imageInfo = albumInfo.images.get(position);

		if(imageInfo.title == null || imageInfo.title.trim().isEmpty()) {
			vh.text.setText("Image " + (position + 1));
		} else {
			vh.text.setText((position + 1) + ". " + imageInfo.title.trim());
		}

		String subtitle = "";

		if(imageInfo.type != null) {
			subtitle += imageInfo.type;
		}

		if(imageInfo.width != null && imageInfo.height != null) {
			if(!subtitle.isEmpty()) {
				subtitle += ", ";
			}
			subtitle += imageInfo.width + "x" + imageInfo.height;
		}

		if(imageInfo.size != null) {
			if(!subtitle.isEmpty()) {
				subtitle += ", ";
			}

			final long size = imageInfo.size;
			if(size < 512 * 1024) {
				subtitle += String.format(Locale.US, "%.1f kB", (float)size / 1024);
			} else {
				subtitle += String.format(
						Locale.US,
						"%.1f MB",
						(float)size / (1024 * 1024));
			}
		}


		vh.text2.setVisibility(subtitle.isEmpty() ? View.GONE : View.VISIBLE);

		vh.text2.setText(subtitle);

		if(imageInfo.caption != null && imageInfo.caption.length() > 0) {
			vh.text3.setText(imageInfo.caption);
			vh.text3.setVisibility(View.VISIBLE);
		} else {
			vh.text3.setVisibility(View.GONE);
		}

		vh.icon.setImageBitmap(null);

		final boolean isConnectionWifi = General.isConnectionWifi(activity);

		final PrefsUtility.AppearanceThumbnailsShow thumbnailsPref
				= PrefsUtility.appearance_thumbnails_show(
				activity,
				General.getSharedPrefs(activity));

		final boolean downloadThumbnails = thumbnailsPref
				== PrefsUtility.AppearanceThumbnailsShow.ALWAYS
				|| (thumbnailsPref
				== PrefsUtility.AppearanceThumbnailsShow.WIFIONLY
				&& isConnectionWifi);

		if(!downloadThumbnails || imageInfo.urlBigSquare == null) {
			vh.icon.setVisibility(View.GONE);

		} else {
			vh.text2.setVisibility(View.VISIBLE);

			CacheManager.getInstance(activity).makeRequest(new CacheRequest(
					General.uriFromString(imageInfo.urlBigSquare),
					RedditAccountManager.getAnon(),
					null,
					Constants.Priority.THUMBNAIL,
					position,
					DownloadStrategyIfNotCached.INSTANCE,
					Constants.FileType.THUMBNAIL,
					CacheRequest.DOWNLOAD_QUEUE_IMMEDIATE,
					false,
					false,
					activity
			) {
				@Override
				protected void onCallbackException(final Throwable t) {
					Log.e("AlbumAdapter", "Error in album thumbnail fetch callback", t);
				}

				@Override
				protected void onDownloadNecessary() {
				}

				@Override
				protected void onDownloadStarted() {
				}

				@Override
				protected void onFailure(
						final @CacheRequest.RequestFailureType int type,
						final Throwable t,
						final Integer status,
						final String readableMessage) {
					Log.e("AlbumAdapter", "Failed to fetch thumbnail " + url.toString());
				}

				@Override
				protected void onProgress(
						final boolean authorizationInProgress,
						final long bytesRead,
						final long totalBytes) {
				}

				@Override
				protected void onSuccess(
						final CacheManager.ReadableCacheFile cacheFile,
						final long timestamp,
						final UUID session,
						final boolean fromCache,
						final String mimetype) {

					final Uri uri = cacheFile.getUri();

					AndroidCommon.UI_THREAD_HANDLER.post(() -> {
						if(vh.bindingId == bindingId) {
							vh.icon.setImageURI(uri);
						}
					});
				}
			});
		}

		if(imageInfo.urlOriginal != null) {
			vh.itemView.setOnClickListener(v -> LinkHandler.onLinkClicked(
					activity,
					imageInfo.urlOriginal,
					false,
					null,
					albumInfo,
					vh.getAdapterPosition()));

		} else {
			vh.itemView.setOnClickListener(v -> General.showResultDialog(
					activity,
					new RRError(
							activity.getString(R.string.image_gallery_no_image_present_title),
							activity.getString(R.string.image_gallery_no_image_present_message),
							new RuntimeException(),
							null,
							albumInfo.url)));
		}

		vh.itemView.setOnLongClickListener(v -> {
			LinkHandler.onLinkLongClicked(activity, imageInfo.urlOriginal, false);
			return true;
		});

	}

	@Override
	public int getItemCount() {
		return albumInfo.images.size();
	}
}
