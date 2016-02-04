package ee.it.trailers.tmdb;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.List;

public class MovieDetails {
    public static class Trailer {
        public String name;
        public String source;
        public String type;
        public String size;
    }

    public static class Trailers {
        public List<Trailer> quicktime;
        public List<Trailer> youtube;
    }

    public static class Cast {
        public String name;
    }

    public static class Crew {
        public String name;
        public String job;
    }

    public static class Credits {
        public List<Cast> cast;
        public List<Crew> crew;
    }

    public long id;
    public String imdbId;
    public String title;
    public String posterPath;
    public String originalTitle;
    public String overview;
    public String tagline;
    public String releaseDate;
    public long runtime;

    public List<Genres.Genre> genres;
    public Trailers trailers;
    public Credits credits;

    public String genres() {
        final StringBuilder sb = new StringBuilder();
        boolean addComma = false;
        for (Genres.Genre genre : genres) {
            if (addComma) {
                sb.append(" / ");
            } else {
                addComma = true;
            }
            sb.append(genre.name);
        }

        return sb.toString();
    }

    public String directorNames() {
        if (credits == null || credits.crew == null) {
            return "";
        }

        final StringBuilder sb = new StringBuilder();
        boolean addComma = false;
        for (Crew c : credits.crew) {
            if (TextUtils.equals("Director", c.job)) {
                if (addComma) {
                    sb.append(", ");
                } else {
                    addComma = true;
                }

                sb.append(c.name);
            }
        }

        return sb.toString();
    }

    public String writerNames() {
        if (credits == null || credits.crew == null) {
            return "";
        }

        final StringBuilder sb = new StringBuilder();
        boolean addComma = false;
        for (Crew c : credits.crew) {
            if (TextUtils.equals("Writer", c.job)) {
                if (addComma) {
                    sb.append(", ");
                } else {
                    addComma = true;
                }

                sb.append(c.name);
            }
        }

        return sb.toString();
    }

    public String castNames(int max) {
        if (credits == null || credits.cast == null) {
            return "";
        }

        final StringBuilder sb = new StringBuilder();
        int m = Math.min(credits.cast.size(), max);
        boolean addComma = false;
        for (int i = 0; i < m; i++) {
            if (addComma) {
                sb.append(", ");
            } else {
                addComma = true;
            }
            final Cast c = credits.cast.get(i);
            sb.append(c.name);
        }

        return sb.toString();
    }

    @Nullable
    public String youtubeTrailer() {
        if (trailers != null && trailers.youtube != null && !trailers.youtube.isEmpty()) {
            return trailers.youtube.get(0).source;
        } else {
            return null;
        }
    }
}
