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

package com.abielinski.marooned.activities;


import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.abielinski.marooned.R;
import com.abielinski.marooned.account.RedditAccount;
import com.abielinski.marooned.account.RedditAccountChangeListener;
import com.abielinski.marooned.account.RedditAccountManager;
import com.abielinski.marooned.common.DialogUtils;
import com.abielinski.marooned.common.General;
import com.abielinski.marooned.common.LinkHandler;
import com.abielinski.marooned.common.PrefsUtility;
import com.abielinski.marooned.fragments.PostListingFragment;
import com.abielinski.marooned.fragments.SessionListDialog;
import com.abielinski.marooned.listingcontrollers.PostListingController;
import com.abielinski.marooned.reddit.PostSort;
import com.abielinski.marooned.reddit.api.RedditSubredditSubscriptionManager;
import com.abielinski.marooned.reddit.api.SubredditSubscriptionState;
import com.abielinski.marooned.reddit.prepared.RedditPreparedPost;
import com.abielinski.marooned.reddit.things.InvalidSubredditNameException;
import com.abielinski.marooned.reddit.things.RedditSubreddit;
import com.abielinski.marooned.reddit.url.PostCommentListingURL;
import com.abielinski.marooned.reddit.url.PostListingURL;
import com.abielinski.marooned.reddit.url.RedditURLParser;
import com.abielinski.marooned.reddit.url.SearchPostListURL;
import com.abielinski.marooned.reddit.url.SubredditPostListURL;
import com.abielinski.marooned.views.RedditPostView;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class PostListingActivity extends RefreshableActivity
		implements RedditAccountChangeListener,
		RedditPostView.PostSelectionListener,
		OptionsMenuUtility.OptionsMenuPostsListener,
		SessionChangeListener,
		RedditSubredditSubscriptionManager.SubredditSubscriptionStateChangeListener {

	private static final String SAVEDSTATE_SESSION = "pla_session";
	private static final String SAVEDSTATE_SORT = "pla_sort";
	private static final String SAVEDSTATE_FRAGMENT = "pla_fragment";

	private PostListingFragment fragment;
	private PostListingController controller;

	private final AtomicReference<RedditSubredditSubscriptionManager.ListenerContext>
			mSubredditSubscriptionListenerContext = new AtomicReference<>(null);

	private static long lastBackPress = -1;

	@Override
	public void onCreate(final Bundle savedInstanceState) {

		PrefsUtility.applyTheme(this);

		super.onCreate(savedInstanceState);

		final TypedArray typedArray
				= obtainStyledAttributes(new int[] {R.attr.rrListBackgroundCol});

		try {
			getWindow().setBackgroundDrawable(
					new ColorDrawable(typedArray.getColor(0, 0)));

		} finally {
			typedArray.recycle();
		}

		RedditAccountManager.getInstance(this).addUpdateListener(this);

		if(getIntent() != null) {

			final Intent intent = getIntent();

			final RedditURLParser.RedditURL url
					= RedditURLParser.parseProbablePostListing(intent.getData());

			if(!(url instanceof PostListingURL)) {
				throw new RuntimeException(String.format(
						Locale.US,
						"'%s' is not a post listing URL!",
						url.generateJsonUri()));
			}

			controller = new PostListingController((PostListingURL)url, this);

			Bundle fragmentSavedInstanceState = null;

			if(savedInstanceState != null) {

				if(savedInstanceState.containsKey(SAVEDSTATE_SESSION)) {
					controller.setSession(UUID.fromString(savedInstanceState.getString(
							SAVEDSTATE_SESSION)));
				}

				if(savedInstanceState.containsKey(SAVEDSTATE_SORT)) {
					controller.setSort(PostSort.valueOf(
							savedInstanceState.getString(SAVEDSTATE_SORT)));
				}

				if(savedInstanceState.containsKey(SAVEDSTATE_FRAGMENT)) {
					fragmentSavedInstanceState = savedInstanceState.getBundle(
							SAVEDSTATE_FRAGMENT);
				}
			}

			setTitle(url.humanReadableName(this, false));

			setBaseActivityContentView(R.layout.main_single);
			doRefresh(RefreshableFragment.POSTS, false, fragmentSavedInstanceState);

		} else {
			throw new RuntimeException("Nothing to show!");
		}

		recreateSubscriptionListener();
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);

		final UUID session = controller.getSession();
		if(session != null) {
			outState.putString(SAVEDSTATE_SESSION, session.toString());
		}

		final PostSort sort = controller.getSort();
		if(sort != null) {
			outState.putString(SAVEDSTATE_SORT, sort.name());
		}

		if(fragment != null) {
			outState.putBundle(SAVEDSTATE_FRAGMENT, fragment.onSaveInstanceState());
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		final RedditSubredditSubscriptionManager.ListenerContext listenerContext
				= mSubredditSubscriptionListenerContext.get();

		if(listenerContext != null) {
			listenerContext.removeListener();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {

		final RedditAccount user = RedditAccountManager.getInstance(this)
				.getDefaultAccount();
		final SubredditSubscriptionState
				subredditSubscriptionState;
		final RedditSubredditSubscriptionManager subredditSubscriptionManager
				= RedditSubredditSubscriptionManager.getSingleton(this, user);

		if(fragment != null
				&& controller.isRandomSubreddit()
				&& fragment.getSubreddit() != null) {
			SubredditPostListURL url = SubredditPostListURL.parse(controller.getUri());
			if(url != null && url.type == SubredditPostListURL.Type.RANDOM) {
				try {
					final String newSubreddit
							= RedditSubreddit.stripRPrefix(fragment.getSubreddit().url);
					url = url.changeSubreddit(newSubreddit);
					controller = new PostListingController(url, this);
				} catch(final InvalidSubredditNameException e) {
					throw new RuntimeException(e);
				}
			}
		}

		if(!user.isAnonymous()
				&& (controller.isSubreddit() || controller.isRandomSubreddit())
				&& subredditSubscriptionManager.areSubscriptionsReady()
				&& fragment != null
				&& fragment.getSubreddit() != null) {

			subredditSubscriptionState = subredditSubscriptionManager.getSubscriptionState(
					controller.subredditCanonicalName());

		} else {
			subredditSubscriptionState = null;
		}

		final String subredditDescription = fragment != null
				&& fragment.getSubreddit() != null
				? fragment.getSubreddit().description_html
				: null;

		Boolean subredditPinState = null;
		Boolean subredditBlockedState = null;

		if((controller.isSubreddit() || controller.isRandomSubreddit())
				&& fragment != null
				&& fragment.getSubreddit() != null) {

			try {
				subredditPinState = PrefsUtility.pref_pinned_subreddits_check(
						this,
						PreferenceManager.getDefaultSharedPreferences(this),
						fragment.getSubreddit().getCanonicalId());

				subredditBlockedState = PrefsUtility.pref_blocked_subreddits_check(
						this,
						PreferenceManager.getDefaultSharedPreferences(this),
						fragment.getSubreddit().getCanonicalId());

			} catch(final InvalidSubredditNameException e) {
				subredditPinState = null;
				subredditBlockedState = null;
			}
		}

		OptionsMenuUtility.prepare(
				this,
				menu,
				false,
				true,
				false,
				controller.isSearchResults(),
				controller.isUserPostListing(),
				false,
				controller.isSortable(),
				true,
				controller.isFrontPage(),
				subredditSubscriptionState,
				subredditDescription != null && subredditDescription.length() > 0,
				false,
				subredditPinState,
				subredditBlockedState);

		return true;
	}

	private void recreateSubscriptionListener() {

		final RedditSubredditSubscriptionManager.ListenerContext oldContext
				= mSubredditSubscriptionListenerContext.getAndSet(
				RedditSubredditSubscriptionManager
						.getSingleton(
								this,
								RedditAccountManager.getInstance(this)
										.getDefaultAccount())
						.addListener(this));

		if(oldContext != null) {
			oldContext.removeListener();
		}
	}

	@Override
	public void onRedditAccountChanged() {
		recreateSubscriptionListener();
		postInvalidateOptionsMenu();
		requestRefresh(RefreshableFragment.ALL, false);
	}

	@Override
	protected void doRefresh(
			final RefreshableFragment which,
			final boolean force,
			final Bundle savedInstanceState) {
		if(fragment != null) {
			fragment.cancel();
		}
		fragment = controller.get(this, force, savedInstanceState);

		final View view = fragment.getView();
		setBaseActivityContentView(view);
		General.setLayoutMatchParent(view);
	}

	@Override
	public void onPostSelected(final RedditPreparedPost post) {
		LinkHandler.onLinkClicked(this, post.src.getUrl(), false, post.src.getSrc());
	}

	@Override
	public void onPostCommentsSelected(final RedditPreparedPost post) {
		LinkHandler.onLinkClicked(
				this,
				PostCommentListingURL.forPostId(post.src.getIdAlone()).toString(),
				false);
	}

	@Override
	public void onRefreshPosts() {
		controller.setSession(null);
		requestRefresh(RefreshableFragment.POSTS, true);
	}

	@Override
	public void onPastPosts() {
		final SessionListDialog sessionListDialog = SessionListDialog.newInstance(
				controller.getUri(),
				controller.getSession(),
				SessionChangeType.POSTS);
		sessionListDialog.show(getSupportFragmentManager(), "SessionListDialog");
	}

	@Override
	public void onSubmitPost() {
		final Intent intent = new Intent(this, PostSubmitActivity.class);
		if(controller.isSubreddit()) {
			intent.putExtra("subreddit", controller.subredditCanonicalName().toString());
		}
		startActivity(intent);
	}

	@Override
	public void onSortSelected(final PostSort order) {
		controller.setSort(order);
		requestRefresh(RefreshableFragment.POSTS, false);
	}

	@Override
	public void onSearchPosts() {
		onSearchPosts(controller, this);
	}

	public static void onSearchPosts(
			final PostListingController controller,
			final AppCompatActivity activity) {

		DialogUtils.showSearchDialog(activity, new DialogUtils.OnSearchListener() {
			@Override
			public void onSearch(@Nullable final String query) {
				if(query == null) {
					return;
				}

				final SearchPostListURL url;

				if(controller != null && (controller.isSubreddit()
						|| controller.isSubredditSearchResults())) {
					url = SearchPostListURL.build(controller.subredditCanonicalName()
							.toString(), query);
				} else {
					url = SearchPostListURL.build(null, query);
				}

				final Intent intent = new Intent(activity, PostListingActivity.class);
				intent.setData(url.generateJsonUri());
				activity.startActivity(intent);
			}
		});
	}

	@Override
	public void onSubscribe() {
		fragment.onSubscribe();
	}

	@Override
	public void onUnsubscribe() {
		fragment.onUnsubscribe();
	}

	@Override
	public void onSidebar() {
		if(fragment.getSubreddit() != null) {
			final Intent intent = new Intent(this, HtmlViewActivity.class);
			intent.putExtra(
					"html",
					fragment.getSubreddit()
							.getSidebarHtml(PrefsUtility.isNightMode(this)));
			intent.putExtra("title", String.format(Locale.US, "%s: %s",
					getString(R.string.sidebar_activity_title),
					fragment.getSubreddit().url));
			startActivityForResult(intent, 1);
		}
	}

	@Override
	public void onPin() {

		if(fragment == null) {
			return;
		}

		try {
			PrefsUtility.pref_pinned_subreddits_add(
					this,
					PreferenceManager.getDefaultSharedPreferences(this),
					fragment.getSubreddit().getCanonicalId());

		} catch(final InvalidSubredditNameException e) {
			throw new RuntimeException(e);
		}

		invalidateOptionsMenu();
	}

	@Override
	public void onUnpin() {

		if(fragment == null) {
			return;
		}

		try {
			PrefsUtility.pref_pinned_subreddits_remove(
					this,
					PreferenceManager.getDefaultSharedPreferences(this),
					fragment.getSubreddit().getCanonicalId());

		} catch(final InvalidSubredditNameException e) {
			throw new RuntimeException(e);
		}

		invalidateOptionsMenu();
	}

	@Override
	public void onBlock() {
		if(fragment == null) {
			return;
		}

		try {
			PrefsUtility.pref_blocked_subreddits_add(
					this,
					PreferenceManager.getDefaultSharedPreferences(this),
					fragment.getSubreddit().getCanonicalId());

		} catch(final InvalidSubredditNameException e) {
			throw new RuntimeException(e);
		}

		invalidateOptionsMenu();
	}

	@Override
	public void onUnblock() {
		if(fragment == null) {
			return;
		}

		try {
			PrefsUtility.pref_blocked_subreddits_remove(
					this,
					PreferenceManager.getDefaultSharedPreferences(this),
					fragment.getSubreddit().getCanonicalId());

		} catch(final InvalidSubredditNameException e) {
			throw new RuntimeException(e);
		}

		invalidateOptionsMenu();
	}

	@Override
	public void onSessionSelected(final UUID session, final SessionChangeType type) {
		controller.setSession(session);
		requestRefresh(RefreshableFragment.POSTS, false);
	}

	@Override
	public void onSessionRefreshSelected(final SessionChangeType type) {
		onRefreshPosts();
	}

	@Override
	public void onSessionChanged(
			final UUID session,
			final SessionChangeType type,
			final long timestamp) {
		controller.setSession(session);
	}

	@Override
	public void onBackPressed() {

		if(PrefsUtility.pref_behaviour_back_again(this,
			PreferenceManager.getDefaultSharedPreferences(this)) &&
			(lastBackPress < SystemClock.uptimeMillis() - 2000)) {

			lastBackPress = SystemClock.uptimeMillis();
			General.quickToast(this, R.string.press_back_again);
			return;
		}
		super.onBackPressed();
	}

	@Override
	public void onSubredditSubscriptionListUpdated(
			final RedditSubredditSubscriptionManager subredditSubscriptionManager) {
		postInvalidateOptionsMenu();
	}

	@Override
	public void onSubredditSubscriptionAttempted(
			final RedditSubredditSubscriptionManager subredditSubscriptionManager) {
		postInvalidateOptionsMenu();
	}

	@Override
	public void onSubredditUnsubscriptionAttempted(
			final RedditSubredditSubscriptionManager subredditSubscriptionManager) {
		postInvalidateOptionsMenu();
	}

	private void postInvalidateOptionsMenu() {
		runOnUiThread(this::invalidateOptionsMenu);
	}
}