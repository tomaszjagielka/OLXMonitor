package com.lerabytes.olxmonitor.layoutmanagers

import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import androidx.recyclerview.widget.StaggeredGridLayoutManager


/**
 * No Predictive Animations StaggeredGridLayoutManager
 */
class NpaStaggeredGridLayoutManager(
    spanCount: Int,
    orientation: Int
) : StaggeredGridLayoutManager(spanCount, orientation) {
    private val sTag = "NpaStGridLayoutManager"

    /**
     * Disable predictive animations. There is a bug in RecyclerView which causes views that
     * are being reloaded to pull invalid ViewHolders from the internal recycler stack if the
     * adapter size has decreased since the ViewHolder was recycled.
     */

    override fun supportsPredictiveItemAnimations(): Boolean {
        return false
    }

    override fun onLayoutChildren(recycler: Recycler?, state: RecyclerView.State?) {
        try {
            super.onLayoutChildren(recycler, state)
        } catch (e: IndexOutOfBoundsException) {
            Log.e(sTag, "Meet a IOOBE in RecyclerView")
        }
    }
}