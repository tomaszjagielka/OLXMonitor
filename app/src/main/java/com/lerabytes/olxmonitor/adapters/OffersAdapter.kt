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
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.chootdev.recycleclick.RecycleClick
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
import kotlinx.android.synthetic.main.fragment_offers.*
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
    private val layoutManager: StaggeredGridLayoutManager
) :
    RecyclerView.Adapter<ViewHolder>(), MonitorOffersService.Callbacks {
    private val sTag = "OffersAdapter"

    init {
        fun setRecyclerClickListener() {
            RecycleClick.addTo(recyclerView)
                .setOnItemClickListener { recyclerView, position, v ->
                    fun recyclerItemClicked(
                        position: Int
                    ) {
                        val url = when (getItemViewType(position)) {
                            OfferViewTypes.OfferText.ordinal -> {
                                (items[position].`object` as OfferText).offerUrl
                            }
                            else -> {
                                (items[position].`object` as OfferImage).offerUrl
                            }
                        }

                        val browse = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(url)
                        )

                        recyclerView.context.startActivity(browse)
                    }

                    recyclerItemClicked(position)
                }
        }

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
        setRecyclerClickListener()
        setInfiniteScrolling()
        registerOffersFragmentCallbacks()
        setShownIndex()
        addFirstOffers()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        fun setRefreshOffersFabClickListener() {
            val context = recyclerView.context as AppCompatActivity
            val fabRefreshOffers: FloatingActionButton = context.findViewById(R.id.fabRefreshOffers)

            fabRefreshOffers.setOnClickListener {
                fabRefreshOffers.visibility = View.GONE
                items.addAll(0, offersFragmentTemp)
                notifyDataSetChanged()
                recyclerView.scrollToPosition(0)

                offersFragmentTemp.clear()
            }
        }

        setRefreshOffersFabClickListener()

        when (viewType) {
            OfferViewTypes.OfferText.ordinal -> {

                return OfferTextViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.offer_container_text,
                        parent,
                        false
                    )
                )
            }
            else -> {

                return OfferImageViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.offer_container_image,
                        parent,
                        false
                    )
                )
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        when (getItemViewType(position)) {
            OfferViewTypes.OfferText.ordinal -> {
                val offerText = items[position].`object` as OfferText
                (holder as OfferTextViewHolder).setData(
                    offerText
                )
            }
            else -> {
                val offerImage = items[position].`object` as OfferImage
                (holder as OfferImageViewHolder).setData(
                    offerImage
                )
            }
        }
    }

    // VIEWHOLDERS.

    private class OfferTextViewHolder(itemView: View) :
        ViewHolder(itemView) {

        private val textOfferTitle: TextView = itemView.findViewById(R.id.textOfferTitle)
        private val textOfferPrice: TextView = itemView.findViewById(R.id.textOfferPrice)
        private val textOfferLocation: TextView = itemView.findViewById(R.id.textOfferLocation)
        private val textOfferDate: TextView = itemView.findViewById(R.id.textOfferDate)

        fun setData(offerText: OfferText) {
            textOfferTitle.text = offerText.offerTitle
            textOfferPrice.text = offerText.offerPrice
            textOfferLocation.text = offerText.offerLocation
            textOfferDate.text = offerText.offerDate
        }
    }

    private inner class OfferImageViewHolder(itemView: View) :
        ViewHolder(itemView) {

        private val imageOffer: ImageView = itemView.findViewById(R.id.imageOffer)
        private val textOfferTitle: TextView = itemView.findViewById(R.id.textOfferTitle)
        private val textOfferPrice: TextView = itemView.findViewById(R.id.textOfferPrice)
        private val textOfferLocation: TextView = itemView.findViewById(R.id.textOfferLocation)
        private val textOfferDate: TextView = itemView.findViewById(R.id.textOfferDate)

        fun setData(offerImage: OfferImage) {
            Picasso.get()
                .load(offerImage.offerImageUrl)
                .into(imageOffer, object : Callback {
                    override fun onSuccess() {}

                    override fun onError(e: Exception?) {
                        Log.d("Error loading images.", e.toString())
                    }
                })

            textOfferTitle.text = offerImage.offerTitle
            textOfferPrice.text = offerImage.offerPrice
            textOfferLocation.text = offerImage.offerLocation
            textOfferDate.text = offerImage.offerDate
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
    val offersHtml = html?.select("#offers_table tbody")

    // Delete duplicate.
    offersHtml?.removeAt(0)

    if (offersHtml != null) {
        for (offerContent in offersHtml) {
            val imageUrl = offerContent.select("img").first()
            val title = offerContent.select("a > strong").first()
            val price = offerContent.select("p > strong").first()
            val location =
                offerContent.select("small.x-normal.breadcrumb:nth-of-type(1) > span")
                    .first()
            val date =
                offerContent.select("small.x-normal.breadcrumb:nth-of-type(2) > span")
                    .first()
            val url = offerContent.select("a").first()

            var imageUrlText: String = imageUrl?.absUrl("src").toString()
            var titleText: String = title?.text().toString()
            var priceText: String = price?.text().toString()
            var locationText: String = location?.text().toString()
            var dateText: String = date?.text().toString()
            var urlText: String = url?.absUrl("href").toString()

            if (imageUrlText == "null") imageUrlText = "Brak obrazu"
            if (titleText == "null") titleText = "Brak tytułu"
            if (priceText == "null") priceText = "Brak ceny"
            if (locationText == "null") locationText = "Brak lokalizacji"
            if (dateText == "null") dateText = "Brak daty"
            if (urlText == "null") urlText = "https://www.olx.pl/"

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
