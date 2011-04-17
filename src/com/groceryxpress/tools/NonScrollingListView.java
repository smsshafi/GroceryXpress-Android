package com.groceryxpress.tools;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;

public class NonScrollingListView extends LinearLayout {
	private int dividerResourceId = -1;
	private BaseAdapter baseAdapter = null;
	private int sizeLimit = 0;

	public NonScrollingListView( final Context context ) {
		super( context );

		this.setOrientation( LinearLayout.VERTICAL );
	}

	public NonScrollingListView( final Context context, final AttributeSet attrs ) {
		super( context, attrs );

		this.setOrientation( LinearLayout.VERTICAL );
	}

	public BaseAdapter getAdapter() {
		return this.baseAdapter;
	}

	private void redrawList() {
		final int limit;

		this.removeAllViews();

		if (this.baseAdapter == null) {
			return;
		}

		limit =
			( this.sizeLimit > 0 ? this.sizeLimit
					: this.baseAdapter.getCount() );

		for ( int i = 0; i < limit; ++i ) {
			this.addView( this.baseAdapter.getView( i, null,
					NonScrollingListView.this ) );

			if ( ( this.dividerResourceId != -1 ) && ( i != limit - 1 ) ) {
				this.addView( View.inflate( this.getContext(),
						this.dividerResourceId,
						new LinearLayout( this.getContext() ) ) );
			}
		}
	}

	public void setAdapter( final BaseAdapter baseAdapter ) {
		this.baseAdapter = baseAdapter;

		this.redrawList();
	}

	public void setDividerResourceId( final int dividerResourceId ) {
		this.dividerResourceId = dividerResourceId;

		this.redrawList();
	}

	public void setSizeLimit( final int sizeLimit ) {
		this.sizeLimit = sizeLimit;

		this.redrawList();
	}
}
