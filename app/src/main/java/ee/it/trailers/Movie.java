package ee.it.trailers;

import android.text.TextUtils;

import java.util.Collections;
import java.util.List;

class Movie {
    String id;
    String title;
    String releaseDate;
    List<String> genres = Collections.emptyList();
    List<String> directors = Collections.emptyList();
    List<String> actors = Collections.emptyList();
    String posterUrl;

    public String genreString() {
        if (!genres.isEmpty()) {
            return TextUtils.join(", ", genres);
        } else {
            return "";
        }
    }

    public String directorsString() {
        if (!directors.isEmpty()) {
            return TextUtils.join(", ", directors);
        } else {
            return "";
        }
    }

    public String actorsString() {
        if (!actors.isEmpty()) {
            return TextUtils.join(", ", actors);
        } else {
            return "";
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("id: ")
                .append(id)
                .append(" title: ")
                .append(title)
                .append(" date: ")
                .append(releaseDate);

        if (!genres.isEmpty()) {
            sb.append(" genres: ");
            for (String s : genres) {
                sb.append(s);
                sb.append(' ');
            }
        }

        if (!directors.isEmpty()) {
            sb.append(" directors: ");
            for (String s : directors) {
                sb.append(s);
                sb.append(' ');
            }
        }

        if (!actors.isEmpty()) {
            sb.append(" actors: ");
            for (String s : actors) {
                sb.append(s);
                sb.append(' ');
            }
        }

        sb.append(" poster: ")
                .append(posterUrl);

        return sb.toString();
    }
}
