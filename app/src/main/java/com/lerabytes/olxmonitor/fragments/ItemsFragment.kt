package com.lerabytes.olxmonitor.fragments

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.lerabytes.olxmonitor.R
import com.lerabytes.olxmonitor.activities.OffersActivity
import com.lerabytes.olxmonitor.adapters.ItemsAdapter
import com.lerabytes.olxmonitor.models.Item
import com.lerabytes.olxmonitor.models.ItemLink
import com.lerabytes.olxmonitor.models.ItemTitle
import com.lerabytes.olxmonitor.services.MonitorOffersService
import com.lerabytes.olxmonitor.services.isMonitorOffersServiceStarted
import java.lang.Thread.sleep
import kotlin.concurrent.thread


// These can't be inside class, because they will reset on each object created.
var shownIndex = -1
var clickedOptionsIndex = 0
var isDualPane: Boolean = false

val itemsFragment = ArrayList<Item>()
val itemsFragmentImportant = ArrayList<Item>()
var itemsContent = ArrayList<ArrayList<Item>>()

var serviceIntent: Intent? = null
var monitorOffersService: MonitorOffersService? = null
var itemsRecyclerView: RecyclerView? = null

class ItemsFragment : Fragment(), ItemsAdapter.ItemClickListener {
    private val sTag = "ItemsFragment"
    private var adapter: ItemsAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (!isMonitorOffersServiceStarted) startCheckOffersService()

        // Inflate the layout for this fragment.
        val inflaterView = inflater.inflate(R.layout.fragment_items, container, false)
        val recyclerView = inflaterView.findViewById<RecyclerView>(R.id.recyclerItems)

        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = addExampleItems(itemsFragment, recyclerView)
        adapter = recyclerView.adapter as ItemsAdapter
        adapter?.setClickListener(this)
        
        setupSwipeToDelete(recyclerView)

        // Disable notifyItemChanged animation.
        recyclerView.itemAnimator = null
        itemsRecyclerView = recyclerView

        // Reset shownIndex. It should always be -1 when Offers Activity isn't shown to the user.
        if (!isDualPane) shownIndex = -1

        // Inflate the layout for this fragment.
        return inflaterView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val detailsFrame: View? = activity?.findViewById(R.id.frameOffers)
        isDualPane = detailsFrame != null && detailsFrame.visibility == View.VISIBLE

        if (isDualPane) {
            thread {
                while (true) {
                    if (itemsContent.isNotEmpty()) {
                        while (itemsContent[0].isEmpty()) {
                            sleep(1)
                        }
                        break
                    }
                }

                shownIndex = 0
                
                if (itemsFragment.isNotEmpty() && itemsFragment[0] in itemsFragmentImportant) {
                    activity?.runOnUiThread {
                        removeItemImportant(0)
                    }
                }

                if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
                    OffersFragment().showDualPaneOffers(activity)
            }
        }
    }

    private fun addExampleItems(
        items: ArrayList<Item>,
        recyclerView: RecyclerView
    ): ItemsAdapter {
        // Types: 0-ItemTitle, 1-ItemLink.

//        // ItemTitle.
//        val itemTitle1 =
//            ItemTitle("Buty")
//
//        // ItemLink.
//        val itemLink1 = ItemLink(
//            "Buty",
//            "https://www.olx.pl/oferty/q-buty"
//        )

        if (items.isEmpty()) {
            items.add(Item(0, ItemTitle("Ryzen 3 1200")))
            items.add(Item(0, ItemTitle("Ryzen 5 3600")))
            items.add(Item(1, ItemLink("Buty", "https://www.olx.pl/oferty/q-Buty/")))
            itemsContent.add(ArrayList())
            itemsContent.add(ArrayList())
            itemsContent.add(ArrayList())
//            items.add(Item(0, ItemTitle("Lays")))
//            items.add(Item(0, ItemTitle("Bilbord")))
//            items.add(Item(0, ItemTitle("Kubek")))
//            items.add(Item(0, ItemTitle("Samochód")))
//            items.add(Item(0, ItemTitle("Rower")))
//            items.add(Item(1, ItemLink("Auto (RO)", "https://www.olx.ro/oferte/q-Auto/")))
//            items.add(Item(0, ItemTitle("Auto")))
//            items.add(Item(1, ItemLink("Toster", "https://www.olx.pl/oferty/q-Toster/")))
//            items.add(Item(0, ItemTitle("Auto")))

//            for (i in 0..20) {
//                if (i % 2 == 0)
//                    items.add(Item(0, ItemTitle("Buty $i")))
//                else
//                    items.add(
//                        Item(
//                            1, ItemLink(
//                                "Buty $i",
//                                "https://www.olx.pl/oferty/q-buty-$i/"
//                            )
//                        )
//                    )
//            }

//            for (i in 0 until items.size) {
//                if (i % 3 == 0) {
//                    itemsFragmentImportant.add(items[i])
//                }
//            }
        }

        return ItemsAdapter(items, itemsFragmentImportant, recyclerView)
    }

    private fun startCheckOffersService() {
        serviceIntent = Intent(context, MonitorOffersService::class.java)

        val mConnection = object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, service: IBinder) {
                Log.d(sTag, "onServiceConnected called.")

                val binder: MonitorOffersService.LocalBinder =
                    service as MonitorOffersService.LocalBinder

                monitorOffersService = binder.getServiceInstance()

                Log.d(sTag, "Connected to service.")
            }

            override fun onServiceDisconnected(arg0: ComponentName) {
                Log.d(sTag, "onServiceDisconnected called.")
                Log.d(sTag, "Disconnected from service.")
            }
        }

        context?.startService(serviceIntent)
        context?.bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE)
    }

    fun setItemImportant(i: Int) {
        if (itemsFragment[i] !in itemsFragmentImportant) {
            val context = itemsRecyclerView?.context as AppCompatActivity
            itemsFragmentImportant.add(itemsFragment[i])
            context.runOnUiThread { itemsRecyclerView?.adapter?.notifyItemChanged(i) }
        }
    }

    fun removeItemImportant(i: Int) {
        val context = itemsRecyclerView?.context as AppCompatActivity
        itemsFragmentImportant.remove(itemsFragment[i])
        context.runOnUiThread { itemsRecyclerView?.adapter?.notifyItemChanged(i) }
    }

    override fun onClick(view: View, position: Int, isLongClick: Boolean) {
        if (position < 0 || position >= itemsFragment.size) {
            Log.e(sTag, "Invalid position: $position")
            return
        }

        shownIndex = position

        if (itemsFragment[position] in itemsFragmentImportant) {
            removeItemImportant(position)
        }

        if (isDualPane) {
            val offersFragment = OffersFragment()
            offersFragment.showDualPaneOffers(activity)
        } else {
            val intent = Intent(activity, OffersActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupSwipeToDelete(recyclerView: RecyclerView) {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView, 
                viewHolder: RecyclerView.ViewHolder, 
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val removedItem = itemsFragment[position]
                
                itemsFragment.removeAt(position)
                itemsFragmentImportant.remove(removedItem)
                adapter?.notifyItemRemoved(position)
                
                view?.let {
                    val snackbar = Snackbar.make(
                        it,
                        getString(R.string.gesture_item_removed),
                        Snackbar.LENGTH_LONG
                    )
                    snackbar.setAction(getString(R.string.gesture_undo)) {
                        itemsFragment.add(position, removedItem)
                        if (removedItem in itemsFragmentImportant) {
                            itemsFragmentImportant.add(removedItem)
                        }
                        adapter?.notifyItemInserted(position)
                    }
                    snackbar.show()
                }
            }
        }
        
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }
}