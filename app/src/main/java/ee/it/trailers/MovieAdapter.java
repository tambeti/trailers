package ee.it.trailers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.Collections;
import java.util.List;

public class MovieAdapter extends BaseAdapter {
    private List<Movie> mMovies = Collections.emptyList();

    public void swapData(List<Movie> movies) {
        mMovies = movies;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mMovies.size();
    }

    @Override
    public Object getItem(int position) {
        return mMovies.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View view;
        final ViewHolder holder;
        if (convertView != null) {
            view = convertView;
            holder = (ViewHolder) convertView.getTag();
        } else {
            LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService
                    (Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.movie_item, parent, false);

            holder = new ViewHolder();
            holder.poster = (ImageView) view.findViewById(R.id.poster);
            holder.title = (TextView) view.findViewById(R.id.title);
            holder.genre = (TextView) view.findViewById(R.id.genre);
            holder.director = (TextView) view.findViewById(R.id.director);
            holder.actors = (TextView) view.findViewById(R.id.actors);
            holder.releaseDate = (TextView) view.findViewById(R.id.release_date);

            view.setTag(holder);
        }

        final Movie movie = mMovies.get(position);
        holder.title.setText(movie.title);
        holder.genre.setText(movie.genreString());
        holder.director.setText(movie.directorsString());
        holder.actors.setText(movie.actorsString());
        holder.releaseDate.setText(movie.releaseDate);

        Picasso.with(parent.getContext())
                .load(movie.posterUrl)
                .resize(200, 200)
                .centerCrop()
                .into(holder.poster);

        return view;
    }

    static class ViewHolder {
        ImageView poster;
        TextView title;
        TextView genre;
        TextView director;
        TextView actors;
        TextView releaseDate;
    }
}
