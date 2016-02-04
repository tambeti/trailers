package ee.it.trailers.tmdb;

import java.util.List;

public class Genres {
    public static class Genre {
        public int id;
        public String name;
    }

    public List<Genre> genres;

    public String name(int id) {
        for (Genre g : genres) {
            if (g.id == id) {
                return g.name;
            }
        }

        return null;
    }
}
