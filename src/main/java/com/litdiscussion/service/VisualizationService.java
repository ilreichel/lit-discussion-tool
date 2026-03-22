package com.litdiscussion.service;

import com.litdiscussion.model.*;
import com.litdiscussion.repository.*;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class VisualizationService {

    private final DiscussionService discussionService;
    private final BookRepository bookRepository;
    private final DiscussionPostRepository postRepository;

    public VisualizationService(DiscussionService discussionService, BookRepository bookRepository,
                                DiscussionPostRepository postRepository) {
        this.discussionService = discussionService;
        this.bookRepository = bookRepository;
        this.postRepository = postRepository;
    }

    private double computeAdaptiveThreshold(int themeCount) {
        if (themeCount <= 5) return 0.15;
        if (themeCount <= 10) return 0.25;
        if (themeCount <= 20) return 0.3;
        if (themeCount <= 40) return 0.35;
        return 0.4;
    }

    public VisualizationData generateVisualization(Long bookId) {
        Map<String, Long> frequencies;
        if (bookId != null) {
            frequencies = discussionService.getThemeFrequencies(bookId);
        } else {
            frequencies = discussionService.getAllThemeFrequencies();
        }

        double threshold = computeAdaptiveThreshold(frequencies.size());
        Map<String, Set<Long>> themePostMap = buildThemePostMap(bookId);
        Map<String, Map<String, Integer>> cooccurrence = computeCooccurrence(bookId);
        List<VisualizationNode> nodes = buildNodes(frequencies);
        List<VisualizationEdge> edges = buildEdges(frequencies.keySet(), themePostMap, threshold);
        assignClusters(nodes, cooccurrence);

        return new VisualizationData(nodes, edges);
    }

    private Map<String, Set<Long>> buildThemePostMap(Long bookId) {
        Map<String, Set<Long>> map = new HashMap<>();
        List<DiscussionPost> posts;
        if (bookId != null) {
            posts = discussionService.getPostsByBook(bookId);
        } else {
            posts = postRepository.findAll();
        }

        for (DiscussionPost post : posts) {
            for (Quote quote : post.getQuotes()) {
                for (Theme theme : quote.getThemes()) {
                    map.computeIfAbsent(theme.getName(), k -> new HashSet<>()).add(post.getId());
                }
            }
        }
        return map;
    }

    private Map<String, Map<String, Integer>> computeCooccurrence(Long bookId) {
        Map<String, Map<String, Integer>> cooccurrence = new HashMap<>();
        List<DiscussionPost> posts;
        if (bookId != null) {
            posts = discussionService.getPostsByBook(bookId);
        } else {
            posts = discussionService.getPostsByClassroom(null);
        }

        for (DiscussionPost post : posts) {
            Set<String> postThemes = new HashSet<>();
            for (Quote quote : post.getQuotes()) {
                for (Theme theme : quote.getThemes()) {
                    postThemes.add(theme.getName());
                }
            }
            List<String> themeList = new ArrayList<>(postThemes);
            for (int i = 0; i < themeList.size(); i++) {
                for (int j = i + 1; j < themeList.size(); j++) {
                    String t1 = themeList.get(i);
                    String t2 = themeList.get(j);
                    cooccurrence.computeIfAbsent(t1, k -> new HashMap<>()).merge(t2, 1, Integer::sum);
                    cooccurrence.computeIfAbsent(t2, k -> new HashMap<>()).merge(t1, 1, Integer::sum);
                }
            }
        }
        return cooccurrence;
    }

    private List<VisualizationNode> buildNodes(Map<String, Long> frequencies) {
        List<VisualizationNode> nodes = new ArrayList<>();
        for (Map.Entry<String, Long> entry : frequencies.entrySet()) {
            VisualizationNode node = new VisualizationNode();
            node.setId(entry.getKey());
            node.setLabel(entry.getKey());
            node.setFrequency(entry.getValue());
            node.setSize(computeNodeSize(entry.getValue()));
            nodes.add(node);
        }
        return nodes;
    }

    private List<VisualizationEdge> buildEdges(Set<String> themeNames, Map<String, Set<Long>> themePostMap, double threshold) {
        List<VisualizationEdge> edges = new ArrayList<>();
        List<String> themes = new ArrayList<>(themeNames);

        for (int i = 0; i < themes.size(); i++) {
            for (int j = i + 1; j < themes.size(); j++) {
                String t1 = themes.get(i);
                String t2 = themes.get(j);
                Set<Long> posts1 = themePostMap.getOrDefault(t1, Collections.emptySet());
                Set<Long> posts2 = themePostMap.getOrDefault(t2, Collections.emptySet());

                if (posts1.isEmpty() || posts2.isEmpty()) continue;

                double similarity = computePostCosineSimilarity(posts1, posts2);
                if (similarity >= threshold) {
                    long sharedCount = posts1.stream().filter(posts2::contains).count();
                    VisualizationEdge edge = new VisualizationEdge();
                    edge.setSource(t1);
                    edge.setTarget(t2);
                    edge.setWeight((int) sharedCount);
                    edge.setSimilarity(similarity);
                    edges.add(edge);
                }
            }
        }
        return edges;
    }

    private double computePostCosineSimilarity(Set<Long> posts1, Set<Long> posts2) {
        Set<Long> allPosts = new HashSet<>(posts1);
        allPosts.addAll(posts2);

        double dotProduct = 0;
        for (Long postId : allPosts) {
            double v1 = posts1.contains(postId) ? 1.0 : 0.0;
            double v2 = posts2.contains(postId) ? 1.0 : 0.0;
            dotProduct += v1 * v2;
        }

        double norm1 = Math.sqrt(posts1.size());
        double norm2 = Math.sqrt(posts2.size());

        if (norm1 == 0 || norm2 == 0) return 0;
        return dotProduct / (norm1 * norm2);
    }

    private void assignClusters(List<VisualizationNode> nodes, Map<String, Map<String, Integer>> cooccurrence) {
        String[] colors = {"#335c67", "#fff3b0", "#e09f3e", "#9e2a2b"};
        Map<String, Integer> clusterMap = new HashMap<>();
        int clusterId = 0;

        for (VisualizationNode node : nodes) {
            if (clusterMap.containsKey(node.getId())) continue;
            clusterMap.put(node.getId(), clusterId);

            Map<String, Integer> neighbors = cooccurrence.getOrDefault(node.getId(), Collections.emptyMap());
            for (String neighbor : neighbors.keySet()) {
                if (!clusterMap.containsKey(neighbor)) {
                    clusterMap.put(neighbor, clusterId);
                }
            }
            clusterId++;
        }

        for (VisualizationNode node : nodes) {
            int cid = clusterMap.getOrDefault(node.getId(), 0);
            node.setCluster(cid);
            node.setColor(colors[cid % colors.length]);
        }
    }

    private double computeNodeSize(long frequency) {
        return Math.max(20, Math.min(80, 20 + frequency * 10));
    }

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public static class VisualizationData {
        private List<VisualizationNode> nodes;
        private List<VisualizationEdge> edges;

        public VisualizationData(List<VisualizationNode> nodes, List<VisualizationEdge> edges) {
            this.nodes = nodes;
            this.edges = edges;
        }

        public List<VisualizationNode> getNodes() { return nodes; }
        public List<VisualizationEdge> getEdges() { return edges; }
    }

    public static class VisualizationNode {
        private String id;
        private String label;
        private long frequency;
        private double size;
        private int cluster;
        private String color;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public long getFrequency() { return frequency; }
        public void setFrequency(long frequency) { this.frequency = frequency; }
        public double getSize() { return size; }
        public void setSize(double size) { this.size = size; }
        public int getCluster() { return cluster; }
        public void setCluster(int cluster) { this.cluster = cluster; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
    }

    public static class VisualizationEdge {
        private String source;
        private String target;
        private int weight;
        private double similarity;

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }
        public int getWeight() { return weight; }
        public void setWeight(int weight) { this.weight = weight; }
        public double getSimilarity() { return similarity; }
        public void setSimilarity(double similarity) { this.similarity = similarity; }
    }
}
