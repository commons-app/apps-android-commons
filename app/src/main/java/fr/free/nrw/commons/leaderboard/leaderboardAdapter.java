package fr.free.nrw.commons.leaderboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;

public class leaderboardAdapter extends RecyclerView.Adapter<leaderboardAdapter.leaderboardViewHolder> {

    private String[] rank_array;
 //   private List<Media> avatar_array;
    private String[] userName_array;
    private String[] score_array;

    public leaderboardAdapter(String[] rank_array, String[] userName_array, String[] score_array){
        this.rank_array = rank_array;
    //    this.avatar_array = avatar_array;
        this.userName_array = userName_array;
        this.score_array = score_array;
    }

    @NonNull
    @Override
    public leaderboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.layout_leaderboard_item,parent,false);
        return  new leaderboardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull leaderboardViewHolder holder, int position) {
        holder.rank.setText(rank_array[position]);
       // holder.userImageView;
        holder.user_name.setText(userName_array[position]);
        holder.score.setText(score_array[position]);

    }

    @Override
    public int getItemCount() {
        return rank_array.length;
    }

    public class leaderboardViewHolder extends RecyclerView.ViewHolder{
        TextView rank;
      //  ImageView userImageView;
        TextView user_name;
        TextView score;
        public leaderboardViewHolder(View itemView){
            super(itemView);
            rank = itemView.findViewById(R.id.rank);
         //   userImageView = itemView.findViewById(R.id.userImageView);
            user_name = itemView.findViewById(R.id.user_name);
            score = itemView.findViewById(R.id.score);
        }
    }
}
