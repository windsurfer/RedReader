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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import info.guardianproject.netcipher.web.WebkitProxy;
import com.abielinski.marooned.R;
import com.abielinski.marooned.Marooned;
import com.abielinski.marooned.common.PrefsUtility;
import com.abielinski.marooned.common.TorCommon;
import com.abielinski.marooned.reddit.api.RedditOAuth;

import java.io.ByteArrayInputStream;

public class OAuthLoginActivity extends BaseActivity {

	private WebView mWebView;

	private static final String CSS_FIXES
			= "li {\n" +
			"  list-style-type: none;\n" +
			"  margin:14px\n" +
			"}\n" +
			"\n" +
			"label {\n" +
			"  margin-right: 10px;\n" +
			"}\n" +
			"a, a:visited {\n" +
			"  color: #8ac5fc;\n" +
			"}\n" +
			"\n" +
			"div.icon, div.infobar, div.mobile-web-redirect-bar, div#topbar {\n" +
			"  display: none;\n" +
			"  visibility: collapse;\n" +
			"  height: 0px;\n" +
			"  padding: 0px;\n" +
			"  margin:0px;\n" +
			"}\n" +
			"\n" +
			"div.content {\n" +
			"  padding: 0px;\n" +
			"  padding-top: 1px;\n" +
			"  padding-bottom: 1px;\n" +
			"  margin: 20px;\n" +
			"  background-color: #333;\n" +
			"}\n" +
			"div.content a:not(.recover-password) {\n" +
			"  pointer-events: none;\n" +
			"}\n" +
			"#login_login, #login_reg {\n" +
			"  background-color: #333;\n" +
			"  padding-top: 24px;\n" +
			"  padding-bottom: 24px;\n" +
			"  max-width: 600px;\n" +
			"  margin: auto;\n" +
			"}\n" +
			"\n" +
			"body {\n" +
			"  background-color: #000; color: #fff; text-transform: capitalize; padding-top: 10px; padding-bottom: 60px; margin-bottom: 60px; font-size: 110%;\n" +
			"}\n" +
			"\n" +
			"input.newbutton {\n" +
			"  background-color: #444;\n" +
			"  font-size: 20pt;\n" +
			"  margin: 16px auto;\n" +
			"  border-image-source: none;\n" +
			"  color: #FFF;\n" +
			"  border: none;\n" +
			"  padding-left:14px;\n" +
			"  padding-right:14px;\n" +
			"  padding-top:6px;\n" +
			"  padding-bottom:6px;\n" +
			"}\n" +
			"\n" +
			"button {\n" +
			"  display: block;\n" +
			"  background-color: #555;\n" +
			"  text-transform: capitalize;\n" +
			"  font-size: 16pt;\n" +
			"  border-image-source: none;\n" +
			"  color: #FFF;\n" +
			"  border: none;\n" +
			"  padding-left:16px;\n" +
			"  padding-right:16px;\n" +
			"  padding-top:6px;\n" +
			"  padding-bottom:6px;\n" +
			"  margin: 16px;\n" +
			"  margin-left: auto;\n" +
			"}\n" +
			"\n" +
			"input.allow {\n" +
			"  background-color: #0A0;\n" +
			"}\n" +
			"\n" +
			"input.allow:active, input.allow:hover {\n" +
			"  background-color: #0F0;\n" +
			"}\n" +
			"\n" +
			"input.decline {\n" +
			"  background-color: #A00;\n" +
			"}\n" +
			"\n" +
			"input.decline:active, input.decline:hover {\n" +
			"  background-color: #F00;\n" +
			"}\n" +
			"\n" +
			"form.pretty-form {\n" +
			"  margin-bottom: 40px;\n" +
			"}\n" +
			"form input {\n" +
			"  background: #444; color: #fff; padding: 6px; border: none; border-bottom: 1px solid #aaa;\n" +
			"}\n" +
			".error {\n" +
			"  color: #f44; display: block; margin-top: 5px;\n" +
			"}\n" +
			"\n";

	@Override
	protected void onDestroy() {
		super.onDestroy();
		final CookieManager cookieManager = CookieManager.getInstance();
		cookieManager.removeAllCookie();
	}

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public void onCreate(final Bundle savedInstanceState) {

		PrefsUtility.applyTheme(this);

		super.onCreate(savedInstanceState);

		mWebView = new WebView(this);

		if(TorCommon.isTorEnabled()) {
			try {
				final boolean result = WebkitProxy.setProxy(
						Marooned.class.getCanonicalName(),
						getApplicationContext(),
						mWebView,
						"127.0.0.1",
						8118);
				if(!result) {
					BugReportActivity.handleGlobalError(
							this,
							getResources().getString(R.string.error_tor_setting_failed));
				}
			} catch(final Exception e) {
				e.printStackTrace();
			}
		}

		final WebSettings settings = mWebView.getSettings();

		settings.setBuiltInZoomControls(false);
		settings.setJavaScriptEnabled(true);
		settings.setJavaScriptCanOpenWindowsAutomatically(false);
		settings.setUseWideViewPort(true);
		settings.setLoadWithOverviewMode(true);
		settings.setDomStorageEnabled(true);
		settings.setSaveFormData(false);
		settings.setSavePassword(false);
		settings.setDatabaseEnabled(false);
		settings.setAppCacheEnabled(false);
		settings.setDisplayZoomControls(false);

		setTitle(R.string.firstrun_login_title);
		mWebView.loadUrl(RedditOAuth.getPromptUri().toString());
		mWebView.setBackgroundColor(Color.BLACK);
		mWebView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(
					final WebView view,
					final String url) {

				if(url.startsWith("http://marooned_oauth_redir")
						|| url.startsWith("marooned://marooned_oauth_redir")) { // TODO constant

					final Intent intent = new Intent();
					intent.putExtra("url", url);
					setResult(123, intent);
					finish();

				} else if (url.startsWith("https://www.reddit.com/password")){
					Intent browserIntent = new Intent(Intent.ACTION_VIEW);
					browserIntent.setData(Uri.parse(url));
					startActivity(browserIntent);
					return true;
				} else {
					setTitle(url);
					return false;
				}

				return true;
			}

			@Override
			public WebResourceResponse shouldInterceptRequest(
					final WebView view,
					final String url) {

				if(url.matches(".*compact.*\\.css")) {
					return new WebResourceResponse(
							"text/css",
							"UTF-8",
							new ByteArrayInputStream(CSS_FIXES.getBytes()));
				}

				return null;
			}
		});

		setBaseActivityContentView(mWebView);
	}

	@Override
	protected void onPause() {

		super.onPause();

		if(mWebView != null) {
			mWebView.onPause();
			mWebView.pauseTimers();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		if(mWebView != null) {
			mWebView.resumeTimers();
			mWebView.onResume();
		}
	}
}
