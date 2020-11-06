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

package com.abielinski.marooned.common;

import android.content.Context;
import android.content.pm.PackageManager;
import com.abielinski.marooned.Marooned;
import com.abielinski.marooned.common.collections.CollectionStream;
import com.abielinski.marooned.reddit.things.SubredditCanonicalId;

import java.net.URI;
import java.util.ArrayList;

public final class Constants {

	public static String version(final Context context) {
		try {
			return context.getPackageManager()
					.getPackageInfo(context.getPackageName(), 0).versionName;
		} catch(final PackageManager.NameNotFoundException e) {
			throw new RuntimeException(e); // Internal error
		}
	}

	public static final class Mime {

		public static boolean isImage(final String mimetype) {
			return StringUtils.asciiLowercase(mimetype).startsWith("image/");
		}

		public static boolean isImageGif(final String mimetype) {
			return mimetype.equalsIgnoreCase("image/gif");
		}

		public static boolean isVideo(final String mimetype) {
			return mimetype.startsWith("video/");
		}
	}

	public static final class Reddit {

		public static final ArrayList<SubredditCanonicalId> DEFAULT_SUBREDDITS;

		static {
			final String[] defaultSubredditStrings = {
					"/r/announcements",
					"/r/Art",
					"/r/AskReddit",
					"/r/askscience",
					"/r/aww",
					"/r/blog",
					"/r/books",
					"/r/dataisbeautiful",
					"/r/DIY",
					"/r/Documentaries",
					"/r/EarthPorn",
					"/r/explainlikeimfive",
					"/r/Fitness",
					"/r/food",
					"/r/funny",
					"/r/Futurology",
					"/r/gadgets",
					"/r/gaming",
					"/r/GetMotivated",
					"/r/gifs",
					"/r/history",
					"/r/IAmA",
					"/r/InternetIsBeautiful",
					"/r/Jokes",
					"/r/LifeProTips",
					"/r/listentothis",
					"/r/mildlyinteresting",
					"/r/movies",
					"/r/Music",
					"/r/news",
					"/r/nosleep",
					"/r/nottheonion",
					"/r/oldschoolcool",
					"/r/personalfinance",
					"/r/philosophy",
					"/r/photoshopbattles",
					"/r/pics",
					"/r/science",
					"/r/Showerthoughts",
					"/r/space",
					"/r/sports",
					"/r/television",
					"/r/tifu",
					"/r/todayilearned",
					"/r/TwoXChromosomes",
					"/r/UpliftingNews",
					"/r/videos",
					"/r/worldnews",
					"/r/writingprompts"
			};

			DEFAULT_SUBREDDITS = new CollectionStream<>(defaultSubredditStrings)
					.mapRethrowExceptions(SubredditCanonicalId::new)
					.collect(new ArrayList<>(defaultSubredditStrings.length));
		}

		public static final String
				SCHEME_HTTPS = "https",
				DOMAIN_HTTPS = "oauth.reddit.com",
				DOMAIN_HTTPS_HUMAN = "reddit.com",
				PATH_VOTE = "/api/vote",
				PATH_SAVE = "/api/save",
				PATH_HIDE = "/api/hide",
				PATH_UNSAVE = "/api/unsave",
				PATH_UNHIDE = "/api/unhide",
				PATH_REPORT = "/api/report",
				PATH_DELETE = "/api/del",
				PATH_SUBSCRIBE = "/api/subscribe",
				PATH_SUBREDDITS_MINE_SUBSCRIBER
						= "/subreddits/mine/subscriber.json?limit=100",
				PATH_SUBREDDITS_MINE_MODERATOR
						= "/subreddits/mine/moderator.json?limit=100",
				PATH_SUBREDDITS_POPULAR = "/subreddits/popular.json",
				PATH_MULTIREDDITS_MINE = "/api/multi/mine.json",
				PATH_COMMENTS = "/comments/",
				PATH_ME = "/api/v1/me";

		public static String getScheme() {
			return SCHEME_HTTPS;
		}

		public static String getDomain() {
			return DOMAIN_HTTPS;
		}

		public static String getHumanReadableDomain() {
			return DOMAIN_HTTPS_HUMAN;
		}

		public static URI getUri(final String path) {
			return General.uriFromString(getScheme() + "://" + getDomain() + path);
		}

		public static URI getNonAPIUri(final String path) {
			return General.uriFromString(getScheme() + "://reddit.com" + path);
		}

		public static boolean isApiErrorUser(final String str) {
			return ".error.USER_REQUIRED".equals(str) || "please login to do that".equals(
					str);
		}

		public static boolean isApiErrorCaptcha(final String str) {
			return ".error.BAD_CAPTCHA.field-captcha".equals(str)
					|| "care to try these again?".equals(str);
		}

		public static boolean isApiErrorNotAllowed(final String str) {
			return ".error.SUBREDDIT_NOTALLOWED.field-sr".equals(str)
					|| "you aren't allowed to post there.".equals(str);
		}

		public static boolean isApiErrorSubredditRequired(final String str) {
			return ".error.SUBREDDIT_REQUIRED.field-sr".equals(str)
					|| "you must specify a subreddit".equals(str);
		}

		public static boolean isApiErrorURLRequired(final String str) {
			return ".error.NO_URL.field-url".equals(str)
					|| "a url is required".equals(str);
		}

		public static boolean isApiTooFast(final String str) {
			return ".error.RATELIMIT.field-ratelimit".equals(str)
					|| (str != null && str.contains("you are doing that too much"));
		}

		public static boolean isApiTooLong(final String str) {
			return "TOO_LONG".equals(str)
					|| (str != null && str.contains("this is too long"));
		}

		public static boolean isApiAlreadySubmitted(final String str) {
			return ".error.ALREADY_SUB.field-url".equals(str)
					|| (str != null
					&& str.contains("that link has already been submitted"));
		}

		public static boolean isApiError(final String str) {
			return str != null && str.startsWith(".error.");
		}
	}

	public static String ua(final Context context) {
		final String canonicalName = Marooned.class.getCanonicalName();
		return canonicalName.substring(0, canonicalName.lastIndexOf('.')) + "/" + version(
				context);
	}

	public static final class Priority {
		public static final int
				CAPTCHA = -600,
				API_ACTION = -500,
				API_MULTIREDDIT_LIST = -200,
				API_SUBREDDIT_LIST = -100,
				API_SUBREDDIT_INVIDIVUAL = -250,
				API_POST_LIST = -200,
				API_COMMENT_LIST = -300,
				THUMBNAIL = 100,
				IMAGE_PRECACHE = 500,
				COMMENT_PRECACHE = 400,
				IMAGE_VIEW = -400,
				API_USER_ABOUT = -500,
				API_INBOX_LIST = -500;
	}

	public static final class FileType {
		public static final int NOCACHE = -1,
				SUBREDDIT_LIST = 100,
				SUBREDDIT_ABOUT = 101,
				MULTIREDDIT_LIST = 102,
				POST_LIST = 110,
				COMMENT_LIST = 120,
				USER_ABOUT = 130,
				INBOX_LIST = 140,
				THUMBNAIL = 200,
				IMAGE = 201,
				CAPTCHA = 202,
				IMAGE_INFO = 300;
	}
}