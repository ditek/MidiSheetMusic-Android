/*
 * Copyright (c) 2012 Madhav Vaidyanathan
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 */

package com.midisheetmusic;

import androidx.preference.*;
import android.view.*;
import android.content.*;

/**
 *  Used in a PreferenceScreen to let
 *  the user choose a color for an option.
 *  <p/>
 *  This Preference displays text, plus an additional color box
 */
public class ColorPreference extends Preference 
        implements ColorChangedListener {

    private View colorview;    /* The view displaying the selected color */
    private int color;         /* The selected color */
    private Context context;

    public ColorPreference(Context ctx) {
        super(ctx);
        context = ctx;
        setWidgetLayoutResource(R.layout.color_preference);
    }

    public void setColor(int value) { 
        color = value; 
        if (colorview != null) {
            colorview.setBackgroundColor(color);
        }
    }
    public int getColor() { return color; }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        colorview = holder.findViewById(R.id.color_preference_widget);
        if (color != 0) {
            colorview.setBackgroundColor(color);
        }
    }

    /* When clicked, display the color picker dialog */
    protected void onClick() {
        ColorDialog dialog = new ColorDialog(context, this, color);
        dialog.show();
    }

    /* When the color picker dialog returns, update the color */
    public void colorChanged(int value) {
        color = value;
        colorview.setBackgroundColor(color);
    }
}


