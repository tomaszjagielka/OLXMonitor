package com.lerabytes.olxmonitor.adapters

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.lerabytes.olxmonitor.R
import com.lerabytes.olxmonitor.activities.OffersActivity
import com.lerabytes.olxmonitor.enums.ItemViewTypes
import com.lerabytes.olxmonitor.fragments.*
import com.lerabytes.olxmonitor.models.Item
import com.lerabytes.olxmonitor.models.ItemLink
import com.lerabytes.olxmonitor.models.ItemTitle
import java.util.*

class ItemsAdapter(
    private val items: ArrayList<Item>,
    private val itemsImportant: ArrayList<Item>,
    private val recyclerView: RecyclerView
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val sTag = "ItemsAdapter"

    private var clickListener: ItemClickListener? = null

    fun setClickListener(itemClickListener: ItemClickListener) {
        this.clickListener = itemClickListener
    }

    interface ItemClickListener {
        fun onClick(view: View, position: Int, isLongClick: Boolean)
    }

    init {
        fun setDragAndDrop() {
            val ithCallback: ItemTouchHelper.Callback = object : ItemTouchHelper.Callback() {

                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: ViewHolder,
                    target: ViewHolder
                ): Boolean {

                    // Get the viewHolder's and target's positions in your adapter data, swap them.
                    Collections.swap(
                        items,
                        viewHolder.adapterPosition,
                        target.adapterPosition
                    )
                    // And notify the adapter that its dataset has changed.
                    notifyItemMoved(
                        viewHolder.adapterPosition,
                        target.adapterPosition
                    )

                    if (itemsContent.size >= viewHolder.adapterPosition && itemsContent.size >= target.adapterPosition)
                        Collections.swap(
                            itemsContent,
                            viewHolder.adapterPosition,
                            target.adapterPosition
                        )

                    return true
                }

                override fun onSwiped(viewHolder: ViewHolder, direction: Int) {}

                // Defines the enabled move directions in each state (idle, swiping, dragging).
                override fun getMovementFlags(
                    recyclerView: RecyclerView,
                    viewHolder: ViewHolder
                ): Int {
                    return makeFlag(
                        ItemTouchHelper.ACTION_STATE_DRAG,
                        ItemTouchHelper.DOWN or ItemTouchHelper.UP or ItemTouchHelper.START or ItemTouchHelper.END
                    )
                }
            }

            val ith = ItemTouchHelper(ithCallback)
            ith.attachToRecyclerView(recyclerView)
        }

        fun setDivider() {
            val itemDecoration: RecyclerView.ItemDecoration =
                DividerItemDecoration(recyclerView.context, DividerItemDecoration.VERTICAL)
            recyclerView.addItemDecoration(itemDecoration)
        }

        setDragAndDrop()
        setDivider()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        when (viewType) {
            ItemViewTypes.ItemTitle.ordinal -> {
                val itemTitleViewHolder = ItemTitleViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.item_container_title,
                        parent,
                        false
                    ),
                    clickListener
                )

                itemTitleViewHolder.setOptionsClickListener(
                    ItemOptions(
                        itemTitleViewHolder,
                        viewType
                    )
                )
                return itemTitleViewHolder
            }
            else -> {
                val itemLinkViewHolder = ItemLinkViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.item_container_link,
                        parent,
                        false
                    ),
                    clickListener
                )

                itemLinkViewHolder.setOptionsClickListener(
                    ItemOptions(
                        itemLinkViewHolder,
                        viewType
                    )
                )
                return itemLinkViewHolder
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        when (getItemViewType(position)) {
            ItemViewTypes.ItemTitle.ordinal -> {
                val itemTitle = items[position].`object` as ItemTitle
                (holder as ItemTitleViewHolder).setData(itemTitle)

                // Don't need to cast the holder, because it has been casted earlier.
                if (items[position] in itemsImportant) holder.setBold()
                else holder.setNormal()
            }
            else -> {
                val itemLink = items[position].`object` as ItemLink
                (holder as ItemLinkViewHolder).setData(itemLink)

                if (items[position] in itemsImportant) holder.setBold()
                else holder.setNormal()
            }
        }
    }

    // VIEWHOLDERS.

    private class ItemTitleViewHolder(itemView: View, private val clickListener: ItemClickListener?) :
        ViewHolder(itemView), View.OnClickListener {

        private val textItemTitle: TextView = itemView.findViewById(R.id.textItemTitle)

        private val ibOptions: ImageButton =
            itemView.findViewById(R.id.ibOptions)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            clickListener?.onClick(v, adapterPosition, false)
        }

        fun setData(itemTitle: ItemTitle) {
            textItemTitle.text = itemTitle.itemTitle
        }

        fun setNormal() {
            textItemTitle.typeface = Typeface.DEFAULT
        }

        fun setBold() {
            textItemTitle.typeface = Typeface.DEFAULT_BOLD
        }

        fun setOptionsClickListener(
            itemsOptions: ItemOptions
        ) {
            ibOptions.setOnClickListener {
                itemsOptions.itemOptionsClicked(
                    itemView,
                    ibOptions
                )
            }
        }
    }

    private class ItemLinkViewHolder(itemView: View, private val clickListener: ItemClickListener?) :
        ViewHolder(itemView), View.OnClickListener {

        private val textItemName: TextView = itemView.findViewById(R.id.textItemName)
        private val textItemLink: TextView = itemView.findViewById(R.id.textItemLink)
        private val ibOptions: ImageButton =
            itemView.findViewById(R.id.ibOptions)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            clickListener?.onClick(v, adapterPosition, false)
        }

        fun setData(itemLink: ItemLink) {
            textItemName.text = itemLink.itemName
            textItemLink.text = itemLink.itemLink
        }

        fun setNormal() {
            textItemName.typeface = Typeface.DEFAULT
            textItemLink.typeface = Typeface.DEFAULT
        }

        fun setBold() {
            textItemName.typeface = Typeface.DEFAULT_BOLD
            textItemLink.typeface = Typeface.DEFAULT_BOLD
        }

        fun setOptionsClickListener(
            itemsOptions: ItemOptions
        ) {
            ibOptions.setOnClickListener {
                itemsOptions.itemOptionsClicked(
                    itemView,
                    ibOptions
                )
            }
        }
    }

    // GETTERS.

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].type
    }

    // OTHER FUNCTIONS.

    private inner class ItemOptions(
        private val holder: Any,
        private val itemViewType: Int
    ) {
        private var optionsShown = false

        fun itemOptionsClicked(
            itemView: View,
            ibOptions: ImageButton
        ) {
            if (optionsShown)
                return

            val popup = PopupMenu(itemView.context, ibOptions)
            popup.inflate(R.menu.item_options_menu)
            popup.setOnMenuItemClickListener { item ->

                val position = if (itemViewType == ItemViewTypes.ItemTitle.ordinal)
                    (holder as ItemTitleViewHolder).adapterPosition
                else
                    (holder as ItemLinkViewHolder).adapterPosition

                clickedOptionsIndex = position

                fun deleteItem() {
                    itemsImportant.remove(items[position])
                    items.removeAt(position)
                    notifyItemRemoved(position)

                    if (itemsContent.size > position)
                        itemsContent.removeAt(position)

                    if (isDualPane) {
                        val context = recyclerView.context as AppCompatActivity

                        popup.dismiss()

                        offersFragment.clear()

                        OffersFragment().showDualPaneOffers(
                            context
                        ).apply {
                            if (shownIndex == position) {
                                if (position == 0) {
                                    shownIndex = 0
                                } else if (position >= items.size) {
                                    shownIndex = items.size - 1
                                }
                            }
                        }
                    }
                }

                fun copyItemURL(itemViewType: Int) {
                    val context = recyclerView.context as AppCompatActivity
                    when (itemViewType) {
                        0 -> {
                            val url =
                                context.getString(R.string.first_url_part) + (items[position].`object` as ItemTitle).itemTitle.replace(
                                    " ",
                                    "-"
                                ) + "/"
                            val clipboard =
                                itemView.context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText(
                                "itemTitle",
                                url
                            )

                            clipboard.setPrimaryClip(clip)
                        }

                        else -> {
                            val clipboard =
                                itemView.context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText(
                                "itemLink",
                                (items[position].`object` as ItemLink).itemLink
                            )

                            clipboard.setPrimaryClip(clip)
                        }
                    }
                }

                fun goToItemURL(itemViewType: Int) {
                    val context = recyclerView.context as AppCompatActivity
                    when (itemViewType) {
                        ItemViewTypes.ItemTitle.ordinal -> {
                            val url = context.getString(R.string.first_url_part) +
                                    (items[position].`object` as ItemTitle).itemTitle.replace(
                                        " ",
                                        "-"
                                    ) + "/"

                            val browse = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(url)
                            )

                            itemView.context.startActivity(browse)
                        }

                        else -> {
                            val browse = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse((items[position].`object` as ItemLink).itemLink)
                            )

                            itemView.context.startActivity(browse)
                        }
                    }

                    itemsImportant.remove(items[position])
                    notifyItemChanged(position, Unit)
                }

                when (item.itemId) {
                    R.id.itemOptionsDelete -> {
                        deleteItem()
                        true
                    }
                    R.id.itemOptionsCopyURL -> {
                        copyItemURL(itemViewType)
                        true
                    }
                    R.id.itemOptionsGoToURL -> {
                        goToItemURL(itemViewType)
                        true
                    }
                    else -> false
                }
            }

            popup.setOnDismissListener {
                optionsShown = false
            }

            popup.dismiss()
            popup.show()

            optionsShown = true
        }
    }
}