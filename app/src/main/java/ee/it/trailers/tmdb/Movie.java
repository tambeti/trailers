package ee.it.trailers.tmdb;

import java.util.List;

public class Movie {
    public static class Trailer {
        public String name;
        public String source;
        public String type;
        public String size;
    }

    public long id;
    public String imdbId;
    public String title;
    public String posterPath;
    public String originalTitle;
    public String overview;
    public String tagline;
    public String releaseDate;
    public List<Integer> genreIds;

    public String genres(Genres genres) {
        final StringBuilder sb = new StringBuilder();
        boolean addComma = false;
        for (int genreId : genreIds) {
            if (addComma) {
                sb.append(" / ");
            } else {
                addComma = true;
            }
            sb.append(genres.name(genreId));
        }

        return sb.toString();
    }
}
