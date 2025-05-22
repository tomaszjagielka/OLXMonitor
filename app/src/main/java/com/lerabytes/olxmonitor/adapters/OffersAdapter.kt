package com.lerabytes.olxmonitor.adapters

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.lerabytes.olxmonitor.R
import com.lerabytes.olxmonitor.decorators.SpacesItemDecoration
import com.lerabytes.olxmonitor.enums.ItemViewTypes
import com.lerabytes.olxmonitor.enums.OfferViewTypes
import com.lerabytes.olxmonitor.fragments.*
import com.lerabytes.olxmonitor.models.*
import com.lerabytes.olxmonitor.services.MonitorOffersService
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

var offersPageIndex = 2
var offersLastPageIndex = 25

class OffersAdapter(
    private val items: ArrayList<Item>,
    private val recyclerView: RecyclerView,
    private val layoutManager: StaggeredGridLayoutManager,
    private val fragment: Fragment
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(), MonitorOffersService.Callbacks {
    private val sTag = "OffersAdapter"

    interface ItemClickListener {
        fun onClick(view: View, position: Int, isLongClick: Boolean)
    }

    private var clickListener: ItemClickListener? = null

    fun setClickListener(itemClickListener: ItemClickListener) {
        this.clickListener = itemClickListener
        Log.d(sTag, "Click listener set: $itemClickListener")
    }

    init {
        fun setSpacersAroundItems() {
            val spacers = SpacesItemDecoration(8)
            recyclerView.addItemDecoration(spacers)
        }

        fun setInfiniteScrolling() {
            var isLoading = false
            val context = recyclerView.context as AppCompatActivity

            fun setOffersLastPageIndex() {
                var html: Document?
                thread {
                    html = getHTML(
                        shownIndex,
                        context.getString(R.string.first_url_part)
                    )

                    // Don't exceed maximum amount of available pages,
                    // or else it'll be retrieving random offers.
                    val pages = html?.select("div.pager.rel.clr a")

                    if (pages?.isNotEmpty() == true) {
                        for (page in pages) {
                            val lastPage = page.attr("data-cy")

                            if (lastPage == "page-link-last") {
                                offersLastPageIndex =
                                    page.select("span").text().toInt()
                            }
                        }
                    } else offersLastPageIndex = 1
                }
            }

            setOffersLastPageIndex()

            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (!isLoading && offersPageIndex <= offersLastPageIndex) {
                        var firstCompletelyVisibleOffers: IntArray? = null
                        firstCompletelyVisibleOffers =
                            layoutManager.findFirstCompletelyVisibleItemPositions(
                                firstCompletelyVisibleOffers
                            )

                        if (firstCompletelyVisibleOffers[0] > layoutManager.itemCount - 10) {
                            if (!isLoading) {
                                isLoading = true
                                thread {
                                    val html = getHTML(
                                        shownIndex,
                                        context.getString(R.string.first_url_part),
                                        offersPageIndex
                                    )

                                    val t = thread {
//                                        val currentSize = items.size
                                        val offers = getOffers(html)

                                        items.addAll(offers)
                                        itemsContent[shownIndex].addAll(
                                            offers
                                        )

                                        context.runOnUiThread {
//                                            notifyItemRemoved(currentSize)
                                            notifyItemInserted(items.size)
                                        }
                                    }

                                    t.join()

                                    offersPageIndex++
                                    isLoading = false
                                }
                            }
                        }
                    }
                }
            })
        }

        fun registerOffersFragmentCallbacks() {
            monitorOffersService?.registerOffersFragmentCallbacks(this@OffersAdapter)
        }

        // If the user has added a new item to the list of items,
        // but the service has not yet added new offers to the item.
        fun addFirstOffers() {
            if (items.isEmpty()) {
                val context = recyclerView.context as AppCompatActivity

                thread {
                    val offers = getOffers(
                        getHTML(
                            shownIndex,
                            context.getString(R.string.first_url_part)
                        )
                    )

                    if (itemsContent.lastIndex >= 0) itemsContent[itemsContent.lastIndex].addAll(
                        offers
                    )

                    items.addAll(offers)
                    context.runOnUiThread { notifyItemRangeChanged(0, itemCount) }
                }
            }
        }

        fun setShownIndex() {
            if (shownIndex == -1) shownIndex = 0
        }

        setSpacersAroundItems()
        setInfiniteScrolling()
        registerOffersFragmentCallbacks()
        setShownIndex()
        addFirstOffers()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        when (viewType) {
            OfferViewTypes.OfferText.ordinal -> {
                val view = inflater.inflate(R.layout.offer_container_text, parent, false)
                return OfferTextViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.offer_container_image, parent, false)
                return OfferImageViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {        
        holder.itemView.isClickable = true
        holder.itemView.isFocusable = true
        
        when (getItemViewType(position)) {
            OfferViewTypes.OfferText.ordinal -> {
                val offerText = items[position].`object` as OfferText
                (holder as OfferTextViewHolder).setData(
                    offerText
                )
                
                holder.itemView.setOnClickListener { view ->
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(offerText.offerUrl))
                        fragment.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e(sTag, "Error opening URL: ${offerText.offerUrl}", e)
                    }
                    
                    if (clickListener != null) {
                        clickListener?.onClick(view, position, false)
                    }
                }
            }
            else -> {
                val offerImage = items[position].`object` as OfferImage
                (holder as OfferImageViewHolder).setData(
                    offerImage
                )
                
                holder.itemView.setOnClickListener { view ->
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(offerImage.offerUrl))
                        fragment.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e(sTag, "Error opening URL: ${offerImage.offerUrl}", e)
                    }
                    
                    if (clickListener != null) {
                        clickListener?.onClick(view, position, false)
                    }
                }
            }
        }
    }

    // VIEWHOLDERS.

    private inner class OfferTextViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {

        private val textOfferTitle: TextView = itemView.findViewById(R.id.textOfferTitle)
        private val textOfferPrice: TextView = itemView.findViewById(R.id.textOfferPrice)
        private val textOfferLocation: TextView = itemView.findViewById(R.id.textOfferLocation)
        private val textOfferDate: TextView = itemView.findViewById(R.id.textOfferDate)

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
            
            itemView.isClickable = true
            itemView.isFocusable = true
        }

        fun setData(offerText: OfferText) {
            textOfferTitle.text = offerText.offerTitle
            textOfferPrice.text = offerText.offerPrice
            textOfferLocation.text = offerText.offerLocation
            textOfferDate.text = offerText.offerDate
        }

        override fun onClick(view: View) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val offerText = items[position].`object` as OfferText
                
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(offerText.offerUrl))
                    fragment.startActivity(intent)
                } catch (e: Exception) {
                    Log.e(sTag, "Error opening URL from ViewHolder: ${offerText.offerUrl}", e)
                }
                
                clickListener?.onClick(view, position, false)
            }
        }

        override fun onLongClick(view: View): Boolean {
            clickListener?.onClick(view, adapterPosition, true)
            return true
        }
    }

    private inner class OfferImageViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {

        private val imageOffer: ImageView = itemView.findViewById(R.id.imageOffer)
        private val textOfferTitle: TextView = itemView.findViewById(R.id.textOfferTitle)
        private val textOfferPrice: TextView = itemView.findViewById(R.id.textOfferPrice)
        private val textOfferLocation: TextView = itemView.findViewById(R.id.textOfferLocation)
        private val textOfferDate: TextView = itemView.findViewById(R.id.textOfferDate)

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
            
            itemView.isClickable = true
            itemView.isFocusable = true
        }

        fun setData(offerImage: OfferImage) {
            Picasso.get()
                .load(offerImage.offerImageUrl)
                .into(imageOffer, object : Callback {
                    override fun onSuccess() {
                        Log.d(sTag, "Image loaded successfully: ${offerImage.offerImageUrl}")
                    }

                    override fun onError(e: Exception?) {
                        Log.e(sTag, "Error loading image: ${offerImage.offerImageUrl}", e)
                    }
                })

            textOfferTitle.text = offerImage.offerTitle
            textOfferPrice.text = offerImage.offerPrice
            textOfferLocation.text = offerImage.offerLocation
            textOfferDate.text = offerImage.offerDate
        }

        override fun onClick(view: View) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val offerImage = items[position].`object` as OfferImage

                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(offerImage.offerUrl))
                    fragment.startActivity(intent)
                } catch (e: Exception) {
                    Log.e(sTag, "Error opening URL from ViewHolder: ${offerImage.offerUrl}", e)
                }
                
                clickListener?.onClick(view, position, false)
            }
        }

        override fun onLongClick(view: View): Boolean {
            clickListener?.onClick(view, adapterPosition, true)
            return true
        }
    }

    // GETTERS.

    override fun getItemCount(): Int {
        val count = items.size
        return count
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].type
    }

    // OTHER FUNCTIONS.

    @ExperimentalStdlibApi
    override fun updateOffers(itemIndex: Int) {
        if (shownIndex == itemIndex) {
            val context = recyclerView.context as AppCompatActivity

            var fabRefreshOffers: FloatingActionButton? = null
            context.runOnUiThread { fabRefreshOffers = context.findViewById(R.id.fabRefreshOffers) }

            // If can't scroll up...
            if (!recyclerView.canScrollVertically(-1)) {
                items.add(
                    0,
                    itemsContent[itemIndex][0]
                )

                context.runOnUiThread {
                    fabRefreshOffers?.visibility = View.GONE
                    notifyItemRangeChanged(0, items.size - 1)
                    recyclerView.scrollToPosition(0)
                }
            } else {
                offersFragmentTemp.add(0, itemsContent[itemIndex][0])
                context.runOnUiThread { fabRefreshOffers?.visibility = View.VISIBLE }
            }

            // Needed for infinite scrolling to work properly, without repeating already contained
            // offers, and without extensive network usage.
            if (items.size > 39) {
                items.removeLast()
                context.runOnUiThread { notifyItemRemoved(items.size - 1) }
            }
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {
        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        override fun onClick(view: View) {
            clickListener?.onClick(view, adapterPosition, false)
        }

        override fun onLongClick(view: View): Boolean {
            clickListener?.onClick(view, adapterPosition, true)
            return true
        }
    }

    private fun openOfferLink(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            fragment.startActivity(intent)
        } catch (e: Exception) {
            Log.e(sTag, "Error opening URL: $url", e)
        }
    }

    fun logItems() {
        for (i in 0 until minOf(items.size, 5)) {
            when (items[i].type) {
                OfferViewTypes.OfferText.ordinal -> {
                    val offerText = items[i].`object` as OfferText
                }
                else -> {
                    val offerImage = items[i].`object` as OfferImage
                }
            }
        }
    }
}

fun getHTML(i: Int, firstUrlPart: String, pageIndex: Int = -1): Document? {
    if (itemsFragment.isEmpty())
        return null

    if (i < itemsFragment.size) {
        var url = firstUrlPart
        when (itemsFragment[i].type) {
            ItemViewTypes.ItemTitle.ordinal -> {
                url += (itemsFragment[i].`object` as ItemTitle).itemTitle
            }
            else -> {
                url = (itemsFragment[i].`object` as ItemLink).itemLink
            }
        }

        url = url.replace(" ", "-")
        if (url.last().toString() != "/") url += "/"

        if (pageIndex != -1) {
            url += if ("search" in url) "&page=$pageIndex"
            else "?page=$pageIndex"
        }

        fun getDoc(url: String?): Document? {
            return try {
                Jsoup.connect(url).get()
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }

        return getDoc(url)
    }

    return null
}

fun getOffers(html: Document?): ArrayList<Item> {
    val offerItems = ArrayList<Item>()
    
    val offersHtml = html?.select("div[data-cy=l-card]")

    if (offersHtml != null) {
        for (offerContent in offersHtml) {
            val imageUrl = offerContent.select("img.css-8wsg1m").first()
            val title = offerContent.select("h4.css-1g61gc2").first()
            val price = offerContent.select("p[data-testid=ad-price]").first()
            
            val locationDate = offerContent.select("p[data-testid=location-date]").first()?.text()
            val locationDateParts = locationDate?.split(" - ")
            
            val locationText = locationDateParts?.firstOrNull() ?: "Brak lokalizacji"
            val dateText = locationDateParts?.lastOrNull() ?: "Brak daty"
            
            // URL is in the href attribute of the main link
            val url = offerContent.select("a.css-1tqlkj0").first()

            var imageUrlText: String = imageUrl?.attr("src").toString()
            var titleText: String = title?.text().toString()
            var priceText: String = price?.ownText().toString() // Using ownText to get just the price without negotiation text
            var urlText: String = url?.attr("href").toString()
            
            if (!urlText.startsWith("http")) {
                urlText = "https://www.olx.pl" + urlText
            }

            if (imageUrlText == "null") imageUrlText = "Brak obrazu"
            if (titleText == "null") titleText = "Brak tytułu"
            if (priceText == "null") priceText = "Brak ceny"

            if (imageUrl == null) {
                offerItems.add(
                    Item(
                        0,
                        OfferText(
                            titleText,
                            priceText,
                            locationText,
                            dateText,
                            urlText
                        )
                    )
                )
            } else {
                offerItems.add(
                    Item(
                        1,
                        OfferImage(
                            imageUrlText,
                            titleText,
                            priceText,
                            locationText,
                            dateText,
                            urlText
                        )
                    )
                )
            }
        }
    }

    return offerItems
}
