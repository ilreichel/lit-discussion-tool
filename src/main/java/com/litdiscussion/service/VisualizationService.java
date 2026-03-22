package com.litdiscussion.service;

import com.litdiscussion.model.*;
import com.litdiscussion.repository.*;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class VisualizationService {

    private final DiscussionService discussionService;
    private final BookRepository bookRepository;

    public VisualizationService(DiscussionService discussionService, BookRepository bookRepository) {
        this.discussionService = discussionService;
        this.bookRepository = bookRepository;
    }

    public VisualizationData generateVisualization(Long bookId) {
        Map<String, Long> frequencies;
        if (bookId != null) {
            frequencies = discussionService.getThemeFrequencies(bookId);
        } else {
            frequencies = discussionService.getAllThemeFrequencies();
        }

        Map<String, Map<String, Integer>> cooccurrence = computeCooccurrence(bookId);
        List<VisualizationNode> nodes = buildNodes(frequencies);
        List<VisualizationEdge> edges = buildEdges(cooccurrence);
        assignClusters(nodes, cooccurrence);

        return new VisualizationData(nodes, edges);
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

    private List<VisualizationEdge> buildEdges(Map<String, Map<String, Integer>> cooccurrence) {
        List<VisualizationEdge> edges = new ArrayList<>();
        Set<String> added = new HashSet<>();
        for (Map.Entry<String, Map<String, Integer>> entry : cooccurrence.entrySet()) {
            String source = entry.getKey();
            for (Map.Entry<String, Integer> target : entry.getValue().entrySet()) {
                String edgeKey = source.compareTo(target.getKey()) < 0
                        ? source + "|||" + target.getKey()
                        : target.getKey() + "|||" + source;
                if (!added.contains(edgeKey)) {
                    VisualizationEdge edge = new VisualizationEdge();
                    edge.setSource(source);
                    edge.setTarget(target.getKey());
                    edge.setWeight(target.getValue());
                    edge.setSimilarity(computeCosineSimilarity(source, target.getKey(), cooccurrence));
                    edges.add(edge);
                    added.add(edgeKey);
                }
            }
        }
        return edges;
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

    private double computeCosineSimilarity(String theme1, String theme2,
                                           Map<String, Map<String, Integer>> cooccurrence) {
        Map<String, Integer> vec1 = cooccurrence.getOrDefault(theme1, Collections.emptyMap());
        Map<String, Integer> vec2 = cooccurrence.getOrDefault(theme2, Collections.emptyMap());

        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(vec1.keySet());
        allKeys.addAll(vec2.keySet());

        double dotProduct = 0;
        double norm1 = 0;
        double norm2 = 0;

        for (String key : allKeys) {
            double v1 = vec1.getOrDefault(key, 0);
            double v2 = vec2.getOrDefault(key, 0);
            dotProduct += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }

        if (norm1 == 0 || norm2 == 0) return 0;
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
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
