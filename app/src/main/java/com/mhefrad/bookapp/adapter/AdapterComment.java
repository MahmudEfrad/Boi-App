package com.mhefrad.bookapp.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mhefrad.bookapp.MyApplication;
import com.mhefrad.bookapp.R;
import com.mhefrad.bookapp.databinding.RowCommentsBinding;
import com.mhefrad.bookapp.models.ModelComment;

import java.util.ArrayList;

public class AdapterComment extends RecyclerView.Adapter<AdapterComment.HolderComment> {

    //context
    private Context context;
    //arraylist to hold comments
    private ArrayList<ModelComment> commentArrayList;

    //firebase auth
    private FirebaseAuth firebaseAuth;

    //view binding
    private RowCommentsBinding binding;

    //constructor
    public AdapterComment(Context context, ArrayList<ModelComment> commentArrayList) {
        this.context = context;
        this.commentArrayList = commentArrayList;

        firebaseAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public HolderComment onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate/ bind the view row_comment.xlm
        binding = RowCommentsBinding.inflate(LayoutInflater.from(context),parent,false);
        return new HolderComment(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull HolderComment holder, int position) {
        /*--Get data from specific position of list, set data, handle click etc..--*/

        //get data
        ModelComment modelComment = commentArrayList.get(position);
        String id = modelComment.getId();
        String bookId = modelComment.getBookId();
        String comment = modelComment.getComment();
        String uid = modelComment.getUid();
        String timestamp = modelComment.getTimestamp();

        //format dat
        String date = MyApplication.formatTimestamp(Long.parseLong(timestamp));

        //set data
        holder.dateTv.setText(date);
        holder.commentTv.setText(comment);

        //we don't have user's name, profile picture, so we will load it using uid we stored in each comment
        loadUserDetails(modelComment,holder);

        //handle click, show option to delete comment
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Requirements to delete a comment
                * 1) user must be  logged in
                * 2) uid in comment (to be deleted) must be same as uid of logged in user */

                if (firebaseAuth.getCurrentUser() !=null && uid.equals(firebaseAuth.getUid())){
                    deleteComment(modelComment,holder);
                }
            }
        });
    }

    private void deleteComment(ModelComment modelComment, HolderComment holder) {
        //show confirm dialog before deleting comment
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Delete Comment")
                .setMessage("Are you sure you want to delete this comment")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //delete from dialog clicked, begin delete
                        
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
                        ref.child(modelComment.getBookId())
                                .child("Comments")
                                .child(modelComment.getId())
                                .removeValue()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Toast.makeText(context, "Deleted...", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(context, "Failed to delete due to "+e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void loadUserDetails(ModelComment modelComment, HolderComment holder) {
        String uid = modelComment.getUid();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users");
        ref.child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = ""+snapshot.child("name").getValue();
                        String profileImage = ""+snapshot.child("profileImage").getValue();

                        //set data
                        holder.nameTv.setText(name);
                        try {
                            Glide.with(context)
                                    .load(profileImage)
                                    .placeholder(R.drawable.ic_person_gray)
                                    .into(holder.profileIv);
                        }catch (Exception e){
                            holder.profileIv.setImageResource(R.drawable.ic_person_gray);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    @Override
    public int getItemCount() {
        return commentArrayList.size(); //return records size, number of records
    }

    //view holder class for row_comment.xml
    class HolderComment extends RecyclerView.ViewHolder {

        //UI views of row_comment.xml
        ShapeableImageView profileIv;
        TextView nameTv, dateTv, commentTv;

        public HolderComment(@NonNull View itemView) {
            super(itemView);

            //init UI views
            profileIv = binding.profileIv;
            nameTv = binding.nameTv;
            dateTv = binding.dateTv;
            commentTv = binding.commentTv;

        }
    }
}