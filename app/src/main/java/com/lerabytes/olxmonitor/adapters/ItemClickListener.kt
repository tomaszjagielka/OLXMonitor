package com.lerabytes.olxmonitor.adapters

import android.view.View

interface ItemClickListener {
    fun onClick(view: View, position: Int, isLongClick: Boolean)
} 