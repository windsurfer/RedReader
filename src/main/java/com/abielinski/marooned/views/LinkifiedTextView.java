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

package com.abielinski.marooned.views;

import android.preference.PreferenceManager;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;

import com.abielinski.marooned.common.General;
import com.abielinski.marooned.common.PrefsUtility;

public class LinkifiedTextView extends AppCompatTextView {

	private final AppCompatActivity mActivity;

	public LinkifiedTextView(final AppCompatActivity activity) {
		super(activity);
		float lineHeight = PrefsUtility.pref_appearance_comment_spacing(
				activity,
				General.getSharedPrefs(activity));
		this.setLineSpacing(0, lineHeight);
		mActivity = activity;
	}

	public AppCompatActivity getActivity() {
		return mActivity;
	}

	@Override
	public boolean onTouchEvent(final MotionEvent event) {
		final CharSequence text = getText();

		if(!(text instanceof Spannable)) {
			return false;
		}

		if(!PrefsUtility.pref_appearance_link_text_clickable(
				mActivity,
				General.getSharedPrefs(mActivity))) {

			return false;
		}

		final Spannable buffer = (Spannable)text;

		final int action = event.getAction();

		if(action == MotionEvent.ACTION_UP ||
				action == MotionEvent.ACTION_DOWN) {
			int x = (int)event.getX();
			int y = (int)event.getY();

			x -= getTotalPaddingLeft();
			y -= getTotalPaddingTop();

			x += getScrollX();
			y += getScrollY();

			final Layout layout = getLayout();
			final int line = layout.getLineForVertical(y);
			final int off = layout.getOffsetForHorizontal(line, x);

			final ClickableSpan[] links = buffer.getSpans(off, off, ClickableSpan.class);

			if(links.length != 0) {
				if(action == MotionEvent.ACTION_UP) {
					links[0].onClick(this);
				} else if(action == MotionEvent.ACTION_DOWN) {
					Selection.setSelection(
							buffer,
							buffer.getSpanStart(links[0]),
							buffer.getSpanEnd(links[0]));
				}

				return true;

			} else {
				Selection.removeSelection(buffer);
			}
		}

		return false;
	}
}
