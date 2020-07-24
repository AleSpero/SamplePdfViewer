package com.alespero.samplepdfviewer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class PDFAdapter(pdfParcelDescriptor: ParcelFileDescriptor, context : Context) : RecyclerView.Adapter<PDFAdapter.PDFPageViewHolder>() {

    private var bitmapPool: PdfBitmapPool? = null
    private val pdfRenderer: PdfRenderer = PdfRenderer(pdfParcelDescriptor)

    init {
        bitmapPool = PdfBitmapPool(pdfRenderer, Bitmap.Config.ARGB_8888,
            context.resources.displayMetrics.densityDpi)
    }

    override fun getItemCount(): Int = pdfRenderer.pageCount

    override fun onBindViewHolder(holder: PDFPageViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PDFPageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.pdf_page_item, parent, false)
        return PDFPageViewHolder(view)
    }

    fun clear() {
        pdfRenderer.close()
        bitmapPool?.bitmaps?.clear()
    }


    inner class PDFPageViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

        fun bind(pagePosition: Int) {
            val imgPage = view.findViewById<ImageView>(R.id.pageImgView)
            imgPage.setImageBitmap(bitmapPool!!.getPage(pagePosition))
            bitmapPool!!.loadMore(pagePosition)
        }
    }
}




