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

package com.abielinski.marooned.http;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.abielinski.marooned.cache.CacheRequest;
import com.abielinski.marooned.http.okhttp.OKHTTPBackend;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.List;

public abstract class HTTPBackend {
	/**
	 * Factory method can read configuration information to choose a backend
	 */
	public static HTTPBackend getBackend() {
		return OKHTTPBackend.getHttpBackend();
	}

	public static class RequestDetails {

		@NonNull private final URI mUrl;
		@Nullable private final List<PostField> mPostFields;

		public RequestDetails(
				@NonNull final URI url,
				@Nullable final List<PostField> postFields) {
			mUrl = url;
			mPostFields = postFields;
		}

		@NonNull
		public URI getUrl() {
			return mUrl;
		}

		@Nullable
		public List<PostField> getPostFields() {
			return mPostFields;
		}

		@NonNull
		@Override
		public String toString() {
			return "RequestDetails("
					+ mUrl.toString()
					+ ", "
					+ (mPostFields != null ? mPostFields.toString() : null)
					+ ")";
		}
	}

	public static class PostField {

		public final String name;
		public final String value;

		public PostField(final String name, final String value) {
			this.name = name;
			this.value = value;
		}

		public String encode() {
			try {
				return URLEncoder.encode(name, "UTF-8") + "=" + URLEncoder.encode(
						value,
						"UTF-8");
			} catch(final UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}

		public static String encodeList(final List<PostField> fields) {

			final StringBuilder result = new StringBuilder();

			for(final PostField field : fields) {

				if(result.length() > 0) {
					result.append('&');
				}

				result.append(field.encode());
			}

			return result.toString();
		}

		@NonNull
		@Override
		public String toString() {
			return "PostField(name=" + name + ")";
		}
	}

	public interface Request {
		void executeInThisThread(final Listener listener);

		void cancel();

		void addHeader(String name, String value);
	}

	public interface Listener {
		void onError(
				@CacheRequest.RequestFailureType int failureType,
				Throwable exception,
				Integer httpStatus);

		void onSuccess(String mimetype, Long bodyBytes, InputStream body);
	}

	public abstract Request prepareRequest(Context context, RequestDetails details);

	public abstract void recreateHttpBackend();

}