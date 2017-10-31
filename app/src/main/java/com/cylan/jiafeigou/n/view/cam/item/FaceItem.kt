package com.cylan.jiafeigou.n.view.cam.item

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.cylan.jiafeigou.R
import com.cylan.jiafeigou.dp.DpMsgDefine
import com.cylan.jiafeigou.support.photoselect.CircleImageView
import com.cylan.jiafeigou.utils.TimeUtils
import com.mikepenz.fastadapter.items.AbstractItem

/**
 * Created by yanzhendong on 2017/10/9.
 */
class FaceItem() : AbstractItem<FaceItem, FaceItem.FaceItemViewHolder>(), Parcelable, Comparable<FaceItem> {

    override fun compareTo(other: FaceItem): Int {
        return (other.version - this.version).toInt()
    }


    var uuid: String? = null

    var version: Long = 0

    //对应 msgType =5返回的列表 item
    var visitor: DpMsgDefine.Visitor? = null//熟人

    var strangerVisitor: DpMsgDefine.StrangerVisitor? = null//定义为陌生人

    private var faceType: Int = 0 //熟人或者陌生人

    var markHint: Boolean = false//红点标记

    override fun getViewHolder(v: View): FaceItemViewHolder {
        return FaceItemViewHolder(v)
    }

    @SuppressLint("ResourceType")
    override fun getType(): Int {
        return R.layout.item_face_selection
    }

    fun withVersion(version: Long) {
        this.version = version
    }

    fun withFaceType(faceType: Int): FaceItem {
        this.faceType = faceType
        return this
    }

    fun getFaceType(): Int {
        return faceType
    }

    fun withUuid(uuid: String): FaceItem {
        this.uuid = uuid
        return this
    }

    fun withVisitor(visitor: DpMsgDefine.Visitor): FaceItem {
        version = visitor.lastTime
        this.visitor = visitor
        return this
    }

    fun withStrangerVisitor(visitor: DpMsgDefine.StrangerVisitor): FaceItem {
        this.strangerVisitor = visitor
        version = visitor.lastTime
        return this
    }

    override fun withSetSelected(selected: Boolean): FaceItem {
        if (selected) {

        }
        return super.withSetSelected(selected)

    }

    override fun getLayoutRes(): Int {
        return R.layout.item_face_selection
    }

    override fun bindView(holder: FaceItemViewHolder, payloads: MutableList<Any>?) {
        super.bindView(holder, payloads)
        //todo 全部是默认图,陌生人是组合图片,需要特殊处理
        when (faceType) {
            FACE_TYPE_ALL -> {
                //todo UI图导入
                holder.text.text = holder.itemView.context.getText(R.string.MESSAGES_FILTER_ALL)
                holder.icon.setImageResource(R.drawable.news_icon_all_selector)
                holder.icon.showBorder(isSelected)
                holder.strangerIcon.visibility = View.GONE
                holder.icon.showHint(markHint)
            }
            FACE_TYPE_STRANGER -> {
                //todo 多图片合成
                //http://img.taopic.com/uploads/allimg/120727/201995-120HG1030762.jpg
//                Glide.with(holder.itemView.context)
//                        .load(MiscUtils.getCamWarnUrl(uuid, message, 1))
//                        .placeholder(R.drawable.news_icon_stranger)
//                        .error(R.drawable.news_icon_stranger)
////                        .bitmapTransform(AvatarTransform(holder.itemView.context, message!!.alarmMsg!!.face_id))
//                        .into(holder.icon)
                holder.text.text = holder.itemView.context.getText(R.string.MESSAGES_FILTER_STRANGER)
//                holder.icon.isDisableCircularTransformation = true
                holder.strangerIcon.visibility = View.GONE
                holder.icon.showHint(true)
                holder.icon.setImageResource(R.drawable.news_icon_stranger)
                holder.icon.showBorder(isSelected)
                holder.icon.showHint(markHint)
            }
        //todo 可能会有猫狗车辆行人,这些都是预制的图片,需要判断
            FACE_TYPE_ACQUAINTANCE -> {
                holder.text.text = TimeUtils.getVisitorTime(visitor?.lastTime!! * 1000L)
                holder.icon.showBorder(isSelected)
                holder.strangerIcon.visibility = View.GONE
                holder.icon.showHint(markHint)
                Glide.with(holder.itemView.context)
                        .load(visitor?.detailList?.get(0)?.imgUrl)
                        .placeholder(R.drawable.icon_mine_head_normal)
                        .error(R.drawable.icon_mine_head_normal)
                        .into(holder.icon)
            }
            FACE_TYPE_STRANGER_SUB -> {
                holder.text.text = TimeUtils.getVisitorTime(strangerVisitor?.lastTime!! * 1000L)
                holder.icon.showBorder(isSelected)
                holder.icon.showHint(markHint)
                Glide.with(holder.itemView.context)
                        .load(strangerVisitor?.image_url)
                        .placeholder(R.drawable.icon_mine_head_normal)
                        .error(R.drawable.icon_mine_head_normal)
                        .into(holder.icon)
            }
            else -> {
                //todo 陌生人详情页的处理逻辑
            }
        }

    }

    class FaceItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: CircleImageView = view.findViewById(R.id.img_item_face_selection) as CircleImageView
        val text: TextView = view.findViewById(R.id.text_item_face_selection) as TextView
        val strangerIcon: ImageView = view.findViewById(R.id.stranger_icon) as ImageView

    }

    constructor(source: Parcel) : this(
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {}
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        if (!super.equals(other)) return false

        other as FaceItem

        if (uuid != other.uuid) return false
        if (version != other.version) return false
        if (visitor != other.visitor) return false
        if (strangerVisitor != other.strangerVisitor) return false
        if (faceType != other.faceType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (uuid?.hashCode() ?: 0)
        result = 31 * result + version.hashCode()
        result = 31 * result + (visitor?.hashCode() ?: 0)
        result = 31 * result + (strangerVisitor?.hashCode() ?: 0)
        result = 31 * result + faceType
        return result
    }

    companion object {
        const val FACE_TYPE_ALL: Int = -1

        const val FACE_TYPE_STRANGER: Int = 0//陌生人

        const val FACE_TYPE_ACQUAINTANCE: Int = 1//熟人

        const val FACE_TYPE_STRANGER_SUB: Int = 2

        @JvmField
        val CREATOR: Parcelable.Creator<FaceItem> = object : Parcelable.Creator<FaceItem> {
            override fun createFromParcel(source: Parcel): FaceItem = FaceItem(source)
            override fun newArray(size: Int): Array<FaceItem?> = arrayOfNulls(size)
        }
    }
}