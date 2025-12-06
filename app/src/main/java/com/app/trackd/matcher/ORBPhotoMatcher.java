//package com.app.trackd.matcher;
//
//import android.graphics.Bitmap;
//import android.util.Log;
//
//import com.app.trackd.model.Album;
//
//import org.opencv.android.Utils;
//import org.opencv.core.Mat;
//import org.opencv.features2d.BFMatcher;
//import org.opencv.features2d.DescriptorMatcher;
//import org.opencv.features2d.ORB;
//import org.opencv.core.MatOfDMatch;
//import org.opencv.core.MatOfKeyPoint;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class ORBPhotoMatcher {
//
//    private ORB orb;
//    private DescriptorMatcher matcher;
//
//    public ORB getOrb() {
//        return orb;
//    }
//
//    public ORBPhotoMatcher() {
//        orb = ORB.create();
//        matcher = BFMatcher.create(BFMatcher.BRUTEFORCE_HAMMING, true);
//    }
//
//    public List<Album> findTopMatches(Bitmap captured, List<Album> albums, int maxResults) {
//        List<AlbumDescriptor> albumDescriptors = albums.stream().map(album -> {
//            Mat descriptors = new Mat(album.getOrbRows(), album.getOrbCols(), album.getOrbType());
//            descriptors.put(0, 0, album.getOrbDescriptor());
//
//            MatOfKeyPoint keyPoints = new MatOfKeyPoint();
//            return new AlbumDescriptor(album, keyPoints, descriptors);
//        }).toList();
//
//        Mat capturedMat = new Mat();
//        Utils.bitmapToMat(captured, capturedMat);
//        MatOfKeyPoint keyPoints = new MatOfKeyPoint();
//        Mat descriptors = new Mat();
//        orb.detectAndCompute(capturedMat, new Mat(), keyPoints, descriptors);
//
//        List<MatchResult> results = new ArrayList<>();
//
//        Log.d("MATCHING", "--- Starting matching process ---");
//        for (AlbumDescriptor ad : albumDescriptors) {
//            if (ad.descriptors.empty() || descriptors.empty()) continue;
//
//            MatOfDMatch matches = new MatOfDMatch();
//            matcher.match(descriptors, ad.descriptors, matches);
//
//            double sumDistance = 0;
//            int count = matches.toArray().length;
//            for (int i = 0; i < count; i++) {
//                sumDistance += matches.toArray()[i].distance;
//            }
//            double avgDistance = count > 0 ? sumDistance / count : Double.MAX_VALUE;
//
//            Log.d("MATCHING", "Album: " + ad.album.getTitle() + " - Avg Distance: " + avgDistance);
//
//            results.add(new MatchResult(ad.album, avgDistance));
//        }
//        Log.d("MATCHING", "--- Finish matching process ---");
//
//        // Sort by ascending distance (smaller = better)
//        results.sort((a, b) -> Double.compare(a.distance, b.distance));
//
//        List<Album> topMatches = new ArrayList<>();
//        for (int i = 0; i < Math.min(maxResults, results.size()); i++) {
//            topMatches.add(results.get(i).album);
//        }
//        return topMatches;
//    }
//
//    public static class AlbumDescriptor {
//        public Album album;
//        public MatOfKeyPoint keyPoints;
//        public Mat descriptors;
//
//        public AlbumDescriptor(Album album, MatOfKeyPoint keyPoints, Mat descriptors) {
//            this.album = album;
//            this.keyPoints = keyPoints;
//            this.descriptors = descriptors;
//        }
//    }
//
//    private static class MatchResult {
//        Album album;
//        double distance;
//
//        MatchResult(Album album, double distance) {
//            this.album = album;
//            this.distance = distance;
//        }
//    }
//}
