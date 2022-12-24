package com.lerabytes.olxmonitor.activities

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lerabytes.olxmonitor.R
import com.lerabytes.olxmonitor.enums.ItemViewTypes
import com.lerabytes.olxmonitor.fragments.itemsContent
import com.lerabytes.olxmonitor.fragments.itemsFragment
import com.lerabytes.olxmonitor.models.*

class AddItemActivity : AppCompatActivity(), OnItemSelectedListener {
    private val sTag = "AddItemActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_item)
        createSpinner(R.id.spinnerItemType, R.array.itemtype_array)
        createSpinner(R.id.spinnerService, R.array.service_array)
    }

    private fun createSpinner(id: Int, textArrayResId: Int) {
        // Create an ArrayAdapter using the string array and a default spinner layout.
        val adapter = ArrayAdapter.createFromResource(
            this,
            textArrayResId, android.R.layout.simple_spinner_dropdown_item
        )
        // Specify the layout to use when the list of choices appears.
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        val spinner = findViewById<Spinner>(id)
        // Apply the adapter to the spinner.
        spinner.adapter = adapter
        // Set listeners.
        spinner.onItemSelectedListener = this
    }

    private fun existsInItemsFragment(nameTitle: String, link: String): Boolean {
        for (item in itemsFragment) {
            when (item.type) {
                ItemViewTypes.ItemTitle.ordinal ->
                    if (nameTitle == (item.`object` as ItemTitle).itemTitle && link.isEmpty())
                        return true
                else ->
                    if (nameTitle == (item.`object` as ItemLink).itemName && link == item.`object`.itemLink && link.isNotEmpty())
                        return true
            }
        }

        return false
    }

    fun addItem(view: View?) {
        val ptItemNameTitle = findViewById<TextView>(R.id.ptItemNameTitle)
        val ptItemLink = findViewById<TextView>(R.id.ptItemLink)
        val itemNameTitle = ptItemNameTitle.text.toString()
        var itemLink = ptItemLink.text.toString()

        val spinnerItemType = findViewById<Spinner>(R.id.spinnerItemType)
        val itemType = spinnerItemType.selectedItem.toString()

        fun removePageFromLink() {
            itemLink = Regex("\\?page=.").replace(itemLink, "")
            itemLink = Regex("&page=.").replace(itemLink, "")
        }

        if (itemType == getString(R.string.additem_title)) {
            if (itemNameTitle.isEmpty()) {
                createNotification("Please enter item title.")
                return
            }
        } else if (itemNameTitle.isEmpty() || itemLink.isEmpty()) {
            createNotification("Please enter item name and item link.")
            return
        } else if (!Patterns.WEB_URL.matcher(itemLink)
                .matches() || "olx.pl" !in itemLink
        ) {
            createNotification("Please enter valid URL.")
            return
        }

        if (existsInItemsFragment(itemNameTitle, itemLink)) {
            createNotification("$itemNameTitle already exists in item list.")
            return
        }

        removePageFromLink()

        if (itemType == getString(R.string.additem_title)) {
            itemsFragment.add(Item(0, ItemTitle(itemNameTitle)))
            itemsContent.add(ArrayList())
        } else {
            itemsFragment.add(Item(1, ItemLink(itemNameTitle, itemLink)))
            itemsContent.add(ArrayList())
        }

        createNotification("Successfully added $itemNameTitle to the item list.")
    }

    private fun createNotification(message: String?) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
        val spinnerItemType = findViewById<Spinner>(R.id.spinnerItemType)
        val spinnerService = findViewById<Spinner>(R.id.spinnerService)

        val itemType = spinnerItemType.selectedItem.toString()

        val ptItemLink = findViewById<TextView>(R.id.ptItemLink)

        val tvItemNameTitle = findViewById<TextView>(R.id.tvItemTitleName)
        val tvItemLink = findViewById<TextView>(R.id.tvItemHyperlink)
        val tvService = findViewById<TextView>(R.id.tvService)

        if (itemType == getString(R.string.additem_title)) {
            tvService.visibility = View.VISIBLE
            spinnerService.visibility = View.VISIBLE
            ptItemLink.visibility = View.GONE
            tvItemLink.visibility = View.GONE

            tvItemNameTitle.text = getString(R.string.additem_title)
//            ptItemHyperlink.text = ""

//            tvService.setAlpha(1);
//
//            spinnerService.setEnabled(true);
//            spinnerService.setAlpha(1);
//
//            ptItem.setHint(getString(R.string.additem_title));
//
//            ptName.setEnabled(false);
//            ptName.setAlpha((float) 0.5);
        } else {
            tvService.visibility = View.GONE
            spinnerService.visibility = View.GONE
            ptItemLink.visibility = View.VISIBLE
            tvItemLink.visibility = View.VISIBLE

            tvItemNameTitle.text = getString(R.string.additem_name)
//            ptItemHyperlink.text = ""
            ptItemLink.hint =
                getString(R.string.additem_hyperlink_example)

//            tvService.setAlpha((float) 0.5);
//
//            spinnerService.setEnabled(false);
//            spinnerService.setAlpha((float) 0.5);
//
//            ptItem.setHint(getString(R.string.additem_hyperlink) + " (" + getString(R.string.additem_hyperlink_example) + ")");
//
//            ptName.setEnabled(true);
//            ptName.setAlpha(1);
        }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {}
}
