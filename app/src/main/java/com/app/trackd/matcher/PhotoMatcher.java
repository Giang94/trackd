package com.app.trackd.matcher;

public class PhotoMatcher {

//    private final ImagePHash pHash = new ImagePHash();
//    private final int HAMMING_THRESHOLD = 25; // tweak for sensitivity
//
//    public List<Album> findTopMatches(Bitmap bitmap, List<Album> albums, int maxResults) {
//        List<MatchResult> results = new ArrayList<>();
//
//        Bitmap normalized = ImageUtils.resizeSquare(bitmap, 224); // make 224x224 square
//        normalized = ImageUtils.toGrayscale(normalized);          // convert to grayscale
//        String inputHash = pHash.getHash(normalized);
//
//        for (Album album : albums) {
//            int distance = ImagePHash.hammingDistance(inputHash, album.getPhash());
//            Log.d("DISTANCE-X", album.getTitle() + ": " + distance);
//            // Only consider matches within threshold
////            if (distance <= HAMMING_THRESHOLD) {
////                results.add(new MatchResult(album, distance));
////            }
//            album.setYear(distance);
//            results.add(new MatchResult(album, distance));
//        }
//
//        // Sort by distance (smaller = better)
//        results.sort(Comparator.comparingInt(r -> r.distance));
//
//        // Return top matches
//        List<Album> topMatches = new ArrayList<>();
//        for (int i = 0; i < Math.min(maxResults, results.size()); i++) {
//            topMatches.add(results.get(i).album);
//        }
//
//        return topMatches;
//    }
//
//    private static class MatchResult {
//        Album album;
//        int distance;
//
//        MatchResult(Album album, int distance) {
//            this.album = album;
//            this.distance = distance;
//        }
//    }
}
