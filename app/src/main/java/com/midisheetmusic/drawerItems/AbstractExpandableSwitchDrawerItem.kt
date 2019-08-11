package com.midisheetmusic.drawerItems

import com.mikepenz.materialdrawer.model.AbstractDrawerItem
import com.mikepenz.materialdrawer.model.BaseDescribeableDrawerItem
import com.mikepenz.materialdrawer.model.BaseViewHolder

import com.midisheetmusic.R
import android.graphics.Color
import android.view.View
import android.widget.CompoundButton
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import com.mikepenz.iconics.IconicsColor
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.IconicsSize
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.holder.ColorHolder
import com.mikepenz.materialdrawer.icons.MaterialDrawerFont
import com.mikepenz.materialdrawer.interfaces.OnCheckedChangeListener
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem

/**
 * Created by ditek on 09-Jul-19
 */
abstract class AbstractExpandableSwitchDrawerItem<Item : AbstractExpandableSwitchDrawerItem<Item>> : BaseDescribeableDrawerItem<Item, AbstractExpandableSwitchDrawerItem.ViewHolder>() {

    override val type: Int
        get() = R.id.drawer_item_expandable_switch

    override val layoutRes: Int
        @LayoutRes
        get() = R.layout.drawer_item_expandable_switch


    /* *************************************/
    /* Switch Specific Part                */
    /* *************************************/

    var isSwitchEnabled = true
    var isChecked = false
    var onCheckedChangeListener: OnCheckedChangeListener? = null

    private val checkedChangeListener = object : CompoundButton.OnCheckedChangeListener {
        override fun onCheckedChanged(buttonView: CompoundButton, ic: Boolean) {
            if (isEnabled) {
                isChecked = ic
                onCheckedChangeListener?.onCheckedChanged(this@AbstractExpandableSwitchDrawerItem, buttonView, ic)
            } else {
                buttonView.setOnCheckedChangeListener(null)
                buttonView.isChecked = !ic
                buttonView.setOnCheckedChangeListener(this)
            }
        }
    }

    fun withChecked(checked: Boolean): Item {
        this.isChecked = checked
        return this as Item
    }

    fun withSwitchEnabled(switchEnabled: Boolean): Item {
        this.isSwitchEnabled = switchEnabled
        return this as Item
    }

    fun withOnCheckedChangeListener(onCheckedChangeListener: OnCheckedChangeListener): Item {
        this.onCheckedChangeListener = onCheckedChangeListener
        return this as Item
    }

    fun withCheckable(checkable: Boolean): Item {
        return withSelectable(checkable)
    }


    /* *************************************/
    /* Expandable Specific Part                */
    /* *************************************/

    var mOnDrawerItemClickListener: Drawer.OnDrawerItemClickListener? = null
    var arrowColor: ColorHolder? = null
    var arrowRotationAngleStart = 0
    var arrowRotationAngleEnd = 180

    /**
     * our internal onDrawerItemClickListener which will handle the arrow animation
     */
    override var onDrawerItemClickListener: Drawer.OnDrawerItemClickListener? = object : Drawer.OnDrawerItemClickListener {
        override fun onItemClick(view: View?, position: Int, drawerItem: IDrawerItem<*>): Boolean {
            if (drawerItem is AbstractDrawerItem<*, *> && drawerItem.isEnabled) {
                view?.let {
                    if (drawerItem.isExpanded) {
                        ViewCompat.animate(view.findViewById(R.id.material_drawer_arrow)).rotation(this@AbstractExpandableSwitchDrawerItem.arrowRotationAngleEnd.toFloat()).start()
                    } else {
                        ViewCompat.animate(view.findViewById(R.id.material_drawer_arrow)).rotation(this@AbstractExpandableSwitchDrawerItem.arrowRotationAngleStart.toFloat()).start()
                    }
                }
            }

            return mOnDrawerItemClickListener?.onItemClick(view, position, drawerItem) ?: false
        }
    }

    fun withArrowColor(@ColorInt arrowColor: Int): Item {
        this.arrowColor = ColorHolder.fromColor(arrowColor)
        return this as Item
    }

    fun withArrowColorRes(@ColorRes arrowColorRes: Int): Item {
        this.arrowColor = ColorHolder.fromColorRes(arrowColorRes)
        return this as Item
    }

    fun withArrowRotationAngleStart(angle: Int): Item {
        this.arrowRotationAngleStart = angle
        return this as Item
    }

    fun withArrowRotationAngleEnd(angle: Int): Item {
        this.arrowRotationAngleEnd = angle
        return this as Item
    }

    override fun withOnDrawerItemClickListener(onDrawerItemClickListener: Drawer.OnDrawerItemClickListener): Item {
        mOnDrawerItemClickListener = onDrawerItemClickListener
        return this as Item
    }

    override fun bindView(holder: ViewHolder, payloads: MutableList<Any>) {
        super.bindView(holder, payloads)

        val ctx = holder.itemView.context
        //bind the basic view parts
        bindViewHelper(holder)

        //handle the switch
        holder.switchView.setOnCheckedChangeListener(null)
        holder.switchView.isChecked = isChecked
        holder.switchView.setOnCheckedChangeListener(checkedChangeListener)
        holder.switchView.isEnabled = isSwitchEnabled

        //add a onDrawerItemClickListener here to be able to check / uncheck if the drawerItem can't be selected
        withOnDrawerItemClickListener(object : Drawer.OnDrawerItemClickListener {
            override fun onItemClick(view: View?, position: Int, drawerItem: IDrawerItem<*>): Boolean {
                if (!isSelectable) {
                    isChecked = !isChecked
                    holder.switchView.isChecked = isChecked
                }

                return false
            }
        })

        //make sure all animations are stopped
        if (holder.arrow.drawable is IconicsDrawable) {
            (holder.arrow.drawable as IconicsDrawable).color(IconicsColor.colorInt(this.arrowColor?.color(ctx)
                    ?: getIconColor(ctx)))
        }
        holder.arrow.clearAnimation()
        if (!isExpanded) {
            holder.arrow.rotation = this.arrowRotationAngleStart.toFloat()
        } else {
            holder.arrow.rotation = this.arrowRotationAngleEnd.toFloat()
        }

        //call the onPostBindView method to trigger post bind view actions (like the listener to modify the item if required)
        onPostBindView(this, holder.itemView)
    }

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(v)
    }

    class ViewHolder(view: View) : BaseViewHolder(view) {
        internal val switchView: SwitchCompat = view.findViewById<View>(R.id.material_drawer_switch) as SwitchCompat
        var arrow: ImageView = view.findViewById(R.id.material_drawer_arrow)

        init {
            arrow.setImageDrawable(IconicsDrawable(view.context, MaterialDrawerFont.Icon.mdf_expand_more).size(IconicsSize.dp(16)).padding(IconicsSize.dp(2)).color(IconicsColor.colorInt(Color.BLACK)))
        }
    }
}
