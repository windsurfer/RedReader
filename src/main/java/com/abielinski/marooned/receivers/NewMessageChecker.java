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

package com.abielinski.marooned.receivers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.abielinski.marooned.R;
import com.abielinski.marooned.account.RedditAccount;
import com.abielinski.marooned.account.RedditAccountManager;
import com.abielinski.marooned.activities.BugReportActivity;
import com.abielinski.marooned.activities.InboxListingActivity;
import com.abielinski.marooned.cache.CacheManager;
import com.abielinski.marooned.cache.CacheRequest;
import com.abielinski.marooned.cache.downloadstrategy.DownloadStrategyAlways;
import com.abielinski.marooned.common.Constants;
import com.abielinski.marooned.common.General;
import com.abielinski.marooned.common.PrefsUtility;
import com.abielinski.marooned.jsonwrap.JsonBufferedArray;
import com.abielinski.marooned.jsonwrap.JsonBufferedObject;
import com.abielinski.marooned.jsonwrap.JsonValue;
import com.abielinski.marooned.reddit.things.RedditComment;
import com.abielinski.marooned.reddit.things.RedditMessage;
import com.abielinski.marooned.reddit.things.RedditThing;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class NewMessageChecker extends BroadcastReceiver {

	private static final String TAG = "NewMessageChecker";

	private static final String NOTIFICATION_CHANNEL_ID = "RRNewMessageChecker";

	private static final String PREFS_SAVED_MESSAGE_ID = "LastMessageId";
	private static final String PREFS_SAVED_MESSAGE_TIMESTAMP = "LastMessageTimestamp";


	@Override
	public void onReceive(final Context context, final Intent intent) {
		checkForNewMessages(context);
	}

	public static void checkForNewMessages(final Context context) {

		Log.i("Marooned", "Checking for new messages.");

		final boolean notificationsEnabled = PrefsUtility.pref_behaviour_notifications(
				context,
				General.getSharedPrefs(context));
		if(!notificationsEnabled) {
			return;
		}

		final RedditAccount user;

		try {
			user = RedditAccountManager.getInstance(context).getDefaultAccount();

		} catch(final SQLiteDatabaseCorruptException e) {
			// Avoid background crash
			Log.e(TAG, "Accounts database corrupt", e);
			return;
		}

		if(user.isAnonymous()) {
			return;
		}

		final CacheManager cm = CacheManager.getInstance(context);

		final URI url = Constants.Reddit.getUri("/message/unread.json?limit=2");

		final CacheRequest request = new CacheRequest(
				url,
				user,
				null,
				Constants.Priority.API_INBOX_LIST,
				0,
				DownloadStrategyAlways.INSTANCE,
				Constants.FileType.INBOX_LIST,
				CacheRequest.DOWNLOAD_QUEUE_REDDIT_API,
				true,
				true,
				context) {

			@Override
			protected void onDownloadNecessary() {
			}

			@Override
			protected void onDownloadStarted() {
			}

			@Override
			protected void onCallbackException(final Throwable t) {
				BugReportActivity.handleGlobalError(context, t);
			}

			@Override
			protected void onFailure(
					final @CacheRequest.RequestFailureType int type,
					final Throwable t,
					final Integer status,
					final String readableMessage) {
				Log.e(TAG, "Request failed", t);
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
			}

			@Override
			public void onJsonParseStarted(
					final JsonValue value,
					final long timestamp,
					final UUID session,
					final boolean fromCache) {

				try {

					final JsonBufferedObject root = value.asObject();
					final JsonBufferedObject data = root.getObject("data");
					final JsonBufferedArray children = data.getArray("children");

					children.join();
					final int messageCount = children.getCurrentItemCount();

					Log.e(TAG, "Got response. Message count = " + messageCount);

					if(messageCount < 1) {
						return;
					}

					final RedditThing thing = children.get(0).asObject(RedditThing.class);

					String title;
					final String text
							= context.getString(R.string.notification_message_action);

					final String messageID;
					final long messageTimestamp;

					switch(thing.getKind()) {
						case COMMENT: {
							final RedditComment comment = thing.asComment();
							title = context.getString(
									R.string.notification_comment,
									comment.author);
							messageID = comment.name;
							messageTimestamp = comment.created_utc;
							break;
						}

						case MESSAGE: {
							final RedditMessage message = thing.asMessage();
							title = context.getString(
									R.string.notification_message,
									message.author);
							messageID = message.name;
							messageTimestamp = message.created_utc;
							break;
						}

						default: {
							throw new RuntimeException("Unknown item in list.");
						}
					}

					// Check if the previously saved message is the same as the one we just received

					final SharedPreferences prefs
							= General.getSharedPrefs(context);
					final String oldMessageId = prefs.getString(
							PREFS_SAVED_MESSAGE_ID,
							"");
					final long oldMessageTimestamp = prefs.getLong(
							PREFS_SAVED_MESSAGE_TIMESTAMP,
							0);

					if(oldMessageId == null || (!messageID.equals(oldMessageId)
							&& oldMessageTimestamp
							<= messageTimestamp)) {

						Log.e(TAG, "New messages detected. Showing notification.");

						prefs.edit()
								.putString(PREFS_SAVED_MESSAGE_ID, messageID)
								.putLong(PREFS_SAVED_MESSAGE_TIMESTAMP, messageTimestamp)
								.apply();

						if(messageCount > 1) {
							title
									= context.getString(R.string.notification_message_multiple);
						}

						createNotification(title, text, context);

					} else {
						Log.e(TAG, "All messages have been previously seen.");
					}

				} catch(final Throwable t) {
					notifyFailure(
							CacheRequest.REQUEST_FAILURE_PARSE,
							t,
							null,
							"Parse failure");
				}
			}
		};

		cm.makeRequest(request);
	}

	private static final AtomicBoolean sChannelCreated = new AtomicBoolean(false);

	private static void createNotification(
			final String title,
			final String text,
			final Context context) {

		final NotificationManager nm = (NotificationManager)context.getSystemService(
				Context.NOTIFICATION_SERVICE);

		synchronized(sChannelCreated) {

			if(!sChannelCreated.getAndSet(true)) {

				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

					if(nm.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {

						Log.i(TAG, "Creating notification channel");

						final NotificationChannel channel = new NotificationChannel(
								NOTIFICATION_CHANNEL_ID,
								context.getString(
										R.string.notification_channel_name_reddit_messages),
								NotificationManager.IMPORTANCE_DEFAULT);

						nm.createNotificationChannel(channel);

					} else {
						Log.i(
								TAG,
								"Not creating notification channel as it already exists");
					}

				} else {
					Log.i(
							TAG,
							"Not creating notification channel due to old Android version");
				}
			}
		}

		final NotificationCompat.Builder notification = new NotificationCompat.Builder(
				context)
				.setSmallIcon(R.drawable.icon_notif)
				.setContentTitle(title)
				.setContentText(text)
				.setAutoCancel(true)
				.setChannelId(
						NOTIFICATION_CHANNEL_ID);

		final Intent intent = new Intent(context, InboxListingActivity.class);
		notification.setContentIntent(PendingIntent.getActivity(context, 0, intent, 0));

		nm.notify(0, notification.getNotification());
	}
}
