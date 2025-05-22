package com.lerabytes.olxmonitor.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.*
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.lerabytes.olxmonitor.R
import com.lerabytes.olxmonitor.adapters.OffersAdapter
import com.lerabytes.olxmonitor.adapters.offersLastPageIndex
import com.lerabytes.olxmonitor.adapters.offersPageIndex
import com.lerabytes.olxmonitor.adapters.getHTML
import com.lerabytes.olxmonitor.adapters.getOffers
import com.lerabytes.olxmonitor.enums.OfferViewTypes
import com.lerabytes.olxmonitor.models.*
import com.lerabytes.olxmonitor.adapters.OffersAdapter.ItemClickListener
import kotlin.concurrent.thread


// These can't be inside class, because they will reset on each object created.
var offersFragment = ArrayList<Item>()
var offersFragmentTemp = ArrayList<Item>()
private var oldPosition = 0

class OffersFragment : Fragment(), ItemClickListener {
    private val sTag = "OffersFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val inflaterView = inflater.inflate(R.layout.fragment_offers, container, false)
        val recyclerView = inflaterView.findViewById<RecyclerView>(R.id.recyclerOffers)
        
        recyclerView.isClickable = true
        recyclerView.isFocusable = true
        
        val layoutManager = StaggeredGridLayoutManager(
            calculateNoOfColumns(150),
            StaggeredGridLayoutManager.VERTICAL
        )

        recyclerView.layoutManager = layoutManager
        val adapter = addRealItems(recyclerView, layoutManager)
        
        adapter.setClickListener(this)
        
        recyclerView.adapter = adapter
        
        adapter.logItems()

        recyclerView.setHasFixedSize(true)

        recyclerView.setOnClickListener { 
            Log.d(sTag, "RecyclerView clicked directly")
        }

        Log.d(sTag, "RecyclerView setup complete, items count: ${offersFragment.size}")

        return inflaterView
    }

//    private fun setTitle() {
//        val context = context as AppCompatActivity
//
//        when (itemsFragment[shownIndex].type) {
//            ItemViewTypes.ItemTitle.ordinal -> {
//                context.title = (itemsFragment[shownIndex].`object` as ItemTitle).itemTitle
//            }
//            else -> {
//                context.title = (itemsFragment[shownIndex].`object` as ItemLink).itemName
//            }
//        }
//    }

    private fun calculateNoOfColumns(itemWidth: Int): Int {
        val displayMetrics: DisplayMetrics = resources.displayMetrics
        var dpWidth = displayMetrics.widthPixels / displayMetrics.density
        
        if (isDualPane) {
            // Get dp width of Offers fragment by subtracting dp width of Items fragment.
            // Need to hardcode it, because fragments are not yet initialized.
            dpWidth -= 300
        }

        var result: Int = (dpWidth / itemWidth).toInt()
        if (result == 0) result = 1
        
        return result
    }

    private fun addRealItems(
        recyclerView: RecyclerView,
        layoutManager: StaggeredGridLayoutManager
    ): OffersAdapter {       
        if (shownIndex == -1) shownIndex = 0

        if (shownIndex < itemsContent.size) {
              
            if (shownIndex != oldPosition) {
                offersPageIndex = 2
                offersLastPageIndex = 25
                offersFragment.clear()
            }

            if (itemsContent[shownIndex].isNotEmpty() && offersFragment.isEmpty()) {
                offersFragment.addAll(itemsContent[shownIndex])

                if (itemsFragment[shownIndex] in itemsFragmentImportant) {
                    itemsFragmentImportant.remove(itemsFragment[shownIndex])
                }
            } else if (offersFragment.isEmpty()) {
                val tempAdapter = OffersAdapter(offersFragment, recyclerView, layoutManager, this)
                
                activity?.let { context ->
                    thread {
                        val offers = getOffers(
                            getHTML(
                                shownIndex,
                                context.getString(R.string.first_url_part)
                            )
                        )
                        
                        Log.d(sTag, "Loaded ${offers.size} offers")
                        
                        if (itemsContent.size > shownIndex) {
                            Log.d(sTag, "Adding offers to itemsContent[$shownIndex]")
                            itemsContent[shownIndex].addAll(offers)
                        }
                        
                        offersFragment.addAll(offers)
                        
                        context.runOnUiThread { 
                            Log.d(sTag, "Notifying adapter of new items")
                            recyclerView.adapter?.notifyItemRangeChanged(0, offers.size) 
                        }
                    }
                }
            }

            oldPosition = shownIndex
        } else {
            Log.e(sTag, "shownIndex ($shownIndex) out of bounds for itemsContent (size: ${itemsContent.size})")
        }

        val adapter = OffersAdapter(offersFragment, recyclerView, layoutManager, this)
        Log.d(sTag, "Created adapter with ${offersFragment.size} items")
        return adapter
    }

//    private fun addExampleItems(
//        recyclerView: RecyclerView,
//        layoutManager: StaggeredGridLayoutManager
//    ): OffersAdapter {
//        if (offersFragment.isEmpty()) {
//            for (i in 0..22) {
//                if (i % 2 == 0) {
//                    offersFragment.add(
//                        Item(
//                            0,
//                            OfferText(
//                                "Claas Lexion Medion Tucano Mega Dominator John Dere Fendt Dronninborg $i",
//                                "${100 + i} zł",
//                                "Poznań $i",
//                                "Dzisiaj 10:00"
//                            )
//                        )
//                    )
//                } else {
//                    offersFragment.add(
//                        Item(
//                            1,
//                            OfferImage(
//                                "https://ireland.apollo.olxcdn.com/v1/files/cw9p6dkacruk3-PL/image;s=1000x1000",
//                                "Claas Lexion Medion Tucano Mega Dominator John Dere Fendt Dronninborg $i",
//                                "${100 + i} zł",
//                                "Poznań $i",
//                                "Dzisiaj 10:00"
//                            )
//                        )
//                    )
//                    offersFragment.add(
//                        Item(
//                            1,
//                            OfferImage(
//                                "https://ireland.apollo.olxcdn.com/v1/files/uwj818x5m5mf3-PL/image;s=1000x1000",
//                                "Claas Lexion Medion Tucano Mega Dominator John Dere Fendt Dronninborg $i",
//                                "${100 + i} zł",
//                                "Poznań $i",
//                                "Dzisiaj 10:00"
//                            )
//                        )
//                    )
//                    offersFragment.add(
//                        Item(
//                            1,
//                            OfferImage(
//                                "https://ireland.apollo.olxcdn.com/v1/files/f9v1u3z9w5em1-PL/image;s=1000x700",
//                                "Claas Lexion Medion Tucano Mega Dominator John Dere Fendt Dronninborg $i",
//                                "${100 + i} zł",
//                                "Poznań $i",
//                                "Dzisiaj 10:00"
//                            )
//                        )
//                    )
//                    offersFragment.add(
//                        Item(
//                            1,
//                            OfferImage(
//                                "https://ireland.apollo.olxcdn.com/v1/files/698vg0c08q803-PL/image;s=1000x700",
//                                "Claas Lexion Medion Tucano Mega Dominator John Dere Fendt Dronninborg $i",
//                                "${100 + i} zł",
//                                "Poznań $i",
//                                "Dzisiaj 10:00"
//                            )
//                        )
//                    )
//                }
//            }
//        }
//
//        return OffersAdapter(offersFragment, importantOffers, recyclerView, layoutManager)
//    }

    private fun newInstance(): OffersFragment {
        val fragmentOffers = OffersFragment()
        val args = Bundle()
        fragmentOffers.arguments = args
        return fragmentOffers
    }

    fun showDualPaneOffers(context: FragmentActivity?) {
        val fm: FragmentManager? = context?.supportFragmentManager
        val ft: FragmentTransaction? = fm?.beginTransaction()
        val fragmentOffers: OffersFragment = OffersFragment().newInstance()
        ft?.replace(R.id.frameOffers, fragmentOffers)
        ft?.commit()
    }

    override fun onClick(view: View, position: Int, isLongClick: Boolean) {
        if (isLongClick) {
            return
        }
        
        if (position < offersFragment.size) {
            try {
                val url = when (offersFragment[position].type) {
                    OfferViewTypes.OfferText.ordinal -> {
                        val offerText = offersFragment[position].`object` as OfferText
                        offerText.offerUrl
                    }
                    else -> {
                        val offerImage = offersFragment[position].`object` as OfferImage
                        offerImage.offerUrl
                    }
                }
                
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(sTag, "Error opening URL for position $position", e)
            }
        } else {
            Log.e(sTag, "Invalid position: $position, offersFragment size: ${offersFragment.size}")
        }
    }
}