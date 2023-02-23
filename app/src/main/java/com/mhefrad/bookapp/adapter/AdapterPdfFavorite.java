package com.mhefrad.bookapp.adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.barteksc.pdfviewer.PDFView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mhefrad.bookapp.MyApplication;
import com.mhefrad.bookapp.activities.PdfDetailActivity;
import com.mhefrad.bookapp.databinding.RowPdfFavoriteBinding;
import com.mhefrad.bookapp.models.ModelPdf;

import java.util.ArrayList;

public class AdapterPdfFavorite extends RecyclerView.Adapter<AdapterPdfFavorite.HolderPdfFavorite> {

    private Context context;
    private ArrayList<ModelPdf> pdfArrayList;

    //view binding for row_pdf_favorite.xml
    private RowPdfFavoriteBinding binding;

    private static final String TAG = "FAV_BOOK_TAG";

    //constructor
    public AdapterPdfFavorite(Context context, ArrayList<ModelPdf> pdfArrayList) {
        this.context = context;
        this.pdfArrayList = pdfArrayList;
    }

    @NonNull
    @Override
    public HolderPdfFavorite onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //bind / inflate row_pdf_favorite.xml
        binding = RowPdfFavoriteBinding.inflate(LayoutInflater.from(context),parent,false);

        return new HolderPdfFavorite(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull HolderPdfFavorite holder, int position) {

        /*-- Get data, set data , handle click --*/

        ModelPdf model = pdfArrayList.get(position);

        loadBookDetails(holder,model);

        //handle click, open pdf details page
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, PdfDetailActivity.class);
                intent.putExtra("bookId", model.getId()); //pass book id not category id
                context.startActivity(intent);
            }
        });

        //handle click, remove from favorite
        holder.removeFavBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyApplication.removeFromFavorite(context,model.getId());
            }
        });

    }

    private void loadBookDetails(HolderPdfFavorite holder, ModelPdf model) {

        String bookId = model.getId();
        Log.d(TAG, "loadBookDetails: book Details of Book Id: "+bookId);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.child(bookId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        //get book info
                        String bookTitle = ""+snapshot.child("title").getValue();
                        String description = ""+snapshot.child("description").getValue();
                        String categoryId = ""+snapshot.child("categoryId").getValue();
                        String bookUrl = ""+snapshot.child("url").getValue();
                        String timestamp = ""+snapshot.child("timestamp").getValue();
                        String uid = ""+snapshot.child("uid").getValue();
                        String viewsCount = ""+snapshot.child("viewsCount").getValue();
                        String downloadsCount = ""+snapshot.child("downloadsCount").getValue();

                        //set to model
                        model.setFavorite(true);
                        model.setTitle(bookTitle);
                        model.setDescription(description);
                        model.setTimestamp(Long.parseLong(timestamp));
                        model.setCategoryId(categoryId);
                        model.setUid(uid);
                        model.setUrl(bookUrl);

                        //format date
                        String date = MyApplication.formatTimestamp(Long.parseLong(timestamp));

                        MyApplication.loadCategory(
                                categoryId,
                                binding.categoryTv
                        );
                        MyApplication.loadPdfFromUrlSinglePage(
                                ""+bookUrl,
                                ""+bookTitle,
                                holder.pdfView,
                                holder.progressBar,
                                null
                        );
                        MyApplication.loadPdfSize(
                                ""+bookUrl,
                                ""+bookTitle,
                                holder.sizeTv
                        );

                        //set data to views
                        holder.titleTv.setText(bookTitle);
                        holder.descriptionTv.setText(description);
                        holder.dateTv.setText(date);


                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    @Override
    public int getItemCount() {
        return pdfArrayList.size(); //return list size || number of records
    }

    //ViewHolder class
    class HolderPdfFavorite extends RecyclerView.ViewHolder{

        PDFView pdfView;
        ProgressBar progressBar;
        TextView titleTv, descriptionTv, sizeTv, dateTv, categoryTv;
        ImageButton removeFavBtn;


        public HolderPdfFavorite(@NonNull View itemView) {
            super(itemView);

            //init UI views of row_pdf_favorite.xml
            pdfView = binding.pdfView;
            progressBar = binding.progressBar;
            titleTv = binding.titleTv;
            descriptionTv = binding.descriptionTv;
            sizeTv = binding.sizeTv;
            dateTv = binding.dateTv;
            categoryTv = binding.categoryTv;
            removeFavBtn = binding.removeFavBtn;
        }
    }

}
